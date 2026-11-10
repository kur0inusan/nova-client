package dev.kuroinu.nova.v26;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.Events;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public final class NovaFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NovaClient.init(new V26Platform());

        NovaKeys.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> NovaClient.get().events().post(Events.Tick.INSTANCE));

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "hud"), (graphics, delta) ->
                NovaClient.get().events().post(new Events.Render2D(
                        new V26Renderer(graphics), graphics.guiWidth(), graphics.guiHeight(),
                        delta.getGameTimeDeltaPartialTick(false))));
    }
}
