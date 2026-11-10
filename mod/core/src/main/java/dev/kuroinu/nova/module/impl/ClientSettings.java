package dev.kuroinu.nova.module.impl;

import dev.kuroinu.nova.module.Category;
import dev.kuroinu.nova.module.Module;
import dev.kuroinu.nova.setting.BooleanSetting;

/** Always-on module holding client-wide preferences, edited in the ClickGUI like any other module. */
public class ClientSettings extends Module {
    public final BooleanSetting toggleNotifications = add(new BooleanSetting("Toggle notifications", true));

    public ClientSettings() {
        super("client", "Client", "General client preferences", Category.CLIENT);
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
