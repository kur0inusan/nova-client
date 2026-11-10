package dev.kuroinu.nova.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.kuroinu.nova.hud.HudElement;
import dev.kuroinu.nova.module.Module;
import dev.kuroinu.nova.module.ModuleManager;
import dev.kuroinu.nova.setting.Setting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists module state to {@code config/nova/profile.json}. Unknown keys are ignored and
 * missing keys keep their defaults, so adding or removing modules never breaks old configs.
 */
public final class ConfigManager {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File dir;
    private final ModuleManager modules;
    private final String keySpace;
    /** Binds as loaded, per module, including other versions' key spaces (preserved on save). */
    private final Map<String, JsonObject> loadedBinds = new HashMap<String, JsonObject>();

    public ConfigManager(File dir, ModuleManager modules) {
        this(dir, modules, "default");
    }

    /**
     * @param keySpace the platform's key-code space; binds are stored under it so a config shared
     *                 between Minecraft versions keeps each version's binds separate
     */
    public ConfigManager(File dir, ModuleManager modules, String keySpace) {
        this.dir = dir;
        this.modules = modules;
        this.keySpace = keySpace;
    }

    public File getProfileFile() {
        return new File(dir, "profile.json");
    }

    public synchronized void load() {
        File file = getProfileFile();
        if (!file.isFile()) {
            return;
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), UTF8);
            JsonElement root = new JsonParser().parse(reader);
            if (root != null && root.isJsonObject()) {
                apply(root.getAsJsonObject());
            }
        } catch (Exception e) {
            // A corrupt config must never stop the game from starting; keep a copy for debugging.
            file.renameTo(new File(dir, "profile.json.broken"));
        } finally {
            closeQuietly(reader);
        }
    }

    public synchronized void save() {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            return;
        }
        File tmp = new File(dir, "profile.json.tmp");
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(tmp), UTF8);
            GSON.toJson(serialize(), writer);
            writer.close();
            writer = null;
            File target = getProfileFile();
            if (target.exists() && !target.delete()) {
                return;
            }
            tmp.renameTo(target);
        } catch (IOException ignored) {
            // best effort
        } finally {
            closeQuietly(writer);
        }
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("layoutVersion", HudElement.LAYOUT_VERSION);
        JsonObject mods = new JsonObject();
        for (Module m : modules.getModules()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("enabled", m.isEnabled());
            obj.addProperty("favorite", m.isFavorite());
            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getSettings()) {
                settings.add(s.getName(), s.toJson());
            }
            obj.add("settings", settings);
            if (m instanceof HudElement) {
                JsonObject layout = new JsonObject();
                ((HudElement) m).writeLayout(layout);
                obj.add("layout", layout);
            }
            JsonObject binds = new JsonObject();
            JsonObject previous = loadedBinds.get(m.getId());
            if (previous != null) {
                for (Map.Entry<String, JsonElement> e : previous.entrySet()) {
                    binds.add(e.getKey(), e.getValue());
                }
            }
            binds.remove(keySpace);
            if (m.getBindKey() != Module.NO_KEY) {
                binds.addProperty(keySpace, m.getBindKey());
            }
            obj.add("binds", binds);
            mods.add(m.getId(), obj);
        }
        root.add("modules", mods);
        return root;
    }

    public void apply(JsonObject root) {
        JsonElement modsEl = root.get("modules");
        if (modsEl == null || !modsEl.isJsonObject()) {
            return;
        }
        JsonObject mods = modsEl.getAsJsonObject();
        // Layouts saved in an older coordinate system would land in the wrong place; use defaults instead.
        JsonElement lv = root.get("layoutVersion");
        boolean layoutsCurrent = lv != null && lv.isJsonPrimitive() && lv.getAsJsonPrimitive().isNumber()
                && lv.getAsInt() == HudElement.LAYOUT_VERSION;
        for (Module m : modules.getModules()) {
            JsonElement el = mods.get(m.getId());
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            JsonElement settingsEl = obj.get("settings");
            if (settingsEl != null && settingsEl.isJsonObject()) {
                JsonObject settings = settingsEl.getAsJsonObject();
                for (Setting<?> s : m.getSettings()) {
                    s.fromJson(settings.get(s.getName()));
                }
            }
            JsonElement layout = obj.get("layout");
            if (m instanceof HudElement && layoutsCurrent && layout != null && layout.isJsonObject()) {
                ((HudElement) m).readLayout(layout.getAsJsonObject());
            }
            JsonElement binds = obj.get("binds");
            if (binds != null && binds.isJsonObject()) {
                JsonObject b = binds.getAsJsonObject();
                loadedBinds.put(m.getId(), b);
                JsonElement key = b.get(keySpace);
                if (key != null && key.isJsonPrimitive() && key.getAsJsonPrimitive().isNumber()) {
                    m.setBindKey(key.getAsInt());
                }
            }
            JsonElement favorite = obj.get("favorite");
            if (favorite != null && favorite.isJsonPrimitive() && favorite.getAsJsonPrimitive().isBoolean()) {
                m.setFavorite(favorite.getAsBoolean());
            }
            JsonElement enabled = obj.get("enabled");
            if (m.isToggleable() && enabled != null && enabled.isJsonPrimitive()
                    && enabled.getAsJsonPrimitive().isBoolean()) {
                m.setEnabled(enabled.getAsBoolean());
            }
        }
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }
}
