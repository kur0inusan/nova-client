package dev.kuroinu.nova.ui;

/**
 * Frame-rate independent exponential easing toward a target value. Call {@link #update} once per
 * frame; the value covers ~90% of the distance in {@code duration} seconds regardless of FPS.
 */
public final class Animation {
    private final float duration;
    private float value;
    private float target;
    private long lastNanos = -1;

    public Animation(float durationSeconds, float initial) {
        this.duration = durationSeconds;
        this.value = initial;
        this.target = initial;
    }

    public float update(float target) {
        this.target = target;
        long now = System.nanoTime();
        if (lastNanos < 0) {
            lastNanos = now;
        }
        float dt = Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        // 1 - 0.1^(dt/duration): reaches 90% after `duration` seconds.
        float k = 1f - (float) Math.pow(0.1, dt / duration);
        value += (target - value) * k;
        if (Math.abs(target - value) < 0.001f) {
            value = target;
        }
        return value;
    }

    public float get() {
        return value;
    }

    public void snap(float v) {
        value = v;
        target = v;
        lastNanos = -1;
    }

    public boolean isDone() {
        return value == target;
    }
}
