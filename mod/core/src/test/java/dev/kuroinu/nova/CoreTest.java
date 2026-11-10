package dev.kuroinu.nova;

import com.google.gson.JsonObject;
import dev.kuroinu.nova.config.ConfigManager;
import dev.kuroinu.nova.event.EventBus;
import dev.kuroinu.nova.event.Events;
import dev.kuroinu.nova.hud.Anchor;
import dev.kuroinu.nova.hud.HudElement;
import dev.kuroinu.nova.module.ModuleManager;
import dev.kuroinu.nova.module.impl.hud.CoordinatesHud;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.setting.ColorSetting;
import dev.kuroinu.nova.setting.ModeSetting;
import dev.kuroinu.nova.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    /** Fixed-size element with a no-op renderer, for layout math. */
    static class Box extends HudElement {
        Box(String id, Anchor a, float x, float y) {
            super(id, id, "", a, x, y);
        }

        @Override
        public float getWidth(Renderer r, boolean preview) {
            return 40;
        }

        @Override
        public float getHeight(Renderer r, boolean preview) {
            return 10;
        }

        @Override
        public void render(Renderer r, boolean preview) {
        }
    }

    static final Renderer R = new Renderer() {
        public void rect(float x, float y, float w, float h, int c) {
        }

        public void text(String t, float x, float y, int c, boolean s) {
        }

        public int textWidth(String t) {
            return t.length() * 6;
        }

        public int fontHeight() {
            return 9;
        }

        public void pushScale(float x, float y, float s) {
        }

        public void pop() {
        }

        public void pushScissor(float x, float y, float w, float h) {
        }

        public void popScissor() {
        }
    };

    @Test
    void eventBusDeliversOnlyWhileModuleEnabled() {
        EventBus bus = new EventBus();
        final AtomicInteger hits = new AtomicInteger();
        ModuleManager mm = new ModuleManager(bus);
        Box box = new Box("b", Anchor.TOP_LEFT, 0, 0) {
            {
                on(Events.Tick.class, new EventBus.Listener<Events.Tick>() {
                    public void handle(Events.Tick e) {
                        hits.incrementAndGet();
                    }
                });
            }
        };
        mm.register(box); // HUD elements start enabled
        bus.post(Events.Tick.INSTANCE);
        box.setEnabled(false);
        bus.post(Events.Tick.INSTANCE);
        box.setEnabled(true);
        bus.post(Events.Tick.INSTANCE);
        assertEquals(2, hits.get());
    }

    @Test
    void anchoredPositionFollowsScreenEdge() {
        Box box = new Box("b", Anchor.BOTTOM_RIGHT, -4, -4);
        assertEquals(800 - 40 - 4, box.getX(R, 800, true), 0.001);
        assertEquals(600 - 10 - 4, box.getY(R, 600, true), 0.001);
        // Same offset from the edge on a different screen size
        assertEquals(1000 - 40 - 4, box.getX(R, 1000, true), 0.001);
    }

    @Test
    void setPositionReanchorsAndRoundTrips() {
        Box box = new Box("b", Anchor.TOP_LEFT, 0, 0);
        box.setPosition(R, 700, 500, 800, 600);
        assertEquals(Anchor.BOTTOM_RIGHT, box.getAnchor());
        assertEquals(700, box.getX(R, 800, true), 0.001);
        assertEquals(500, box.getY(R, 600, true), 0.001);
        box.setPosition(R, 5000, -50, 800, 600); // clamped on-screen
        assertEquals(760, box.getX(R, 800, true), 0.001);
        assertEquals(0, box.getY(R, 600, true), 0.001);
    }

    @Test
    void configRoundTripRestoresStateAndIgnoresGarbage() {
        EventBus bus = new EventBus();
        ModuleManager a = new ModuleManager(bus);
        Box box = a.register(new Box("b", Anchor.TOP_LEFT, 0, 0));
        box.setPosition(R, 300, 200, 800, 600);
        box.setEnabled(false);
        ((NumberSetting) box.getSettings().get(0)).set(1.5);
        JsonObject json = new ConfigManager(new File("unused"), a).serialize();

        ModuleManager b = new ModuleManager(new EventBus());
        Box copy = b.register(new Box("b", Anchor.TOP_LEFT, 0, 0));
        new ConfigManager(new File("unused"), b).apply(json);
        assertFalse(copy.isEnabled());
        assertEquals(1.5, copy.getScale(), 0.001);
        assertEquals(box.getX(R, 800, true), copy.getX(R, 800, true), 0.001);
        assertEquals(box.getY(R, 600, true), copy.getY(R, 600, true), 0.001);

        JsonObject garbage = new JsonObject();
        garbage.addProperty("modules", "nope");
        new ConfigManager(new File("unused"), b).apply(garbage); // must not throw
    }

    @Test
    void layoutsFromOlderCoordinateSystemAreIgnored() {
        ModuleManager a = new ModuleManager(new EventBus());
        Box box = a.register(new Box("b", Anchor.TOP_LEFT, 16, 16));
        box.setPosition(R, 300, 200, 800, 600);
        JsonObject json = new ConfigManager(new File("unused"), a).serialize();
        json.remove("layoutVersion"); // as written by builds before layout version 2

        ModuleManager b = new ModuleManager(new EventBus());
        Box copy = b.register(new Box("b", Anchor.TOP_LEFT, 16, 16));
        new ConfigManager(new File("unused"), b).apply(json);
        assertEquals(16, copy.getX(R, 800, true), 0.001);
        assertEquals(16, copy.getY(R, 600, true), 0.001);
    }

    @Test
    void bindsArePerKeySpaceAndPreserveOtherVersions() {
        ModuleManager a = new ModuleManager(new EventBus());
        Box box = a.register(new Box("b", Anchor.TOP_LEFT, 0, 0));
        box.setBindKey(33);
        JsonObject legacy = new ConfigManager(new File("unused"), a, "lwjgl2").serialize();

        // A 26.x client reads the same file: no bind in its key space, and saving keeps the 1.8.9 bind.
        ModuleManager b = new ModuleManager(new EventBus());
        Box modern = b.register(new Box("b", Anchor.TOP_LEFT, 0, 0));
        ConfigManager modernConfig = new ConfigManager(new File("unused"), b, "sdl3");
        modernConfig.apply(legacy);
        assertEquals(dev.kuroinu.nova.module.Module.NO_KEY, modern.getBindKey());
        modern.setBindKey(9);
        JsonObject merged = modernConfig.serialize();
        JsonObject binds = merged.getAsJsonObject("modules").getAsJsonObject("b").getAsJsonObject("binds");
        assertEquals(33, binds.get("lwjgl2").getAsInt());
        assertEquals(9, binds.get("sdl3").getAsInt());

        // Clearing a bind removes only this key space.
        modern.setBindKey(dev.kuroinu.nova.module.Module.NO_KEY);
        binds = modernConfig.serialize().getAsJsonObject("modules").getAsJsonObject("b").getAsJsonObject("binds");
        assertFalse(binds.has("sdl3"));
        assertTrue(binds.has("lwjgl2"));
    }

    @Test
    void bundledFontsRasterize() {
        for (dev.kuroinu.nova.render.font.FontFace face : dev.kuroinu.nova.render.font.FontFace.values()) {
            dev.kuroinu.nova.render.font.GlyphAtlas atlas = dev.kuroinu.nova.render.font.GlyphAtlas.get(face, 14);
            assertTrue(atlas.ascent > 8 && atlas.ascent < 20, face + " ascent " + atlas.ascent);
            dev.kuroinu.nova.render.font.Glyph h = atlas.glyph('H');
            assertFalse(h.isEmpty(), face + " H");
            assertTrue(h.advance > 5, face + " advance");
            assertTrue(atlas.glyph(' ').isEmpty());
            // Symbols the faces lack come from the fallback font instead of rendering nothing.
            assertFalse(atlas.glyph('★').isEmpty(), face + " star fallback");
        }
        // The bundled face is really used: Outfit and JetBrains Mono measure differently.
        float outfit = dev.kuroinu.nova.render.font.GlyphAtlas.get(dev.kuroinu.nova.render.font.FontFace.OUTFIT_REGULAR, 14).advance("iiiiii");
        float mono = dev.kuroinu.nova.render.font.GlyphAtlas.get(dev.kuroinu.nova.render.font.FontFace.MONO_REGULAR, 14).advance("iiiiii");
        assertTrue(mono > outfit * 1.3f, "mono " + mono + " vs outfit " + outfit);
    }

    @Test
    void hsbRoundTrip() {
        int[] colors = {0xFF7C5CFF, 0xFFFF0000, 0xFF00FF00, 0xFF123456, 0xFFFFFFFF, 0xFF000000};
        for (int c : colors) {
            float[] hsb = dev.kuroinu.nova.render.Colors.toHsb(c);
            assertEquals(c, dev.kuroinu.nova.render.Colors.hsb(hsb[0], hsb[1], hsb[2]), "color " + Integer.toHexString(c));
        }
    }

    @Test
    void settingsValidateInput() {
        NumberSetting n = new NumberSetting("n", 1, 0, 2, 0.25);
        n.set(1.3);
        assertEquals(1.25, n.get(), 1e-9);
        n.set(99.0);
        assertEquals(2, n.get(), 1e-9);

        ModeSetting m = new ModeSetting("m", "a", "a", "b", "c");
        m.set("zzz");
        assertEquals("a", m.get());
        m.cycle(false);
        assertEquals("c", m.get());

        ColorSetting c = new ColorSetting("c", 0);
        c.fromJson(c.toJson());
        c.set(0x80FF00AA);
        ColorSetting d = new ColorSetting("d", 0);
        d.fromJson(c.toJson());
        assertEquals(0x80FF00AA, d.argb());
    }

    @Test
    void directionNames() {
        assertEquals("S", CoordinatesHud.directionName(0));
        assertEquals("W", CoordinatesHud.directionName(90));
        assertEquals("N", CoordinatesHud.directionName(-180));
        assertEquals("E", CoordinatesHud.directionName(-90));
        assertEquals("SE", CoordinatesHud.directionName(-45));
    }
}
