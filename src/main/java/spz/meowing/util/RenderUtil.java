package spz.meowing.util;

import net.minecraft.client.gui.DrawContext;

public final class RenderUtil {

    private RenderUtil() {}

    public static void drawRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    public static void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        r = Math.min(r, Math.min(w / 2, h / 2));

        // Center vertical strip
        ctx.fill(x + r, y, x + w - r, y + h, color);
        // Left strip
        ctx.fill(x, y + r, x + r, y + h - r, color);
        // Right strip
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);

        // Corner scanlines
        for (int i = 0; i < r; i++) {
            double dist = r - i - 0.5;
            int dx = (int) Math.ceil(Math.sqrt(r * r - dist * dist));
            // Top-left
            ctx.fill(x + r - dx, y + i, x + r, y + i + 1, color);
            // Top-right
            ctx.fill(x + w - r, y + i, x + w - r + dx, y + i + 1, color);
            // Bottom-left
            ctx.fill(x + r - dx, y + h - 1 - i, x + r, y + h - i, color);
            // Bottom-right
            ctx.fill(x + w - r, y + h - 1 - i, x + w - r + dx, y + h - i, color);
        }
    }

    public static void drawRoundedRectTop(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        r = Math.min(r, Math.min(w / 2, h));

        // Body below corners
        ctx.fill(x, y + r, x + w, y + h, color);
        // Top strip between corners
        ctx.fill(x + r, y, x + w - r, y + r, color);

        // Top-left and top-right corners
        for (int i = 0; i < r; i++) {
            double dist = r - i - 0.5;
            int dx = (int) Math.ceil(Math.sqrt(r * r - dist * dist));
            ctx.fill(x + r - dx, y + i, x + r, y + i + 1, color);
            ctx.fill(x + w - r, y + i, x + w - r + dx, y + i + 1, color);
        }
    }

    public static void drawRoundedRectBottom(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        r = Math.min(r, Math.min(w / 2, h));

        // Body above corners
        ctx.fill(x, y, x + w, y + h - r, color);
        // Bottom strip between corners
        ctx.fill(x + r, y + h - r, x + w - r, y + h, color);

        // Bottom-left and bottom-right corners
        for (int i = 0; i < r; i++) {
            double dist = r - i - 0.5;
            int dx = (int) Math.ceil(Math.sqrt(r * r - dist * dist));
            ctx.fill(x + r - dx, y + h - 1 - i, x + r, y + h - i, color);
            ctx.fill(x + w - r, y + h - 1 - i, x + w - r + dx, y + h - i, color);
        }
    }

    public static void drawGradientRect(DrawContext ctx, int x, int y, int w, int h, int colorTop, int colorBottom) {
        for (int i = 0; i < h; i++) {
            float progress = (float) i / Math.max(h - 1, 1);
            int color = ColorUtil.interpolate(colorTop, colorBottom, progress);
            ctx.fill(x, y + i, x + w, y + i + 1, color);
        }
    }

    public static void drawShadow(DrawContext ctx, int x, int y, int w, int h, int r, int layers) {
        for (int i = layers; i > 0; i--) {
            int alpha = (int) (40.0 * i / layers);
            int color = alpha << 24;
            drawRoundedRect(ctx, x - i, y - i, w + 2 * i, h + 2 * i, r + i / 2, color);
        }
    }

    public static boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
