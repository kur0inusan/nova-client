package dev.kuroinu.nova.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private String unit = "";

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double value) {
        double v = Math.max(min, Math.min(max, value));
        if (step > 0) {
            v = min + Math.round((v - min) / step) * step;
            v = Math.max(min, Math.min(max, v));
        }
        super.set(v);
    }

    /** Suffix shown after the value in the menu, e.g. "x", "px", "%". */
    public NumberSetting withUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public String getUnit() {
        return unit;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public float floatValue() {
        return get().floatValue();
    }

    public int intValue() {
        return (int) Math.round(get());
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            set(json.getAsDouble());
        }
    }
}
