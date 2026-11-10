package dev.kuroinu.nova.v26;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.platform.Platform.Hotkey;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

/** Registers Nova's hotkeys as vanilla key mappings (rebindable under Options > Controls). */
public final class NovaKeys {
    private static final Map<Hotkey, KeyMapping> MAPPINGS = new EnumMap<>(Hotkey.class);

    private NovaKeys() {
    }

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "main"));
        for (Hotkey hotkey : Hotkey.values()) {
            MAPPINGS.put(hotkey, KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(hotkey.translationKey, InputConstants.Type.KEYBOARD, defaultKey(hotkey), category)));
        }
    }

    private static int defaultKey(Hotkey hotkey) {
        return switch (hotkey) {
            case OPEN_MENU -> InputConstants.KEY_RSHIFT;
        };
    }

    static KeyMapping get(Hotkey hotkey) {
        return MAPPINGS.get(hotkey);
    }
}
