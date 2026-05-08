package spz.meowing.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import spz.meowing.gui.CategoryPanel;
import spz.meowing.gui.ClickGUI;
import spz.meowing.gui.HudPositions;
import spz.meowing.gui.NotificationManager;
import spz.meowing.module.Module;
import spz.meowing.module.ModuleManager;
import spz.meowing.setting.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PROFILES_DIR = FabricLoader.getInstance().getConfigDir().resolve("meowing");
    private static final Path META_PATH = PROFILES_DIR.resolve("_meta.json");

    private static long lastSaveRequest = 0;
    private static boolean dirty = false;
    private static boolean loading = false;
    private static String activeProfile = "default";

    private ConfigManager() {}

    // ==================== TICK / DIRTY ====================

    public static void markDirty() {
        if (loading) return;
        dirty = true;
        lastSaveRequest = System.currentTimeMillis();
    }

    public static void tick() {
        if (dirty && System.currentTimeMillis() - lastSaveRequest >= 500) {
            save();
            dirty = false;
        }
    }

    // ==================== PROFILE MANAGEMENT ====================

    public static String getActiveProfile() {
        return activeProfile;
    }

    public static List<String> getProfiles() {
        List<String> profiles = new ArrayList<>();
        try {
            Files.createDirectories(PROFILES_DIR);
            try (var stream = Files.list(PROFILES_DIR)) {
                stream.filter(p -> p.toString().endsWith(".json") && !p.getFileName().toString().startsWith("_"))
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            profiles.add(name.substring(0, name.length() - 5));
                        });
            }
        } catch (Exception ignored) {}
        if (profiles.isEmpty()) profiles.add("default");
        return profiles;
    }

    public static void switchProfile(String name) {
        save(); // Save current first
        activeProfile = name;
        load();
        saveMeta();
        try {
            NotificationManager.success("Profile", "Loaded: " + name);
        } catch (Exception ignored) {}
    }

    public static void createProfile(String name) {
        save(); // Save current first
        activeProfile = name;
        save(); // Save as new profile
        saveMeta();
        try {
            NotificationManager.success("Profile", "Created: " + name);
        } catch (Exception ignored) {}
    }

    public static void deleteProfile(String name) {
        if (name.equals("default")) return; // Can't delete default
        try {
            Path path = PROFILES_DIR.resolve(name + ".json");
            Files.deleteIfExists(path);
            if (activeProfile.equals(name)) {
                activeProfile = "default";
                load();
            }
            NotificationManager.info("Profile", "Deleted: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        saveMeta();
    }

    public static String exportProfile() {
        return GSON.toJson(buildJson());
    }

    public static void importProfile(String name, String json) {
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                NotificationManager.error("Import", "Invalid JSON");
                return;
            }

            Path path = PROFILES_DIR.resolve(name + ".json");
            Files.createDirectories(PROFILES_DIR);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
            NotificationManager.success("Import", "Imported: " + name);
        } catch (Exception e) {
            NotificationManager.error("Import", "Failed: " + e.getMessage());
        }
    }

    // ==================== SAVE ====================

    public static void save() {
        try {
            JsonObject root = buildJson();
            Path path = getProfilePath();
            Files.createDirectories(PROFILES_DIR);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JsonObject buildJson() {
        JsonObject root = new JsonObject();

        // Modules
        JsonObject modules = new JsonObject();
        for (Module module : ModuleManager.getInstance().getModules()) {
            JsonObject mod = new JsonObject();
            mod.addProperty("enabled", module.isEnabled());
            mod.addProperty("keyCode", module.getKeyCode());

            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BooleanSetting b) {
                    settings.addProperty(setting.getName(), b.getValue());
                } else if (setting instanceof NumberSetting n) {
                    settings.addProperty(setting.getName(), n.getValue());
                } else if (setting instanceof ModeSetting m) {
                    settings.addProperty(setting.getName(), m.getValue());
                } else if (setting instanceof StringSetting s) {
                    settings.addProperty(setting.getName(), s.getValue());
                }
            }
            mod.add("settings", settings);
            modules.add(module.getName(), mod);
        }
        root.add("modules", modules);

        // Panel positions
        try {
            JsonObject panels = new JsonObject();
            for (CategoryPanel panel : ClickGUI.getInstance().getPanels()) {
                JsonObject pos = new JsonObject();
                pos.addProperty("x", panel.getX());
                pos.addProperty("y", panel.getY());
                panels.add(panel.getCategory().name(), pos);
            }
            root.add("panels", panels);
        } catch (Exception ignored) {}

        // HUD positions + scales
        JsonObject hudPos = new JsonObject();
        for (var entry : HudPositions.getAll().entrySet()) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", entry.getValue()[0]);
            pos.addProperty("y", entry.getValue()[1]);
            pos.addProperty("scale", HudPositions.getScale(entry.getKey()));
            hudPos.add(entry.getKey(), pos);
        }
        root.add("hudPositions", hudPos);

        return root;
    }

    // ==================== LOAD ====================

    public static void load() {
        // Load meta first to get active profile name
        loadMeta();

        Path path = getProfilePath();
        if (!Files.exists(path)) {
            // Try legacy path
            Path legacy = FabricLoader.getInstance().getConfigDir().resolve("meowing.json");
            if (Files.exists(legacy)) {
                try {
                    Files.createDirectories(PROFILES_DIR);
                    Files.copy(legacy, path);
                } catch (Exception ignored) {}
            }
        }
        if (!Files.exists(path)) return;

        loading = true;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) return;

            loadFromJson(root);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            loading = false;
            dirty = false;
        }
    }

    private static final java.util.Set<String> NEVER_AUTO_ENABLE = java.util.Set.of(
            "ClickGUI", "HudEditor", "Whiteboard", "Profiles"
    );

    private static void loadFromJson(JsonObject root) {
        if (!root.has("modules")) return;
        JsonObject modules = root.getAsJsonObject("modules");

        // Pass 1: safely disable all modules
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.isEnabled() && !NEVER_AUTO_ENABLE.contains(module.getName())) {
                module.forceSetEnabled(false);
            }
        }

        // Pass 2: load keybinds and settings
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!modules.has(module.getName())) continue;
            JsonObject mod = modules.getAsJsonObject(module.getName());
            try {
                if (mod.has("keyCode")) module.setKeyCode(mod.get("keyCode").getAsInt());
                if (mod.has("settings")) {
                    JsonObject settings = mod.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        if (!settings.has(setting.getName())) continue;
                        JsonElement val = settings.get(setting.getName());
                        try {
                            if (setting instanceof BooleanSetting b) b.setValue(val.getAsBoolean());
                            else if (setting instanceof NumberSetting n) n.setValue(val.getAsDouble());
                            else if (setting instanceof ModeSetting m) m.setValue(val.getAsString());
                            else if (setting instanceof StringSetting s) s.setValue(val.getAsString());
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }

        // Pass 3: re-enable saved modules (skip screen-opening ones)
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (NEVER_AUTO_ENABLE.contains(module.getName())) continue;
            if (!modules.has(module.getName())) continue;
            JsonObject mod = modules.getAsJsonObject(module.getName());
            if (mod.has("enabled") && mod.get("enabled").getAsBoolean()) {
                try { module.setEnabled(true); } catch (Exception e) { module.forceSetEnabled(false); }
            }
        }

        // Panel positions
        try {
            if (root.has("panels")) {
                JsonObject panels = root.getAsJsonObject("panels");
                for (CategoryPanel panel : ClickGUI.getInstance().getPanels()) {
                    String key = panel.getCategory().name();
                    if (panels.has(key)) {
                        JsonObject pos = panels.getAsJsonObject(key);
                        if (pos.has("x")) panel.setX(pos.get("x").getAsInt());
                        if (pos.has("y")) panel.setY(pos.get("y").getAsInt());
                    }
                }
            }
        } catch (Exception ignored) {}

        // HUD positions
        if (root.has("hudPositions")) {
            JsonObject hudPos = root.getAsJsonObject("hudPositions");
            for (String key : hudPos.keySet()) {
                JsonObject pos = hudPos.getAsJsonObject(key);
                if (pos.has("x") && pos.has("y")) {
                    HudPositions.set(key, pos.get("x").getAsInt(), pos.get("y").getAsInt());
                }
                if (pos.has("scale")) {
                    HudPositions.setScale(key, pos.get("scale").getAsFloat());
                }
            }
        }
    }

    // ==================== META ====================

    private static void saveMeta() {
        try {
            Files.createDirectories(PROFILES_DIR);
            JsonObject meta = new JsonObject();
            meta.addProperty("activeProfile", activeProfile);
            try (Writer writer = Files.newBufferedWriter(META_PATH)) {
                GSON.toJson(meta, writer);
            }
        } catch (Exception ignored) {}
    }

    private static void loadMeta() {
        if (!Files.exists(META_PATH)) return;
        try (Reader reader = Files.newBufferedReader(META_PATH)) {
            JsonObject meta = GSON.fromJson(reader, JsonObject.class);
            if (meta != null && meta.has("activeProfile")) {
                activeProfile = meta.get("activeProfile").getAsString();
            }
        } catch (Exception ignored) {}
    }

    private static Path getProfilePath() {
        return PROFILES_DIR.resolve(activeProfile + ".json");
    }
}
