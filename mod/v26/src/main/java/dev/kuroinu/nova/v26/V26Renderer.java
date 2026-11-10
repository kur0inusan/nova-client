package dev.kuroinu.nova.v26;

import dev.kuroinu.nova.render.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** {@link Renderer} backed by 26.x's GuiGraphicsExtractor. Sub-pixel positions go through the pose matrix. */
public final class V26Renderer implements Renderer {
    private final GuiGraphicsExtractor g;
    private final Font font = Minecraft.getInstance().font;

    public V26Renderer(GuiGraphicsExtractor g) {
        this.g = g;
    }

    @Override
    public void rect(float x, float y, float width, float height, int argb) {
        if (isWhole(x) && isWhole(y) && isWhole(width) && isWhole(height)) {
            g.fill((int) x, (int) y, (int) (x + width), (int) (y + height), argb);
            return;
        }
        // Scale a unit square so fractional sizes and positions render exactly.
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(width, height);
        g.fill(0, 0, 1, 1, argb);
        g.pose().popMatrix();
    }

    @Override
    public void text(String text, float x, float y, int argb, boolean shadow) {
        if (isWhole(x) && isWhole(y)) {
            g.text(font, text, (int) x, (int) y, argb, shadow);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.text(font, text, 0, 0, argb, shadow);
        g.pose().popMatrix();
    }

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int fontHeight() {
        return font.lineHeight;
    }

    @Override
    public void pushScale(float x, float y, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
    }

    @Override
    public void pop() {
        g.pose().popMatrix();
    }

    /**
     * Stub-level for now (1.8.9 is the primary target): enableScissor applies the current pose,
     * so clips are slightly off while a screen is mid open-animation. Rounded corners fall back
     * to the Renderer default (plain rect).
     */
    @Override
    public void pushScissor(float x, float y, float width, float height) {
        g.enableScissor((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.ceil(x + width), (int) Math.ceil(y + height));
    }

    @Override
    public void popScissor() {
        g.disableScissor();
    }

    private static boolean isWhole(float v) {
        return v == (int) v;
    }
}
