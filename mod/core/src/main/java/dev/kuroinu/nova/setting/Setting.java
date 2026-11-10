package dev.kuroinu.nova.setting;

import com.google.gson.JsonElement;

/**
 * A typed, persistable option. The settings UI is generated from these, so a module only
 * needs to declare its settings to get a working configuration screen.
 */
public abstract class Setting<T> {
    private final String name;
    private final T defaultValue;
    protected T value;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public T getDefault() {
        return defaultValue;
    }

    public void reset() {
        set(defaultValue);
    }

    public abstract JsonElement toJson();

    /** Applies a stored value; invalid input must be ignored, keeping the current value. */
    public abstract void fromJson(JsonElement json);
}
