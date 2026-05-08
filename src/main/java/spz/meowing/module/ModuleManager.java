package spz.meowing.module;

import net.minecraft.client.MinecraftClient;
import spz.meowing.module.impl.*;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleManager {

    private static ModuleManager instance;
    private final List<Module> modules = new ArrayList<>();
    private final Set<Integer> pressedKeys = new HashSet<>();

    private ModuleManager() {}

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    public void init() {
        // Dungeons
        modules.add(new spz.meowing.module.impl.dungeons.MaskTimers());
        modules.add(new spz.meowing.module.impl.dungeons.StarESP());
        modules.add(new spz.meowing.module.impl.dungeons.WatcherAlert());

        // Render
        modules.add(new AspectRatio());
        modules.add(new CustomTooltip());
        modules.add(new HUD());
        modules.add(new InventoryHUD());
        modules.add(new Keystrokes());
        modules.add(new ModuleList());
        modules.add(new MediaOverlay());
        modules.add(new TargetHUD());

        // Misc
        modules.add(new Animations());
        modules.add(new FastIT());
        modules.add(new Sprint());
        modules.add(new ChromaHUD());
        modules.add(new HudEditor());
        modules.add(new Profiles());
        modules.add(new PurseSpoofer());
        modules.add(new ThemeModule());
        modules.add(new Whiteboard());
        modules.add(new WorldModule());
        modules.add(new ClickGUIModule());
    }

    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (client.getWindow() == null) return;

        // Update Skyblock location detection
        spz.meowing.util.SkyblockDetector.tick();

        try {
            long handle = client.getWindow().getHandle();

            for (Module module : modules) {
                int key = module.getKeyCode();
                if (key <= 0) continue;

                boolean isPressed = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
                boolean wasPressed = pressedKeys.contains(key);

                if (isPressed && !wasPressed && client.currentScreen == null) {
                    module.toggle();
                }

                if (isPressed) pressedKeys.add(key);
                else pressedKeys.remove(key);
            }
        } catch (Exception e) {
            pressedKeys.clear();
        }

        for (Module module : modules) {
            if (module.isEnabled()) {
                try {
                    module.onTick();
                } catch (Exception e) {
                    // Prevent one module from crashing everything
                }
            }
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public Module getModule(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }
}
