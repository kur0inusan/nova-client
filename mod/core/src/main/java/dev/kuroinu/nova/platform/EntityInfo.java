package dev.kuroinu.nova.platform;

/** Snapshot of an entity in the current world, positioned for one rendered frame. */
public final class EntityInfo {
    public enum Kind {
        PLAYER, MOB, ANIMAL, OTHER
    }

    public final int id;
    public final Kind kind;
    /** Feet position, interpolated for the frame. */
    public final double x;
    public final double y;
    public final double z;
    /** Hitbox size in blocks. */
    public final float width;
    public final float height;
    public final boolean alive;
    public final boolean invisible;
    public final boolean spectator;

    public EntityInfo(int id, Kind kind, double x, double y, double z, float width, float height,
                      boolean alive, boolean invisible, boolean spectator) {
        this.id = id;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.alive = alive;
        this.invisible = invisible;
        this.spectator = spectator;
    }
}
