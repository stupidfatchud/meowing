package spz.meowing.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import spz.meowing.util.AnimationUtil;
import spz.meowing.util.RenderUtil;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {

    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();
    private static final int MAX_VISIBLE = 5;
    private static final int WIDTH = 200;
    private static final int HEIGHT = 30;
    private static final int PADDING = 6;
    private static final int GAP = 4;

    public enum Type {
        INFO(0xFF3498DB, "i"),
        SUCCESS(0xFF2ECC71, "\u2713"),
        WARNING(0xFFE67E22, "!"),
        ERROR(0xFFE74C3C, "\u2717");

        public final int color;
        public final String icon;

        Type(int color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    public static void send(String title, String message, Type type, long durationMs) {
        notifications.add(new Notification(title, message, type, durationMs));
    }

    public static void send(String title, String message, Type type) {
        send(title, message, type, 3000);
    }

    public static void info(String title, String message) { send(title, message, Type.INFO); }
    public static void success(String title, String message) { send(title, message, Type.SUCCESS); }
    public static void warn(String title, String message) { send(title, message, Type.WARNING); }
    public static void error(String title, String message) { send(title, message, Type.ERROR); }

    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        int screenW = mc.getWindow().getScaledWidth();

        // Remove expired
        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            Notification n = it.next();
            if (n.isExpired() && n.slideAnim < 0.01f) {
                notifications.remove(n);
            }
        }

        int y = PADDING;
        int rendered = 0;
        for (Notification n : notifications) {
            if (rendered >= MAX_VISIBLE) break;

            float targetSlide = n.isExpired() ? 0f : 1f;
            n.slideAnim = AnimationUtil.lerp(n.slideAnim, targetSlide, 0.15f);

            if (n.slideAnim < 0.005f && n.isExpired()) continue;

            int toastW = WIDTH;
            int toastX = (int) (screenW - toastW * n.slideAnim) + PADDING / 2;
            int toastY = y;

            // Background
            RenderUtil.drawRoundedRect(context, toastX, toastY, toastW - PADDING, HEIGHT, 4, 0xE0111118);

            // Accent bar
            context.fill(toastX, toastY, toastX + 3, toastY + HEIGHT, n.type.color);

            // Icon
            context.drawText(tr, n.type.icon, toastX + 7, toastY + 4, n.type.color, false);

            // Title
            context.drawText(tr, n.title, toastX + 16, toastY + 4, 0xFFE8E8EE, true);

            // Message
            String msg = n.message;
            if (tr.getWidth(msg) > toastW - 30) {
                msg = tr.trimToWidth(msg, toastW - 34) + "..";
            }
            context.drawText(tr, msg, toastX + 16, toastY + 16, 0xFF8888AA, false);

            // Progress bar
            if (!n.isExpired()) {
                float progress = n.getProgress();
                int barW = (int) ((toastW - PADDING - 6) * progress);
                context.fill(toastX + 3, toastY + HEIGHT - 2, toastX + 3 + barW, toastY + HEIGHT,
                        0x40FFFFFF);
            }

            y += (int) ((HEIGHT + GAP) * n.slideAnim);
            rendered++;
        }
    }

    private static class Notification {
        final String title;
        final String message;
        final Type type;
        final long createdAt;
        final long duration;
        float slideAnim = 0f;

        Notification(String title, String message, Type type, long duration) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.createdAt = System.currentTimeMillis();
            this.duration = duration;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > duration;
        }

        float getProgress() {
            float elapsed = System.currentTimeMillis() - createdAt;
            return Math.max(0, 1f - elapsed / duration);
        }
    }
}
