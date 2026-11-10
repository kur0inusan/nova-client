package dev.kuroinu.nova.hud;

/**
 * The screen point an element's offset is measured from. Anchoring keeps elements in place
 * relative to the nearest edge when the window or GUI scale changes.
 */
public enum Anchor {
    TOP_LEFT(0, 0), TOP_CENTER(0.5f, 0), TOP_RIGHT(1, 0),
    MIDDLE_LEFT(0, 0.5f), CENTER(0.5f, 0.5f), MIDDLE_RIGHT(1, 0.5f),
    BOTTOM_LEFT(0, 1), BOTTOM_CENTER(0.5f, 1), BOTTOM_RIGHT(1, 1);

    /** Fraction of the screen (and of the element) this anchor sits at. */
    public final float fx;
    public final float fy;

    Anchor(float fx, float fy) {
        this.fx = fx;
        this.fy = fy;
    }

    /** Picks the anchor for the screen third the element's center falls in. */
    public static Anchor nearest(float centerX, float centerY, float screenW, float screenH) {
        int col = centerX < screenW / 3f ? 0 : (centerX < screenW * 2f / 3f ? 1 : 2);
        int row = centerY < screenH / 3f ? 0 : (centerY < screenH * 2f / 3f ? 1 : 2);
        return values()[row * 3 + col];
    }
}
