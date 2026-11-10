package dev.kuroinu.nova.render.font;

/**
 * A typeface at a size in layout units (the design's CSS px). The renderer rasterizes it at
 * whatever physical pixel size the current transform implies, so text stays crisp at any scale.
 */
public final class NovaFont {
    public final FontFace face;
    public final float size;

    private NovaFont(FontFace face, float size) {
        this.face = face;
        this.size = size;
    }

    public static NovaFont of(FontFace face, float size) {
        return new NovaFont(face, size);
    }

    public static NovaFont regular(float size) {
        return of(FontFace.OUTFIT_REGULAR, size);
    }

    public static NovaFont medium(float size) {
        return of(FontFace.OUTFIT_MEDIUM, size);
    }

    public static NovaFont semibold(float size) {
        return of(FontFace.OUTFIT_SEMIBOLD, size);
    }

    public static NovaFont bold(float size) {
        return of(FontFace.OUTFIT_BOLD, size);
    }

    public static NovaFont mono(float size) {
        return of(FontFace.MONO_REGULAR, size);
    }

    public static NovaFont monoSemibold(float size) {
        return of(FontFace.MONO_SEMIBOLD, size);
    }
}
