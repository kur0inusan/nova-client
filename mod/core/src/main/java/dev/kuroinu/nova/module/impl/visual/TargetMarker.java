package dev.kuroinu.nova.module.impl.visual;

import dev.kuroinu.nova.event.Events;
import dev.kuroinu.nova.module.Category;
import dev.kuroinu.nova.module.Module;
import dev.kuroinu.nova.platform.EntityInfo;
import dev.kuroinu.nova.platform.Platform;
import dev.kuroinu.nova.render.Colors;
import dev.kuroinu.nova.render.Renderer3D;
import dev.kuroinu.nova.setting.BooleanSetting;
import dev.kuroinu.nova.setting.ColorSetting;
import dev.kuroinu.nova.setting.ModeSetting;
import dev.kuroinu.nova.setting.NumberSetting;
import dev.kuroinu.nova.ui.Easing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Marks the entity you are fighting (see {@link TargetTracker}) with an arrow, a ring or a box.
 * Shapes and timings follow docs/spec/target-marker.md.
 */
public class TargetMarker extends Module {
    private static final String ARROW = "Arrow";
    private static final String RING = "Ring";
    private static final String BOX = "Box";

    private static final long FADE_IN_MS = 180;
    private static final long FADE_OUT_MS = 220;
    private static final long COLOR_FADE_MS = 150;
    private static final long FLASH_MS = 250;
    private static final float THROUGH_WALLS_ALPHA = 0.5f;

    private static final float ARROW_OFFSET = 0.8f; // above the head, clear of the name tag (+0.5)
    private static final float ARROW_SIZE = 0.34f;
    private static final float ARROW_BOB = 0.08f;
    private static final float ARROW_BOB_PERIOD = 1.6f;
    private static final float ARROW_SPIN = (float) (Math.PI * 0.9); // rad/s

    private static final float RING_GAP = 0.2f;
    private static final float RING_PERIOD = 2.0f;
    private static final float RING_TRAIL = 0.25f; // seconds
    private static final int RING_SEGMENTS = 64;

    private static final float BOX_GROW = 0.05f;

    private final ModeSetting mode = add(new ModeSetting("Mode", ARROW, ARROW, RING, BOX));
    private final BooleanSetting crosshair = add(new BooleanSetting("Crosshair target", true));
    private final NumberSetting timeout = add(new NumberSetting("Timeout", 5, 1, 30, 1).withUnit("s"));
    private final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", true));
    private final BooleanSetting hitFlash = add(new BooleanSetting("Hit flash", true));
    private final BooleanSetting arrowSpin = add(new BooleanSetting("Arrow spin", false));
    private final BooleanSetting ringTrail = add(new BooleanSetting("Ring trail", true));
    private final BooleanSetting players = add(new BooleanSetting("Players", true));
    private final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    private final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    private final ColorSetting attackColor = add(new ColorSetting("Attack color", 0xFFFF5A5F));
    private final ColorSetting lookColor = add(new ColorSetting("Look color", 0xFFF3E7C9));

    private final TargetTracker tracker = new TargetTracker();
    private final List<Marker> markers = new ArrayList<Marker>();
    private final long epochMs = System.currentTimeMillis();
    private int pendingFlashId = -1;

    public TargetMarker() {
        super("target_marker", "Target Marker", "Marks the entity you're fighting with an arrow, ring or box",
                Category.VISUAL);
        on(Events.Attack.class, e -> onAttack(e.entityId));
        on(Events.Render3D.class, this::render);
    }

    @Override
    protected void onDisable() {
        tracker.clear();
        markers.clear();
        pendingFlashId = -1;
    }

    private boolean accepts(EntityInfo e) {
        switch (e.kind) {
            case PLAYER:
                return players.get();
            case MOB:
                return mobs.get();
            case ANIMAL:
                return animals.get();
            default:
                return false;
        }
    }

    private void onAttack(int entityId) {
        long now = System.currentTimeMillis();
        Platform platform = nova().platform();
        if (!tracker.onAttack(platform.getEntity(entityId, 1f), platform.getWorld(), now, this::accepts)) {
            return;
        }
        Marker m = live(entityId);
        if (m != null) {
            m.flashMs = now;
        } else {
            pendingFlashId = entityId; // the marker appears on the next frame
        }
    }

    private void render(Events.Render3D event) {
        final Platform platform = nova().platform();
        final float partialTicks = event.partialTicks;
        long now = System.currentTimeMillis();
        TargetTracker.Lookup lookup = id -> platform.getEntity(id, partialTicks);
        TargetTracker.Target want = tracker.update(lookup, platform.getCrosshairEntityId(), platform.getWorld(), now,
                Math.round(timeout.get() * 1000), crosshair.get(), this::accepts);
        updateMarkers(want, lookup, now);
        if (markers.isEmpty()) {
            return;
        }
        Renderer3D r = event.renderer;
        if (throughWalls.get()) {
            r.setDepthTest(false);
            drawAll(r, now, THROUGH_WALLS_ALPHA);
        }
        r.setDepthTest(true);
        drawAll(r, now, 1f);
    }

    // ------------------------------------------------------------------ marker lifecycle

    private static final class Marker {
        final int id;
        final long bornMs;
        TargetTracker.Source source;
        EntityInfo entity;
        long dyingMs = -1;
        long flashMs = -1;
        int colorFrom;
        long colorStartMs = -1;

        Marker(int id, TargetTracker.Source source, long bornMs, int color) {
            this.id = id;
            this.source = source;
            this.bornMs = bornMs;
            this.colorFrom = color;
        }
    }

    private Marker live(int id) {
        for (Marker m : markers) {
            if (m.id == id && m.dyingMs < 0) {
                return m;
            }
        }
        return null;
    }

    private void updateMarkers(TargetTracker.Target want, TargetTracker.Lookup lookup, long now) {
        Marker current = want == null ? null : live(want.entity.id);
        for (Marker m : markers) {
            if (m.dyingMs < 0 && m != current) {
                m.dyingMs = now;
            }
        }
        if (want != null) {
            if (current == null) {
                current = new Marker(want.entity.id, want.source, now, colorOf(want.source));
                if (pendingFlashId == current.id) {
                    current.flashMs = now;
                }
                markers.add(current);
            } else if (current.source != want.source) {
                // Same entity, attack <-> look: keep the shape, cross-fade the color.
                current.colorFrom = baseColor(current, now);
                current.source = want.source;
                current.colorStartMs = now;
            }
            current.entity = want.entity;
        }
        pendingFlashId = -1;
        for (Iterator<Marker> it = markers.iterator(); it.hasNext(); ) {
            Marker m = it.next();
            if (m.dyingMs < 0) {
                continue;
            }
            if (now - m.dyingMs >= FADE_OUT_MS) {
                it.remove();
            } else {
                EntityInfo e = lookup.get(m.id); // keep following while fading; freeze in place once gone
                if (e != null) {
                    m.entity = e;
                }
            }
        }
    }

    private int colorOf(TargetTracker.Source source) {
        return source == TargetTracker.Source.ATTACK ? attackColor.argb() : lookColor.argb();
    }

    private int baseColor(Marker m, long now) {
        float t = m.colorStartMs < 0 ? 1 : (now - m.colorStartMs) / (float) COLOR_FADE_MS;
        return Colors.lerp(m.colorFrom, colorOf(m.source), Easing.clamp01(t));
    }

    private float flash(Marker m, long now) {
        if (!hitFlash.get() || m.flashMs < 0) {
            return 0;
        }
        float u = 1 - Easing.clamp01((now - m.flashMs) / (float) FLASH_MS);
        return u * u;
    }

    // ------------------------------------------------------------------ drawing

    private void drawAll(Renderer3D r, long now, float alphaScale) {
        float time = (now - epochMs) / 1000f;
        for (Marker m : markers) {
            float in = Easing.outCubic((now - m.bornMs) / (float) FADE_IN_MS);
            float out = 1;
            if (m.dyingMs >= 0) {
                float t = Easing.clamp01((now - m.dyingMs) / (float) FADE_OUT_MS);
                out = 1 - t * t * t;
            }
            float alpha = in * out * alphaScale;
            if (alpha <= 0.01f) {
                continue;
            }
            float flash = flash(m, now);
            float scale = (0.7f + 0.3f * in) * (m.dyingMs < 0 ? 1 : 0.85f + 0.15f * out) * (1 + 0.18f * flash);
            int color = Colors.lerp(baseColor(m, now), 0xFFFFFFFF, flash * 0.85f);
            color = Colors.withAlpha(color, Math.round(Colors.alpha(color) * alpha));

            EntityInfo e = m.entity;
            float x = (float) (e.x - r.cameraX());
            float y = (float) (e.y - r.cameraY());
            float z = (float) (e.z - r.cameraZ());
            if (mode.is(RING)) {
                drawRing(r, e, x, y, z, scale, color, time);
            } else if (mode.is(BOX)) {
                drawBox(r, e, x, y, z, scale, color);
            } else {
                drawArrow(r, e, x, y, z, scale, color, time);
            }
        }
    }

    private void drawArrow(Renderer3D r, EntityInfo e, float x, float y, float z, float scale, int color, float time) {
        float cy = y + e.height + ARROW_OFFSET + (float) Math.sin(2 * Math.PI * time / ARROW_BOB_PERIOD) * ARROW_BOB;
        float rx, rz;
        if (arrowSpin.get()) {
            rx = (float) Math.cos(time * ARROW_SPIN);
            rz = (float) Math.sin(time * ARROW_SPIN);
        } else {
            float[] right = r.cameraRight();
            float len = (float) Math.sqrt(right[0] * right[0] + right[2] * right[2]);
            rx = len < 1e-3f ? 1 : right[0] / len;
            rz = len < 1e-3f ? 0 : right[2] / len;
        }
        float s = ARROW_SIZE * scale;
        float ax = x - rx * s / 2, az = z - rz * s / 2;
        float bx = x + rx * s / 2, bz = z + rz * s / 2;
        float top = cy + s * 0.38f, tip = cy - s * 0.5f;

        r.begin(Renderer3D.Mode.TRIANGLES, 0);
        r.vertex(ax, top, az, color);
        r.vertex(bx, top, bz, color);
        r.vertex(x, tip, z, color);
        r.end();

        int edge = Colors.withAlpha(Colors.lerp(color, 0xFF000000, 0.55f), Math.round(Colors.alpha(color) * 0.9f));
        r.begin(Renderer3D.Mode.LINES, 1.5f);
        line(r, ax, top, az, bx, top, bz, edge);
        line(r, bx, top, bz, x, tip, z, edge);
        line(r, x, tip, z, ax, top, az, edge);
        r.end();
    }

    /** Ring height above its lowest point at {@code time}: (1 - cos) / 2 so it eases at both ends. */
    private static float ringLift(float time, float span) {
        return (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * time / RING_PERIOD)) * span;
    }

    private void drawRing(Renderer3D r, EntityInfo e, float x, float y, float z, float scale, int color, float time) {
        float radius = (e.width / 2 + RING_GAP) * scale;
        float base = y + 0.03f;
        float span = Math.max(0, e.height - 0.06f);
        float lift = ringLift(time, span);
        float ringY = base + lift;

        if (ringTrail.get()) {
            // A band from the ring to the farthest height it passed through within the trail window.
            float far = lift;
            for (int k = 1; k <= 16; k++) {
                float l = ringLift(time - RING_TRAIL * k / 16f, span);
                if (Math.abs(l - lift) > Math.abs(far - lift)) {
                    far = l;
                }
            }
            if (Math.abs(far - lift) > 0.005f) {
                float midY = ringY + (far - lift) * 0.4f;
                float endY = base + far;
                int a = Colors.alpha(color);
                int c0 = Colors.withAlpha(color, Math.round(a * 0.35f));
                int c1 = Colors.withAlpha(color, Math.round(a * 0.12f));
                int c2 = Colors.withAlpha(color, 0);
                r.begin(Renderer3D.Mode.TRIANGLES, 0);
                for (int i = 0; i < RING_SEGMENTS; i++) {
                    float x0 = x + cos(i) * radius, z0 = z + sin(i) * radius;
                    float x1 = x + cos(i + 1) * radius, z1 = z + sin(i + 1) * radius;
                    quad(r, x0, z0, x1, z1, ringY, c0, midY, c1);
                    quad(r, x0, z0, x1, z1, midY, c1, endY, c2);
                }
                r.end();
            }
        }

        r.begin(Renderer3D.Mode.LINES, 2.5f);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            line(r, x + cos(i) * radius, ringY, z + sin(i) * radius,
                    x + cos(i + 1) * radius, ringY, z + sin(i + 1) * radius, color);
        }
        r.end();
    }

    private void drawBox(Renderer3D r, EntityInfo e, float x, float y, float z, float scale, int color) {
        float hw = (e.width / 2 + BOX_GROW) * scale;
        float hh = (e.height / 2 + BOX_GROW) * scale;
        float cy = y + e.height / 2;
        float x0 = x - hw, x1 = x + hw, y0 = cy - hh, y1 = cy + hh, z0 = z - hw, z1 = z + hw;

        int fill = Colors.withAlpha(color, Math.round(Colors.alpha(color) * 0.1f));
        r.begin(Renderer3D.Mode.TRIANGLES, 0);
        face(r, fill, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        face(r, fill, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(r, fill, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        face(r, fill, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        face(r, fill, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(r, fill, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        r.end();

        r.begin(Renderer3D.Mode.LINES, 2f);
        for (float yy : new float[]{y0, y1}) {
            line(r, x0, yy, z0, x1, yy, z0, color);
            line(r, x1, yy, z0, x1, yy, z1, color);
            line(r, x1, yy, z1, x0, yy, z1, color);
            line(r, x0, yy, z1, x0, yy, z0, color);
        }
        line(r, x0, y0, z0, x0, y1, z0, color);
        line(r, x1, y0, z0, x1, y1, z0, color);
        line(r, x1, y0, z1, x1, y1, z1, color);
        line(r, x0, y0, z1, x0, y1, z1, color);
        r.end();
    }

    // ------------------------------------------------------------------ primitives

    private static float cos(int segment) {
        return (float) Math.cos(2 * Math.PI * segment / RING_SEGMENTS);
    }

    private static float sin(int segment) {
        return (float) Math.sin(2 * Math.PI * segment / RING_SEGMENTS);
    }

    private static void line(Renderer3D r, float ax, float ay, float az, float bx, float by, float bz, int color) {
        r.vertex(ax, ay, az, color);
        r.vertex(bx, by, bz, color);
    }

    /** A vertical quad between two points on a circle, from height {@code ya} to {@code yb}, with per-edge colors. */
    private static void quad(Renderer3D r, float x0, float z0, float x1, float z1, float ya, int ca, float yb, int cb) {
        r.vertex(x0, ya, z0, ca);
        r.vertex(x1, ya, z1, ca);
        r.vertex(x1, yb, z1, cb);
        r.vertex(x0, ya, z0, ca);
        r.vertex(x1, yb, z1, cb);
        r.vertex(x0, yb, z0, cb);
    }

    private static void face(Renderer3D r, int color, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        r.vertex(ax, ay, az, color);
        r.vertex(bx, by, bz, color);
        r.vertex(cx, cy, cz, color);
        r.vertex(ax, ay, az, color);
        r.vertex(cx, cy, cz, color);
        r.vertex(dx, dy, dz, color);
    }
}
