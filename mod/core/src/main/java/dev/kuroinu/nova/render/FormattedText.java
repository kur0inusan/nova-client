package dev.kuroinu.nova.render;

import dev.kuroinu.nova.render.font.NovaFont;

/**
 * Draws Minecraft's legacy § formatting (colors 0-f, §l bold, §r reset) with Nova fonts.
 * Obfuscated/strikethrough/underline/italic codes are accepted and ignored.
 */
public final class FormattedText {
    private static final int[] COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
    };

    private FormattedText() {
    }

    /** Draws formatted text; returns the width drawn. {@code bold} may equal {@code regular}. */
    public static float draw(Renderer r, NovaFont regular, NovaFont bold, String text, float x, float y, int defaultColor) {
        return run(r, regular, bold, text, x, y, defaultColor, true);
    }

    public static float width(Renderer r, NovaFont regular, NovaFont bold, String text) {
        return run(r, regular, bold, text, 0, 0, 0, false);
    }

    public static String strip(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static float run(Renderer r, NovaFont regular, NovaFont bold, String text, float x, float y,
                             int defaultColor, boolean draw) {
        int alpha = defaultColor & 0xFF000000;
        int color = defaultColor;
        boolean isBold = false;
        float cx = x;
        StringBuilder segment = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                cx += flush(r, isBold ? bold : regular, segment, cx, y, color, draw);
                char code = Character.toLowerCase(text.charAt(++i));
                int idx = "0123456789abcdef".indexOf(code);
                if (idx >= 0) {
                    color = alpha | COLORS[idx];
                    isBold = false;
                } else if (code == 'l') {
                    isBold = true;
                } else if (code == 'r') {
                    color = defaultColor;
                    isBold = false;
                }
            } else {
                segment.append(c);
            }
        }
        cx += flush(r, isBold ? bold : regular, segment, cx, y, color, draw);
        return cx - x;
    }

    private static float flush(Renderer r, NovaFont font, StringBuilder segment, float x, float y, int color, boolean draw) {
        if (segment.length() == 0) return 0;
        String s = segment.toString();
        segment.setLength(0);
        if (draw) r.text(font, s, x, y, color);
        return r.textWidth(font, s);
    }
}
