package dev.kuroinu.nova.ui;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.module.impl.ClickGuiSettings;
import dev.kuroinu.nova.platform.Platform;
import dev.kuroinu.nova.render.AlphaRenderer;
import dev.kuroinu.nova.render.Colors;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.render.font.NovaFont;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Nova's title screen, in the ClickGUI's visual language. Replaces the vanilla title screen when
 * the "Main Menu" module is enabled.
 *
 * <p>Responsive: content shrinks to fit small windows, tightens on short ones, and on wide large
 * screens switches to a split layout (menu left, "What's new" card right), gliding between
 * layouts on resize. Motion: drifting glows and depth particles with mouse parallax, a floating
 * logo, button lift/press/sheen, and fade transitions to and from the screens it opens.</p>
 */
public final class MainMenu extends NovaScreen {
    private static final NovaFont LOGO_BOLD = NovaFont.bold(46);
    private static final NovaFont LOGO_REGULAR = NovaFont.regular(46);
    private static final NovaFont TAGLINE = NovaFont.mono(12);
    private static final NovaFont BUTTON = NovaFont.semibold(15);
    private static final NovaFont SMALL = NovaFont.regular(11);
    private static final NovaFont NAME = NovaFont.semibold(13);
    private static final NovaFont BADGE = NovaFont.bold(13);
    private static final NovaFont FOOTER = NovaFont.mono(10);
    private static final NovaFont CARD_TITLE = NovaFont.semibold(16);
    private static final NovaFont CARD_BODY = NovaFont.regular(13);
    private static final NovaFont PILL = NovaFont.mono(11);

    private static final int BACKGROUND = 0xFF07080B;
    private static final float COLUMN_W = 320;
    private static final float BUTTON_H = 46;
    private static final float BUTTON_GAP = 10;
    private static final float CARD_W = 340;
    private static final long EXIT_MS = 200;
    private static final String[] WHATS_NEW = {
            "Redesigned ClickGUI with list & grid layouts",
            "Custom fonts: Outfit and JetBrains Mono",
            "New HUD style with Armor Status and Potions",
            "Drag-and-drop HUD editor with snapping",
            "This main menu",
    };

    private interface Action {
        void run();
    }

    private static final class Hit {
        final String key;
        final float x, y, w, h;
        final Action action;

        Hit(String key, float x, float y, float w, float h, Action action) {
            this.key = key;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }

        boolean contains(float px, float py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private static final class Particle {
        float x, y;
        final float speed, size, depth, phase;

        Particle(Random rnd) {
            x = rnd.nextFloat();
            y = rnd.nextFloat();
            depth = 0.3f + rnd.nextFloat() * 0.7f;
            speed = (0.006f + rnd.nextFloat() * 0.012f) * depth;
            size = 0.8f + depth * 1.6f;
            phase = rnd.nextFloat() * 6.283f;
        }
    }

    private enum Style {PRIMARY, SECONDARY, GHOST}

    private final List<Hit> hits = new ArrayList<Hit>();
    private final Map<String, Animation> anims = new HashMap<String, Animation>();
    private final Map<String, Long> sweepStart = new HashMap<String, Long>();
    private final List<Particle> particles = new ArrayList<Particle>();
    private final Animation wideAnim = new Animation(0.25f, 0f);
    private final Animation parallaxX = new Animation(0.6f, 0f);
    private final Animation parallaxY = new Animation(0.6f, 0f);
    private final long startNanos = System.nanoTime();
    private long lastNanos = System.nanoTime();

    private long openedAt;
    private long resumedAt;
    private boolean firstEntrance = true;
    private long exitAt;
    private Action pendingExit;
    private boolean exitToBlack;

    private String pressedKey;
    private float unit = 1f;
    /** Content transform within the screen (fit-to-window scale about the content origin). */
    private float contentTx, contentTy, contentScale = 1f;
    private float mouseX, mouseY;
    private int accent;
    private boolean layoutInitialized;
    /** Foreground opacity while drawn as a backdrop behind another screen (1 when shown normally). */
    private float backdropAlpha = 1f;

    public MainMenu() {
        Random rnd = new Random(7);
        for (int i = 0; i < 56; i++) {
            particles.add(new Particle(rnd));
        }
    }

    @Override
    public void onOpen() {
        if (openedAt == 0) {
            openedAt = System.currentTimeMillis(); // entrance plays once
        } else {
            onResume(); // shown again (e.g. back from Multiplayer): clear the finished exit transition
        }
    }

    @Override
    public void onResume() {
        // Back from Singleplayer/Multiplayer/Options: quick fade in, no full staggered entrance.
        resumedAt = System.currentTimeMillis();
        firstEntrance = false;
        exitAt = 0;
        pendingExit = null;
        pressedKey = null;
    }

    // ================================================================== frame

    /** Behind the ClickGUI: keep the moving background, fade the logo/buttons/footer by {@code contentAlpha}. */
    @Override
    public void renderAsBackdrop(Renderer r, float contentAlpha, float delta) {
        backdropAlpha = contentAlpha;
        try {
            renderFrame(r, -10000, -10000, delta);
        } finally {
            backdropAlpha = 1f;
        }
    }

    @Override
    protected void render(Renderer base, int guiMouseX, int guiMouseY, float delta) {
        accent = Theme.accent();
        hits.clear();
        long nowNanos = System.nanoTime();
        float dt = Math.min(0.1f, (nowNanos - lastNanos) / 1e9f);
        lastNanos = nowNanos;
        float time = (nowNanos - startNanos) / 1e9f;

        ClickGuiSettings gui = NovaClient.get().modules().get(ClickGuiSettings.class);
        float[] units = UiScale.push(base, width, height, gui == null ? 1f : gui.scale.floatValue());
        try {
            float sw = units[0], sh = units[1];
            unit = units[2];
            float rawMx = guiMouseX / unit, rawMy = guiMouseY / unit;
            // Parallax follows the mouse lazily; offsets are in [-1, 1].
            float px = parallaxX.update(Math.max(-1, Math.min(1, (rawMx - sw / 2f) / (sw / 2f))));
            float py = parallaxY.update(Math.max(-1, Math.min(1, (rawMy - sh / 2f) / (sh / 2f))));

            base.rect(0, 0, sw, sh, BACKGROUND);
            drawGlows(base, sw, sh, time, px, py);
            drawParticles(base, sw, sh, dt, time, px, py);

            // Exit transition: content fades and drifts up, then the pending action runs.
            float exit = exitAt == 0 ? 0f : Easing.outCubic((System.currentTimeMillis() - exitAt) / (float) EXIT_MS);
            if (exitAt != 0 && exit >= 1f && pendingExit != null) {
                Action a = pendingExit;
                pendingExit = null;
                a.run();
            }
            float enter = firstEntrance ? 1f : Easing.since(resumedAt, 0, 220);
            float contentAlpha = Math.min(enter, 1f - exit) * backdropAlpha;
            if (contentAlpha < 0.01f) {
                return; // fully hidden behind the ClickGUI: background only
            }

            drawContent(base, sw, sh, rawMx, rawMy, time, contentAlpha, exit);
            drawFooter(base, sw, sh, contentAlpha);

            if (exitToBlack && exit > 0) {
                base.rect(0, 0, sw, sh, Colors.withAlpha(0xFF000000, Math.round(255 * exit)));
            }
        } finally {
            base.pop();
        }
    }

    // ================================================================== layout

    private void drawContent(Renderer base, float sw, float sh, float rawMx, float rawMy, float time,
                             float alpha, float exit) {
        boolean wideTarget = sw >= 1000 && sw / sh >= 1.5f;
        if (!layoutInitialized) {
            wideAnim.snap(wideTarget ? 1f : 0f);
            layoutInitialized = true;
        }
        float wide = Easing.outCubic(wideAnim.update(wideTarget ? 1f : 0f));
        boolean compact = sh < 440;

        float logoH = base.lineHeight(LOGO_BOLD);
        float taglineH = compact ? 0 : 16 + 14;
        float gapAfterLogo = compact ? 22 : 40;
        float stackH = logoH + taglineH + gapAfterLogo + (BUTTON_H + BUTTON_GAP) * 4 - BUTTON_GAP;
        float stackW = Math.max(COLUMN_W, logoWidth(base));

        // Fit: shrink the whole stack in small windows (keep room for the footer).
        float availH = sh - (compact ? 90 : 150);
        float availW = (wideTarget ? sw * 0.5f : sw) - 40;
        float fit = Math.min(1f, Math.min(availH / stackH, availW / stackW));
        fit = Math.max(0.55f, fit);

        // Column origin: centered (narrow) or left (wide), interpolated so resizes glide.
        float centerX = sw / 2f - COLUMN_W * fit / 2f;
        float leftX = Math.max(60, sw * 0.09f);
        float colX = centerX + (leftX - centerX) * wide;
        float colY = (sh - stackH * fit) / 2f - 10 - exit * 12;

        contentScale = fit;
        contentTx = colX;
        contentTy = colY;
        mouseX = (rawMx - contentTx) / contentScale;
        mouseY = (rawMy - contentTy) / contentScale;

        AlphaRenderer r = new AlphaRenderer(base);
        r.setAlpha(alpha);
        r.pushScale(contentTx, contentTy, contentScale);
        try {
            // Inside here: column-local units, x in [0, COLUMN_W].
            float logoAlign = (1 - wide); // 1 = centered over the column, 0 = left-aligned
            float y = 0;
            drawLogo(r, logoAlign, y, time);
            y += logoH;
            if (!compact) {
                y += 14;
                AlphaRenderer tag = new AlphaRenderer(r);
                tag.setAlpha(entrance(120, 300));
                String tagline = "v1.0  ·  Minecraft " + NovaClient.get().platform().getMinecraftVersion();
                float tw = r.textWidth(TAGLINE, tagline);
                tag.text(TAGLINE, tagline, (COLUMN_W - tw) / 2f * logoAlign, y, Theme.MUTED_2);
                y += 16;
            }
            y += gapAfterLogo;

            final Platform p = NovaClient.get().platform();
            float half = (COLUMN_W - BUTTON_GAP) / 2f;
            button(r, 0, "single", "Singleplayer", Style.PRIMARY, 0, y, COLUMN_W, time,
                    () -> leave(() -> p.openVanillaScreen(Platform.VanillaScreen.SINGLEPLAYER), false));
            y += BUTTON_H + BUTTON_GAP;
            button(r, 1, "multi", "Multiplayer", Style.SECONDARY, 0, y, COLUMN_W, time,
                    () -> leave(() -> openMultiplayer(p), false));
            y += BUTTON_H + BUTTON_GAP;
            button(r, 2, "options", "Options", Style.SECONDARY, 0, y, half, time,
                    () -> leave(() -> p.openVanillaScreen(Platform.VanillaScreen.OPTIONS), false));
            button(r, 3, "nova", "Nova", Style.SECONDARY, half + BUTTON_GAP, y, half, time,
                    () -> NovaClient.get().openMenu(this)); // the menu opens over this screen; no exit needed
            y += BUTTON_H + BUTTON_GAP;
            button(r, 4, "quit", "Quit Game", Style.GHOST, 0, y, COLUMN_W, time, () -> leave(p::quitGame, true));
        } finally {
            r.pop();
        }

        if (wide > 0.01f) {
            drawWhatsNew(base, sw, sh, wide * alpha);
        }
    }

    /** Nova's server list where the version supports it, otherwise vanilla's. */
    private void openMultiplayer(Platform p) {
        if (p.serverBrowser() != null) {
            p.openScreen(new MultiplayerMenu().withParent(this));
        } else {
            p.openVanillaScreen(Platform.VanillaScreen.MULTIPLAYER);
        }
    }

    private float logoWidth(Renderer r) {
        return 26 + 18 + r.textWidth(LOGO_BOLD, "Nova") + 14 + r.textWidth(LOGO_REGULAR, "Client");
    }

    /** Fraction of a first-open entrance step (instant after resuming). */
    private float entrance(long delayMs, long durationMs) {
        return firstEntrance ? Easing.since(openedAt, delayMs, durationMs) : 1f;
    }

    // ================================================================== background

    /** Large soft glows drifting slowly, nudged by the mouse for depth. */
    private void drawGlows(Renderer r, float w, float h, float t, float px, float py) {
        float fade = Easing.since(openedAt, 0, 900);
        float m = Math.max(w, h);
        glow(r, w * (0.30f + 0.06f * (float) Math.sin(t * 0.11)) - px * 26, h * (0.35f + 0.08f * (float) Math.cos(t * 0.09)) - py * 18,
                m * 0.34f, accent, 0.20f * fade);
        glow(r, w * (0.72f + 0.05f * (float) Math.cos(t * 0.07 + 1)) - px * 40, h * (0.62f + 0.07f * (float) Math.sin(t * 0.1 + 2)) - py * 28,
                m * 0.30f, Theme.PALETTE[1], 0.13f * fade);
        glow(r, w * (0.55f + 0.07f * (float) Math.sin(t * 0.05 + 4)) - px * 16, h * (0.15f + 0.05f * (float) Math.cos(t * 0.08)) - py * 12,
                m * 0.22f, Theme.PALETTE[7], 0.09f * fade);
        r.rect(0, 0, w, h, 0x30000000);
    }

    private static void glow(Renderer r, float cx, float cy, float radius, int color, float strength) {
        int rings = 14;
        for (int i = 0; i < rings; i++) {
            float k = 1f - (float) i / rings;
            float alpha = strength / rings * (1.6f - k);
            r.circle(cx, cy, radius * k, Colors.withAlpha(color, Math.round(alpha * 255)));
        }
    }

    /** Small specks rising slowly and twinkling; nearer ones (higher depth) are bigger, faster and shift more with the mouse. */
    private void drawParticles(Renderer r, float w, float h, float dt, float t, float px, float py) {
        float fade = Easing.since(openedAt, 300, 1200);
        for (Particle p : particles) {
            p.y -= p.speed * dt;
            if (p.y < -0.02f) {
                p.y += 1.04f;
            }
            float x = p.x * w - px * 30 * p.depth;
            float y = p.y * h - py * 20 * p.depth;
            float twinkle = 0.45f + 0.55f * (0.5f + 0.5f * (float) Math.sin(t * (0.8f + p.depth) + p.phase));
            int color = Colors.lerp(0xFFFFFFFF, accent, p.depth * 0.6f);
            r.circle(x, y, p.size, Colors.withAlpha(color, Math.round(255 * 0.35f * p.depth * twinkle * fade)));
        }
    }

    // ================================================================== content pieces

    private void drawLogo(Renderer base, float align, float y, float time) {
        float t = entrance(0, 450);
        AlphaRenderer r = new AlphaRenderer(base);
        r.setAlpha(t);
        float float_ = (float) Math.sin(time * 1.5) * 3f; // gentle hover
        y += -(1 - t) * 12 + float_;
        float boldW = r.textWidth(LOGO_BOLD, "Nova"), regW = r.textWidth(LOGO_REGULAR, "Client");
        float mark = 26, gap = 18, wordGap = 14;
        float total = mark + gap + boldW + wordGap + regW;
        float x = (COLUMN_W - total) / 2f * align;
        float cy = y + r.lineHeight(LOGO_BOLD) / 2f;
        // Pulsing halo behind the mark.
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 2.2);
        for (int i = 0; i < 6; i++) {
            float k = 1f - i / 6f;
            r.circle(x + mark / 2f, cy, (22 + pulse * 6) * k, Colors.withAlpha(accent, Math.round(255 * 0.05f * (1.4f - k))));
        }
        r.diamond(x + mark / 2f, cy, mark, accent);
        r.textCenteredV(LOGO_BOLD, "Nova", x + mark + gap, cy, Theme.TEXT);
        r.textCenteredV(LOGO_REGULAR, "Client", x + mark + gap + boldW + wordGap, cy, Theme.MUTED);
    }

    private void button(Renderer base, int index, String key, String label, Style style, float x, float y, float w,
                        float time, Action action) {
        float t = entrance(180 + index * 55L, 320);
        AlphaRenderer r = new AlphaRenderer(base);
        r.setAlpha(t);
        y += (1 - t) * 14;
        boolean interactive = t > 0.6f && exitAt == 0;
        boolean over = interactive && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + BUTTON_H;
        float hv = anim("hover:" + key, 0.1f).update(over ? 1f : 0f);
        float press = anim("press:" + key, 0.06f).update(over && key.equals(pressedKey) ? 1f : 0f);
        if (over && !sweepStart.containsKey(key)) {
            sweepStart.put(key, System.currentTimeMillis());
        } else if (!over) {
            sweepStart.remove(key);
        }

        // Lift on hover, shrink slightly while pressed (about the button center).
        y -= 2 * hv;
        float s = 1f - 0.03f * press;
        float cx = x + w / 2f, cy = y + BUTTON_H / 2f;
        r.pushScale(cx * (1 - s), cy * (1 - s), s);
        try {
            switch (style) {
                case PRIMARY:
                    if (hv > 0.01f) { // soft accent glow under the primary button
                        r.roundedRect(x - 4, y + 4, w + 8, BUTTON_H + 4, 14, Colors.withAlpha(accent, Math.round(0x30 * hv)));
                    }
                    r.roundedRect(x, y, w, BUTTON_H, 10, Colors.lerp(accent, Colors.lerp(accent, 0xFFFFFFFF, 0.14f), hv));
                    sheen(r, key, x, y, w, 0x40FFFFFF);
                    r.textCentered(BUTTON, label, cx, cy, Theme.ON_ACCENT);
                    break;
                case SECONDARY:
                    r.roundedRect(x, y, w, BUTTON_H, 10, Colors.lerp(0xD9111116, 0xE61A1B22, hv));
                    sheen(r, key, x, y, w, 0x14FFFFFF);
                    r.roundedOutline(x, y, w, BUTTON_H, 10, 1, Colors.lerp(Theme.BORDER_8, Colors.withAlpha(accent, 0x80), hv));
                    r.textCentered(BUTTON, label, cx, cy, Theme.TEXT);
                    break;
                default:
                    if (hv > 0.01f) r.roundedRect(x, y, w, BUTTON_H, 10, Colors.withAlpha(0xFFFF7C80, Math.round(0x1A * hv)));
                    r.roundedOutline(x, y, w, BUTTON_H, 10, 1, Colors.lerp(Theme.BORDER_6, 0x66FF7C80, hv));
                    r.textCentered(BUTTON, label, cx, cy, Colors.lerp(Theme.MUTED, 0xFFFF7C80, hv));
                    break;
            }
        } finally {
            r.pop();
        }
        if (interactive) {
            hits.add(new Hit(key, x, y, w, BUTTON_H, action));
        }
    }

    /** A light band sweeping left-to-right across a button once each time it becomes hovered. */
    private void sheen(Renderer r, String key, float x, float y, float w, int color) {
        Long start = sweepStart.get(key);
        if (start == null) return;
        float t = (System.currentTimeMillis() - start) / 650f;
        if (t >= 1f) return;
        float band = 46;
        float bx = x - band + (w + band * 2) * Easing.outCubic(t);
        // Clip to the button (rect clip; the band is faint enough that the rounded corners don't show it).
        float[] g = toGui(x, y + 1, w, BUTTON_H - 2);
        r.pushScissor(g[0], g[1], g[2], g[3]);
        try {
            int strips = 6;
            for (int i = 0; i < strips; i++) {
                float k = 1f - Math.abs(i - (strips - 1) / 2f) / (strips / 2f); // brightest in the middle
                r.rect(bx + i * band / strips, y, band / strips + 0.5f, BUTTON_H,
                        Colors.withAlpha(color, Math.round(Colors.alpha(color) * k)));
            }
        } finally {
            r.popScissor();
        }
    }

    private void drawWhatsNew(Renderer base, float sw, float sh, float alpha) {
        AlphaRenderer r = new AlphaRenderer(base);
        r.setAlpha(alpha * entrance(420, 400));
        float pad = 22;
        float lineH = 26;
        float h = pad + r.lineHeight(CARD_TITLE) + 16 + WHATS_NEW.length * lineH + pad - 6;
        float x = sw - Math.max(60, sw * 0.09f) - CARD_W + (1 - alpha) * 40;
        float y = (sh - h) / 2f;
        r.roundedRect(x, y, CARD_W, h, 16, 0xB30B0C10);
        r.roundedOutline(x, y, CARD_W, h, 16, 1, Theme.BORDER_7);
        float ty = y + pad;
        r.text(CARD_TITLE, "What's new", x + pad, ty, Theme.TEXT);
        String version = "v1.0";
        float pw = r.textWidth(PILL, version) + 16;
        float pillCy = ty + r.lineHeight(CARD_TITLE) / 2f;
        r.roundedRect(x + CARD_W - pad - pw, pillCy - 10, pw, 20, 10, Theme.accentSoft());
        r.textCentered(PILL, version, x + CARD_W - pad - pw / 2f, pillCy, accent);
        float ly = ty + r.lineHeight(CARD_TITLE) + 16;
        for (int i = 0; i < WHATS_NEW.length; i++) {
            float row = Easing.since(openedAt, 520 + i * 70L, 300);
            AlphaRenderer line = new AlphaRenderer(r);
            line.setAlpha(firstEntrance ? row : 1f);
            float cy = ly + lineH / 2f;
            line.circle(x + pad + 3, cy, 3, accent);
            line.textCenteredV(CARD_BODY, r.trimToWidth(CARD_BODY, WHATS_NEW[i], CARD_W - pad * 2 - 16), x + pad + 16, cy, Theme.TEXT_2);
            ly += lineH;
        }
    }

    private void drawFooter(Renderer base, float sw, float sh, float alpha) {
        AlphaRenderer r = new AlphaRenderer(base);
        r.setAlpha(alpha * Easing.since(openedAt, 400, 400));
        String name = NovaClient.get().platform().getPlayerName();
        float size = 32, x = 20, y = sh - 20 - size;
        r.roundedRect(x, y, size, size, 8, Theme.accentSoft());
        r.textCentered(BADGE, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT), x + size / 2f, y + size / 2f, accent);
        float tx = x + size + 10;
        float lh1 = r.lineHeight(SMALL), lh2 = r.lineHeight(NAME);
        float top = y + (size - lh1 - lh2) / 2f;
        r.text(SMALL, "Logged in as", tx, top, Theme.MUTED_2);
        r.text(NAME, name, tx, top + lh1, Theme.TEXT);

        // The notice would collide with the account block on narrow windows.
        String notice = "Not affiliated with Mojang or Microsoft";
        float nw = r.textWidth(FOOTER, notice);
        if (sw - 20 - nw > tx + Math.max(r.textWidth(NAME, name), r.textWidth(SMALL, "Logged in as")) + 30) {
            r.text(FOOTER, notice, sw - 20 - nw, sh - 20 - r.lineHeight(FOOTER), Theme.DISABLED);
        }
    }

    // ================================================================== input & transitions

    /** Plays the exit transition, then runs {@code action} (e.g. opening a vanilla screen). */
    private void leave(Action action, boolean toBlack) {
        if (exitAt != 0) return;
        exitAt = System.currentTimeMillis();
        exitToBlack = toBlack;
        pendingExit = action;
    }

    @Override
    public boolean mouseClicked(double gx, double gy, int button) {
        if (button != 0 || exitAt != 0) return true;
        Hit hit = hitAt(gx, gy);
        pressedKey = hit == null ? null : hit.key;
        return true;
    }

    @Override
    public void mouseReleased(double gx, double gy, int button) {
        if (button != 0 || pressedKey == null) return;
        Hit hit = hitAt(gx, gy);
        String pressed = pressedKey;
        pressedKey = null;
        if (hit != null && hit.key.equals(pressed) && exitAt == 0) { // act on release over the same button
            NovaClient.get().platform().playClickSound();
            hit.action.run();
        }
    }

    private Hit hitAt(double gx, double gy) {
        float x = ((float) gx / unit - contentTx) / contentScale;
        float y = ((float) gy / unit - contentTy) / contentScale;
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(x, y)) return hits.get(i);
        }
        return null;
    }

    @Override
    public boolean keyPressed(int keyCode, UiKey key) {
        return true; // the title screen has nowhere to go back to: swallow ESC
    }

    // ================================================================== helpers

    /** Converts a column-local rect to GUI coordinates (for scissoring). */
    private float[] toGui(float x, float y, float w, float h) {
        return new float[]{(contentTx + x * contentScale) * unit, (contentTy + y * contentScale) * unit,
                w * contentScale * unit, h * contentScale * unit};
    }

    private Animation anim(String key, float duration) {
        Animation a = anims.get(key);
        if (a == null) {
            a = new Animation(duration, 0f);
            anims.put(key, a);
        }
        return a;
    }
}
