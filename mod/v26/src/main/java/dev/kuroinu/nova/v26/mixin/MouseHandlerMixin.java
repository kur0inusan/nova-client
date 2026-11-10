package dev.kuroinu.nova.v26.mixin;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.Events;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void nova$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        boolean inScreen = Minecraft.getInstance().gui.screen() != null;
        NovaClient.get().events().post(new Events.MouseButton(info.button(), action == 1, inScreen));
    }
}
