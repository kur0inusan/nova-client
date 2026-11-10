package dev.kuroinu.nova.module.impl.hud;

import dev.kuroinu.nova.hud.Anchor;
import dev.kuroinu.nova.hud.TextHudElement;
import dev.kuroinu.nova.setting.ModeSetting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockHud extends TextHudElement {
    private final ModeSetting format = add(new ModeSetting("Format", "24h", "24h", "12h"));
    private final SimpleDateFormat f24 = new SimpleDateFormat("HH:mm", Locale.ROOT);
    private final SimpleDateFormat f12 = new SimpleDateFormat("h:mm a", Locale.ENGLISH);

    public ClockHud() {
        super("clock", "Clock", "Real-world time in 24h or 12h format", Anchor.TOP_RIGHT, -16, 16);
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    protected String getText(boolean preview) {
        return (format.is("12h") ? f12 : f24).format(new Date());
    }
}
