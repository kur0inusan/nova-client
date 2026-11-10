package dev.kuroinu.nova.module.impl.hud;

import dev.kuroinu.nova.hud.Anchor;
import dev.kuroinu.nova.hud.TextHudElement;
import dev.kuroinu.nova.setting.ModeSetting;

public class CpsHud extends TextHudElement {
    private final ModeSetting layout = add(new ModeSetting("Layout", "L | R", "L | R", "Combined"));

    public CpsHud() {
        super("cps", "CPS", "Clicks per second for left and right mouse buttons", Anchor.TOP_LEFT, 16, 50);
    }

    @Override
    protected String getText(boolean preview) {
        int l = nova().cps().getLeft();
        int r = nova().cps().getRight();
        if (layout.is("Combined")) {
            return (l + r) + " CPS";
        }
        return l + " | " + r + " CPS";
    }
}
