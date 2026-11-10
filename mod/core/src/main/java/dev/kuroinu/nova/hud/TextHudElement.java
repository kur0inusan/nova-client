package dev.kuroinu.nova.hud;

import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.setting.BooleanSetting;

/** The design's text pill: padding 4x9, radius 4, optional translucent background, mono 13/600. */
public abstract class TextHudElement extends HudElement {
    private static final float PAD_X = 9;
    private static final float PAD_Y = 4;

    protected final BooleanSetting background = add(new BooleanSetting("Background", true));

    protected TextHudElement(String id, String name, String description, Anchor anchor, float x, float y) {
        super(id, name, description, anchor, x, y);
    }

    /** Text to show. Called every frame; keep it cheap. */
    protected abstract String getText(boolean preview);

    protected int getTextColor() {
        return HudStyle.WHITE;
    }

    @Override
    public float getWidth(Renderer r, boolean preview) {
        return r.textWidth(HudStyle.TEXT, getText(preview)) + PAD_X * 2;
    }

    @Override
    public float getHeight(Renderer r, boolean preview) {
        return r.lineHeight(HudStyle.TEXT) + PAD_Y * 2;
    }

    @Override
    public void render(Renderer r, boolean preview) {
        String text = getText(preview);
        if (background.get()) {
            r.roundedRect(0, 0, getWidth(r, preview), getHeight(r, preview), HudStyle.RADIUS, HudStyle.BACKGROUND);
        }
        HudStyle.text(r, HudStyle.TEXT, text, PAD_X, PAD_Y, getTextColor());
    }
}
