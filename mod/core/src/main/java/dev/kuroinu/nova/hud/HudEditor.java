package dev.kuroinu.nova.hud;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.render.AlphaRenderer;
import dev.kuroinu.nova.render.Colors;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.render.font.NovaFont;
import dev.kuroinu.nova.ui.Animation;
import dev.kuroinu.nova.ui.NovaScreen;
import dev.kuroinu.nova.ui.Theme;
import dev.kuroinu.nova.ui.UiKey;
import dev.kuroinu.nova.ui.UiScale;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * HUD layout editor (design handoff "HUD Editor"): 40px grid, dashed accent outlines with name
 * labels, and a top toolbar with Reset / Done. Drag to move, scroll to resize, right-click to hide.
 * ESC or Done returns to the menu.
 */
public final class HudEditor extends NovaScreen {
    private static final float SNAP = 4f;
    private static final float GRID = 40f;
    private static final NovaFont TITLE = NovaFont.semibold(14);
    private static final NovaFont HINT = NovaFont.regular(13);
    private static final NovaFont BUTTON = NovaFont.medium(13);
    private static final NovaFont LABEL = NovaFont.mono(10);
    private static final int TOOLBAR_BG = 0xF00C0D11;
    private static final int HINT_COLOR = 0xFF9D9EA5;

    private final Animation fade = new Animation(0.12f, 0f);
    private HudElement dragging;
    private float grabX, grabY;
    private float guideX = -1, guideY = -1;
    private float unit = 1f, screenW, screenH;
    private int accent;
    /** Toolbar button rects from the last frame: {x, y, w, h}. */
    private float[] resetButton = new float[4], doneButton = new float[4];

    @Override
    public void onOpen() {
        fade.snap(0f);
        NovaClient.get().hud().setEditorOpen(true);
    }

    @Override
    public void onClose() {
        dragging = null;
        NovaClient.get().hud().setEditorOpen(false);
        NovaClient.get().config().save();
    }

    @Override
    protected void render(Renderer base, int guiMouseX, int guiMouseY, float delta) {
        accent = Theme.accent();
        AlphaRenderer chrome = new AlphaRenderer(base);
        chrome.setAlpha(fade.update(1f));
        float[] units = UiScale.push(base, width, height);
        try {
            screenW = units[0];
            screenH = units[1];
            unit = units[2];
            float mx = guiMouseX / unit, my = guiMouseY / unit;
            if (dragging != null) {
                drag(base, mx, my);
            }

            for (float x = GRID; x < screenW; x += GRID) chrome.rect(x, 0, 1, screenH, 0x0DFFFFFF);
            for (float y = GRID; y < screenH; y += GRID) chrome.rect(0, y, screenW, 1, 0x0DFFFFFF);
            if (guideX >= 0) chrome.rect(guideX, 0, 1, screenH, accent);
            if (guideY >= 0) chrome.rect(0, guideY, screenW, 1, accent);

            HudManager hud = NovaClient.get().hud();
            HudElement hovered = dragging != null ? dragging : elementAt(base, mx, my);
            for (HudElement e : hud.getElements()) {
                if (!e.isEnabled()) continue;
                hud.renderElement(base, e, screenW, screenH, true);
                float x = e.getX(base, screenW, true), y = e.getY(base, screenH, true);
                float w = e.scaledWidth(base, true), h = e.scaledHeight(base, true);
                int outline = e == hovered ? accent : Colors.withAlpha(accent, 0xB0);
                dashedRect(chrome, x - 3, y - 3, w + 6, h + 6, outline);
                String label = e.getName() + (e == hovered ? "  " + Math.round(e.getScale() * 100) + "%" : "");
                float ly = y - 3 - 5 - chrome.lineHeight(LABEL);
                if (ly < 0) ly = y + h + 8;
                chrome.text(LABEL, label, x - 3, ly, accent);
            }

            drawToolbar(chrome, mx, my);
        } finally {
            base.pop();
        }
    }

    private void drawToolbar(Renderer r, float mx, float my) {
        String title = "HUD Editor", hint = "Drag elements to reposition";
        float pad = 8, gap = 14, bh = 30;
        float resetW = r.textWidth(BUTTON, "Reset") + 24, doneW = r.textWidth(BUTTON, "Done") + 24;
        float w = 16 + r.textWidth(TITLE, title) + gap + r.textWidth(HINT, hint) + gap + resetW + 8 + doneW + pad;
        float h = pad * 2 + bh;
        float x = Math.round((screenW - w) / 2f), y = 16;
        for (int i = 4; i >= 1; i--) {
            r.roundedRect(x - i * 3, y - i * 3 + 6, w + i * 6, h + i * 6, 10 + i * 3, Colors.argb(18, 0, 0, 0));
        }
        r.roundedRect(x, y, w, h, 10, TOOLBAR_BG);
        r.roundedOutline(x, y, w, h, 10, 1, Theme.BORDER_7);
        float cy = y + h / 2f, cx = x + 16;
        r.textCenteredV(TITLE, title, cx, cy, Theme.TEXT);
        cx += r.textWidth(TITLE, title) + gap;
        r.textCenteredV(HINT, hint, cx, cy, HINT_COLOR);
        cx += r.textWidth(HINT, hint) + gap;

        resetButton = new float[]{cx, y + pad, resetW, bh};
        if (inside(resetButton, mx, my)) r.roundedRect(cx, y + pad, resetW, bh, 6, Theme.BORDER_4);
        r.roundedOutline(cx, y + pad, resetW, bh, 6, 1, Theme.BORDER_8);
        r.textCentered(BUTTON, "Reset", cx + resetW / 2f, cy, Theme.TEXT_2);
        cx += resetW + 8;

        doneButton = new float[]{cx, y + pad, doneW, bh};
        boolean doneHover = inside(doneButton, mx, my);
        r.roundedRect(cx, y + pad, doneW, bh, 6, doneHover ? Colors.lerp(accent, 0xFFFFFFFF, 0.1f) : accent);
        r.textCentered(BUTTON, "Done", cx + doneW / 2f, cy, Theme.ON_ACCENT);
    }

    private static void dashedRect(Renderer r, float x, float y, float w, float h, int argb) {
        float dash = 4, gap = 3;
        for (float i = 0; i < w; i += dash + gap) {
            float len = Math.min(dash, w - i);
            r.rect(x + i, y, len, 1, argb);
            r.rect(x + i, y + h - 1, len, 1, argb);
        }
        for (float i = 0; i < h; i += dash + gap) {
            float len = Math.min(dash, h - i);
            r.rect(x, y + i, 1, len, argb);
            r.rect(x + w - 1, y + i, 1, len, argb);
        }
    }

    private static boolean inside(float[] rect, float x, float y) {
        return x >= rect[0] && x < rect[0] + rect[2] && y >= rect[1] && y < rect[1] + rect[3];
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double gx, double gy, int button) {
        final Renderer r = measure;
        if (r == null) return false;
        final float mx = (float) gx / unit, my = (float) gy / unit;
        if (button == 0 && inside(doneButton, mx, my)) {
            close();
            return true;
        }
        if (button == 0 && inside(resetButton, mx, my)) {
            for (HudElement e : NovaClient.get().hud().getElements()) e.resetPosition();
            return true;
        }
        final HudElement e = withUnits(r, () -> elementAt(r, mx, my));
        if (e == null) return false;
        if (button == 0) {
            dragging = e;
            grabX = mx - withUnits(r, () -> e.getX(r, screenW, true));
            grabY = my - withUnits(r, () -> e.getY(r, screenH, true));
        } else if (button == 1) {
            e.setEnabled(false);
        }
        return true;
    }

    @Override
    public void mouseReleased(double x, double y, int button) {
        dragging = null;
        guideX = -1;
        guideY = -1;
    }

    @Override
    public boolean mouseScrolled(double gx, double gy, final double amount) {
        final Renderer r = measure;
        if (r == null) return false;
        final float mx = (float) gx / unit, my = (float) gy / unit;
        return withUnits(r, () -> {
            HudElement e = elementAt(r, mx, my);
            if (e == null) return false;
            float x = e.getX(r, screenW, true), y = e.getY(r, screenH, true);
            e.scale.set(e.scale.get() + (amount > 0 ? 0.05 : -0.05));
            e.setPosition(r, x, y, screenW, screenH);
            return true;
        });
    }

    @Override
    public boolean keyPressed(int keyCode, UiKey key) {
        return false; // ESC falls through to close() -> back to the menu
    }

    /**
     * Text measurement depends on the renderer's current scale, so input handling (outside a frame)
     * re-applies the unit transform the frame used.
     */
    private <T> T withUnits(Renderer r, Callable<T> body) {
        r.pushScale(0, 0, unit);
        try {
            return body.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            r.pop();
        }
    }

    // ------------------------------------------------------------------ dragging

    private void drag(Renderer r, float mx, float my) {
        float w = dragging.scaledWidth(r, true), h = dragging.scaledHeight(r, true);
        float x = mx - grabX, y = my - grabY;
        guideX = -1;
        guideY = -1;
        float[] sx = snap(x, w, new float[]{screenW / 2f});
        float[] sy = snap(y, h, new float[]{screenH / 2f});
        for (HudElement o : NovaClient.get().hud().getElements()) {
            if (o == dragging || !o.isEnabled()) continue;
            float ox = o.getX(r, screenW, true), oy = o.getY(r, screenH, true);
            if (sx == null) sx = snap(x, w, new float[]{ox, ox + o.scaledWidth(r, true)});
            if (sy == null) sy = snap(y, h, new float[]{oy, oy + o.scaledHeight(r, true)});
        }
        if (sx != null) {
            x = sx[0];
            guideX = sx[1];
        }
        if (sy != null) {
            y = sy[0];
            guideY = sy[1];
        }
        dragging.setPosition(r, x, y, screenW, screenH);
    }

    /** Snaps start edge, end edge or center onto the first line within range: {snappedPos, line} or null. */
    private static float[] snap(float pos, float size, float[] lines) {
        for (float line : lines) {
            if (Math.abs(pos - line) < SNAP) return new float[]{line, line};
            if (Math.abs(pos + size - line) < SNAP) return new float[]{line - size, line};
            if (Math.abs(pos + size / 2f - line) < SNAP) return new float[]{line - size / 2f, line};
        }
        return null;
    }

    private HudElement elementAt(Renderer r, float mx, float my) {
        List<HudElement> elements = NovaClient.get().hud().getElements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement e = elements.get(i);
            if (!e.isEnabled()) continue;
            float x = e.getX(r, screenW, true), y = e.getY(r, screenH, true);
            if (mx >= x && mx <= x + e.scaledWidth(r, true) && my >= y && my <= y + e.scaledHeight(r, true)) {
                return e;
            }
        }
        return null;
    }
}
