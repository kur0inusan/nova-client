package dev.kuroinu.nova.module.impl.hud;

import dev.kuroinu.nova.hud.Anchor;
import dev.kuroinu.nova.hud.HudElement;
import dev.kuroinu.nova.hud.HudStyle;
import dev.kuroinu.nova.platform.ArmorPiece;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.setting.BooleanSetting;
import dev.kuroinu.nova.setting.ModeSetting;

import java.util.ArrayList;
import java.util.List;

/** Design spec: one 18px item icon per piece plus remaining durability (green at 100+, red below). */
public class ArmorStatusHud extends HudElement {
    private static final float ICON = 18;
    private static final float TEXT_GAP = 4;
    private static final float ROW_GAP = 2;
    private static final float ITEM_GAP = 8;
    private static final ArmorPiece[] SAMPLE = {
            new ArmorPiece(null, 363, 363), new ArmorPiece(null, 240, 528),
            new ArmorPiece(null, 75, 495), new ArmorPiece(null, 195, 429),
    };

    private final ModeSetting orientation = add(new ModeSetting("Orientation", "Vertical", "Vertical", "Horizontal"));
    private final BooleanSetting durability = add(new BooleanSetting("Durability", true));

    public ArmorStatusHud() {
        super("armor", "Armor Status", "Worn armor with remaining durability", Anchor.MIDDLE_RIGHT, -16, 0);
    }

    private List<ArmorPiece> pieces(boolean preview) {
        List<ArmorPiece> out = new ArrayList<ArmorPiece>();
        for (ArmorPiece p : nova().platform().getArmor()) {
            if (p != null) out.add(p);
        }
        if (out.isEmpty() && preview) {
            for (ArmorPiece p : SAMPLE) out.add(p);
        }
        return out;
    }

    private boolean vertical() {
        return orientation.is("Vertical");
    }

    private float pieceWidth(Renderer r, ArmorPiece p) {
        if (!durability.get() || p.durability < 0) return ICON;
        return ICON + TEXT_GAP + r.textWidth(HudStyle.TEXT_12, String.valueOf(p.durability));
    }

    @Override
    public float getWidth(Renderer r, boolean preview) {
        List<ArmorPiece> list = pieces(preview);
        float w = 0;
        for (ArmorPiece p : list) {
            w = vertical() ? Math.max(w, pieceWidth(r, p)) : w + pieceWidth(r, p);
        }
        if (!vertical() && list.size() > 1) w += ITEM_GAP * (list.size() - 1);
        return w;
    }

    @Override
    public float getHeight(Renderer r, boolean preview) {
        int n = pieces(preview).size();
        if (n == 0) return 0;
        return vertical() ? n * ICON + (n - 1) * ROW_GAP : ICON;
    }

    @Override
    public void render(Renderer r, boolean preview) {
        float x = 0, y = 0;
        for (ArmorPiece p : pieces(preview)) {
            if (p.stack != null) {
                nova().platform().drawItem(p.stack, x, y, ICON);
            } else {
                r.roundedRect(x + 2, y + 2, ICON - 4, ICON - 4, 3, 0x40FFFFFF); // editor placeholder
            }
            if (durability.get() && p.durability >= 0) {
                String text = String.valueOf(p.durability);
                HudStyle.textCenteredV(r, HudStyle.TEXT_12, text, x + ICON + TEXT_GAP, y + ICON / 2f,
                        p.durability >= 100 ? HudStyle.GOOD : HudStyle.BAD);
            }
            if (vertical()) {
                y += ICON + ROW_GAP;
            } else {
                x += pieceWidth(r, p) + ITEM_GAP;
            }
        }
    }
}
