package dev.kuroinu.nova.legacy;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.kuroinu.nova.platform.ArmorPiece;
import dev.kuroinu.nova.platform.EffectInfo;
import dev.kuroinu.nova.platform.EntityInfo;
import dev.kuroinu.nova.platform.Platform;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
import dev.kuroinu.nova.ui.NovaScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;

public final class LegacyPlatform implements Platform {
    private static final Logger LOG = LogManager.getLogger("Nova");

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    @Override
    public String getMinecraftVersion() {
        return "1.8.9";
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
        return MinecraftClient.getCurrentFps();
    }

    @Override
    public int getGuiScale() {
        return new net.minecraft.client.util.Window(mc()).getScaleFactor();
    }

    @Override
    public boolean isScreenOpen() {
        return mc().currentScreen != null;
    }

    @Override
    public boolean isInWorld() {
        return mc().player != null && mc().world != null;
    }

    @Override
    public double getPlayerX() {
        return isInWorld() ? mc().player.x : 0;
    }

    @Override
    public double getPlayerY() {
        return isInWorld() ? mc().player.y : 0;
    }

    @Override
    public double getPlayerZ() {
        return isInWorld() ? mc().player.z : 0;
    }

    @Override
    public float getPlayerYaw() {
        return isInWorld() ? mc().player.yaw : 0;
    }

    @Override
    public int getPing() {
        MinecraftClient mc = mc();
        if (mc.player == null || mc.isInSingleplayer()) {
            return -1;
        }
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) {
            return -1;
        }
        PlayerListEntry entry = handler.getPlayerListEntry(mc.player.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    private static final Identifier INVENTORY_TEXTURE = new Identifier("textures/gui/container/inventory.png");
    private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    @Override
    public ArmorPiece[] getArmor() {
        ArmorPiece[] out = new ArmorPiece[4];
        if (!isInWorld()) {
            return out;
        }
        ItemStack[] armor = mc().player.inventory.armor; // index 0 = boots
        for (int i = 0; i < 4; i++) {
            ItemStack stack = armor[3 - i];
            if (stack != null) {
                out[i] = stack.isDamageable()
                        ? new ArmorPiece(stack, stack.getMaxDamage() - stack.getDamage(), stack.getMaxDamage())
                        : new ArmorPiece(stack, -1, 0);
            }
        }
        return out;
    }

    @Override
    public void drawItem(Object stack, float x, float y, float size) {
        if (!(stack instanceof ItemStack)) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(size / 16f, size / 16f, 1f);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        DiffuseLighting.enable();
        mc().getItemRenderer().renderInGuiWithOverrides((ItemStack) stack, 0, 0);
        DiffuseLighting.disable();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public List<EffectInfo> getEffects() {
        List<EffectInfo> out = new ArrayList<EffectInfo>();
        if (!isInWorld()) {
            return out;
        }
        for (StatusEffectInstance inst : mc().player.getStatusEffectInstances()) {
            StatusEffect effect = StatusEffect.STATUS_EFFECTS[inst.getEffectId()];
            if (effect == null) continue;
            int amp = inst.getAmplifier();
            String name = I18n.translate(effect.getTranslationKey()) + (amp > 0 && amp < ROMAN.length ? ROMAN[amp] : "");
            Object icon = effect.hasIcon() ? effect.getIconLevel() : null;
            out.add(new EffectInfo(name, StatusEffect.getFormattedDuration(inst), effect.getColor(), icon));
        }
        return out;
    }

    @Override
    public void drawEffectIcon(Object icon, float x, float y, float size) {
        if (!(icon instanceof Integer)) {
            return;
        }
        int index = (Integer) icon;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(size / 18f, size / 18f, 1f);
        GlStateManager.enableTexture();
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc().getTextureManager().bindTexture(INVENTORY_TEXTURE);
        DrawableHelper.drawTexture(0, 0, index % 8 * 18, 198 + index / 8 * 18, 18, 18, 256, 256);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public boolean isKeyDown(GameKey key) {
        return binding(key).isPressed();
    }

    private static KeyBinding binding(GameKey key) {
        GameOptions o = mc().options;
        switch (key) {
            case FORWARD: return o.forwardKey;
            case BACK: return o.backKey;
            case LEFT: return o.leftKey;
            case RIGHT: return o.rightKey;
            case JUMP: return o.jumpKey;
            case SNEAK: return o.sneakKey;
            case SPRINT: return o.sprintKey;
            case ATTACK: return o.attackKey;
            case USE: return o.useKey;
            default: throw new IllegalArgumentException(String.valueOf(key));
        }
    }

    @Override
    public boolean consumeHotkey(Hotkey hotkey) {
        return NovaKeys.get(hotkey).wasPressed();
    }

    @Override
    public void openScreen(NovaScreen screen) {
        mc().setScreen(screen == null ? null : new NovaScreenWrapper(screen));
    }

    @Override
    public void openVanillaScreen(VanillaScreen screen) {
        MinecraftClient mc = mc();
        net.minecraft.client.gui.screen.Screen parent = mc.currentScreen;
        switch (screen) {
            case SINGLEPLAYER:
                mc.setScreen(new net.minecraft.client.gui.screen.world.SelectWorldScreen(parent));
                break;
            case MULTIPLAYER:
                mc.setScreen(new net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen(parent));
                break;
            case OPTIONS:
                mc.setScreen(new net.minecraft.client.gui.screen.SettingsScreen(parent, mc.options));
                break;
            case LANGUAGE:
                mc.setScreen(new net.minecraft.client.gui.screen.options.LanguageOptionsScreen(parent, mc.options,
                        mc.getLanguageManager()));
                break;
            default:
                break;
        }
    }

    @Override
    public void quitGame() {
        mc().scheduleStop();
    }

    @Override
    public String getPlayerName() {
        return mc().getSession().getUsername();
    }

    @Override
    public void playClickSound() {
        mc().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                new Identifier("gui.button.press"), 1.0f));
    }

    private LegacyServerBrowser serverBrowser;

    @Override
    public dev.kuroinu.nova.platform.ServerBrowser serverBrowser() {
        if (serverBrowser == null) {
            serverBrowser = new LegacyServerBrowser();
        }
        return serverBrowser;
    }

    @Override
    public String getClipboard() {
        String s = net.minecraft.client.gui.screen.Screen.getClipboard();
        return s == null ? "" : s;
    }

    @Override
    public EntityInfo getEntity(int id, float partialTicks) {
        MinecraftClient mc = mc();
        if (mc.world == null) {
            return null;
        }
        Entity e = mc.world.getEntityById(id);
        if (!(e instanceof LivingEntity) || e instanceof ArmorStandEntity || e == mc.player) {
            return null;
        }
        LivingEntity living = (LivingEntity) e;
        EntityInfo.Kind kind = e instanceof PlayerEntity ? EntityInfo.Kind.PLAYER
                : e instanceof Monster ? EntityInfo.Kind.MOB
                : EntityInfo.Kind.ANIMAL;
        return new EntityInfo(id, kind,
                e.prevTickX + (e.x - e.prevTickX) * partialTicks,
                e.prevTickY + (e.y - e.prevTickY) * partialTicks,
                e.prevTickZ + (e.z - e.prevTickZ) * partialTicks,
                e.width, e.height,
                living.isAlive() && living.getHealth() > 0,
                e.isInvisible(),
                e instanceof PlayerEntity && ((PlayerEntity) e).isSpectator());
    }

    @Override
    public int getCrosshairEntityId() {
        BlockHitResult hit = mc().result;
        return hit != null && hit.type == BlockHitResult.Type.ENTITY && hit.entity != null
                ? hit.entity.getEntityId() : -1;
    }

    @Override
    public Object getWorld() {
        return mc().world;
    }

    @Override
    public String keySpace() {
        return "lwjgl2";
    }

    @Override
    public String getKeyName(int keyCode) {
        String name = Keyboard.getKeyName(keyCode);
        return name == null ? "Key " + keyCode : name;
    }
}
