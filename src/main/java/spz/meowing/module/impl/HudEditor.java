package spz.meowing.module.impl;

import net.minecraft.client.MinecraftClient;
import spz.meowing.gui.HudEditorScreen;
import spz.meowing.module.Category;
import spz.meowing.module.Module;

public class HudEditor extends Module {

    public HudEditor() {
        super("HudEditor", "Drag HUD elements to reposition", Category.MISC, -1);
    }

    @Override
    public void onEnable() {
        MinecraftClient.getInstance().setScreen(HudEditorScreen.getInstance());
    }

    @Override
    public void onDisable() {
        // Screen handles its own closing
    }
}
