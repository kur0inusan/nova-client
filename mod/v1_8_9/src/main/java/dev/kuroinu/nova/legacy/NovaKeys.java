package dev.kuroinu.nova.legacy;

import dev.kuroinu.nova.platform.Platform.Hotkey;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.EnumMap;
import java.util.Map;

/**
 * Nova's vanilla key bindings (just the menu key). {@link dev.kuroinu.nova.legacy.mixin.GameOptionsMixin}
 * appends them to GameOptions.allKeys so they appear (and persist) under Options &gt; Controls.
 */
public final class NovaKeys {
    public static final String CATEGORY = "key.category.nova.main";
    private static final Map<Hotkey, KeyBinding> BINDINGS = new EnumMap<Hotkey, KeyBinding>(Hotkey.class);

    static {
        for (Hotkey hotkey : Hotkey.values()) {
            BINDINGS.put(hotkey, new KeyBinding(hotkey.translationKey, defaultKey(hotkey), CATEGORY));
        }
    }

    private NovaKeys() {
    }

    private static int defaultKey(Hotkey hotkey) {
        switch (hotkey) {
            case OPEN_MENU: return Keyboard.KEY_RSHIFT;
            default: return Keyboard.KEY_NONE;
        }
    }

    public static KeyBinding get(Hotkey hotkey) {
        return BINDINGS.get(hotkey);
    }

    public static KeyBinding[] all() {
        return BINDINGS.values().toArray(new KeyBinding[0]);
    }
}
