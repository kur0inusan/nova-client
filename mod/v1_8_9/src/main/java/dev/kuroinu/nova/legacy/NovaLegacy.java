package dev.kuroinu.nova.legacy;

import dev.kuroinu.nova.NovaClient;
import net.fabricmc.api.ClientModInitializer;

public final class NovaLegacy implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NovaClient.init(new LegacyPlatform());
    }
}
