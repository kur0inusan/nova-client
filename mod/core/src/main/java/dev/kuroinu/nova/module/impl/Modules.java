package dev.kuroinu.nova.module.impl;

import dev.kuroinu.nova.module.ModuleManager;
import dev.kuroinu.nova.module.impl.hud.ArmorStatusHud;
import dev.kuroinu.nova.module.impl.hud.ClockHud;
import dev.kuroinu.nova.module.impl.hud.CoordinatesHud;
import dev.kuroinu.nova.module.impl.hud.CpsHud;
import dev.kuroinu.nova.module.impl.hud.FpsHud;
import dev.kuroinu.nova.module.impl.hud.KeystrokesHud;
import dev.kuroinu.nova.module.impl.hud.PingHud;
import dev.kuroinu.nova.module.impl.hud.PotionEffectsHud;

/** The single place built-in modules are registered. Add new modules here. */
public final class Modules {
    private Modules() {
    }

    public static void registerDefaults(ModuleManager m) {
        m.register(new FpsHud());
        m.register(new CpsHud());
        m.register(new KeystrokesHud());
        m.register(new ArmorStatusHud());
        m.register(new CoordinatesHud());
        m.register(new PotionEffectsHud());
        m.register(new PingHud());
        m.register(new ClockHud());
        m.register(new ClickGuiSettings());
        m.register(new MainMenuSettings());
        m.register(new ClientSettings());
    }
}
