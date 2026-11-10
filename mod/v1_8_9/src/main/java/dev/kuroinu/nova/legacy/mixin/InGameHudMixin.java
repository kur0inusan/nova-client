package dev.kuroinu.nova.legacy.mixin;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.Events;
import dev.kuroinu.nova.legacy.LegacyRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void nova$render(float tickDelta, CallbackInfo ci) {
        Window window = new Window(MinecraftClient.getInstance());
        NovaClient.get().events().post(new Events.Render2D(
                LegacyRenderer.INSTANCE, window.getWidth(), window.getHeight(), tickDelta));
    }
}
