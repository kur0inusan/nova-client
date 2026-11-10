package dev.kuroinu.nova.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = Collections.unmodifiableList(Arrays.asList(modes));
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default mode '" + defaultValue + "' is not one of " + this.modes);
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return get().equals(mode);
    }

    @Override
    public void set(String value) {
        if (modes.contains(value)) {
            super.set(value);
        }
    }

    public void cycle(boolean forward) {
        int i = modes.indexOf(get());
        int n = modes.size();
        set(modes.get(((forward ? i + 1 : i - 1) % n + n) % n));
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
