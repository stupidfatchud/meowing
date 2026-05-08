package spz.meowing.module;

import spz.meowing.setting.KeybindSetting;
import spz.meowing.setting.Setting;
import spz.meowing.util.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private final KeybindSetting keybind;
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category, int defaultKeyCode) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keybind = new KeybindSetting("Keybind", defaultKeyCode);
    }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
        ConfigManager.markDirty();
    }

    public void forceSetEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onTick() {}

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeyCode() {
        return keybind.getValue();
    }

    public void setKeyCode(int keyCode) {
        keybind.setValue(keyCode);
    }

    public KeybindSetting getKeybindSetting() {
        return keybind;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }
}
