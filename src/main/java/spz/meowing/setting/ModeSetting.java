package spz.meowing.setting;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = Arrays.asList(modes);
    }

    public void cycle() {
        int index = modes.indexOf(getValue());
        index = (index + 1) % modes.size();
        setValue(modes.get(index));
    }

    public void cycleBack() {
        int index = modes.indexOf(getValue());
        index = (index - 1 + modes.size()) % modes.size();
        setValue(modes.get(index));
    }

    public List<String> getModes() {
        return modes;
    }

    public int getIndex() {
        return modes.indexOf(getValue());
    }
}
