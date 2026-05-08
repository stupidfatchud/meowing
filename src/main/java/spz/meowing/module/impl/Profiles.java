package spz.meowing.module.impl;

import net.minecraft.client.MinecraftClient;
import spz.meowing.gui.NotificationManager;
import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.StringSetting;
import spz.meowing.util.ConfigManager;

import java.util.List;

public class Profiles extends Module {

    private final ModeSetting action = addSetting(new ModeSetting("Action", "Create", "Create", "Load", "Delete", "Export"));
    private final StringSetting profileName = addSetting(new StringSetting("Profile Name", "", 24));

    public Profiles() {
        super("Profiles", "Manage config profiles", Category.MISC, -1);
    }

    @Override
    public void onEnable() {
        String name = profileName.getValue().trim();

        switch (action.getValue()) {
            case "Create" -> {
                if (name.isEmpty()) {
                    NotificationManager.error("Profiles", "Enter a profile name");
                } else {
                    ConfigManager.createProfile(name);
                }
            }
            case "Load" -> {
                if (name.isEmpty()) {
                    NotificationManager.error("Profiles", "Enter profile name to load");
                } else {
                    List<String> profiles = ConfigManager.getProfiles();
                    if (profiles.contains(name)) {
                        ConfigManager.switchProfile(name);
                    } else {
                        NotificationManager.error("Profiles", "Profile not found: " + name);
                    }
                }
            }
            case "Delete" -> {
                if (name.equals("default")) {
                    NotificationManager.error("Profiles", "Cannot delete default");
                } else if (name.isEmpty()) {
                    NotificationManager.error("Profiles", "Enter profile name to delete");
                } else {
                    ConfigManager.deleteProfile(name);
                }
            }
            case "Export" -> {
                String json = ConfigManager.exportProfile();
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.keyboard.setClipboard(json);
                    NotificationManager.success("Profiles", "Copied to clipboard!");
                }
            }
        }

        forceSetEnabled(false);
    }

    @Override
    public String getDescription() {
        return "Active: " + ConfigManager.getActiveProfile() + " | Profiles: " + String.join(", ", ConfigManager.getProfiles());
    }
}
