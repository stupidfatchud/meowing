package spz.meowing.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;
import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.NumberSetting;

public class FastIT extends Module {

    private final NumberSetting cps = addSetting(new NumberSetting("CPS", 15, 1, 20, 1));

    private long lastClick = 0;

    public FastIT() {
        super("FastIT", "Spam right-click with diamond shovel", Category.MISC, -1);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;

        // Only when holding a diamond shovel
        if (mc.player.getMainHandStack().getItem() != Items.DIAMOND_SHOVEL) return;

        // Only when right click is held
        long handle = mc.getWindow().getHandle();
        if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) return;

        long now = System.currentTimeMillis();
        double interval = 1000.0 / cps.getValue();
        if (now - lastClick < interval) return;

        mc.options.useKey.setPressed(true);
        mc.doItemUse();
        mc.options.useKey.setPressed(false);
        lastClick = now;
    }

    @Override
    public void onEnable() {
        lastClick = 0;
    }
}
