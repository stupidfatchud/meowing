package spz.meowing.util;

public final class ColorUtil {

    // Background
    public static final int BACKGROUND_DIM    = 0x88000000;

    // Panels
    public static final int PANEL_BG          = 0xF0111118;
    public static final int PANEL_HEADER      = 0xFF18181F;
    public static final int PANEL_HEADER_END  = 0xFF18181F;

    // Accent (default purple)
    public static final int ACCENT            = 0xFF6C5CE7;
    public static final int ACCENT_DIM        = 0xFF5A4BD4;

    // Modules
    public static final int MODULE_BG         = 0xFF0E0E16;
    public static final int MODULE_HOVER      = 0xFF1A1A28;
    public static final int MODULE_ENABLED    = ACCENT;
    public static final int MODULE_SEPARATOR  = 0xFF1A1A22;

    // Settings
    public static final int SETTING_BG        = 0xFF0A0A12;
    public static final int TOGGLE_ON         = ACCENT;
    public static final int TOGGLE_OFF        = 0xFF2A2A35;
    public static final int SLIDER_BG         = 0xFF1E1E2A;
    public static final int SLIDER_FILL       = ACCENT;

    // Text
    public static final int TEXT_PRIMARY      = 0xFFE8E8EE;
    public static final int TEXT_SECONDARY    = 0xFF6E6E80;
    public static final int TEXT_ACCENT       = ACCENT;

    // Keybind
    public static final int KEYBIND_BG        = 0xFF1E1E2C;
    public static final int KEYBIND_LISTEN    = 0xFFE74C3C;

    private ColorUtil() {}

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static int interpolate(int from, int to, float progress) {
        progress = Math.max(0, Math.min(1, progress));
        int aF = (from >> 24) & 0xFF, aT = (to >> 24) & 0xFF;
        int rF = (from >> 16) & 0xFF, rT = (to >> 16) & 0xFF;
        int gF = (from >> 8)  & 0xFF, gT = (to >> 8)  & 0xFF;
        int bF =  from        & 0xFF, bT =  to         & 0xFF;
        int a = (int) (aF + (aT - aF) * progress);
        int r = (int) (rF + (rT - rF) * progress);
        int g = (int) (gF + (gT - gF) * progress);
        int b = (int) (bF + (bT - bF) * progress);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int brighter(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * (1 + factor)));
        int g = Math.min(255, (int) (((color >>  8) & 0xFF) * (1 + factor)));
        int b = Math.min(255, (int) (( color        & 0xFF) * (1 + factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int darker(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, (int) (((color >> 16) & 0xFF) * (1 - factor)));
        int g = Math.max(0, (int) (((color >>  8) & 0xFF) * (1 - factor)));
        int b = Math.max(0, (int) (( color        & 0xFF) * (1 - factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
