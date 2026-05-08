package spz.meowing.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.dungeons.StarESP;
import spz.meowing.util.RenderUtil;

public class ESPRenderer {

    public static void register() {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(ESPRenderer::renderHudESP);
    }

    // ==================== 2D HUD RENDERING ====================
    private static void renderHudESP(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        renderStarESP(context, mc);
    }

    // ==================== STARRED MOBS ====================
    private static final java.util.Set<Integer> starredEntityIds = new java.util.HashSet<>();

    private static void renderStarESP(DrawContext context, MinecraftClient mc) {
        var mod = ModuleManager.getInstance().getModule("StarESP");
        if (!(mod instanceof StarESP esp) || !esp.isEnabled()) return;

        double rangeSq = esp.getRange() * esp.getRange();
        String renderMode = esp.getMode();
        starredEntityIds.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (entity.squaredDistanceTo(mc.player) > rangeSq) continue;

            // Check for star characters in nametag
            String name = entity.getName().getString();
            boolean isStarred = name.contains("✯") || name.contains("✫") || name.contains("⚚");

            // Also check nearby armor stands for starred nametags
            Entity targetMob = null;
            String starName = "";

            if (isStarred && !(entity instanceof ArmorStandEntity)) {
                targetMob = entity;
                starName = name;
            } else if (entity instanceof LivingEntity && !(entity instanceof ArmorStandEntity)) {
                for (Entity nearby : mc.world.getEntitiesByClass(ArmorStandEntity.class,
                        entity.getBoundingBox().expand(0.5, 2, 0.5), e -> true)) {
                    String asName = nearby.getName().getString();
                    if (asName.contains("✯") || asName.contains("✫")) {
                        targetMob = entity;
                        starName = asName;
                        break;
                    }
                }
            }

            if (targetMob == null || starredEntityIds.contains(targetMob.getId())) continue;
            starredEntityIds.add(targetMob.getId());

            int color = esp.isRainbow() ? rainbow(targetMob.getId(), esp.getRainbowSpeed()) : esp.getColor();

            switch (renderMode) {
                case "3D Box" -> {
                    // Handled by WorldRenderer3D in world-space
                }
                case "2D Box" -> {
                    render2DBox(context, mc, targetMob, color, esp.getLineWidth());
                }
                default -> {
                    // Nametag mode
                    Vec3d pos = new Vec3d(targetMob.getX(), targetMob.getY() + targetMob.getHeight() + 0.3, targetMob.getZ());
                    renderMarkerAt(context, mc, pos, "★ " + cleanName(starName), color);
                }
            }
        }
    }

    // ==================== 2D BOX RENDERING ====================
    private static void render2DBox(DrawContext context, MinecraftClient mc, Entity entity, int color, int lineWidth) {
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        // Project all 8 corners of bounding box to screen
        net.minecraft.util.math.Box box = entity.getBoundingBox();
        double[][] corners = {
                {box.minX, box.minY, box.minZ}, {box.maxX, box.minY, box.minZ},
                {box.minX, box.maxY, box.minZ}, {box.maxX, box.maxY, box.minZ},
                {box.minX, box.minY, box.maxZ}, {box.maxX, box.minY, box.maxZ},
                {box.minX, box.maxY, box.maxZ}, {box.maxX, box.maxY, box.maxZ}
        };

        Vec3d cam = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        Vec3d lookVec = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right = lookVec.crossProduct(worldUp);
        if (right.lengthSquared() < 0.001) right = new Vec3d(1, 0, 0);
        right = right.normalize();
        Vec3d up = right.crossProduct(lookVec).normalize();

        double fov = Math.toRadians(mc.options.getFov().getValue());
        double tanHalf = Math.tan(fov / 2.0);
        double aspect = (double) screenW / screenH;

        int minSX = Integer.MAX_VALUE, minSY = Integer.MAX_VALUE;
        int maxSX = Integer.MIN_VALUE, maxSY = Integer.MIN_VALUE;
        boolean anyVisible = false;

        for (double[] c : corners) {
            Vec3d delta = new Vec3d(c[0], c[1], c[2]).subtract(cam);
            double rz = delta.dotProduct(lookVec);
            if (rz < 0.1) continue;

            double rx = delta.dotProduct(right);
            double ry = delta.dotProduct(up);

            int sx = (int) (((rx / rz) / tanHalf / aspect + 1.0) / 2.0 * screenW);
            int sy = (int) ((-(ry / rz) / tanHalf + 1.0) / 2.0 * screenH);

            minSX = Math.min(minSX, sx);
            minSY = Math.min(minSY, sy);
            maxSX = Math.max(maxSX, sx);
            maxSY = Math.max(maxSY, sy);
            anyVisible = true;
        }

        if (!anyVisible) return;

        // Clamp to screen
        minSX = Math.max(2, minSX);
        minSY = Math.max(2, minSY);
        maxSX = Math.min(screenW - 2, maxSX);
        maxSY = Math.min(screenH - 2, maxSY);

        if (maxSX - minSX < 3 || maxSY - minSY < 3) return;

        // Draw 2D box outline with configurable thickness
        int lw = lineWidth;
        context.fill(minSX, minSY, maxSX, minSY + lw, color); // Top
        context.fill(minSX, maxSY - lw, maxSX, maxSY, color); // Bottom
        context.fill(minSX, minSY, minSX + lw, maxSY, color); // Left
        context.fill(maxSX - lw, minSY, maxSX, maxSY, color); // Right

        // Distance label
        double dist = Math.sqrt(entity.squaredDistanceTo(mc.player));
        String distStr = String.format("%.0fm", dist);
        TextRenderer tr = mc.textRenderer;
        int textW = tr.getWidth(distStr);
        context.drawText(tr, distStr, (minSX + maxSX) / 2 - textW / 2, maxSY + 2, 0xFFAAAAAA, true);
    }

    // ==================== RENDERING ====================
    private static void renderMarkerAt(DrawContext context, MinecraftClient mc, Vec3d worldPos, String label, int color) {
        Vec3d cam = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        Vec3d delta = worldPos.subtract(cam);
        double dist = delta.length();
        if (dist > 64) return;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        int margin = 14;

        Vec3d lookVec = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right = lookVec.crossProduct(worldUp);
        if (right.lengthSquared() < 0.001) right = new Vec3d(1, 0, 0);
        right = right.normalize();
        Vec3d up = right.crossProduct(lookVec).normalize();

        double rx = delta.dotProduct(right);
        double ry = delta.dotProduct(up);
        double rz = delta.dotProduct(lookVec);

        double fov = Math.toRadians(mc.options.getFov().getValue());
        double tanHalfFov = Math.tan(fov / 2.0);
        double aspectRatio = (double) screenW / screenH;

        int sx, sy;
        boolean clamped = false;

        if (rz > 0.1) {
            double screenX = (rx / rz) / tanHalfFov / aspectRatio;
            double screenY = -(ry / rz) / tanHalfFov;
            sx = (int) ((screenX + 1.0) / 2.0 * screenW);
            sy = (int) ((screenY + 1.0) / 2.0 * screenH);
        } else {
            double angle = Math.atan2(ry, rx);
            sx = (int) (screenW / 2.0 + Math.cos(angle) * screenW);
            sy = (int) (screenH / 2.0 - Math.sin(angle) * screenH);
            clamped = true;
        }

        if (sx < margin || sx > screenW - margin || sy < margin || sy > screenH - margin) {
            sx = Math.max(margin, Math.min(screenW - margin, sx));
            sy = Math.max(margin, Math.min(screenH - margin, sy));
            clamped = true;
        }

        TextRenderer tr = mc.textRenderer;
        String distStr = String.format("%.0fm", dist);

        if (clamped) {
            int dotSize = 6;
            RenderUtil.drawRoundedRect(context, sx - dotSize / 2, sy - dotSize / 2, dotSize, dotSize, 3, color);
            int distW = tr.getWidth(distStr);
            context.drawText(tr, distStr, sx - distW / 2, sy + dotSize / 2 + 2, 0xFFAAAAAA, true);
        } else {
            int textW = tr.getWidth(label);
            int distW = tr.getWidth(distStr);
            int totalW = Math.max(textW, distW) + 8;

            RenderUtil.drawRoundedRect(context, sx - totalW / 2, sy - 7, totalW, 20, 3, 0xA0000000);
            context.fill(sx - totalW / 2, sy - 7, sx - totalW / 2 + 2, sy + 13, color);
            context.drawText(tr, label, sx - textW / 2, sy - 5, color, true);
            context.drawText(tr, distStr, sx - distW / 2, sy + 5, 0xFF888888, false);
        }
    }

    private static String cleanName(String name) {
        return name.replaceAll("§.", "").replaceAll("[✯✫⚚★]", "").trim();
    }

    private static int rainbow(int offset, float speed) {
        float hue = ((System.currentTimeMillis() % 36000L) * 0.3f * speed + offset * 30) % 360f;
        return java.awt.Color.HSBtoRGB(hue / 360f, 0.7f, 1.0f) | 0xFF000000;
    }
}
