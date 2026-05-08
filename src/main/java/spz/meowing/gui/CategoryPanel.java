package spz.meowing.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.module.ModuleManager;
import spz.meowing.util.AnimationUtil;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.RenderUtil;
import spz.meowing.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {

    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<>();

    private int x, y;
    private int width = 130;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;
    private float scrollOffset = 0f;
    private float targetScroll = 0f;
    private boolean expanded = true;
    private float expandAnim = 1f;

    public static final int HEADER_HEIGHT = 28;
    public static final int MAX_BODY_HEIGHT = 320;
    private static final int CORNER_RADIUS = 5;

    public CategoryPanel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;

        for (Module module : ModuleManager.getInstance().getModulesByCategory(category)) {
            buttons.add(new ModuleButton(module));
        }
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;
        }

        expandAnim = AnimationUtil.lerp(expandAnim, expanded ? 1f : 0f, 0.2f);
        scrollOffset = (float) AnimationUtil.lerp(scrollOffset, targetScroll, 0.25f);

        int contentHeight = getTotalContentHeight();
        int bodyHeight = Math.min(contentHeight, MAX_BODY_HEIGHT);
        int visibleBodyHeight = (int) (bodyHeight * expandAnim);
        int totalHeight = HEADER_HEIGHT + visibleBodyHeight;

        // Shadow
        RenderUtil.drawShadow(context, x, y, width, totalHeight, CORNER_RADIUS, 5);

        // Header background
        if (visibleBodyHeight > 0) {
            RenderUtil.drawRoundedRectTop(context, x, y, width, HEADER_HEIGHT, CORNER_RADIUS, ThemeHelper.headerBg());
        } else {
            RenderUtil.drawRoundedRect(context, x, y, width, HEADER_HEIGHT, CORNER_RADIUS, ThemeHelper.headerBg());
        }

        // Accent stripe at top of header
        RenderUtil.drawRoundedRectTop(context, x, y, width, 2, CORNER_RADIUS, ThemeHelper.accent());

        // Category name centered
        String name = category.getDisplayName().toUpperCase();
        int textW = textRenderer.getWidth(name);
        context.drawText(textRenderer, name, x + (width - textW) / 2, y + (HEADER_HEIGHT - 8) / 2 + 1, ColorUtil.TEXT_PRIMARY, false);

        // Expand arrow
        String arrow = expanded ? "\u25BC" : "\u25B6";
        context.drawText(textRenderer, arrow, x + width - 14, y + (HEADER_HEIGHT - 8) / 2 + 1, ColorUtil.TEXT_SECONDARY, false);

        // Body
        if (expandAnim > 0.01f && !buttons.isEmpty()) {
            int bodyY = y + HEADER_HEIGHT;

            RenderUtil.drawRoundedRectBottom(context, x, bodyY, width, visibleBodyHeight, CORNER_RADIUS, ThemeHelper.panelBg());

            context.enableScissor(x, bodyY, x + width, bodyY + visibleBodyHeight);

            int cy = bodyY - (int) scrollOffset;
            for (int i = 0; i < buttons.size(); i++) {
                ModuleButton btn = buttons.get(i);
                btn.render(context, textRenderer, x, cy, width, mouseX, mouseY, delta);
                cy += btn.getHeight();

                // Separator line between modules
                if (i < buttons.size() - 1 && !btn.isExpanded()) {
                    context.fill(x + 8, cy - 1, x + width - 8, cy, ColorUtil.MODULE_SEPARATOR);
                }
            }

            context.disableScissor();

            // Scrollbar
            if (contentHeight > bodyHeight && visibleBodyHeight > 0) {
                float maxScroll = Math.max(1, contentHeight - bodyHeight);
                float scrollRatio = scrollOffset / maxScroll;
                int barHeight = Math.max(12, visibleBodyHeight * visibleBodyHeight / contentHeight);
                int barY = bodyY + (int) ((visibleBodyHeight - barHeight) * scrollRatio);
                RenderUtil.drawRoundedRect(context, x + width - 3, barY, 2, barHeight, 1,
                        ColorUtil.withAlpha(ColorUtil.TEXT_SECONDARY, 80));
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = (int) mouseX - x;
                dragOffsetY = (int) mouseY - y;
                return true;
            } else if (button == 1) {
                expanded = !expanded;
                return true;
            }
        }

        if (expanded && expandAnim > 0.5f) {
            int bodyY = y + HEADER_HEIGHT;
            int bodyH = Math.min(getTotalContentHeight(), MAX_BODY_HEIGHT);
            if (mouseX >= x && mouseX < x + width && mouseY >= bodyY && mouseY < bodyY + bodyH) {
                int cy = bodyY - (int) scrollOffset;
                for (ModuleButton btn : buttons) {
                    int btnH = btn.getHeight();
                    if (mouseY >= cy && mouseY < cy + btnH) {
                        if (btn.mouseClicked(mouseX, mouseY, x, cy, width, button)) {
                            return true;
                        }
                    }
                    cy += btnH;
                }
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        for (ModuleButton btn : buttons) {
            if (btn.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) return true;
        if (expanded) {
            int cy = y + HEADER_HEIGHT - (int) scrollOffset;
            for (ModuleButton btn : buttons) {
                if (btn.mouseDragged(mouseX, mouseY, button, x, cy, width)) {
                    return true;
                }
                cy += btn.getHeight();
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int bodyY = y + HEADER_HEIGHT;
        int bodyH = Math.min(getTotalContentHeight(), MAX_BODY_HEIGHT);
        if (mouseX >= x && mouseX < x + width && mouseY >= bodyY && mouseY < bodyY + bodyH) {
            int contentHeight = getTotalContentHeight();
            int maxScroll = Math.max(0, contentHeight - bodyH);
            targetScroll = AnimationUtil.clamp((float) (targetScroll - amount * 16), 0, maxScroll);
            return true;
        }
        return false;
    }

    private int getTotalContentHeight() {
        int h = 0;
        for (ModuleButton btn : buttons) {
            h += btn.getHeight();
        }
        return h;
    }

    public void setSearchFilter(String query) {
        for (ModuleButton btn : buttons) {
            if (query == null || query.isEmpty()) {
                btn.setVisible(true);
            } else {
                btn.setVisible(btn.getModule().getName().toLowerCase().contains(query.toLowerCase()));
            }
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public Category getCategory() { return category; }
}
