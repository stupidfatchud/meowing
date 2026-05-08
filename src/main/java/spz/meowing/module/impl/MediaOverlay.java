package spz.meowing.module.impl;

import spz.meowing.media.MediaProvider;
import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;

public class MediaOverlay extends Module {

    // Display
    private final BooleanSetting showArtist = addSetting(new BooleanSetting("Show Artist", true));
    private final BooleanSetting showProgress = addSetting(new BooleanSetting("Progress Bar", true));
    private final BooleanSetting showTime = addSetting(new BooleanSetting("Show Time", true));
    private final BooleanSetting showWhenPaused = addSetting(new BooleanSetting("Show When Paused", true));
    private final BooleanSetting scrollTitle = addSetting(new BooleanSetting("Scroll Long Title", true));
    private final BooleanSetting showIcon = addSetting(new BooleanSetting("Music Icon", true));

    // Style
    private final NumberSetting opacity = addSetting(new NumberSetting("Opacity", 0.88, 0.2, 1.0, 0.05));
    private final NumberSetting cornerRadius = addSetting(new NumberSetting("Corner Radius", 6, 0, 12, 1));
    private final BooleanSetting showShadow = addSetting(new BooleanSetting("Shadow", true));
    private final ModeSetting accentColor = addSetting(new ModeSetting("Accent", "Green",
            "None", "Green", "Purple", "Blue", "Cyan", "Pink", "Orange", "White", "Chroma"));

    // Animation
    private final NumberSetting animSpeed = addSetting(new NumberSetting("Anim Speed", 1.0, 0.3, 3.0, 0.1));

    public MediaOverlay() {
        super("MediaOverlay", "Shows currently playing media", Category.RENDER, -1);
    }

    @Override
    public void onEnable() { MediaProvider.start(); }

    @Override
    public void onDisable() { MediaProvider.stop(); }

    // Getters
    public boolean showArtist() { return showArtist.getValue(); }
    public boolean showProgress() { return showProgress.getValue(); }
    public boolean showTime() { return showTime.getValue(); }
    public boolean showWhenPaused() { return showWhenPaused.getValue(); }
    public boolean scrollTitle() { return scrollTitle.getValue(); }
    public boolean showIcon() { return showIcon.getValue(); }
    public int getBgAlpha() { return (int) (opacity.getValue() * 255); }
    public int getCornerRadius() { return (int) cornerRadius.getValue().doubleValue(); }
    public boolean hasShadow() { return showShadow.getValue(); }
    public float getAnimSpeed() { return (float) animSpeed.getValue().doubleValue(); }
    public boolean isChroma() { return accentColor.getValue().equals("Chroma"); }
    public boolean hasAccent() { return !accentColor.getValue().equals("None"); }

    public int getAccent() {
        return switch (accentColor.getValue()) {
            case "None" -> 0x00000000;
            case "Purple" -> 0xFF6C5CE7;
            case "Blue" -> 0xFF3498DB;
            case "Cyan" -> 0xFF00D2FF;
            case "Pink" -> 0xFFFF69B4;
            case "Orange" -> 0xFFE67E22;
            case "White" -> 0xFFFFFFFF;
            case "Chroma" -> 0xFFFFFFFF;
            default -> 0xFF1DB954; // Green
        };
    }
}
