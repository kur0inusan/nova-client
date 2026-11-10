package dev.kuroinu.nova.legacy.mixin;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.Events;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.8.9 routes every in-game keyboard and mouse event through KeyBinding.setKeyPressed;
 * mouse buttons arrive as (button - 100). Keyboard codes are posted as raw LWJGL2 codes
 * (key space "lwjgl2").
 */
@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Inject(method = "setKeyPressed", at = @At("HEAD"))
    private static void nova$onInput(int code, boolean pressed, CallbackInfo ci) {
        if (code == 0) {
            return;
        }
        boolean inScreen = MinecraftClient.getInstance().currentScreen != null;
        if (code < 0) {
            NovaClient.get().events().post(new Events.MouseButton(code + 100, pressed, inScreen));
        } else {
            NovaClient.get().events().post(new Events.Key(code, pressed, inScreen));
        }
    }
}
