package dev.kuroinu.nova.render.font;

/** Bundled typefaces (SIL OFL, see assets/nova/fonts/OFL-*.txt). */
public enum FontFace {
    OUTFIT_REGULAR("Outfit-Regular.ttf"),
    OUTFIT_MEDIUM("Outfit-Medium.ttf"),
    OUTFIT_SEMIBOLD("Outfit-SemiBold.ttf"),
    OUTFIT_BOLD("Outfit-Bold.ttf"),
    MONO_REGULAR("JetBrainsMono-Regular.ttf"),
    MONO_SEMIBOLD("JetBrainsMono-SemiBold.ttf");

    public final String resourcePath;

    FontFace(String file) {
        this.resourcePath = "/assets/nova/fonts/" + file;
    }
}
