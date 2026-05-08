package spz.meowing.gui.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import spz.meowing.setting.StringSetting;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.RenderUtil;

public class StringComponent extends SettingComponent {

    public static StringComponent editing = null;
    private float cursorBlink = 0f;

    public StringComponent(StringSetting setting) {
        super(setting);
    }

    public boolean isEditing() {
        return editing == this;
    }

    public void onChar(char c) {
        StringSetting ss = (StringSetting) setting;
        String current = ss.getValue();
        if (current.length() < ss.getMaxLength()) {
            ss.setValue(current + c);
        }
    }

    public void onBackspace() {
        StringSetting ss = (StringSetting) setting;
        String current = ss.getValue();
        if (!current.isEmpty()) {
            ss.setValue(current.substring(0, current.length() - 1));
        }
    }

    @Override
    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int mouseX, int mouseY, float delta) {
        cursorBlink += delta;

        context.drawText(textRenderer, setting.getName(), x + 6, y + 2, ColorUtil.TEXT_SECONDARY, false);

        // Text field
        int fieldX = x + 6;
        int fieldY = y + 12;
        int fieldW = width - 12;
        int fieldH = 12;

        int fieldBg = isEditing() ? 0xFF1A1A30 : 0xFF151522;
        int borderColor = isEditing() ? ColorUtil.ACCENT : 0xFF2A2A3A;

        RenderUtil.drawRoundedRect(context, fieldX, fieldY, fieldW, fieldH, 2, fieldBg);
        // Border
        context.fill(fieldX, fieldY + fieldH - 1, fieldX + fieldW, fieldY + fieldH, borderColor);

        String text = ((StringSetting) setting).getValue();
        String display = text;
        if (isEditing() && ((int) (cursorBlink * 3)) % 2 == 0) {
            display += "_";
        }

        // Trim to fit
        if (textRenderer.getWidth(display) > fieldW - 4) {
            display = ".." + textRenderer.trimToWidth(text, fieldW - 12, true);
            if (isEditing()) display += "_";
        }

        context.drawText(textRenderer, display, fieldX + 3, fieldY + 2, ColorUtil.TEXT_PRIMARY, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int button) {
        if (button == 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getHeight()) {
            if (isEditing()) {
                editing = null;
            } else {
                editing = this;
                cursorBlink = 0;
            }
            return true;
        }
        return false;
    }

    @Override
    public int getHeight() {
        return 26;
    }
}
