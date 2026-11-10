package dev.kuroinu.nova.ui;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.render.Renderer;

/**
 * A version-independent screen. Each Minecraft version has one thin wrapper that hosts any
 * NovaScreen as a vanilla screen and forwards rendering and input to it, so new screens never
 * need version-specific code.
 */
public abstract class NovaScreen {
    protected int width;
    protected int height;
    /** Renderer from the most recent frame; valid for text measurement during input handling. */
    protected Renderer measure;
    private NovaScreen parent;

    /** Called when the screen is shown and on every resize. */
    public final void init(int width, int height) {
        this.width = width;
        this.height = height;
        onResize();
    }

    public final void renderFrame(Renderer r, int mouseX, int mouseY, float delta) {
        this.measure = r;
        render(r, mouseX, mouseY, delta);
    }

    protected abstract void render(Renderer r, int mouseX, int mouseY, float delta);

    /**
     * Draws this screen behind a child screen opened over it (e.g. the ClickGUI over the main
     * menu). {@code contentAlpha} lets screens fade their foreground out while the child is shown;
     * input is never routed here. Default: render normally with the mouse off-screen.
     */
    public void renderAsBackdrop(Renderer r, float contentAlpha, float delta) {
        renderFrame(r, -10000, -10000, delta);
    }

    protected void onResize() {
    }

    /** Called once each time the screen becomes visible. */
    public void onOpen() {
    }

    /**
     * Called when the screen is shown again after a screen it opened (e.g. a vanilla options
     * screen) returns to it. {@link #onOpen} is not called again in that case.
     */
    public void onResume() {
    }

    /** Called once each time the screen stops being visible. */
    public void onClose() {
    }

    public boolean mouseClicked(double x, double y, int button) {
        return false;
    }

    public void mouseReleased(double x, double y, int button) {
    }

    /** @param amount positive when scrolling up */
    public boolean mouseScrolled(double x, double y, double amount) {
        return false;
    }

    /**
     * @param keyCode raw code in the platform's key space (see {@link dev.kuroinu.nova.platform.Platform#keySpace})
     * @return true if handled. An unhandled ESCAPE closes the screen.
     */
    public boolean keyPressed(int keyCode, UiKey key) {
        return false;
    }

    public void charTyped(char c) {
    }

    public boolean pausesGame() {
        return false;
    }

    /** Returns to the parent screen, or to the game if there is none. */
    public void close() {
        NovaClient.get().platform().openScreen(parent);
    }

    public NovaScreen getParent() {
        return parent;
    }

    public <S extends NovaScreen> S withParent(NovaScreen parent) {
        this.parent = parent;
        @SuppressWarnings("unchecked")
        S self = (S) this;
        return self;
    }
}
