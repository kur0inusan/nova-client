package dev.kuroinu.nova.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Free-form single-line text, edited with a text field in the menu. */
public class TextSetting extends Setting<String> {
    private final int maxLength;

    public TextSetting(String name, String defaultValue, int maxLength) {
        super(name, defaultValue);
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public void set(String value) {
        if (value == null) return;
        super.set(value.length() > maxLength ? value.substring(0, maxLength) : value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            set(json.getAsString());
        }
    }
}
