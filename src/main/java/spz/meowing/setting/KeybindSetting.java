package spz.meowing.setting;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindSetting extends Setting<Integer> {

    public KeybindSetting(String name, int defaultKeyCode) {
        super(name, defaultKeyCode);
    }

    public String getKeyName() {
        int keyCode = getValue();
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode == -1) {
            return "None";
        }
        try {
            return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
        } catch (Exception e) {
            return "Key " + keyCode;
        }
    }

    public boolean isUnbound() {
        return getValue() == GLFW.GLFW_KEY_UNKNOWN || getValue() == -1;
    }
}
