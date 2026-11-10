package dev.kuroinu.nova.platform;

/** One worn armor item. {@code stack} is an opaque version-specific handle for {@link Platform#drawItem}. */
public final class ArmorPiece {
    public final Object stack;
    /** Remaining durability points, or -1 for items that don't take damage. */
    public final int durability;
    public final int maxDurability;

    public ArmorPiece(Object stack, int durability, int maxDurability) {
        this.stack = stack;
        this.durability = durability;
        this.maxDurability = maxDurability;
    }
}
