package dev.kuroinu.nova.v26.mixin;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.Events;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    /** GLFW actions: 0 = release, 1 = press, 2 = repeat (ignored). */
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void nova$onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 2) {
            return;
        }
        boolean inScreen = Minecraft.getInstance().gui.screen() != null;        NovaClient.get().events().post(new Events.Key(event.key(), action == 1, inScreen));
    }
}
