package spz.meowing.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;

public class HUD extends Module {

    // Elements
    private final BooleanSetting showFps = addSetting(new BooleanSetting("Show FPS", true));
    private final BooleanSetting showTps = addSetting(new BooleanSetting("Show TPS", true));
    private final BooleanSetting showPing = addSetting(new BooleanSetting("Show Ping", true));
    private final BooleanSetting showSpeed = addSetting(new BooleanSetting("Show Speed", true));
    private final BooleanSetting showCoords = addSetting(new BooleanSetting("Show Coords", true));
    private final BooleanSetting showDirection = addSetting(new BooleanSetting("Show Direction", true));
    private final BooleanSetting showBiome = addSetting(new BooleanSetting("Show Biome", false));
    private final BooleanSetting showTime = addSetting(new BooleanSetting("Show Time", false));
    private final ModeSetting timeFormat = addSetting(new ModeSetting("Time Format", "24h", "24h", "12h"));

    // Style
    private final ModeSetting colorTheme = addSetting(new ModeSetting("Color", "Purple",
            "Purple", "Blue", "Cyan", "Green", "Red", "Orange", "Pink", "White", "Chroma"));
    private final ModeSetting labelColor = addSetting(new ModeSetting("Label Color", "Gray",
            "Gray", "White", "Match Accent", "Dark"));
    private final ModeSetting separator = addSetting(new ModeSetting("Separator", "Space",
            "Space", "Colon", "Dash", "Bracket"));
    private final BooleanSetting shadow = addSetting(new BooleanSetting("Text Shadow", true));
    private final BooleanSetting background = addSetting(new BooleanSetting("Background", true));
    private final NumberSetting bgOpacity = addSetting(new NumberSetting("BG Opacity", 0.5, 0.0, 1.0, 0.05));
    private final NumberSetting lineSpacing = addSetting(new NumberSetting("Line Spacing", 11, 8, 16, 1));

    // TPS tracking — uses server world time to measure actual server tick rate
    private long lastWorldTime = -1;
    private long lastWorldCheckMs = 0;
    private double[] tpsSamples = new double[10];
    private int tpsSampleIdx = 0;
    private double currentTps = 20.0;

    // Ping tracking
    private int measuredPing = -1;

    // Speed tracking
    private double lastX, lastY, lastZ;
    private double currentSpeed = 0.0;

    public HUD() {
        super("HUD", "Displays info on screen", Category.RENDER, -1);
    }

    @Override
    public void onEnable() {
        lastWorldTime = -1;
        lastWorldCheckMs = 0;
        lastX = lastY = lastZ = 0;
        java.util.Arrays.fill(tpsSamples, 20.0);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // TPS: measure how many world ticks pass per real-time second
        long worldTime = mc.world.getTime();
        long now = System.currentTimeMillis();

        if (lastWorldTime >= 0 && lastWorldCheckMs > 0) {
            long realElapsed = now - lastWorldCheckMs;
            long worldElapsed = worldTime - lastWorldTime;

            // Only sample if enough real time passed (avoid division noise)
            if (realElapsed >= 500) {
                double tps = (worldElapsed * 1000.0) / realElapsed;
                tps = Math.min(20.0, Math.max(0.0, tps));
                tpsSamples[tpsSampleIdx] = tps;
                tpsSampleIdx = (tpsSampleIdx + 1) % tpsSamples.length;

                // Average across samples
                double total = 0;
                for (double s : tpsSamples) total += s;
                currentTps = total / tpsSamples.length;

                lastWorldTime = worldTime;
                lastWorldCheckMs = now;
            }
        } else {
            lastWorldTime = worldTime;
            lastWorldCheckMs = now;
        }

        // Speed tracking
        double dx = mc.player.getX() - lastX;
        double dy = mc.player.getY() - lastY;
        double dz = mc.player.getZ() - lastZ;
        currentSpeed = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
        lastX = mc.player.getX();
        lastY = mc.player.getY();
        lastZ = mc.player.getZ();
    }

    public boolean isChroma() { return colorTheme.getValue().equals("Chroma"); }

    public int getAccentColor() {
        return switch (colorTheme.getValue()) {
            case "Blue" -> 0xFF3498DB;
            case "Cyan" -> 0xFF00D2FF;
            case "Green" -> 0xFF2ECC71;
            case "Red" -> 0xFFE74C3C;
            case "Orange" -> 0xFFE67E22;
            case "Pink" -> 0xFFFF69B4;
            case "White" -> 0xFFFFFFFF;
            case "Chroma" -> 0xFFFFFFFF; // Handled dynamically in renderer
            default -> 0xFF6C5CE7;
        };
    }

    public int getLabelColor() {
        return switch (labelColor.getValue()) {
            case "White" -> 0xFFDDDDDD;
            case "Match Accent" -> getAccentColor();
            case "Dark" -> 0xFF555555;
            default -> 0xFF999999;
        };
    }

    public String formatLabel(String label) {
        return switch (separator.getValue()) {
            case "Colon" -> label.trim() + ": ";
            case "Dash" -> label.trim() + " - ";
            case "Bracket" -> "[" + label.trim() + "] ";
            default -> label;
        };
    }

    public boolean shouldShowFps() { return showFps.getValue(); }
    public boolean shouldShowTps() { return showTps.getValue(); }
    public boolean shouldShowPing() { return showPing.getValue(); }
    public boolean shouldShowSpeed() { return showSpeed.getValue(); }
    public boolean shouldShowCoords() { return showCoords.getValue(); }
    public boolean shouldShowDirection() { return showDirection.getValue(); }
    public boolean shouldShowBiome() { return showBiome.getValue(); }
    public boolean shouldShowTime() { return showTime.getValue(); }
    public boolean is12h() { return timeFormat.getValue().equals("12h"); }
    public boolean hasShadow() { return shadow.getValue(); }
    public boolean hasBackground() { return background.getValue(); }
    public int getBgAlpha() { return (int) (bgOpacity.getValue() * 255); }
    public int getLineSpacing() { return (int) lineSpacing.getValue().doubleValue(); }
    public double getCurrentTps() { return currentTps; }
    public double getCurrentSpeed() { return currentSpeed; }

    public int getPing() {
        // Use measured keepalive ping if valid
        if (measuredPing > 1) return measuredPing;

        // Fallback to player list — accept any value >= 1
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return -1;
        try {
            // Try by UUID
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null && entry.getLatency() >= 1) return entry.getLatency();

            // Try by name
            String name = mc.player.getName().getString();
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                String displayName = e.getDisplayName() != null ? e.getDisplayName().getString() : "";
                String profileName = e.getProfile().name();
                if (profileName.equals(name) || displayName.contains(name)) {
                    if (e.getLatency() >= 1) return e.getLatency();
                }
            }
        } catch (Exception ignored) {}
        // Final fallback: use static global ping from any handler instance
        if (globalMeasuredPing > 0) return globalMeasuredPing;
        return measuredPing > 0 ? measuredPing : -1;
    }

    // Called from PingMixin — static so it works even before HUD is enabled
    private static int globalMeasuredPing = -1;

    public void onPongReceived(int ping) {
        if (ping > 0 && ping < 10000) {
            measuredPing = ping;
            globalMeasuredPing = ping;
        }
    }

    public static int getGlobalPing() {
        return globalMeasuredPing;
    }

    public String getDirection() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return "";
        float yaw = mc.player.getYaw() % 360;
        if (yaw < 0) yaw += 360;
        if (yaw >= 315 || yaw < 45) return "S";
        if (yaw >= 45 && yaw < 135) return "W";
        if (yaw >= 135 && yaw < 225) return "N";
        return "E";
    }
}
