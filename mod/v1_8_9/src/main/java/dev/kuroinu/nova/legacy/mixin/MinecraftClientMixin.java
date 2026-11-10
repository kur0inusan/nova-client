package dev.kuroinu.nova.legacy.mixin;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.Events;
import dev.kuroinu.nova.legacy.NovaScreenWrapper;
import dev.kuroinu.nova.ui.MainMenu;
import dev.kuroinu.nova.ui.MultiplayerMenu;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    public ClientWorld world;

    @Inject(method = "tick", at = @At("TAIL"))
    private void nova$tick(CallbackInfo ci) {
        NovaClient.get().events().post(Events.Tick.INSTANCE);
    }

    /**
     * Swaps the vanilla title screen for Nova's main menu. Covers both explicit TitleScreen
     * instances and setScreen(null) outside a world (which vanilla turns into a TitleScreen).
     */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen nova$replaceTitleScreen(Screen screen) {
        if (!NovaClient.isInitialized() || !NovaClient.get().useMainMenu()) {
            return screen;
        }
        boolean title = screen instanceof TitleScreen || (screen == null && world == null);
        if (title) {
            return new NovaScreenWrapper(new MainMenu());
        }
        // e.g. "Back to server list" after a disconnect: show Nova's server list instead.
        if (screen instanceof MultiplayerScreen) {
            return new NovaScreenWrapper(new MultiplayerMenu().withParent(new MainMenu()));
        }
        return screen;
    }
}
