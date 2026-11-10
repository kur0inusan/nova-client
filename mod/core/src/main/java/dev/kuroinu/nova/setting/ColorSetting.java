package dev.kuroinu.nova.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** ARGB color, stored as "#AARRGGBB". */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, int defaultArgb) {
        super(name, defaultArgb);
    }

    public int argb() {
        return get();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(String.format("#%08X", get()));
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
            return;
        }
        String s = json.getAsString();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 8) {
            return;
        }
        try {
            set((int) Long.parseLong(s, 16));
        } catch (NumberFormatException ignored) {
            // keep current value
        }
    }
}
