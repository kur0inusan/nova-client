package dev.kuroinu.nova.module.impl.visual;

import dev.kuroinu.nova.platform.EntityInfo;

/**
 * Decides which entity {@link TargetMarker} marks: the last entity attacked (until it dies, leaves,
 * the world changes or the timeout passes), otherwise the entity under the crosshair.
 */
public final class TargetTracker {
    public enum Source {
        ATTACK, LOOK
    }

    public interface Lookup {
        /** The entity with this id for the current frame, or null when it is gone. */
        EntityInfo get(int id);
    }

    public interface Filter {
        boolean accepts(EntityInfo e);
    }

    public static final class Target {
        public final EntityInfo entity;
        public final Source source;

        Target(EntityInfo entity, Source source) {
            this.entity = entity;
            this.source = source;
        }
    }

    private int attackId = -1;
    private long lastAttackMs;
    private Object world;

    /** Records an attack. Returns false when the entity is not a valid target (it is then ignored). */
    public boolean onAttack(EntityInfo e, Object world, long nowMs, Filter filter) {
        if (!isValid(e, filter)) {
            return false;
        }
        this.world = world;
        attackId = e.id;
        lastAttackMs = nowMs;
        return true;
    }

    /** The target for this frame, or null. */
    public Target update(Lookup lookup, int crosshairId, Object world, long nowMs, long timeoutMs,
                         boolean useCrosshair, Filter filter) {
        if (world != this.world) {
            this.world = world;
            attackId = -1;
        }
        if (attackId != -1) {
            EntityInfo e = lookup.get(attackId);
            if (isValid(e, filter) && nowMs - lastAttackMs <= timeoutMs) {
                return new Target(e, Source.ATTACK);
            }
            attackId = -1;
        }
        if (useCrosshair && crosshairId != -1) {
            EntityInfo e = lookup.get(crosshairId);
            if (isValid(e, filter)) {
                return new Target(e, Source.LOOK);
            }
        }
        return null;
    }

    public void clear() {
        attackId = -1;
        world = null;
    }

    private static boolean isValid(EntityInfo e, Filter filter) {
        return e != null && e.alive && !e.invisible && !e.spectator && filter.accepts(e);
    }
}
