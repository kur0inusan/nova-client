package dev.kuroinu.nova.hud;

import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.render.font.NovaFont;

/** Shared HUD look from the design handoff: JetBrains Mono 600 with a 1px drop shadow, translucent black boxes. */
public final class HudStyle {
    public static final NovaFont TEXT = NovaFont.monoSemibold(13);
    public static final NovaFont TEXT_12 = NovaFont.monoSemibold(12);
    public static final NovaFont TEXT_10 = NovaFont.monoSemibold(10);
    public static final int BACKGROUND = 0x73000000; // rgba(0,0,0,.45)
    public static final int BOX = 0x66000000;        // rgba(0,0,0,.40)
    public static final int WHITE = 0xFFF5F5F5;
    public static final int GOOD = 0xFF8EDFA6;       // oklch(0.85 0.12 150)
    public static final int BAD = 0xFFFF7C80;
    public static final float RADIUS = 4;

    private HudStyle() {
    }

    /** Text with the HUD drop shadow. */
    public static void text(Renderer r, NovaFont font, String text, float x, float y, int argb) {
        int shadowAlpha = (argb >>> 24) / 2; // rgba(0,0,0,.5) for opaque text, fading with the text
        r.text(font, text, x, y + 1, shadowAlpha << 24);
        r.text(font, text, x, y, argb);
    }

    public static void textCenteredV(Renderer r, NovaFont font, String text, float x, float centerY, int argb) {
        text(r, font, text, x, centerY - r.lineHeight(font) / 2f, argb);
    }

    public static void textCentered(Renderer r, NovaFont font, String text, float centerX, float centerY, int argb) {
        textCenteredV(r, font, text, centerX - r.textWidth(font, text) / 2f, centerY, argb);
    }
}
