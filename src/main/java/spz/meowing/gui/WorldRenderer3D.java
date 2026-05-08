package spz.meowing.gui;

import com.mojang.blaze3d.platform.DepthTestFunction;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.joml.Vector3f;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.dungeons.StarESP;

import java.util.HashSet;
import java.util.Set;

/**
 * 3D world-space renderer for entity hitbox outlines and block highlights.
 * Uses custom RenderLayers for depth-tested and see-through rendering.
 */
public class WorldRenderer3D {

    private static final Set<Integer> starredEntityIds = new HashSet<>();

    // Lines with no depth test (see through walls) — for StarESP, etherwarp phase mode
    private static final RenderPipeline LINES_NO_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withLocation("pipeline/meowing_lines_no_depth")
            .build();

    private static final RenderLayer LINES_NO_DEPTH = RenderLayer.of(
            "meowing_lines_no_depth",
            RenderSetup.builder(LINES_NO_DEPTH_PIPELINE)
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .build()
    );

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldRenderer3D::onWorldRender);
    }

    private static void onWorldRender(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        renderStarESP3D(context, mc);
    }

    // ==================== STAR ESP ====================

    private static void renderStarESP3D(WorldRenderContext context, MinecraftClient mc) {
        var mod = ModuleManager.getInstance().getModule("StarESP");
        if (!(mod instanceof StarESP esp) || !esp.isEnabled()) return;
        if (!esp.getMode().equals("3D Box")) return;

        double rangeSq = esp.getRange() * esp.getRange();
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        MatrixStack matrices = context.matrices();
        float lineWidth = Math.max(mc.getWindow().getMinimumLineWidth(), esp.getLineWidth());
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(LINES_NO_DEPTH.getDrawMode(), LINES_NO_DEPTH.getVertexFormat());

        starredEntityIds.clear();
        boolean hasVertices = false;

        for (Entity entity : mc.world.getEntities()) {
            if (entity.squaredDistanceTo(mc.player) > rangeSq) continue;

            String name = entity.getName().getString();
            boolean isStarred = name.contains("✯") || name.contains("✫") || name.contains("⚚");
            Entity targetMob = null;

            if (isStarred && !(entity instanceof ArmorStandEntity)) {
                targetMob = entity;
            } else if (entity instanceof LivingEntity && !(entity instanceof ArmorStandEntity)) {
                for (Entity nearby : mc.world.getEntitiesByClass(ArmorStandEntity.class,
                        entity.getBoundingBox().expand(0.5, 2, 0.5), e -> true)) {
                    String asName = nearby.getName().getString();
                    if (asName.contains("✯") || asName.contains("✫")) {
                        targetMob = entity;
                        break;
                    }
                }
            }

            if (targetMob == null || starredEntityIds.contains(targetMob.getId())) continue;
            starredEntityIds.add(targetMob.getId());

            int color = esp.isRainbow() ? rainbow(targetMob.getId(), esp.getRainbowSpeed()) : esp.getColor();

            Vec3d lerpedPos = targetMob.getLerpedPos(tickDelta);
            Box box = targetMob.getBoundingBox();
            double hw = (box.maxX - box.minX) / 2.0;
            double hd = (box.maxZ - box.minZ) / 2.0;
            double height = box.maxY - box.minY;

            float minX = (float) (lerpedPos.x - hw - camPos.x);
            float minY = (float) (lerpedPos.y - camPos.y);
            float minZ = (float) (lerpedPos.z - hd - camPos.z);
            float maxX = (float) (lerpedPos.x + hw - camPos.x);
            float maxY = (float) (lerpedPos.y + height - camPos.y);
            float maxZ = (float) (lerpedPos.z + hd - camPos.z);

            drawBoxOutline(buffer, matrices.peek(), minX, minY, minZ, maxX, maxY, maxZ, color, lineWidth);
            hasVertices = true;
        }

        if (hasVertices) {
            BuiltBuffer built = buffer.endNullable();
            if (built != null) LINES_NO_DEPTH.draw(built);
        }
        tessellator.clear();
    }

    // ==================== DRAWING HELPERS ====================

    private static void drawBoxOutline(BufferBuilder buffer, MatrixStack.Entry entry,
                                       float x1, float y1, float z1, float x2, float y2, float z2,
                                       int color, float lineWidth) {
        // Bottom face
        line(buffer, entry, x1, y1, z1, x2, y1, z1, color, lineWidth);
        line(buffer, entry, x2, y1, z1, x2, y1, z2, color, lineWidth);
        line(buffer, entry, x2, y1, z2, x1, y1, z2, color, lineWidth);
        line(buffer, entry, x1, y1, z2, x1, y1, z1, color, lineWidth);
        // Top face
        line(buffer, entry, x1, y2, z1, x2, y2, z1, color, lineWidth);
        line(buffer, entry, x2, y2, z1, x2, y2, z2, color, lineWidth);
        line(buffer, entry, x2, y2, z2, x1, y2, z2, color, lineWidth);
        line(buffer, entry, x1, y2, z2, x1, y2, z1, color, lineWidth);
        // Vertical edges
        line(buffer, entry, x1, y1, z1, x1, y2, z1, color, lineWidth);
        line(buffer, entry, x2, y1, z1, x2, y2, z1, color, lineWidth);
        line(buffer, entry, x2, y1, z2, x2, y2, z2, color, lineWidth);
        line(buffer, entry, x1, y1, z2, x1, y2, z2, color, lineWidth);
    }

    private static void line(BufferBuilder buffer, MatrixStack.Entry entry,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             int color, float lineWidth) {
        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize();
        buffer.vertex(entry, x1, y1, z1).color(color).normal(entry, normal).lineWidth(lineWidth);
        buffer.vertex(entry, x2, y2, z2).color(color).normal(entry, normal).lineWidth(lineWidth);
    }

    private static int rainbow(int offset, float speed) {
        float hue = ((System.currentTimeMillis() % 36000L) * 0.3f * speed + offset * 30) % 360f;
        return java.awt.Color.HSBtoRGB(hue / 360f, 0.7f, 1.0f) | 0xFF000000;
    }
}
