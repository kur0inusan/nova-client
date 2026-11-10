package dev.kuroinu.nova.hud;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.EventBus;
import dev.kuroinu.nova.event.Events;
import dev.kuroinu.nova.module.Module;
import dev.kuroinu.nova.module.ModuleManager;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.ui.ToastManager;
import dev.kuroinu.nova.ui.UiScale;

import java.util.ArrayList;
import java.util.List;

/** Draws enabled HUD elements (and toasts) every frame, in design units. */
public final class HudManager {
    private final ModuleManager modules;
    private boolean editorOpen;

    public HudManager(EventBus bus, final ModuleManager modules) {
        this.modules = modules;
        bus.subscribe(Events.Render2D.class, event -> {
            Renderer r = event.renderer;
            float[] units = UiScale.push(r, event.width, event.height);
            try {
                if (!editorOpen) {
                    renderAll(r, units[0], units[1], false);
                }
                ToastManager toasts = NovaClient.get().toasts();
                if (!toasts.isDrawnByScreen() && !toasts.isEmpty()) {
                    toasts.render(r, units[0], units[1]);
                }
            } finally {
                r.pop();
            }
        });
    }

    public List<HudElement> getElements() {
        List<HudElement> out = new ArrayList<HudElement>();
        for (Module m : modules.getModules()) {
            if (m instanceof HudElement) {
                out.add((HudElement) m);
            }
        }
        return out;
    }

    public void renderAll(Renderer r, float screenW, float screenH, boolean preview) {
        for (HudElement e : getElements()) {
            if (e.isEnabled()) {
                renderElement(r, e, screenW, screenH, preview);
            }
        }
    }

    public void renderElement(Renderer r, HudElement e, float screenW, float screenH, boolean preview) {
        if (e.getWidth(r, preview) <= 0) {
            return;
        }
        r.pushScale(e.getX(r, screenW, preview), e.getY(r, screenH, preview), e.getScale());
        try {
            e.render(r, preview);
        } finally {
            r.pop();
        }
    }

    public void setEditorOpen(boolean open) {
        this.editorOpen = open;
    }
}
