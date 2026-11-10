package dev.kuroinu.nova.v26;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kuroinu.nova.platform.Platform;
import dev.kuroinu.nova.ui.NovaScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class V26Platform implements Platform {
    private static final Logger LOG = LoggerFactory.getLogger("Nova");

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public String getMinecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("26.x");
    }

    @Override
    public File getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().toFile();
    }

    @Override
    public void log(String message) {
        LOG.info(message);
    }

    @Override
    public int getFps() {
        return mc().getFps();
    }

    @Override
    public int getGuiScale() {
        return mc().getWindow().getGuiScale();
    }

    @Override
    public boolean isScreenOpen() {
        return mc().gui.screen() != null;
    }

    @Override
    public boolean isInWorld() {
        return mc().player != null && mc().level != null;
    }

    @Override
    public double getPlayerX() {
        return isInWorld() ? mc().player.getX() : 0;
    }

    @Override
    public double getPlayerY() {
        return isInWorld() ? mc().player.getY() : 0;
    }

    @Override
    public double getPlayerZ() {
        return isInWorld() ? mc().player.getZ() : 0;
    }

    @Override
    public float getPlayerYaw() {
        return isInWorld() ? mc().player.getYRot() : 0;
    }

    @Override
    public int getPing() {
        Minecraft mc = mc();
        if (mc.player == null || mc.hasSingleplayerServer()) {
            return -1;
        }
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) {
            return -1;
        }
        PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    // Stubs until 26.x is brought up to date (1.8.9 is the primary target).
    // The main menu is not hooked in on 26.x yet, so these are only reachable from future screens.
    @Override
    public void openVanillaScreen(VanillaScreen screen) {
    }

    @Override
    public void quitGame() {
        mc().stop();
    }

    @Override
    public String getPlayerName() {
        return mc().getUser().getName();
    }

    @Override
    public void playClickSound() {
    }

    @Override
    public dev.kuroinu.nova.platform.ServerBrowser serverBrowser() {
        return null; // Nova's server list isn't implemented for 26.x yet
    }

    @Override
    public String getClipboard() {
        return "";
    }

    @Override
    public dev.kuroinu.nova.platform.ArmorPiece[] getArmor() {
        return new dev.kuroinu.nova.platform.ArmorPiece[4];
    }

    @Override
    public void drawItem(Object stack, float x, float y, float size) {
    }

    @Override
    public java.util.List<dev.kuroinu.nova.platform.EffectInfo> getEffects() {
        return java.util.Collections.emptyList();
    }

    @Override
    public void drawEffectIcon(Object icon, float x, float y, float size) {
    }

    @Override
    public boolean isKeyDown(GameKey key) {
        return mapping(key).isDown();
    }

    private static KeyMapping mapping(GameKey key) {
        var o = mc().options;
        return switch (key) {
            case FORWARD -> o.keyUp;
            case BACK -> o.keyDown;
            case LEFT -> o.keyLeft;
            case RIGHT -> o.keyRight;
            case JUMP -> o.keyJump;
            case SNEAK -> o.keyShift;
            case SPRINT -> o.keySprint;
            case ATTACK -> o.keyAttack;
            case USE -> o.keyUse;
        };
    }

    @Override
    public void openScreen(NovaScreen screen) {
        mc().gui.setScreen(screen == null ? null : new NovaScreenWrapper(screen));
    }

    @Override
    public String keySpace() {
        return "sdl3";
    }

    @Override
    public String getKeyName(int keyCode) {
        return InputConstants.Type.KEYBOARD.getOrCreate(keyCode).getDisplayName().getString();
    }

    @Override
    public boolean consumeHotkey(Hotkey hotkey) {
        return NovaKeys.get(hotkey).consumeClick();
    }
}
