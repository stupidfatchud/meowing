package spz.meowing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spz.meowing.gui.HudRenderer;
import spz.meowing.module.ModuleManager;
import spz.meowing.util.ConfigManager;

public class Meowing implements ClientModInitializer {

    public static final String MOD_ID = "meowing";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Meowing client initializing...");

        ModuleManager.getInstance().init();
        ConfigManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ModuleManager.getInstance().onTick(client);
            ConfigManager.tick();
        });

        HudRenderCallback.EVENT.register(HudRenderer::render);
        spz.meowing.gui.ESPRenderer.register(); // ESP markers on HUD
        spz.meowing.gui.MediaRenderer.register(); // Media overlay
        spz.meowing.gui.WorldRenderer3D.register(); // 3D world-space rendering

        // /meow commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var cmd = net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("meow");

            // /meow — opens ClickGUI
            cmd.executes(ctx -> {
                MinecraftClient.getInstance().send(() -> {
                    var clickGui = ModuleManager.getInstance().getModule("ClickGUI");
                    if (clickGui != null && !clickGui.isEnabled()) clickGui.toggle();
                });
                return 1;
            });

            // /meow maskreset — resets all mask timers
            cmd.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("maskreset")
                    .executes(ctx -> {
                        var mod = ModuleManager.getInstance().getModule("MaskTimers");
                        if (mod instanceof spz.meowing.module.impl.dungeons.MaskTimers mt) {
                            mt.resetAll();
                            spz.meowing.gui.NotificationManager.success("MaskTimers", mt.getResetMessage());
                        }
                        return 1;
                    }));

            dispatcher.register(cmd);
        });

        // Save on game shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::save));

        LOGGER.info("Meowing client initialized!");
    }
}
