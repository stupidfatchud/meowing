package spz.meowing.module.impl;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import spz.meowing.gui.ClickGUI;
import spz.meowing.module.Category;
import spz.meowing.module.Module;

public class ClickGUIModule extends Module {

    public ClickGUIModule() {
        super("ClickGUI", "Opens the GUI", Category.MISC, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setScreen(ClickGUI.getInstance());
    }

    @Override
    public void onDisable() {
        // Screen handles its own closing via removed()
    }
}
