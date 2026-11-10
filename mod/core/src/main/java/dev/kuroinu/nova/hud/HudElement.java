package dev.kuroinu.nova.hud;

import com.google.gson.JsonObject;
import dev.kuroinu.nova.module.Category;
import dev.kuroinu.nova.module.Module;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.setting.NumberSetting;

/**
 * A movable on-screen element. Sizes and positions are in design units (screen pixels at 1080p,
 * see {@link dev.kuroinu.nova.ui.UiScale}). Position is an {@link Anchor} plus an offset, so the
 * element keeps its place relative to the nearest screen edge when the window is resized.
 */
public abstract class HudElement extends Module {
    /** Bump when the meaning of stored layouts changes; older layouts are then ignored. */
    public static final int LAYOUT_VERSION = 2;

    protected final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.5, 0.05).withUnit("x"));

    private final Anchor defaultAnchor;
    private final float defaultX;
    private final float defaultY;
    private Anchor anchor;
    private float offsetX;
    private float offsetY;

    protected HudElement(String id, String name, String description, Anchor anchor, float offsetX, float offsetY) {
        super(id, name, description, Category.HUD);
        this.defaultAnchor = anchor;
        this.defaultX = offsetX;
        this.defaultY = offsetY;
        resetPosition();
    }

    /** Unscaled width in units. May be 0 when there is nothing to show. */
    public abstract float getWidth(Renderer r, boolean preview);

    /** Unscaled height in units. */
    public abstract float getHeight(Renderer r, boolean preview);

    /**
     * Draws the element at the origin of an already translated/scaled space.
     * {@code preview} is true inside the HUD editor, where elements should show sample content.
     */
    public abstract void render(Renderer r, boolean preview);

    public float getScale() {
        return scale.floatValue();
    }

    public float scaledWidth(Renderer r, boolean preview) {
        return getWidth(r, preview) * getScale();
    }

    public float scaledHeight(Renderer r, boolean preview) {
        return getHeight(r, preview) * getScale();
    }

    /** Screen-space top-left X of the element. */
    public float getX(Renderer r, float screenW, boolean preview) {
        return screenW * anchor.fx - scaledWidth(r, preview) * anchor.fx + offsetX;
    }

    /** Screen-space top-left Y of the element. */
    public float getY(Renderer r, float screenH, boolean preview) {
        return screenH * anchor.fy - scaledHeight(r, preview) * anchor.fy + offsetY;
    }

    /** Moves the element so its top-left lands at (x, y), clamped on screen and re-anchored to the nearest region. */
    public void setPosition(Renderer r, float x, float y, float screenW, float screenH) {
        float w = scaledWidth(r, true);
        float h = scaledHeight(r, true);
        x = Math.max(0, Math.min(screenW - w, x));
        y = Math.max(0, Math.min(screenH - h, y));
        anchor = Anchor.nearest(x + w / 2f, y + h / 2f, screenW, screenH);
        offsetX = x - (screenW * anchor.fx - w * anchor.fx);
        offsetY = y - (screenH * anchor.fy - h * anchor.fy);
    }

    public void resetPosition() {
        anchor = defaultAnchor;
        offsetX = defaultX;
        offsetY = defaultY;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    public void writeLayout(JsonObject json) {
        json.addProperty("anchor", anchor.name());
        json.addProperty("x", offsetX);
        json.addProperty("y", offsetY);
    }

    public void readLayout(JsonObject json) {
        try {
            if (json.has("anchor")) {
                anchor = Anchor.valueOf(json.get("anchor").getAsString());
            }
            if (json.has("x")) {
                offsetX = json.get("x").getAsFloat();
            }
            if (json.has("y")) {
                offsetY = json.get("y").getAsFloat();
            }
        } catch (RuntimeException ignored) {
            // malformed layout: keep defaults
        }
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
