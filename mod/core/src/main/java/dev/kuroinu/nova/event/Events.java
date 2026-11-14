package dev.kuroinu.nova.event;

import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.render.Renderer3D;

/** Built-in events posted by the version-specific hooks. */
public final class Events {
    private Events() {
    }

    /** Posted once at the end of every client tick (20 per second). */
    public static final class Tick {
        public static final Tick INSTANCE = new Tick();

        private Tick() {
        }
    }

    /** Posted after the vanilla HUD has been drawn, in GUI-scaled coordinates. */
    public static final class Render2D {
        public final Renderer renderer;
        public final int width;
        public final int height;
        public final float partialTicks;

        public Render2D(Renderer renderer, int width, int height, float partialTicks) {
            this.renderer = renderer;
            this.width = width;
            this.height = height;
            this.partialTicks = partialTicks;
        }
    }

    /**
     * A keyboard key press/release. Key codes are version-specific (SDL3 scancodes on 26.x,
     * GLFW codes on 1.8.9 after translation), and 26.x reports modifier keys as 0 — so core must
     * not compare them against constants. Use {@link dev.kuroinu.nova.platform.Platform#consumeHotkey} instead.
     */
    public static final class Key {
        public final int key;
        public final boolean pressed;
        public final boolean inScreen;

        public Key(int key, boolean pressed, boolean inScreen) {
            this.key = key;
            this.pressed = pressed;
            this.inScreen = inScreen;
        }
    }

    /** Posted after the world is drawn and before the first-person hand, for world-space overlays. */
    public static final class Render3D {
        public final Renderer3D renderer;
        public final float partialTicks;

        public Render3D(Renderer3D renderer, float partialTicks) {
            this.renderer = renderer;
            this.partialTicks = partialTicks;
        }
    }

    /** The local player attacked an entity (by id, see {@link dev.kuroinu.nova.platform.Platform#getEntity}). */
    public static final class Attack {
        public final int entityId;

        public Attack(int entityId) {
            this.entityId = entityId;
        }
    }

    /** A mouse button press/release. 0 = left, 1 = right, 2 = middle. */
    public static final class MouseButton {
        public final int button;
        public final boolean pressed;
        public final boolean inScreen;

        public MouseButton(int button, boolean pressed, boolean inScreen) {
            this.button = button;
            this.pressed = pressed;
            this.inScreen = inScreen;
        }
    }
}
