package spz.meowing.module.impl.dungeons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import spz.meowing.gui.NotificationManager;
import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;
import spz.meowing.setting.StringSetting;
import spz.meowing.util.SkyblockDetector;

public class WatcherAlert extends Module {

    private final BooleanSetting dungeonOnly = addSetting(new BooleanSetting("Dungeon Only", true));
    private final ModeSetting alertMode = addSetting(new ModeSetting("Alert Style", "Title",
            "Title", "Notification", "Both"));
    private final NumberSetting titleScale = addSetting(new NumberSetting("Title Scale", 2.0, 1.0, 5.0, 0.5));
    private final ModeSetting alertColor = addSetting(new ModeSetting("Alert Color", "White",
            "White", "Red", "Green", "Blue", "Cyan", "Purple", "Orange", "Yellow", "Pink"));

    // Editable alert texts
    private final StringSetting watcherMobText = addSetting(new StringSetting("Watcher Mob Text", "Watcher!", 32));
    private final StringSetting bloodCompleteText = addSetting(new StringSetting("Blood Complete Text", "Blood Complete!", 32));
    private final StringSetting bloodDoorText = addSetting(new StringSetting("Blood Door Text", "Blood Door Opened!", 32));
    private final StringSetting keyText = addSetting(new StringSetting("Key Text", "Key Obtained!", 32));
    private final StringSetting bossText = addSetting(new StringSetting("Boss Text", "Boss!", 32));
    private final StringSetting deathText = addSetting(new StringSetting("Death Text", "Death!", 32));

    // Toggles
    private final BooleanSetting watcherSpawn = addSetting(new BooleanSetting("Watcher Spawn", true));
    private final BooleanSetting bloodDone = addSetting(new BooleanSetting("Blood Done", true));
    private final BooleanSetting keyPickup = addSetting(new BooleanSetting("Key Pickup", true));
    private final BooleanSetting bossSpawn = addSetting(new BooleanSetting("Boss Spawn", true));
    private final BooleanSetting deathAlert = addSetting(new BooleanSetting("Death Alert", true));

    // Alert state
    private String currentTitle = "";
    private String currentSubtitle = "";
    private int currentColor = 0xFFFFFFFF;
    private long alertStartTime = 0;
    private static final long FADE_IN = 200;
    private static final long STAY = 1500;
    private static final long FADE_OUT = 500;

    public WatcherAlert() {
        super("WatcherAlert", "Dungeon chat notifications", Category.DUNGEONS, -1);
    }

    private int getColorFromSetting() {
        return switch (alertColor.getValue()) {
            case "Red" -> 0xFFE74C3C;
            case "Green" -> 0xFF2ECC71;
            case "Blue" -> 0xFF3498DB;
            case "Cyan" -> 0xFF00D2FF;
            case "Purple" -> 0xFF6C5CE7;
            case "Orange" -> 0xFFE67E22;
            case "Yellow" -> 0xFFFFD700;
            case "Pink" -> 0xFFFF69B4;
            default -> 0xFFFFFFFF;
        };
    }

    public void onChatMessage(String message) {
        if (!isEnabled()) return;
        if (dungeonOnly.getValue() && !SkyblockDetector.isInDungeon()) return;
        String clean = message.replaceAll("§.", "").trim();

        if (watcherSpawn.getValue() && clean.contains("[BOSS] The Watcher:")) {
            if (clean.contains("You have proven yourself") || clean.contains("That will be enough")) {
                alert(bloodCompleteText.getValue(), "The Watcher is satisfied");
            } else if (clean.contains("Let me see") || clean.contains("I could beat")) {
                alert(watcherMobText.getValue(), "Spawning mobs!");
            }
        }

        if (bloodDone.getValue() && clean.contains("The BLOOD DOOR has been opened!")) {
            alert(bloodDoorText.getValue(), "");
        }

        if (keyPickup.getValue()) {
            if (clean.contains("picked up a Wither Key") || clean.contains("has obtained Wither Key")) {
                alert(keyText.getValue(), "Wither Key");
            }
            if (clean.contains("picked up a Blood Key") || clean.contains("has obtained Blood Key")) {
                alert(keyText.getValue(), "Blood Key");
            }
        }

        if (bossSpawn.getValue() && clean.contains("[BOSS]") && !clean.contains("The Watcher")) {
            if (clean.contains("Bonzo") || clean.contains("Scarf") || clean.contains("Professor") ||
                    clean.contains("Thorn") || clean.contains("Livid") || clean.contains("Sadan") ||
                    clean.contains("Maxor") || clean.contains("Storm") || clean.contains("Goldor") ||
                    clean.contains("Necron") || clean.contains("Wither King")) {
                String bossMsg = clean.substring(clean.indexOf("]") + 1).trim();
                alert(bossText.getValue(), bossMsg);
            }
        }

        if (deathAlert.getValue()) {
            if (clean.contains("became a ghost") || clean.contains("was killed")) {
                alert(deathText.getValue(), clean);
            }
        }
    }

    private void alert(String title, String subtitle) {
        String mode = alertMode.getValue();

        if (mode.equals("Notification") || mode.equals("Both")) {
            NotificationManager.warn(title, subtitle);
        }

        if (mode.equals("Title") || mode.equals("Both")) {
            currentTitle = title;
            currentSubtitle = subtitle;
            currentColor = getColorFromSetting();
            alertStartTime = System.currentTimeMillis();
        }
    }

    // Getters for HudRenderer
    public boolean hasActiveTitle() {
        return alertStartTime > 0 && System.currentTimeMillis() - alertStartTime < FADE_IN + STAY + FADE_OUT;
    }

    public float getTitleAlpha() {
        long elapsed = System.currentTimeMillis() - alertStartTime;
        if (elapsed < FADE_IN) return (float) elapsed / FADE_IN;
        if (elapsed < FADE_IN + STAY) return 1f;
        long fadeElapsed = elapsed - FADE_IN - STAY;
        if (fadeElapsed < FADE_OUT) return 1f - (float) fadeElapsed / FADE_OUT;
        return 0f;
    }

    public String getCurrentTitle() { return currentTitle; }
    public String getCurrentSubtitle() { return currentSubtitle; }
    public int getCurrentColor() { return currentColor; }
    public float getTitleScale() { return (float) titleScale.getValue().doubleValue(); }
}
