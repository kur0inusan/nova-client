package dev.kuroinu.nova.module.impl;

import dev.kuroinu.nova.module.Category;
import dev.kuroinu.nova.module.Module;

/** When enabled, Nova's main menu replaces the vanilla title screen (versions that support it). */
public class MainMenuSettings extends Module {
    public MainMenuSettings() {
        super("mainmenu", "Main Menu", "Replace the title screen with Nova's main menu", Category.CLIENT);
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
