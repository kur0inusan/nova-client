package dev.kuroinu.nova.legacy.mixin;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.EventBus;
import dev.kuroinu.nova.event.Events;
import dev.kuroinu.nova.legacy.LegacyRenderer3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Posts {@link Events.Render3D} after the world (including translucent blocks) and before the hand. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand"))
    private void nova$renderWorld(int pass, float tickDelta, long limitTime, CallbackInfo ci) {
        if (!NovaClient.isInitialized()) {
            return;
        }
        EventBus bus = NovaClient.get().events();
        Entity camera = MinecraftClient.getInstance().getCameraEntity();
        if (camera == null || bus.listenerCount(Events.Render3D.class) == 0) {
            return;
        }
        LegacyRenderer3D renderer = LegacyRenderer3D.INSTANCE;
        renderer.prepare(camera, tickDelta);
        try {
            bus.post(new Events.Render3D(renderer, tickDelta));
        } finally {
            renderer.finish();
        }
    }
}
