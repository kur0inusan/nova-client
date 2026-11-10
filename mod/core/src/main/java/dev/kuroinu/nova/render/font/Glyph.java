package dev.kuroinu.nova.render.font;

/** One rasterized character. Pixel units at the atlas' pixel size; offsets are relative to the pen on the baseline. */
public final class Glyph {
    public final GlyphAtlas.Page page;
    public final int texX, texY, width, height;
    public final int xOffset, yOffset;
    public final float advance;

    Glyph(GlyphAtlas.Page page, int texX, int texY, int width, int height, int xOffset, int yOffset, float advance) {
        this.page = page;
        this.texX = texX;
        this.texY = texY;
        this.width = width;
        this.height = height;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.advance = advance;
    }

    public boolean isEmpty() {
        return width == 0 || height == 0;
    }
}
