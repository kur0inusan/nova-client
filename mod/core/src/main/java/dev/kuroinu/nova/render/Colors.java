package dev.kuroinu.nova.render;

public final class Colors {
    private Colors() {
    }

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    public static int withAlpha(int argb, int alpha) {
        return (clamp(alpha) << 24) | (argb & 0x00FFFFFF);
    }

    /** Linear blend between two ARGB colors, t in [0,1]. */
    public static int lerp(int from, int to, float t) {
        if (t <= 0) return from;
        if (t >= 1) return to;
        int a = (int) (((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
        int r = (int) (((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t));
        int g = (int) (((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t));
        int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
        return argb(a, r, g, b);
    }

    /** HSB (each 0..1) to opaque RGB. */
    public static int hsb(float hue, float sat, float bri) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        int sector = (int) Math.floor(h);
        float f = h - sector;
        float p = bri * (1 - sat);
        float q = bri * (1 - sat * f);
        float t = bri * (1 - sat * (1 - f));
        float r, g, b;
        switch (sector) {
            case 0: r = bri; g = t; b = p; break;
            case 1: r = q; g = bri; b = p; break;
            case 2: r = p; g = bri; b = t; break;
            case 3: r = p; g = q; b = bri; break;
            case 4: r = t; g = p; b = bri; break;
            default: r = bri; g = p; b = q; break;
        }
        return argb(255, Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    /** RGB part of an ARGB color to {hue, saturation, brightness}, each 0..1. */
    public static float[] toHsb(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) h = ((g - b) / d) % 6f;
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h /= 6f;
            if (h < 0) h += 1f;
        }
        float s = max == 0 ? 0 : d / max;
        return new float[]{h, s, max};
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
