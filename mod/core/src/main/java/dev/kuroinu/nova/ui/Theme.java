package dev.kuroinu.nova.ui;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.module.impl.ClickGuiSettings;

/**
 * Design tokens from the Nova ClickGUI handoff (docs/design). Hex values are the handoff's sRGB
 * conversions of its OKLCH sources; "@ N%" alphas are folded into the ARGB constants.
 */
public final class Theme {
    /** Accent presets: oklch(0.74 0.16 h). Order matches the theme dots. */
    public enum Preset {
        NOVA("Nova", 0xFFAB97FF),
        CRIMSON("Crimson", 0xFFFF7C80),
        MINT("Mint", 0xFF23C987),
        ICE("Ice", 0xFF00BBFB),
        AMBER("Amber", 0xFFE49900);

        public final String displayName;
        public final int accent;

        Preset(String displayName, int accent) {
            this.displayName = displayName;
            this.accent = accent;
        }

        public static Preset byName(String name) {
            for (Preset p : values()) {
                if (p.displayName.equals(name)) return p;
            }
            return NOVA;
        }
    }

    /** Color-setting palette: oklch(0.72 0.17 h) for h = 290, 250, 200, 160, 120, 75, 40, 20, plus white. */
    public static final int[] PALETTE = {
            0xFFA58FFF, 0xFF40A9FF, 0xFF00C2CE, 0xFF00C47F, 0xFF98B305,
            0xFFE19100, 0xFFFB794A, 0xFFFD7277, 0xFFF5F5F5,
    };

    // Surfaces
    public static final int OVERLAY = 0x6608080C;
    public static final int WINDOW = 0xED08090C;
    public static final int SIDEBAR = 0xE6050608;
    public static final int TOAST = 0xF20A0B0F;
    public static final int CARD = 0xE6111115;
    public static final int CARD_ON = 0xE614151F;
    public static final int INPUT = 0xFF111116;
    public static final int CHIP = 0xFF14141A;
    public static final int TRACK = 0xFF28282E;
    public static final int SWITCH_OFF = 0xFF323238;
    public static final int KNOB = 0xFFF5F5F5;
    public static final int ON_ACCENT = 0xFF0A0B0F;

    // Text
    public static final int TEXT = 0xFFEEEEF2;
    public static final int TEXT_2 = 0xFFCCCDD4;
    public static final int TEXT_3 = 0xFFC2C3CB;
    public static final int MUTED = 0xFF8E8F95;
    public static final int MUTED_2 = 0xFF7F8086;
    public static final int DISABLED = 0xFF626369;

    // Borders (white at low alpha)
    public static final int BORDER_4 = 0x0AFFFFFF;
    public static final int BORDER_5 = 0x0DFFFFFF;
    public static final int BORDER_6 = 0x0FFFFFFF;
    public static final int BORDER_7 = 0x12FFFFFF;
    public static final int BORDER_8 = 0x14FFFFFF;
    public static final int BORDER_20 = 0x33FFFFFF;

    private Theme() {
    }

    public static Preset preset() {
        ClickGuiSettings s = NovaClient.get().modules().get(ClickGuiSettings.class);
        return s == null ? Preset.NOVA : Preset.byName(s.theme.get());
    }

    public static int accent() {
        return preset().accent;
    }

    /** Accent at 15% alpha ("soft" variant). */
    public static int accentSoft() {
        return (accent() & 0x00FFFFFF) | 0x26000000;
    }
}
