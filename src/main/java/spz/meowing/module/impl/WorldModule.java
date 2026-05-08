package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;

public class WorldModule extends Module {

    // Time
    private final ModeSetting timeMode = addSetting(new ModeSetting("Time", "Day",
            "Server", "Day", "Night", "Sunrise", "Sunset", "Custom", "Real Time"));
    private final NumberSetting customTime = addSetting(new NumberSetting("Custom Time", 6000, 0, 24000, 100));

    // Weather
    private final ModeSetting weather = addSetting(new ModeSetting("Weather", "Clear",
            "Server", "Clear", "Rain", "Thunder"));

    public WorldModule() {
        super("World", "Client-side time & weather overrides", Category.MISC, -1);
        customTime.setVisibility(() -> timeMode.getValue().equals("Custom"));
    }

    public long getWorldTime() {
        return switch (timeMode.getValue()) {
            case "Day" -> 1000;
            case "Night" -> 13000;
            case "Sunrise" -> 23000;
            case "Sunset" -> 12500;
            case "Custom" -> (long) customTime.getValue().doubleValue();
            case "Real Time" -> {
                java.time.LocalTime now = java.time.LocalTime.now();
                int seconds = now.toSecondOfDay();
                long ticks = (long) ((seconds / 86400.0) * 24000.0) - 6000;
                if (ticks < 0) ticks += 24000;
                yield ticks;
            }
            default -> -1; // Server = don't override
        };
    }

    public boolean shouldOverrideTime() {
        return !timeMode.getValue().equals("Server");
    }

    public boolean shouldClearWeather() {
        return weather.getValue().equals("Clear");
    }

    public boolean shouldRain() {
        return weather.getValue().equals("Rain") || weather.getValue().equals("Thunder");
    }

    public boolean shouldThunder() {
        return weather.getValue().equals("Thunder");
    }

    public boolean shouldOverrideWeather() {
        return !weather.getValue().equals("Server");
    }
}
