package dev.kuroinu.nova.legacy.mixin;

import dev.kuroinu.nova.legacy.NovaKeys;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

/** Adds Nova's key bindings to the controls list before options.txt is read, so rebinds persist. */
@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
    @Shadow
    public KeyBinding[] allKeys;

    @Inject(method = "load", at = @At("HEAD"))
    private void nova$addKeys(CallbackInfo ci) {
        if (allKeys == null) {
            return;
        }
        List<KeyBinding> current = Arrays.asList(allKeys);
        KeyBinding[] extra = NovaKeys.all();
        if (current.contains(extra[0])) {
            return;
        }
        KeyBinding[] merged = Arrays.copyOf(allKeys, allKeys.length + extra.length);
        System.arraycopy(extra, 0, merged, allKeys.length, extra.length);
        allKeys = merged;
    }
}
