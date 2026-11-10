package dev.kuroinu.nova.module.impl.hud;

import dev.kuroinu.nova.hud.Anchor;
import dev.kuroinu.nova.hud.TextHudElement;
import dev.kuroinu.nova.platform.Platform;
import dev.kuroinu.nova.setting.BooleanSetting;

public class CoordinatesHud extends TextHudElement {
    private static final String[] DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private final BooleanSetting facing = add(new BooleanSetting("Facing direction", true));

    public CoordinatesHud() {
        super("coordinates", "Coordinates", "Your block position and the direction you face", Anchor.TOP_LEFT, 16, 84);
    }

    @Override
    protected String getText(boolean preview) {
        Platform p = nova().platform();
        String text;
        float yaw;
        if (p.isInWorld()) {
            text = "XYZ " + (int) Math.floor(p.getPlayerX()) + ", " + (int) Math.floor(p.getPlayerY()) + ", "
                    + (int) Math.floor(p.getPlayerZ());
            yaw = p.getPlayerYaw();
        } else {
            text = "XYZ 128, 64, -312";
            yaw = 180;
        }
        return facing.get() ? text + "  " + directionName(yaw) : text;
    }

    /** Minecraft yaw: 0 = south, 90 = west, 180 = north, 270 = east. */
    public static String directionName(float yaw) {
        float y = ((yaw % 360) + 360) % 360;
        int idx = (int) Math.floor((y + 22.5f) / 45f) % 8;
        return DIRECTIONS[idx];
    }
}
