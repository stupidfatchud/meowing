package spz.meowing.module.impl;

import net.minecraft.client.MinecraftClient;
import spz.meowing.gui.WhiteboardScreen;
import spz.meowing.module.Category;
import spz.meowing.module.Module;

public class Whiteboard extends Module {

    public Whiteboard() {
        super("Whiteboard", "Open a drawing canvas", Category.MISC, -1);
    }

    @Override
    public void onEnable() {
        MinecraftClient.getInstance().setScreen(WhiteboardScreen.getInstance());
    }

    @Override
    public void onDisable() {
        // Screen handles its own closing
    }
}
