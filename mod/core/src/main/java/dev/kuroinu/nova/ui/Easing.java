package dev.kuroinu.nova.ui;

/** Easing curves for time-based UI transitions (input and output in [0,1]). */
public final class Easing {
    private Easing() {
    }

    public static float clamp01(float t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    public static float outCubic(float t) {
        t = clamp01(t);
        float u = 1 - t;
        return 1 - u * u * u;
    }

    /** Progress of a transition that started at {@code startMs} and lasts {@code durationMs}, eased. */
    public static float since(long startMs, long delayMs, long durationMs) {
        return outCubic((System.currentTimeMillis() - startMs - delayMs) / (float) durationMs);
    }
}
