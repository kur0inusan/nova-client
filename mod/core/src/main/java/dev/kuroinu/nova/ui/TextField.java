package dev.kuroinu.nova.ui;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.render.Colors;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.render.font.NovaFont;

/** Single-line text input in Nova's style: typing, backspace, Ctrl+V paste, blinking caret, focus border. */
public final class TextField {
    private final NovaFont font;
    private final String placeholder;
    private final int maxLength;
    private final Animation focusAnim = new Animation(0.08f, 0f);
    private final Animation hoverAnim = new Animation(0.08f, 0f);
    private String value = "";
    private boolean focused;
    private long lastEdit;

    public TextField(NovaFont font, String placeholder, int maxLength) {
        this.font = font;
        this.placeholder = placeholder;
        this.maxLength = maxLength;
    }

    public String get() {
        return value;
    }

    public void set(String v) {
        value = v.length() > maxLength ? v.substring(0, maxLength) : v;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        lastEdit = System.currentTimeMillis();
    }

    public void type(char c) {
        if (c >= 32 && c != 127 && c != '§' && value.length() < maxLength) {
            value += c;
            lastEdit = System.currentTimeMillis();
        }
    }

    /** Handles editing keys; returns true when consumed. */
    public boolean key(UiKey key) {
        if (key == UiKey.BACKSPACE) {
            if (!value.isEmpty()) value = value.substring(0, value.length() - 1);
            lastEdit = System.currentTimeMillis();
            return true;
        }
        if (key == UiKey.PASTE) {
            String clip = NovaClient.get().platform().getClipboard().replace("\n", "").replace("\r", "").trim();
            for (int i = 0; i < clip.length(); i++) type(clip.charAt(i));
            return true;
        }
        return false;
    }

    public void draw(Renderer r, float x, float y, float w, float h, boolean hovered, int accent) {
        float f = focusAnim.update(focused ? 1f : 0f);
        float hv = hoverAnim.update(hovered ? 1f : 0f);
        r.roundedRect(x, y, w, h, 8, Theme.INPUT);
        r.roundedOutline(x, y, w, h, 8, 1,
                Colors.lerp(Colors.lerp(Theme.BORDER_8, 0x24FFFFFF, hv), Colors.withAlpha(accent, 0x90), f));
        float cy = y + h / 2f;
        // Caret stays solid while typing, then blinks.
        boolean caret = focused && ((System.currentTimeMillis() - lastEdit) < 500 || (System.currentTimeMillis() / 530) % 2 == 0);
        if (value.isEmpty()) {
            r.textCenteredV(font, placeholder, x + 12, cy, Theme.MUTED_2);
            if (caret) r.rect(x + 12, cy - 7, 1, 14, Theme.TEXT);
        } else {
            String shown = value;
            while (r.textWidth(font, shown) > w - 26 && shown.length() > 1) shown = shown.substring(1);
            r.textCenteredV(font, shown, x + 12, cy, Theme.TEXT);
            if (caret) r.rect(x + 12 + r.textWidth(font, shown) + 1, cy - 7, 1, 14, Theme.TEXT);
        }
    }
}
