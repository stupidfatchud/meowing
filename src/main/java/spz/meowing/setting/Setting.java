package spz.meowing.setting;

import spz.meowing.util.ConfigManager;

import java.util.function.Supplier;

public abstract class Setting<T> {

    private final String name;
    private T value;
    private final T defaultValue;
    private Supplier<Boolean> visible = () -> true;

    public Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        ConfigManager.markDirty();
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public Setting<T> setVisibility(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
