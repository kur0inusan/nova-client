package dev.kuroinu.nova;

import dev.kuroinu.nova.config.ConfigManager;
import dev.kuroinu.nova.event.EventBus;
import dev.kuroinu.nova.event.Events;
import dev.kuroinu.nova.hud.HudManager;
import dev.kuroinu.nova.input.CpsCounter;
import dev.kuroinu.nova.module.Module;
import dev.kuroinu.nova.module.ModuleManager;
import dev.kuroinu.nova.module.impl.ClientSettings;
import dev.kuroinu.nova.module.impl.MainMenuSettings;
import dev.kuroinu.nova.ui.NovaScreen;
import dev.kuroinu.nova.module.impl.Modules;
import dev.kuroinu.nova.ui.ToastManager;
import dev.kuroinu.nova.platform.Platform;
import dev.kuroinu.nova.ui.clickgui.ClickGui;

import java.io.File;

/**
 * Entry point shared by every supported Minecraft version.
 * Each version module provides a {@link Platform} implementation and calls {@link #init}.
 */
public final class NovaClient {
    public static final String NAME = "Nova";
    public static final String MOD_ID = "nova";

    private static NovaClient instance;

    private final Platform platform;
    private final EventBus eventBus = new EventBus();
    private final ModuleManager moduleManager = new ModuleManager(eventBus);
    private final HudManager hudManager = new HudManager(eventBus, moduleManager);
    private final CpsCounter cpsCounter = new CpsCounter(eventBus);
    private final ConfigManager configManager;
    private final ToastManager toastManager = new ToastManager();
    private ClickGui clickGui;

    private NovaClient(Platform platform) {
        this.platform = platform;
        this.configManager = new ConfigManager(new File(platform.getConfigDir(), MOD_ID), moduleManager, platform.keySpace());
    }

    public static synchronized void init(Platform platform) {
        if (instance != null) {
            return;
        }
        instance = new NovaClient(platform);
        instance.start();
    }

    private void start() {
        Modules.registerDefaults(moduleManager);
        configManager.load();

        // The menu key is the only vanilla key binding; everything else is configured in the menu.
        eventBus.subscribe(Events.Tick.class, e -> {
            if (platform.consumeHotkey(Platform.Hotkey.OPEN_MENU) && !platform.isScreenOpen() && platform.isInWorld()) {
                openMenu();
            }
        });

        // Per-module toggle binds, set in the ClickGUI.
        eventBus.subscribe(Events.Key.class, e -> {
            if (!e.pressed || e.inScreen || e.key <= 0) {
                return;
            }
            for (Module m : moduleManager.getModules()) {
                if (m.getBindKey() == e.key && m.isToggleable()) {
                    toggleByUser(m);
                }
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(configManager::save, "nova-config-save"));
        platform.log("Initialized " + moduleManager.getModules().size() + " modules on Minecraft " + platform.getMinecraftVersion());
    }

    /** Toggles a module in response to the player (menu click or bind) and shows a toast if enabled. */
    public void toggleByUser(Module m) {
        m.toggle();
        ClientSettings client = moduleManager.get(ClientSettings.class);
        if (client == null || client.toggleNotifications.get()) {
            toastManager.push(m);
        }
    }

    public void openMenu() {
        openMenu(null);
    }

    /** Opens the ClickGUI; closing it returns to {@code parent} (null = back to the game). */
    public void openMenu(NovaScreen parent) {
        if (clickGui == null) {
            clickGui = new ClickGui();
        }
        platform.openScreen(clickGui.withParent(parent));
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    /** Whether Nova's main menu should replace the vanilla title screen. */
    public boolean useMainMenu() {
        MainMenuSettings s = moduleManager.get(MainMenuSettings.class);
        return s != null && s.isEnabled();
    }

    public static NovaClient get() {
        if (instance == null) {
            throw new IllegalStateException("NovaClient has not been initialized");
        }
        return instance;
    }

    public Platform platform() {
        return platform;
    }

    public EventBus events() {
        return eventBus;
    }

    public ModuleManager modules() {
        return moduleManager;
    }

    public HudManager hud() {
        return hudManager;
    }

    public CpsCounter cps() {
        return cpsCounter;
    }

    public ConfigManager config() {
        return configManager;
    }

    public ToastManager toasts() {
        return toastManager;
    }
}
