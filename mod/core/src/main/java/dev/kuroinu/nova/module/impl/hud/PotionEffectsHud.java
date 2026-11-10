package dev.kuroinu.nova.module.impl.hud;

import dev.kuroinu.nova.hud.Anchor;
import dev.kuroinu.nova.hud.HudElement;
import dev.kuroinu.nova.hud.HudStyle;
import dev.kuroinu.nova.platform.EffectInfo;
import dev.kuroinu.nova.render.Colors;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.setting.BooleanSetting;

import java.util.Arrays;
import java.util.List;

/** Design spec: a box (padding 6x9, radius 4) listing each effect's colored name and remaining time. */
public class PotionEffectsHud extends HudElement {
    private static final float PAD_X = 9;
    private static final float PAD_Y = 6;
    private static final float ROW_GAP = 3;
    private static final float ICON = 16;
    private static final float ICON_GAP = 6;
    private static final float TIME_GAP = 10;
    private static final int TIME_COLOR = 0xFFC2C3CB;
    private static final List<EffectInfo> SAMPLE = Arrays.asList(
            new EffectInfo("Speed II", "1:30", 0x7CAFC6, null),
            new EffectInfo("Strength", "0:45", 0x932423, null));

    private final BooleanSetting icons = add(new BooleanSetting("Show icons", true));

    public PotionEffectsHud() {
        super("potions", "Potion Effects", "Active effects with their remaining time", Anchor.TOP_RIGHT, -16, 54);
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    private List<EffectInfo> effects(boolean preview) {
        List<EffectInfo> list = nova().platform().getEffects();
        return list.isEmpty() && preview ? SAMPLE : list;
    }

    private float rowHeight(Renderer r) {
        return Math.max(r.lineHeight(HudStyle.TEXT_12), icons.get() ? ICON : 0);
    }

    private float rowWidth(Renderer r, EffectInfo e) {
        float w = r.textWidth(HudStyle.TEXT_12, e.name) + TIME_GAP + r.textWidth(HudStyle.TEXT_12, e.duration);
        return icons.get() ? w + ICON + ICON_GAP : w;
    }

    @Override
    public float getWidth(Renderer r, boolean preview) {
        List<EffectInfo> list = effects(preview);
        if (list.isEmpty()) return 0;
        float w = 0;
        for (EffectInfo e : list) w = Math.max(w, rowWidth(r, e));
        return w + PAD_X * 2;
    }

    @Override
    public float getHeight(Renderer r, boolean preview) {
        int n = effects(preview).size();
        if (n == 0) return 0;
        return n * rowHeight(r) + (n - 1) * ROW_GAP + PAD_Y * 2;
    }

    @Override
    public void render(Renderer r, boolean preview) {
        List<EffectInfo> list = effects(preview);
        if (list.isEmpty()) return;
        r.roundedRect(0, 0, getWidth(r, preview), getHeight(r, preview), HudStyle.RADIUS, HudStyle.BOX);
        float rowH = rowHeight(r);
        float y = PAD_Y;
        for (EffectInfo e : list) {
            float x = PAD_X;
            float cy = y + rowH / 2f;
            if (icons.get()) {
                if (e.icon != null) {
                    nova().platform().drawEffectIcon(e.icon, x, cy - ICON / 2f, ICON);
                } else {
                    r.circle(x + ICON / 2f, cy, 5, 0xFF000000 | e.color);
                }
                x += ICON + ICON_GAP;
            }
            // Effect colors can be very dark; lift them toward white so they stay readable on the dark box.
            int nameColor = Colors.lerp(0xFF000000 | e.color, 0xFFFFFFFF, 0.35f);
            HudStyle.textCenteredV(r, HudStyle.TEXT_12, e.name, x, cy, nameColor);
            x += r.textWidth(HudStyle.TEXT_12, e.name) + TIME_GAP;
            HudStyle.textCenteredV(r, HudStyle.TEXT_12, e.duration, x, cy, TIME_COLOR);
            y += rowH + ROW_GAP;
        }
    }
}
