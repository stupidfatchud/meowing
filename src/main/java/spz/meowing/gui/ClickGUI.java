package spz.meowing.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import spz.meowing.gui.component.KeybindComponent;
import spz.meowing.gui.component.StringComponent;
import spz.meowing.module.Category;
import spz.meowing.module.ModuleManager;
import spz.meowing.util.AnimationUtil;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.ConfigManager;
import spz.meowing.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {

    private static ClickGUI instance;
    private final List<CategoryPanel> panels = new ArrayList<>();
    private float openAnim = 0f;
    private boolean initialized = false;

    // Search
    private String searchQuery = "";
    private boolean searchActive = false;
    private float searchAnim = 0f;

    private ClickGUI() {
        super(Text.literal("Meowing ClickGUI"));
    }

    public static ClickGUI getInstance() {
        if (instance == null) {
            instance = new ClickGUI();
        }
        return instance;
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            int startX = 20;
            int startY = 20;
            int gap = 140;

            Category[] categories = Category.values();
            for (int i = 0; i < categories.length; i++) {
                panels.add(new CategoryPanel(categories[i], startX + i * gap, startY));
            }
            initialized = true;
        }
        openAnim = 0f;
        searchQuery = "";
        searchActive = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.width <= 0 || this.height <= 0) return;

        openAnim = AnimationUtil.lerp(openAnim, 1f, 0.15f);
        searchAnim = AnimationUtil.lerp(searchAnim, searchActive ? 1f : 0f, 0.2f);

        // Dim background
        int alpha = (int) (136 * openAnim);
        context.fill(0, 0, this.width, this.height, ColorUtil.withAlpha(0x000000, alpha));

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        // Search bar
        if (searchAnim > 0.01f) {
            int barW = 200;
            int barH = 18;
            int barX = (this.width - barW) / 2;
            int barY = 4;

            RenderUtil.drawRoundedRect(context, barX, barY, barW, barH, 4,
                    ColorUtil.interpolate(0x00000000, 0xE0111118, searchAnim));

            // Accent bottom line
            context.fill(barX + 4, barY + barH - 1, barX + barW - 4, barY + barH,
                    ColorUtil.withAlpha(ColorUtil.ACCENT, (int) (180 * searchAnim)));

            String display = searchQuery.isEmpty() ? "Search modules..." : searchQuery + "_";
            int textColor = searchQuery.isEmpty() ? ColorUtil.TEXT_SECONDARY : ColorUtil.TEXT_PRIMARY;
            context.drawText(tr, display, barX + 6, barY + 5, textColor, false);
        }

        // Render panels
        for (CategoryPanel panel : panels) {
            panel.render(context, tr, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // Check search bar click
        int barW = 200;
        int barX = (this.width - barW) / 2;
        if (mouseY >= 4 && mouseY < 22 && mouseX >= barX && mouseX < barX + barW) {
            searchActive = true;
            return true;
        }

        // Click outside search bar deactivates it
        if (searchActive && button == 0) {
            searchActive = false;
        }

        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked(mouseX, mouseY, button)) {
                CategoryPanel p = panels.remove(i);
                panels.add(p);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        for (CategoryPanel panel : panels) {
            if (panel.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        for (CategoryPanel panel : panels) {
            if (panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseScrolled(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        // Handle keybind listening
        if (KeybindComponent.listening != null) {
            KeybindComponent.listening.onKeyPressed(keyCode);
            return true;
        }

        // Handle string editing
        if (StringComponent.editing != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                StringComponent.editing = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                StringComponent.editing.onBackspace();
                return true;
            }
            return true;
        }

        // Search mode key handling
        if (searchActive) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (searchQuery.isEmpty()) {
                    searchActive = false;
                } else {
                    searchQuery = "";
                    updateSearch();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                updateSearch();
                return true;
            }
            return true; // Consume all keys when search is active
        }

        // Close on ESC or GUI keybind
        int guiKey = ModuleManager.getInstance().getModule("ClickGUI").getKeyCode();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == guiKey) {
            this.close();
            return true;
        }

        // Any letter key activates search
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            searchActive = true;
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        // String editing char input
        if (StringComponent.editing != null) {
            char c = (char) input.codepoint();
            if (c >= 32) {
                StringComponent.editing.onChar(c);
                return true;
            }
        }

        if (searchActive) {
            char c = (char) input.codepoint();
            if (Character.isLetterOrDigit(c) || c == ' ') {
                searchQuery += c;
                updateSearch();
                return true;
            }
        }
        return super.charTyped(input);
    }

    private void updateSearch() {
        for (CategoryPanel panel : panels) {
            panel.setSearchFilter(searchQuery);
        }
    }

    @Override
    public void removed() {
        super.removed();
        ModuleManager.getInstance().getModule("ClickGUI").forceSetEnabled(false);
        // Clear search on close
        searchQuery = "";
        searchActive = false;
        for (CategoryPanel panel : panels) {
            panel.setSearchFilter("");
        }
        ConfigManager.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public List<CategoryPanel> getPanels() {
        return panels;
    }
}
