package spz.meowing.util;

import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.ThemeModule;

public final class ThemeHelper {

    private ThemeHelper() {}

    private static ThemeModule getTheme() {
        try {
            var mod = ModuleManager.getInstance().getModule("Theme");
            if (mod instanceof ThemeModule t && t.isEnabled()) return t;
        } catch (Exception ignored) {}
        return null;
    }

    public static int accent() {
        ThemeModule t = getTheme();
        return t != null ? t.getAccent() : ColorUtil.ACCENT;
    }

    public static int panelBg() {
        ThemeModule t = getTheme();
        return t != null ? t.getPanelBg() : ColorUtil.PANEL_BG;
    }

    public static int headerBg() {
        ThemeModule t = getTheme();
        return t != null ? t.getHeaderBg() : ColorUtil.PANEL_HEADER;
    }

    public static int moduleBg() {
        ThemeModule t = getTheme();
        return t != null ? t.getModuleBg() : ColorUtil.MODULE_BG;
    }

    public static int moduleHover() {
        ThemeModule t = getTheme();
        return t != null ? t.getModuleHover() : ColorUtil.MODULE_HOVER;
    }
}
