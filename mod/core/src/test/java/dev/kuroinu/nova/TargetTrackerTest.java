package dev.kuroinu.nova;

import dev.kuroinu.nova.module.impl.visual.TargetTracker;
import dev.kuroinu.nova.platform.EntityInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TargetTrackerTest {
    private static final Object WORLD = new Object();
    private static final long TIMEOUT = 5000;
    private static final TargetTracker.Filter ALL = e -> true;

    private final Map<Integer, EntityInfo> entities = new HashMap<Integer, EntityInfo>();
    private final TargetTracker.Lookup lookup = entities::get;

    private EntityInfo spawn(int id, EntityInfo.Kind kind) {
        EntityInfo e = new EntityInfo(id, kind, 0, 0, 0, 0.6f, 1.8f, true, false, false);
        entities.put(id, e);
        return e;
    }

    private TargetTracker.Target update(TargetTracker t, int crosshair, long now) {
        return t.update(lookup, crosshair, WORLD, now, TIMEOUT, true, ALL);
    }

    @Test
    void attackTargetWinsOverCrosshairUntilTimeout() {
        TargetTracker t = new TargetTracker();
        EntityInfo a = spawn(1, EntityInfo.Kind.PLAYER);
        spawn(2, EntityInfo.Kind.PLAYER);
        assertTrue(t.onAttack(a, WORLD, 0, ALL));

        TargetTracker.Target target = update(t, 2, 1000);
        assertEquals(1, target.entity.id);
        assertEquals(TargetTracker.Source.ATTACK, target.source);

        target = update(t, 2, TIMEOUT + 1);
        assertEquals(2, target.entity.id);
        assertEquals(TargetTracker.Source.LOOK, target.source);
        assertNull(update(t, -1, TIMEOUT + 2));
    }

    @Test
    void attackingAgainRefreshesAndSwitches() {
        TargetTracker t = new TargetTracker();
        EntityInfo a = spawn(1, EntityInfo.Kind.PLAYER);
        EntityInfo b = spawn(2, EntityInfo.Kind.MOB);
        t.onAttack(a, WORLD, 0, ALL);
        t.onAttack(a, WORLD, 4000, ALL);
        assertEquals(1, update(t, -1, 8000).entity.id);
        t.onAttack(b, WORLD, 8000, ALL);
        assertEquals(2, update(t, -1, 8001).entity.id);
    }

    @Test
    void deathRemovalAndWorldChangeClearTheTarget() {
        TargetTracker t = new TargetTracker();
        EntityInfo a = spawn(1, EntityInfo.Kind.PLAYER);
        t.onAttack(a, WORLD, 0, ALL);
        entities.put(1, new EntityInfo(1, EntityInfo.Kind.PLAYER, 0, 0, 0, 0.6f, 1.8f, false, false, false));
        assertNull(update(t, -1, 10));

        t.onAttack(spawn(1, EntityInfo.Kind.PLAYER), WORLD, 20, ALL);
        entities.remove(1);
        assertNull(update(t, -1, 30));

        t.onAttack(spawn(1, EntityInfo.Kind.PLAYER), WORLD, 40, ALL);
        assertNotNull(update(t, -1, 50));
        assertNull(t.update(lookup, -1, new Object(), 60, TIMEOUT, true, ALL));
    }

    @Test
    void filteredInvisibleAndSpectatorsAreIgnored() {
        TargetTracker t = new TargetTracker();
        TargetTracker.Filter playersOnly = e -> e.kind == EntityInfo.Kind.PLAYER;
        assertFalse(t.onAttack(spawn(1, EntityInfo.Kind.ANIMAL), WORLD, 0, playersOnly));
        EntityInfo ghost = new EntityInfo(2, EntityInfo.Kind.PLAYER, 0, 0, 0, 0.6f, 1.8f, true, true, false);
        EntityInfo spectator = new EntityInfo(3, EntityInfo.Kind.PLAYER, 0, 0, 0, 0.6f, 1.8f, true, false, true);
        assertFalse(t.onAttack(ghost, WORLD, 0, ALL));
        assertFalse(t.onAttack(spectator, WORLD, 0, ALL));
        entities.put(3, spectator);
        assertNull(update(t, 3, 0));
        assertNull(t.update(lookup, 1, WORLD, 0, TIMEOUT, true, playersOnly));
    }

    @Test
    void crosshairCanBeDisabled() {
        TargetTracker t = new TargetTracker();
        spawn(1, EntityInfo.Kind.PLAYER);
        assertNull(t.update(lookup, 1, WORLD, 0, TIMEOUT, false, ALL));
    }
}
