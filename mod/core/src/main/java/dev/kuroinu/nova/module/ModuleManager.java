package dev.kuroinu.nova.module;

import dev.kuroinu.nova.event.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    private final EventBus bus;
    private final Map<String, Module> modules = new LinkedHashMap<String, Module>();

    public ModuleManager(EventBus bus) {
        this.bus = bus;
    }

    public <M extends Module> M register(M module) {
        if (modules.containsKey(module.getId())) {
            throw new IllegalArgumentException("Duplicate module id: " + module.getId());
        }
        module.attach(bus);
        modules.put(module.getId(), module);
        if (module.isEnabledByDefault()) {
            module.setEnabled(true);
        }
        module.setFavorite(module.isFavoriteByDefault());
        return module;
    }

    public Module get(String id) {
        return modules.get(id);
    }

    @SuppressWarnings("unchecked")
    public <M extends Module> M get(Class<M> type) {
        for (Module m : modules.values()) {
            if (type.isInstance(m)) {
                return (M) m;
            }
        }
        return null;
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(new ArrayList<Module>(modules.values()));
    }

    public List<Module> getModules(Category category) {
        List<Module> out = new ArrayList<Module>();
        for (Module m : modules.values()) {
            if (m.getCategory() == category) {
                out.add(m);
            }
        }
        return out;
    }
}
