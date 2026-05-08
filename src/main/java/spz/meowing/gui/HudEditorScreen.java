package spz.meowing.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import spz.meowing.module.ModuleManager;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.ConfigManager;
import spz.meowing.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD Editor screen. The real HUD elements render underneath (since shouldPause=false),
 * and this screen overlays draggable selection outlines around each element.
 * Users see the actual modules while moving them.
 */
public class HudEditorScreen extends Screen {

    private static HudEditorScreen instance;
    private final List<HudBox> boxes = new ArrayList<>();
    private HudBox dragging = null;
    private int dragOffsetX, dragOffsetY;
    private static final int SNAP = 2;

    // Flag so HudRenderer knows to render preview data for disabled modules
    public static boolean isEditing = false;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
    }

    public static HudEditorScreen getInstance() {
        if (instance == null) instance = new HudEditorScreen();
        return instance;
    }

    @Override
    protected void init() {
        super.init();
        boxes.clear();
        isEditing = true;

        int sw = this.width;
        int sh = this.height;

        // Each box matches the actual rendered size of its HUD element
        boxes.add(new HudBox("HUD", 4, 4, 130, 80));
        boxes.add(new HudBox("Keystrokes", 4, 100, 70, 90));
        boxes.add(new HudBox("ModuleList", sw - 110, 2, 110, 100));
        boxes.add(new HudBox("Inventory", sw / 2 - 82, sh - 80, 9 * 18 + 4, 3 * 18 + 4));
        boxes.add(new HudBox("SpotifyOverlay", 4, sh - 40, 180, 35));
        boxes.add(new HudBox("TargetHUD", sw / 2 + 10, sh / 2 + 10, 150, 44));
        boxes.add(new HudBox("MaskTimers", 4, sh / 2, 120, 50));
        boxes.add(new HudBox("MaskAlert", sw / 2, sh / 3 + 30, 120, 30));
        boxes.add(new HudBox("WatcherAlert", sw / 2, sh / 3, 120, 30));

        // Load saved positions
        for (HudBox box : boxes) {
            int[] pos = HudPositions.get(box.name);
            if (pos != null) {
                box.x = pos[0];
                box.y = pos[1];
            }
            box.scale = HudPositions.getScale(box.name);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim background slightly
        context.fill(0, 0, this.width, this.height, 0x30000000);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        // Subtle grid dots
        for (int gx = 0; gx < this.width; gx += 32)
            for (int gy = 0; gy < this.height; gy += 32)
                context.fill(gx, gy, gx + 1, gy + 1, 0x18FFFFFF);

        // Center crosshair guides
        context.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x15FF6666);
        context.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x15FF6666);

        // Alignment guides when dragging
        if (dragging != null) {
            int midX = dragging.x + dragging.scaledW() / 2;
            int midY = dragging.y + dragging.scaledH() / 2;

            // Snap to center
            if (Math.abs(midX - this.width / 2) < 6)
                context.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x506C5CE7);
            if (Math.abs(midY - this.height / 2) < 6)
                context.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x506C5CE7);

            // Snap to other boxes
            for (HudBox other : boxes) {
                if (other == dragging) continue;
                if (Math.abs(dragging.x - other.x) < 4)
                    context.fill(other.x, 0, other.x + 1, this.height, 0x3000D2FF);
                if (Math.abs(dragging.y - other.y) < 4)
                    context.fill(0, other.y, this.width, other.y + 1, 0x3000D2FF);
                int r1 = dragging.x + dragging.scaledW(), r2 = other.x + other.scaledW();
                if (Math.abs(r1 - r2) < 4)
                    context.fill(r2, 0, r2 + 1, this.height, 0x3000D2FF);
            }
        }

        // Draw selection outlines around each element
        for (HudBox box : boxes) {
            int sw = box.scaledW();
            int sh = box.scaledH();
            boolean hovered = mouseX >= box.x && mouseX < box.x + sw && mouseY >= box.y && mouseY < box.y + sh;
            boolean isDrag = dragging == box;

            if (isDrag) {
                // Highlighted selection border
                drawOutline(context, box.x - 2, box.y - 2, sw + 4, sh + 4, ColorUtil.ACCENT, 2);
                // Corner handles
                int cs = 4;
                int ac = ColorUtil.ACCENT;
                context.fill(box.x - 3, box.y - 3, box.x - 3 + cs, box.y - 3 + cs, ac);
                context.fill(box.x + sw - 1, box.y - 3, box.x + sw - 1 + cs, box.y - 3 + cs, ac);
                context.fill(box.x - 3, box.y + sh - 1, box.x - 3 + cs, box.y + sh - 1 + cs, ac);
                context.fill(box.x + sw - 1, box.y + sh - 1, box.x + sw - 1 + cs, box.y + sh - 1 + cs, ac);
            } else if (hovered) {
                // Subtle hover outline
                drawOutline(context, box.x - 1, box.y - 1, sw + 2, sh + 2, 0x80AAAAAA, 1);
            } else {
                // Faint outline to show draggable area
                drawOutline(context, box.x, box.y, sw, sh, 0x30FFFFFF, 1);
            }

            // Name label (small, above the box)
            String label = box.name;
            int labelW = tr.getWidth(label);
            int labelX = box.x + (sw - labelW) / 2;
            int labelY = box.y - 11;
            if (labelY < 1) labelY = box.y + sh + 2;

            if (hovered || isDrag) {
                RenderUtil.drawRoundedRect(context, labelX - 3, labelY - 1, labelW + 6, 10, 2, 0xD0111118);
                context.drawText(tr, label, labelX, labelY, isDrag ? ColorUtil.ACCENT : 0xFFDDDDEE, true);

                // Scale indicator
                String scaleText = (int) (box.scale * 100) + "%";
                int scaleW = tr.getWidth(scaleText);
                context.drawText(tr, scaleText, box.x + sw - scaleW - 1, labelY, 0xFF888899, false);
            }

            // Position readout when dragging
            if (isDrag) {
                String posText = box.x + ", " + box.y;
                int posW = tr.getWidth(posText);
                RenderUtil.drawRoundedRect(context, box.x + sw / 2 - posW / 2 - 3, box.y + sh + 3, posW + 6, 10, 2, 0xD0111118);
                context.drawText(tr, posText, box.x + sw / 2 - posW / 2, box.y + sh + 4, 0xFF888899, true);
            }
        }

        // Title bar
        String title = "HUD Editor";
        String sub = "Drag to move  |  Scroll to resize  |  ESC to save";
        int titleW = tr.getWidth(title);
        int subW = tr.getWidth(sub);
        int barW = Math.max(titleW, subW) + 16;
        RenderUtil.drawRoundedRect(context, (this.width - barW) / 2, 2, barW, 22, 4, 0xE0111118);
        context.drawText(tr, title, (this.width - titleW) / 2, 4, 0xFFEEEEFF, true);
        context.drawText(tr, sub, (this.width - subW) / 2, 14, 0xFF666688, false);
    }

    private static void drawOutline(DrawContext ctx, int x, int y, int w, int h, int color, int thickness) {
        ctx.fill(x, y, x + w, y + thickness, color);
        ctx.fill(x, y + h - thickness, x + w, y + h, color);
        ctx.fill(x, y, x + thickness, y + h, color);
        ctx.fill(x + w - thickness, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        for (int i = boxes.size() - 1; i >= 0; i--) {
            HudBox box = boxes.get(i);
            if (mx >= box.x && mx < box.x + box.scaledW() && my >= box.y && my < box.y + box.scaledH()) {
                dragging = box;
                dragOffsetX = mx - box.x;
                dragOffsetY = my - box.y;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            saveAll();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging != null) {
            int rawX = (int) click.x() - dragOffsetX;
            int rawY = (int) click.y() - dragOffsetY;

            // Snap to edges and other boxes
            rawX = snapX(rawX, dragging);
            rawY = snapY(rawY, dragging);

            // Clamp to screen
            rawX = Math.max(0, Math.min(this.width - dragging.scaledW(), rawX));
            rawY = Math.max(0, Math.min(this.height - dragging.scaledH(), rawY));

            dragging.x = rawX;
            dragging.y = rawY;

            // Update position live so the real HUD element moves too
            HudPositions.set(dragging.name, dragging.x, dragging.y);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    private int snapX(int x, HudBox box) {
        int w = box.scaledW();
        int midX = x + w / 2;
        int rightX = x + w;

        // Snap to screen center
        if (Math.abs(midX - this.width / 2) < 5) return this.width / 2 - w / 2;

        // Snap to other boxes
        for (HudBox other : boxes) {
            if (other == box) continue;
            if (Math.abs(x - other.x) < 4) return other.x;
            if (Math.abs(x - (other.x + other.scaledW())) < 4) return other.x + other.scaledW();
            if (Math.abs(rightX - other.x) < 4) return other.x - w;
            if (Math.abs(rightX - (other.x + other.scaledW())) < 4) return other.x + other.scaledW() - w;
        }
        return x;
    }

    private int snapY(int y, HudBox box) {
        int h = box.scaledH();
        int midY = y + h / 2;
        int bottomY = y + h;

        if (Math.abs(midY - this.height / 2) < 5) return this.height / 2 - h / 2;

        for (HudBox other : boxes) {
            if (other == box) continue;
            if (Math.abs(y - other.y) < 4) return other.y;
            if (Math.abs(y - (other.y + other.scaledH())) < 4) return other.y + other.scaledH();
            if (Math.abs(bottomY - other.y) < 4) return other.y - h;
            if (Math.abs(bottomY - (other.y + other.scaledH())) < 4) return other.y + other.scaledH() - h;
        }
        return y;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (int i = boxes.size() - 1; i >= 0; i--) {
            HudBox box = boxes.get(i);
            if (mouseX >= box.x && mouseX < box.x + box.scaledW() && mouseY >= box.y && mouseY < box.y + box.scaledH()) {
                box.scale = Math.max(0.5f, Math.min(3.0f, box.scale + (float) verticalAmount * 0.1f));
                HudPositions.setScale(box.name, box.scale);
                ConfigManager.markDirty();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            saveAll();
            this.close();
            return true;
        }
        // Arrow keys for precise movement
        if (dragging == null) {
            // Find hovered box for arrow key movement
        }
        return super.keyPressed(input);
    }

    private void saveAll() {
        for (HudBox box : boxes) {
            HudPositions.set(box.name, box.x, box.y);
            HudPositions.setScale(box.name, box.scale);
        }
        ConfigManager.markDirty();
    }

    @Override
    public void removed() {
        super.removed();
        isEditing = false;
        ModuleManager.getInstance().getModule("HudEditor").forceSetEnabled(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static class HudBox {
        String name;
        int x, y, w, h;
        float scale = 1.0f;

        HudBox(String name, int x, int y, int w, int h) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        int scaledW() { return (int) (w * scale); }
        int scaledH() { return (int) (h * scale); }
    }
}
