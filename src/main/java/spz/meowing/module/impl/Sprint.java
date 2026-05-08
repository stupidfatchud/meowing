package spz.meowing.module.impl;

import net.minecraft.client.MinecraftClient;
import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;

public class Sprint extends Module {

    private final BooleanSetting omniSprint = addSetting(new BooleanSetting("Omni-Directional", false));

    public Sprint() {
        super("Sprint", "Automatically sprints", Category.MISC, -1);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (omniSprint.getValue()) {
            mc.player.setSprinting(true);
        } else if (mc.player.forwardSpeed > 0) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }
}
