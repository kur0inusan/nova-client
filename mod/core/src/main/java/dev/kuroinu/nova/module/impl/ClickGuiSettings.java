package dev.kuroinu.nova.module.impl;

import dev.kuroinu.nova.module.Category;
import dev.kuroinu.nova.module.Module;
import dev.kuroinu.nova.setting.ModeSetting;
import dev.kuroinu.nova.setting.NumberSetting;
import dev.kuroinu.nova.ui.Theme;

/**
 * Menu appearance. The open key itself is a vanilla key binding (Options &gt; Controls), by design:
 * it is the one setting that must stay reachable even if the menu can't be opened.
 */
public class ClickGuiSettings extends Module {
    public final ModeSetting theme = add(new ModeSetting("Theme", Theme.Preset.NOVA.displayName, presetNames()));
    /** Multiplies the whole menu (text, window, controls) on top of the automatic resolution scaling. */
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.1, 0.8, 1.6, 0.05).withUnit("x"));
    public final ModeSetting layout = add(new ModeSetting("Layout", "List", "List", "Grid"));

    public ClickGuiSettings() {
        super("clickgui", "ClickGUI", "Menu theme, size and layout", Category.CLIENT);
    }

    private static String[] presetNames() {
        Theme.Preset[] presets = Theme.Preset.values();
        String[] names = new String[presets.length];
        for (int i = 0; i < presets.length; i++) {
            names[i] = presets[i].displayName;
        }
        return names;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public boolean isToggleable() {
        return false;
    }
}
