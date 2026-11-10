package dev.kuroinu.nova.platform;

/** An active status effect, already localized. {@code icon} is an opaque handle for {@link Platform#drawEffectIcon}. */
public final class EffectInfo {
    public final String name;
    /** e.g. "1:30", or "**:**" for permanent effects. */
    public final String duration;
    /** Opaque RGB of the effect. */
    public final int color;
    public final Object icon;

    public EffectInfo(String name, String duration, int color, Object icon) {
        this.name = name;
        this.duration = duration;
        this.color = color;
        this.icon = icon;
    }
}
