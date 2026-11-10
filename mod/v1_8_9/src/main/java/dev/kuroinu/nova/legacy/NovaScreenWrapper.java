package dev.kuroinu.nova.legacy;

import dev.kuroinu.nova.ui.NovaScreen;
import dev.kuroinu.nova.ui.UiKey;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Hosts any core {@link NovaScreen} as a 1.8.9 vanilla screen. */
public final class NovaScreenWrapper extends Screen {
    private final NovaScreen screen;
    private boolean opened;
    private boolean hidden;

    public NovaScreenWrapper(NovaScreen screen) {
        this.screen = screen;
    }

    public NovaScreen getNovaScreen() {
        return screen;
    }

    @Override
    public void init() {
        if (!opened) {
            opened = true;
            screen.onOpen();
        } else if (hidden) {
            screen.onResume(); // back from a screen this one opened (init also runs on plain resizes)
        }
        hidden = false;
        Keyboard.enableRepeatEvents(true);
        screen.init(width, height);
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        screen.renderFrame(LegacyRenderer.INSTANCE, mouseX, mouseY, delta);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        screen.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        screen.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void handleMouse() {
        super.handleMouse();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            // LWJGL2 reports window pixels with a bottom-left origin; convert to GUI coordinates.
            int mx = Mouse.getEventX() * width / client.width;
            int my = height - Mouse.getEventY() * height / client.height - 1;
            screen.mouseScrolled(mx, my, wheel);
        }
    }

    @Override
    protected void keyPressed(char character, int keyCode) {
        // LWJGL2 reports Ctrl+V as the control character SYN (0x16).
        UiKey key = character == 22 ? UiKey.PASTE : translate(keyCode);
        boolean handled = screen.keyPressed(keyCode, key);
        if (!handled && key == UiKey.ESCAPE) {
            screen.close();
            return;
        }
        if (!handled && character >= 32 && character != 127) {
            screen.charTyped(character);
        }
    }

    private static UiKey translate(int keyCode) {
        switch (keyCode) {
            case Keyboard.KEY_ESCAPE: return UiKey.ESCAPE;
            case Keyboard.KEY_BACK: return UiKey.BACKSPACE;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER: return UiKey.ENTER;
            case Keyboard.KEY_DELETE: return UiKey.DELETE;
            case Keyboard.KEY_TAB: return UiKey.TAB;
            default: return UiKey.OTHER;
        }
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
        hidden = true;
        screen.onClose();
    }

    @Override
    public boolean shouldPauseGame() {
        return screen.pausesGame();
    }
}
