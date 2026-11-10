package dev.kuroinu.nova.ui;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.render.Renderer;

/**
 * Nova's menus are laid out in design units (the handoff's CSS px at 1:1 screen pixels),
 * independent of Minecraft's GUI scale. One unit is {@link #pixelsPerUnit} physical pixels:
 * 1 up to 1080p, growing proportionally on larger screens.
 */
public final class UiScale {
    private UiScale() {
    }

    public static float pixelsPerUnit(float physicalWidth, float physicalHeight) {
        return Math.max(1f, Math.min(physicalWidth / 1920f, physicalHeight / 1080f));
    }

    /**
     * Pushes a transform from GUI coordinates to design units and returns the screen size in
     * units as {width, height}. Pair with {@link Renderer#pop()}.
     */
    public static float[] push(Renderer r, int guiWidth, int guiHeight) {
        return push(r, guiWidth, guiHeight, 1f);
    }

    /** Like {@link #push(Renderer, int, int)} with an extra user scale (e.g. the menu's "Scale" setting). */
    public static float[] push(Renderer r, int guiWidth, int guiHeight, float userScale) {
        int gs = NovaClient.get().platform().getGuiScale();
        float ppu = pixelsPerUnit(guiWidth * gs, guiHeight * gs) * userScale;
        float k = ppu / gs; // GUI coordinates per unit
        r.pushScale(0, 0, k);
        return new float[]{guiWidth / k, guiHeight / k, k};
    }
}
