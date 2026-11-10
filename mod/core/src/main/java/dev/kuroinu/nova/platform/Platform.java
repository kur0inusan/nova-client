package dev.kuroinu.nova.platform;

import dev.kuroinu.nova.ui.NovaScreen;

import java.io.File;

/**
 * Game-state access that differs between Minecraft versions.
 * Keep this interface small; add methods only when a module needs them.
 */
public interface Platform {
    String getMinecraftVersion();

    File getConfigDir();

    void log(String message);

    int getFps();

    /** Minecraft's current GUI scale factor (GUI coordinate to framebuffer pixels). */
    int getGuiScale();

    /** True while any GUI screen (inventory, chat, menu, ...) is open. */
    boolean isScreenOpen();

    /** True when a world is loaded and the player exists. */
    boolean isInWorld();

    double getPlayerX();

    double getPlayerY();

    double getPlayerZ();

    /** Player yaw in degrees, as reported by the game (unwrapped). */
    float getPlayerYaw();

    /** Latency to the current server in ms, or -1 when unknown (e.g. singleplayer). */
    int getPing();

    /** Worn armor from helmet to boots (length 4); empty slots are null. */
    ArmorPiece[] getArmor();

    /**
     * Draws an item (from {@link ArmorPiece#stack}) with its top-left at (x, y) in the renderer's
     * current coordinate space, {@code size} units square.
     */
    void drawItem(Object stack, float x, float y, float size);

    /** Active status effects of the player, in display order. */
    java.util.List<EffectInfo> getEffects();

    /** Draws an effect's icon (from {@link EffectInfo#icon}); no-op when the version has none. */
    void drawEffectIcon(Object icon, float x, float y, float size);

    /** Whether the given key binding is currently held. */
    boolean isKeyDown(GameKey key);

    /**
     * Returns true once per press of the given hotkey. Hotkeys are registered as vanilla key
     * bindings so they can be rebound in Options &gt; Controls. Only the menu key lives there;
     * every other setting (including per-module binds) is configured in the ClickGUI.
     */
    boolean consumeHotkey(Hotkey hotkey);

    /** Shows a Nova screen, or returns to the game when {@code screen} is null. */
    void openScreen(NovaScreen screen);

    /**
     * Identifies the key-code space of {@link dev.kuroinu.nova.event.Events.Key} codes on this
     * version (e.g. "lwjgl2", "sdl3"). Per-module binds are stored per key space so one config
     * file can be shared between versions without binds turning into the wrong keys.
     */
    String keySpace();

    /** Human-readable name of a key code from this version's key space. */
    String getKeyName(int keyCode);

    /** Opens a vanilla screen with the current screen as its parent (so "Done"/"Back" returns here). */
    void openVanillaScreen(VanillaScreen screen);

    void quitGame();

    /** Name of the logged-in account. */
    String getPlayerName();

    /** The vanilla button click sound. */
    void playClickSound();

    /** The multiplayer server list, or null when this version doesn't support Nova's Multiplayer screen yet. */
    ServerBrowser serverBrowser();

    /** Current clipboard text ("" when empty or unavailable). */
    String getClipboard();

    enum VanillaScreen {
        SINGLEPLAYER, MULTIPLAYER, OPTIONS, LANGUAGE
    }

    enum GameKey {
        FORWARD, BACK, LEFT, RIGHT, JUMP, SNEAK, SPRINT, ATTACK, USE
    }

    /** Rebindable vanilla key bindings. Intentionally just the menu key. */
    enum Hotkey {
        /** Default: Right Shift. */
        OPEN_MENU("key.nova.open_menu");

        public final String translationKey;

        Hotkey(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
