package dev.kuroinu.nova.module.impl.hud;

import dev.kuroinu.nova.hud.Anchor;
import dev.kuroinu.nova.hud.HudElement;
import dev.kuroinu.nova.hud.HudStyle;
import dev.kuroinu.nova.platform.Platform;
import dev.kuroinu.nova.platform.Platform.GameKey;
import dev.kuroinu.nova.render.Colors;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.setting.BooleanSetting;
import dev.kuroinu.nova.setting.ColorSetting;
import dev.kuroinu.nova.ui.Animation;

import java.util.EnumMap;
import java.util.Map;

/** Design spec: 3-column grid of 36px keys (gap 3, radius 4), optional LMB/RMB row (30px) and space bar (16px). */
public class KeystrokesHud extends HudElement {
    private static final float KEY = 36;
    private static final float GAP = 3;
    private static final float WIDTH = KEY * 3 + GAP * 2;
    private static final float MOUSE_H = 30;
    private static final float SPACE_H = 16;
    private static final int PRESSED_TEXT = 0xFF0A0B0F;

    private final BooleanSetting mouseButtons = add(new BooleanSetting("Mouse buttons", true));
    private final BooleanSetting spaceBar = add(new BooleanSetting("Space bar", true));
    private final ColorSetting pressedColor = add(new ColorSetting("Pressed color", 0xFFA58FFF));

    private final Map<GameKey, Animation> anims = new EnumMap<GameKey, Animation>(GameKey.class);

    public KeystrokesHud() {
        super("keystrokes", "Keystrokes", "Movement keys and mouse buttons, lit up as you press them",
                Anchor.BOTTOM_LEFT, 16, -60);
    }

    @Override
    public boolean isFavoriteByDefault() {
        return true;
    }

    @Override
    public float getWidth(Renderer r, boolean preview) {
        return WIDTH;
    }

    @Override
    public float getHeight(Renderer r, boolean preview) {
        float h = KEY * 2 + GAP;
        if (mouseButtons.get()) h += GAP + MOUSE_H;
        if (spaceBar.get()) h += GAP + SPACE_H;
        return h;
    }

    @Override
    public void render(Renderer r, boolean preview) {
        Platform p = nova().platform();
        key(r, p, GameKey.FORWARD, "W", KEY + GAP, 0, KEY, KEY, null);
        key(r, p, GameKey.LEFT, "A", 0, KEY + GAP, KEY, KEY, null);
        key(r, p, GameKey.BACK, "S", KEY + GAP, KEY + GAP, KEY, KEY, null);
        key(r, p, GameKey.RIGHT, "D", (KEY + GAP) * 2, KEY + GAP, KEY, KEY, null);
        float y = (KEY + GAP) * 2;
        if (mouseButtons.get()) {
            float half = (WIDTH - GAP) / 2f;
            key(r, p, GameKey.ATTACK, "LMB", 0, y, half, MOUSE_H, nova().cps().getLeft() + " CPS");
            key(r, p, GameKey.USE, "RMB", half + GAP, y, half, MOUSE_H, nova().cps().getRight() + " CPS");
            y += MOUSE_H + GAP;
        }
        if (spaceBar.get()) {
            float t = press(GameKey.JUMP, p.isKeyDown(GameKey.JUMP));
            r.roundedRect(0, y, WIDTH, SPACE_H, HudStyle.RADIUS, Colors.lerp(HudStyle.BACKGROUND, pressedColor.argb(), t));
            r.roundedRect(WIDTH / 2f - 15, y + SPACE_H / 2f - 1, 30, 2, 1, Colors.lerp(HudStyle.WHITE, PRESSED_TEXT, t));
        }
    }

    private void key(Renderer r, Platform p, GameKey k, String label, float x, float y, float w, float h, String sub) {
        float t = press(k, p.isKeyDown(k));
        r.roundedRect(x, y, w, h, HudStyle.RADIUS, Colors.lerp(HudStyle.BACKGROUND, pressedColor.argb(), t));
        int color = Colors.lerp(HudStyle.WHITE, PRESSED_TEXT, t);
        float cx = x + w / 2f;
        if (sub == null) {
            labelText(r, HudStyle.TEXT_12, label, cx, y + h / 2f, color, t);
        } else {
            float l1 = r.lineHeight(HudStyle.TEXT_12), l2 = r.lineHeight(HudStyle.TEXT_10);
            float top = y + (h - l1 - l2 + 2) / 2f;
            labelText(r, HudStyle.TEXT_12, label, cx, top + l1 / 2f, color, t);
            labelText(r, HudStyle.TEXT_10, sub, cx, top + l1 - 2 + l2 / 2f, color, t);
        }
    }

    /** Shadowed text on dark keys, flat text on pressed (light) keys. */
    private static void labelText(Renderer r, dev.kuroinu.nova.render.font.NovaFont font, String s, float cx, float cy, int color, float t) {
        if (t < 0.5f) {
            HudStyle.textCentered(r, font, s, cx, cy, color);
        } else {
            r.textCentered(font, s, cx, cy, color);
        }
    }

    private float press(GameKey k, boolean down) {
        Animation a = anims.get(k);
        if (a == null) {
            a = new Animation(0.08f, 0f);
            anims.put(k, a);
        }
        return a.update(down ? 1f : 0f);
    }
}
