package dev.kuroinu.nova.module.impl.hud;

import dev.kuroinu.nova.hud.Anchor;
import dev.kuroinu.nova.hud.HudStyle;
import dev.kuroinu.nova.hud.TextHudElement;
import dev.kuroinu.nova.setting.ColorSetting;
import dev.kuroinu.nova.setting.TextSetting;

public class FpsHud extends TextHudElement {
    private final ColorSetting textColor = add(new ColorSetting("Text color", HudStyle.WHITE));
    private final TextSetting format = add(new TextSetting("Format", "{fps} FPS", 32));

    public FpsHud() {
        super("fps", "FPS", "Frames per second, with a customizable format", Anchor.TOP_LEFT, 16, 16);
    }

    @Override
    protected String getText(boolean preview) {
        return format.get().replace("{fps}", String.valueOf(nova().platform().getFps()));
    }

    @Override
    protected int getTextColor() {
        return textColor.argb();
    }
}
