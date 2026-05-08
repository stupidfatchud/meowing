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
import spz.meowing.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class WhiteboardScreen extends Screen {

    private static WhiteboardScreen instance;

    private final List<Stroke> strokes = new ArrayList<>();
    private Stroke currentStroke = null;
    private boolean erasing = false;
    private int brushColor = 0xFFFFFFFF;
    private int brushWidth = 3;

    private static final int[] PALETTE = {
            0xFFFFFFFF, 0xFF000000, 0xFFE74C3C, 0xFFE67E22, 0xFFFFD700,
            0xFF2ECC71, 0xFF3498DB, 0xFF6C5CE7, 0xFFFF69B4, 0xFF888888
    };

    private static final int TOOLBAR_H = 32;

    public WhiteboardScreen() {
        super(Text.literal("Whiteboard"));
    }

    public static WhiteboardScreen getInstance() {
        if (instance == null) instance = new WhiteboardScreen();
        return instance;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // White canvas
        context.fill(0, TOOLBAR_H, this.width, this.height, 0xFFFFFFFF);

        // Draw all strokes
        for (Stroke stroke : strokes) {
            drawStroke(context, stroke);
        }
        if (currentStroke != null) {
            drawStroke(context, currentStroke);
        }

        // Toolbar background
        RenderUtil.drawRect(context, 0, 0, this.width, TOOLBAR_H, 0xFF1A1A22);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int tx = 6;

        // Color palette
        for (int i = 0; i < PALETTE.length; i++) {
            int px = tx + i * 16;
            int py = 4;
            boolean selected = brushColor == PALETTE[i] && !erasing;

            if (selected) {
                RenderUtil.drawRoundedRect(context, px - 1, py - 1, 14, 14, 2, 0xFF6C5CE7);
            }
            RenderUtil.drawRoundedRect(context, px, py, 12, 12, 2, PALETTE[i]);
            // Border for white color visibility
            if (PALETTE[i] == 0xFFFFFFFF) {
                context.fill(px, py, px + 12, py + 1, 0xFFCCCCCC);
                context.fill(px, py + 11, px + 12, py + 12, 0xFFCCCCCC);
                context.fill(px, py, px + 1, py + 12, 0xFFCCCCCC);
                context.fill(px + 11, py, px + 12, py + 12, 0xFFCCCCCC);
            }
        }
        tx += PALETTE.length * 16 + 8;

        // Eraser button
        int eraserX = tx;
        int eraserBg = erasing ? 0xFF6C5CE7 : 0xFF2A2A3A;
        RenderUtil.drawRoundedRect(context, eraserX, 4, 36, 12, 3, eraserBg);
        context.drawText(tr, "Erase", eraserX + 4, 6, 0xFFFFFFFF, false);
        tx += 44;

        // Width controls
        context.drawText(tr, "Width:", tx, 6, 0xFF999999, false);
        tx += 34;

        for (int w : new int[]{1, 2, 3, 5, 8, 12}) {
            boolean sel = brushWidth == w && !erasing;
            int btnBg = sel ? 0xFF6C5CE7 : 0xFF2A2A3A;
            RenderUtil.drawRoundedRect(context, tx, 4, 14, 12, 2, btnBg);
            String label = String.valueOf(w);
            context.drawText(tr, label, tx + (14 - tr.getWidth(label)) / 2, 6, 0xFFFFFFFF, false);
            tx += 16;
        }
        tx += 8;

        // Clear button
        RenderUtil.drawRoundedRect(context, tx, 4, 30, 12, 3, 0xFFE74C3C);
        context.drawText(tr, "Clear", tx + 3, 6, 0xFFFFFFFF, false);
        tx += 38;

        // Eraser width display
        if (erasing) {
            context.drawText(tr, "Eraser: " + brushWidth + "px", tx, 6, 0xFFAAAAAA, false);
        }

        // Bottom hint
        context.drawText(tr, "ESC to close | Draw with left click | Right click to erase",
                6, this.height - 12, 0xFF888888, false);

        // Cursor preview
        if (mouseY > TOOLBAR_H) {
            int previewColor = erasing ? 0x40FF0000 : ColorUtil.withAlpha(brushColor, 100);
            int half = brushWidth;
            context.fill(mouseX - half, mouseY - half, mouseX + half, mouseY + half, previewColor);
        }
    }

    private void drawStroke(DrawContext context, Stroke stroke) {
        if (stroke.points.size() < 2) {
            if (stroke.points.size() == 1) {
                int[] p = stroke.points.get(0);
                int half = stroke.width;
                context.fill(p[0] - half, p[1] - half, p[0] + half, p[1] + half, stroke.color);
            }
            return;
        }

        for (int i = 1; i < stroke.points.size(); i++) {
            int[] a = stroke.points.get(i - 1);
            int[] b = stroke.points.get(i);
            drawLine(context, a[0], a[1], b[0], b[1], stroke.width, stroke.color);
        }
    }

    private void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int width, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            context.fill(x1 - width, y1 - width, x1 + width, y1 + width, color);
            return;
        }

        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            context.fill(x - width, y - width, x + width, y + width, color);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        int button = click.button();

        // Toolbar clicks
        if (my < TOOLBAR_H && button == 0) {
            int tx = 6;

            // Color palette
            for (int i = 0; i < PALETTE.length; i++) {
                int px = tx + i * 16;
                if (mx >= px && mx < px + 12 && my >= 4 && my < 16) {
                    brushColor = PALETTE[i];
                    erasing = false;
                    return true;
                }
            }
            tx += PALETTE.length * 16 + 8;

            // Eraser
            if (mx >= tx && mx < tx + 36 && my >= 4 && my < 16) {
                erasing = !erasing;
                return true;
            }
            tx += 44;

            // Width buttons
            tx += 34; // skip "Width:" label
            for (int w : new int[]{1, 2, 3, 5, 8, 12}) {
                if (mx >= tx && mx < tx + 14 && my >= 4 && my < 16) {
                    brushWidth = w;
                    return true;
                }
                tx += 16;
            }
            tx += 8;

            // Clear button
            if (mx >= tx && mx < tx + 30 && my >= 4 && my < 16) {
                strokes.clear();
                return true;
            }

            return true;
        }

        // Canvas drawing
        if (my > TOOLBAR_H) {
            if (button == 1) {
                // Right click always erases
                currentStroke = new Stroke(0xFFFFFFFF, brushWidth + 4);
            } else {
                currentStroke = new Stroke(erasing ? 0xFFFFFFFF : brushColor, brushWidth);
            }
            currentStroke.points.add(new int[]{mx, my});
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (currentStroke != null && click.y() > TOOLBAR_H) {
            currentStroke.points.add(new int[]{(int) click.x(), (int) click.y()});
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (currentStroke != null) {
            if (currentStroke.points.size() > 0) {
                strokes.add(currentStroke);
            }
            currentStroke = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        // Ctrl+Z undo
        if (key == GLFW.GLFW_KEY_Z && (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            if (!strokes.isEmpty()) {
                strokes.remove(strokes.size() - 1);
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void removed() {
        super.removed();
        ModuleManager.getInstance().getModule("Whiteboard").forceSetEnabled(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static class Stroke {
        final int color;
        final int width;
        final List<int[]> points = new ArrayList<>();

        Stroke(int color, int width) {
            this.color = color;
            this.width = width;
        }
    }
}
