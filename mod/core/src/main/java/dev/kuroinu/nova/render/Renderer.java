package dev.kuroinu.nova.render;

import dev.kuroinu.nova.render.font.NovaFont;

/**
 * Version-independent 2D drawing surface. Coordinates are GUI-scaled pixels; colors are ARGB.
 * Each Minecraft version provides an implementation backed by its own rendering API.
 */
public interface Renderer {
    void rect(float x, float y, float width, float height, int argb);

    /** Draws text with its top-left at (x, y). */
    void text(String text, float x, float y, int argb, boolean shadow);

    int textWidth(String text);

    int fontHeight();

    void pushScale(float x, float y, float scale);

    void pop();

    /**
     * Restricts drawing to a rectangle, intersected with any enclosing scissor. Coordinates are
     * absolute GUI coordinates and are NOT affected by {@link #pushScale}.
     */
    void pushScissor(float x, float y, float width, float height);

    void popScissor();

    /** Rectangle with rounded corners. Versions without support fall back to a plain rect. */
    default void roundedRect(float x, float y, float width, float height, float radius, int argb) {
        rect(x, y, width, height, argb);
    }

    /** Border of a rounded rectangle, drawn inside the given bounds. */
    default void roundedOutline(float x, float y, float width, float height, float radius, float thickness, int argb) {
        outline(x, y, width, height, thickness, argb);
    }

    // ---- custom fonts. Versions without a glyph renderer approximate with the vanilla font.

    /** Draws text with the top of its line box at y. */
    default void text(NovaFont font, String text, float x, float y, int argb) {
        float k = font.size / 10f;
        pushScale(x, y + (lineHeight(font) - 8 * k) / 2f, k);
        text(text, 0, 0, argb, false);
        pop();
    }

    default float textWidth(NovaFont font, String text) {
        return textWidth(text) * font.size / 10f;
    }

    /** Height of the font's line box (ascent + descent). */
    default float lineHeight(NovaFont font) {
        return font.size * 1.26f;
    }

    /** Draws text vertically centered on centerY. */
    default void textCenteredV(NovaFont font, String text, float x, float centerY, int argb) {
        text(font, text, x, centerY - lineHeight(font) / 2f, argb);
    }

    /** Draws text centered on (centerX, centerY). */
    default void textCentered(NovaFont font, String text, float centerX, float centerY, int argb) {
        textCenteredV(font, text, centerX - textWidth(font, text) / 2f, centerY, argb);
    }

    default String trimToWidth(NovaFont font, String text, float maxWidth) {
        if (textWidth(font, text) <= maxWidth) {
            return text;
        }
        int end = text.length();
        while (end > 0 && textWidth(font, text.substring(0, end) + "…") > maxWidth) {
            end--;
        }
        return text.substring(0, end) + "…";
    }

    /** A square rotated 45 degrees, given its side length (the logo mark). Built from thin horizontal strips. */
    default void diamond(float centerX, float centerY, float side, int argb) {
        float half = side * 0.70710678f; // half diagonal
        int strips = Math.max(4, Math.round(half * 4));
        float stripH = half * 2 / strips;
        for (int i = 0; i < strips; i++) {
            float yMid = -half + (i + 0.5f) * stripH;
            float halfW = half - Math.abs(yMid);
            rect(centerX - halfW, centerY - half + i * stripH, halfW * 2, stripH, argb);
        }
    }

    default void circle(float centerX, float centerY, float radius, int argb) {
        roundedRect(centerX - radius, centerY - radius, radius * 2, radius * 2, radius, argb);
    }

    default void outline(float x, float y, float width, float height, float thickness, int argb) {
        rect(x, y, width, thickness, argb);
        rect(x, y + height - thickness, width, thickness, argb);
        rect(x, y + thickness, thickness, height - thickness * 2, argb);
        rect(x + width - thickness, y + thickness, thickness, height - thickness * 2, argb);
    }

    default void centeredText(String text, float centerX, float y, int argb, boolean shadow) {
        text(text, centerX - textWidth(text) / 2f, y, argb, shadow);
    }

    /** Text drawn at a fraction of the normal font size. */
    default void scaledText(String text, float x, float y, float scale, int argb, boolean shadow) {
        pushScale(x, y, scale);
        text(text, 0, 0, argb, shadow);
        pop();
    }

    /** Cuts text to fit {@code maxWidth}, appending ".." when shortened. */
    default String trimToWidth(String text, float maxWidth) {
        if (textWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "..";
        int end = text.length();
        while (end > 0 && textWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }
}
