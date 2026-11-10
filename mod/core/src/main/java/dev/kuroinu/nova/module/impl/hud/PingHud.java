package dev.kuroinu.nova.module.impl.hud;

import dev.kuroinu.nova.hud.Anchor;
import dev.kuroinu.nova.hud.TextHudElement;

public class PingHud extends TextHudElement {
    public PingHud() {
        super("ping", "Ping", "Latency to the server you're playing on", Anchor.TOP_LEFT, 16, 118);
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    protected String getText(boolean preview) {
        int ping = nova().platform().getPing();
        if (ping < 0) {
            return preview ? "23 ms" : "-- ms";
        }
        return ping + " ms";
    }
}
