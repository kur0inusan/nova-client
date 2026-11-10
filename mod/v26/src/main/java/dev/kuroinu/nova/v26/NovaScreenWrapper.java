package dev.kuroinu.nova.v26;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kuroinu.nova.ui.NovaScreen;
import dev.kuroinu.nova.ui.UiKey;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Hosts any core {@link NovaScreen} as a 26.x vanilla screen. */
public final class NovaScreenWrapper extends Screen {
    private final NovaScreen screen;
    private boolean opened;
    private boolean hidden;

    public NovaScreenWrapper(NovaScreen screen) {
        super(Component.literal("Nova"));
        this.screen = screen;
    }

    @Override
    protected void init() {
        if (!opened) {
            opened = true;
            screen.onOpen();
        } else if (hidden) {
            screen.onResume(); // back from a screen this one opened (init also runs on plain resizes)
        }
        hidden = false;
        screen.init(width, height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        screen.renderFrame(new V26Renderer(g), mouseX, mouseY, delta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // Nova screens draw their own backdrop; skip the vanilla blur.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return screen.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        screen.mouseReleased(event.x(), event.y(), event.button());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return screen.mouseScrolled(mouseX, mouseY, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        UiKey key = translate(event.key());
        if (screen.keyPressed(event.key(), key)) {
            return true;
        }
        if (key == UiKey.ESCAPE) {
            screen.close();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (Character.isBmpCodePoint(cp)) {
            screen.charTyped((char) cp);
        }
        return true;
    }

    private static UiKey translate(int key) {
        return switch (key) {
            case InputConstants.KEY_ESCAPE -> UiKey.ESCAPE;
            case InputConstants.KEY_BACKSPACE -> UiKey.BACKSPACE;
            case InputConstants.KEY_RETURN -> UiKey.ENTER;
            case InputConstants.KEY_DELETE -> UiKey.DELETE;
            default -> UiKey.OTHER;
        };
    }

    @Override
    public void removed() {
        hidden = true;
        screen.onClose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return screen.pausesGame();
    }
}
