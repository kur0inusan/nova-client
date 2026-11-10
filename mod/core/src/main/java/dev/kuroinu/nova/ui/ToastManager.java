package dev.kuroinu.nova.ui;

import dev.kuroinu.nova.module.Module;
import dev.kuroinu.nova.render.AlphaRenderer;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.render.font.NovaFont;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** "Module enabled/disabled" toasts, bottom-right, newest at the bottom (design handoff spec). */
public final class ToastManager {
    private static final long LIFETIME_MS = 1800;
    private static final int MAX = 4;
    private static final NovaFont NAME = NovaFont.semibold(13);
    private static final NovaFont STATE = NovaFont.regular(13);

    private static final class Toast {
        final String name;
        final boolean on;
        final long createdAt = System.currentTimeMillis();

        Toast(String name, boolean on) {
            this.name = name;
            this.on = on;
        }
    }

    private final List<Toast> toasts = new ArrayList<Toast>();
    /** True while a screen draws the toasts itself (so they sit above its overlay). */
    private boolean drawnByScreen;

    public synchronized void push(Module module) {
        toasts.add(new Toast(module.getName(), module.isEnabled()));
        while (toasts.size() > MAX) {
            toasts.remove(0);
        }
    }

    public void setDrawnByScreen(boolean drawnByScreen) {
        this.drawnByScreen = drawnByScreen;
    }

    public boolean isDrawnByScreen() {
        return drawnByScreen;
    }

    /** Draws in design units; {@code w}/{@code h} are the screen size in units. */
    public synchronized void render(Renderer r, float w, float h) {
        long now = System.currentTimeMillis();
        for (Iterator<Toast> it = toasts.iterator(); it.hasNext(); ) {
            if (now - it.next().createdAt > LIFETIME_MS) it.remove();
        }
        float bottom = h - 18;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            String state = t.on ? "Enabled" : "Disabled";
            float nameW = r.textWidth(NAME, t.name);
            float stateW = r.textWidth(STATE, state);
            float boxH = 20 + r.lineHeight(NAME);
            float boxW = Math.max(180, 14 + 8 + 10 + nameW + 16 + stateW + 14);
            // Slide in from the right, then fade and drift out over the last 250 ms.
            long age = now - t.createdAt;
            float enter = Easing.outCubic(age / 220f);
            float exit = Easing.clamp01((age - (LIFETIME_MS - 250)) / 250f);
            AlphaRenderer a = new AlphaRenderer(r);
            a.setAlpha(enter * (1 - exit));
            float x = w - 18 - boxW + (1 - enter) * 28 + exit * 16;
            float y = bottom - boxH;
            float cy = y + boxH / 2f;
            a.roundedRect(x, y, boxW, boxH, 8, Theme.TOAST);
            a.roundedOutline(x, y, boxW, boxH, 8, 1, Theme.BORDER_7);
            a.circle(x + 14 + 4, cy, 4, t.on ? Theme.accent() : Theme.DISABLED);
            a.textCenteredV(NAME, t.name, x + 14 + 8 + 10, cy, Theme.TEXT);
            a.textCenteredV(STATE, state, x + boxW - 14 - stateW, cy, Theme.MUTED);
            bottom = y - 6;
        }
    }

    public synchronized boolean isEmpty() {
        return toasts.isEmpty();
    }
}
