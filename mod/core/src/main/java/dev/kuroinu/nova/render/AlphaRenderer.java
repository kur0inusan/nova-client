package dev.kuroinu.nova.render;

import dev.kuroinu.nova.render.font.NovaFont;

/** Decorator that multiplies the alpha of everything drawn, for fading whole screens in and out. */
public final class AlphaRenderer implements Renderer {
    private final Renderer base;
    private float alpha = 1f;

    public AlphaRenderer(Renderer base) {
        this.base = base;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
    }

    private int apply(int argb) {
        if (alpha >= 1f) return argb;
        return Colors.withAlpha(argb, Math.round(Colors.alpha(argb) * alpha));
    }

    @Override
    public void rect(float x, float y, float width, float height, int argb) {
        base.rect(x, y, width, height, apply(argb));
    }

    @Override
    public void roundedRect(float x, float y, float width, float height, float radius, int argb) {
        base.roundedRect(x, y, width, height, radius, apply(argb));
    }

    @Override
    public void text(String text, float x, float y, int argb, boolean shadow) {
        int c = apply(argb);
        // 1.8.9's font renderer treats alpha < 4 as fully opaque, so skip nearly invisible text.
        if (Colors.alpha(c) < 8) return;
        base.text(text, x, y, c, shadow && alpha > 0.6f);
    }

    @Override
    public void roundedOutline(float x, float y, float width, float height, float radius, float thickness, int argb) {
        base.roundedOutline(x, y, width, height, radius, thickness, apply(argb));
    }

    @Override
    public void text(NovaFont font, String text, float x, float y, int argb) {
        base.text(font, text, x, y, apply(argb));
    }

    @Override
    public float textWidth(NovaFont font, String text) {
        return base.textWidth(font, text);
    }

    @Override
    public float lineHeight(NovaFont font) {
        return base.lineHeight(font);
    }

    @Override
    public int textWidth(String text) {
        return base.textWidth(text);
    }

    @Override
    public int fontHeight() {
        return base.fontHeight();
    }

    @Override
    public void pushScale(float x, float y, float scale) {
        base.pushScale(x, y, scale);
    }

    @Override
    public void pop() {
        base.pop();
    }

    @Override
    public void pushScissor(float x, float y, float width, float height) {
        base.pushScissor(x, y, width, height);
    }

    @Override
    public void popScissor() {
        base.popScissor();
    }
}
