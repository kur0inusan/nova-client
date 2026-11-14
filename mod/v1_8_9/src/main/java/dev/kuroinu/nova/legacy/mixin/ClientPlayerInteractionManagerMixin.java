package dev.kuroinu.nova.legacy.mixin;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.Events;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Posts {@link Events.Attack} whenever the local player attacks an entity. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void nova$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target != null && NovaClient.isInitialized()) {
            NovaClient.get().events().post(new Events.Attack(target.getEntityId()));
        }
    }
}
