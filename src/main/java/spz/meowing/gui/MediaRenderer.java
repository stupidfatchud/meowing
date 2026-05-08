package spz.meowing.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import spz.meowing.media.MediaProvider;
import spz.meowing.media.MediaState;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.MediaOverlay;
import spz.meowing.util.AnimationUtil;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.RenderUtil;

/**
 * Premium media overlay renderer.
 *
 * Layout:
 * ┌─────────────────────────────────┐
 * │ ♪  Song Title                   │
 * │    Artist Name        0:42/3:15 │
 * │ ━━━━━━━━━━━━━━━━━━━━━░░░░░░░░░ │
 * └─────────────────────────────────┘
 *
 * Features:
 * - Smooth fade in/out animations
 * - Smooth progress bar interpolation
 * - Scrolling text for long titles
 * - Accent bar, shadow, configurable corners
 * - Chroma support
 */
public final class MediaRenderer {

    // Animation state
    private static float showAnim = 0f;
    private static float progressAnim = 0f;
    private static String lastTrackId = "";
    private static long scrollOffset = 0;
    private static long lastFrameTime = 0;

    // Layout constants
    private static final int OVERLAY_W = 180;
    private static final int PAD_X = 8;
    private static final int PAD_Y = 5;
    private static final int LINE_H = 10;
    private static final int BAR_H = 3;
    private static final int ACCENT_W = 3;

    private MediaRenderer() {}

    public static void register() {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(MediaRenderer::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null && !HudEditorScreen.isEditing) return;

        MediaOverlay mo = (MediaOverlay) ModuleManager.getInstance().getModule("MediaOverlay");
        if (mo == null || !mo.isEnabled()) return;

        MediaState state = MediaProvider.getState();
        TextRenderer tr = mc.textRenderer;

        // Determine visibility
        boolean shouldShow = state.isActive() && state.hasTrackInfo();
        if (!state.isPlaying() && !mo.showWhenPaused()) shouldShow = false;

        // Fade animation
        float animSpeed = 0.08f * mo.getAnimSpeed();
        showAnim = AnimationUtil.lerp(showAnim, shouldShow ? 1f : 0f, animSpeed);
        if (showAnim < 0.01f) return;

        // Track change detection
        String trackId = state.getTrackId();
        if (!trackId.equals(lastTrackId)) {
            lastTrackId = trackId;
            progressAnim = 0f;
            scrollOffset = 0;
        }

        // Build content
        String title = state.hasTrackInfo() ? state.getTitle() : "";
        String artist = mo.showArtist() ? state.getArtist() : "";
        boolean hasTime = mo.showTime() && state.isActive() && state.hasTrackInfo();
        boolean hasBar = mo.showProgress() && state.getDurationMs() > 0;

        // Time string
        String timeStr = "";
        if (hasTime) {
            timeStr = state.getPositionFormatted();
            if (state.getDurationMs() > 0) timeStr += " / " + state.getDurationFormatted();
            if (!state.isPlaying()) timeStr = "paused";
        }

        // Calculate box size
        int boxW = OVERLAY_W;
        int boxH = PAD_Y * 2 + LINE_H; // title line
        if (!artist.isEmpty() || !timeStr.isEmpty()) boxH += LINE_H; // artist/time line
        if (hasBar) boxH += BAR_H + 4; // progress bar

        // Position
        int screenH = mc.getWindow().getScaledHeight();
        int defX = 4, defY = screenH - boxH - 4;
        int x = HudPositions.getX("SpotifyOverlay", defX);
        int y = HudPositions.getY("SpotifyOverlay", defY);

        // Scale
        float scale = HudPositions.getScale("SpotifyOverlay");
        if (scale != 1.0f) {
            org.joml.Matrix3x2fStack mat = ((spz.meowing.mixin.DrawContextAccessor) (Object) context).meowing_getMatrices();
            mat.pushMatrix();
            mat.translate(x, y);
            mat.scale(scale, scale);
            mat.translate(-x, -y);
        }

        int alpha = (int) (mo.getBgAlpha() * showAnim);
        int textAlpha = (int) (255 * showAnim);
        int accent = mo.isChroma() ? chromaColor(0) : mo.getAccent();
        int radius = mo.getCornerRadius();

        // Shadow
        if (mo.hasShadow() && showAnim > 0.5f) {
            RenderUtil.drawShadow(context, x, y, boxW, boxH, radius, 4);
        }

        // Background
        int bgColor = (alpha << 24) | 0x0E0E16;
        RenderUtil.drawRoundedRect(context, x, y, boxW, boxH, radius, bgColor);

        // Accent bar (left edge)
        if (mo.hasAccent()) {
            int accentAlpha = (int) (255 * showAnim);
            int barColor = ColorUtil.withAlpha(accent, accentAlpha);
            // Draw rounded accent bar
            context.fill(x, y + radius, x + ACCENT_W, y + boxH - radius, barColor);
            // Rounded caps
            for (int i = 0; i < Math.min(radius, ACCENT_W); i++) {
                context.fill(x, y + radius - i, x + ACCENT_W - (radius - i > ACCENT_W ? ACCENT_W : 0), y + radius - i + 1, barColor);
                context.fill(x, y + boxH - radius + i, x + ACCENT_W, y + boxH - radius + i + 1, barColor);
            }
        }

        int contentX = x + PAD_X + (mo.hasAccent() ? ACCENT_W : 0);
        int contentW = boxW - PAD_X * 2 - (mo.hasAccent() ? ACCENT_W : 0);
        int cy = y + PAD_Y;

        // ♪ icon + Title
        int titleX = contentX;
        int titleMaxW = contentW;
        if (mo.showIcon()) {
            String icon = state.isPlaying() ? "♪" : "❚❚";
            int iconW = tr.getWidth(icon);
            int iconColor = isGlobalChroma() ? chromaColor(0) : (mo.hasAccent() ? accent : 0xFFE8E8EE);
            context.drawText(tr, icon, contentX, cy, ColorUtil.withAlpha(iconColor, textAlpha), true);
            titleX = contentX + iconW + 3;
            titleMaxW = contentW - iconW - 3;
        }

        // Scrolling title
        String displayTitle = title;
        int titleW = tr.getWidth(title);
        if (mo.scrollTitle() && titleW > titleMaxW && titleMaxW > 10) {
            long now = System.currentTimeMillis();
            if (lastFrameTime > 0) {
                scrollOffset += (now - lastFrameTime);
            }
            lastFrameTime = now;

            String padded = title + "    " + title;
            int paddedW = tr.getWidth(padded) / 2 + tr.getWidth("    ");
            int scrollPixels = (int) ((scrollOffset / 30) % paddedW);

            // Scissor-like rendering: draw character by character within bounds
            int scrollColor = isGlobalChroma() ? chromaColor(10) : ColorUtil.withAlpha(0xFFE8E8EE, textAlpha);
            renderScrollingText(context, tr, padded, titleX, cy, titleMaxW, scrollPixels, scrollColor);
        } else {
            lastFrameTime = System.currentTimeMillis();
            if (titleW > titleMaxW) {
                displayTitle = truncate(tr, title, titleMaxW);
            }
            int titleColor = isGlobalChroma() ? chromaColor(10) : ColorUtil.withAlpha(0xFFE8E8EE, textAlpha);
            context.drawText(tr, displayTitle, titleX, cy, titleColor, true);
        }

        cy += LINE_H;

        // Artist + Time on same line
        if (!artist.isEmpty() || !timeStr.isEmpty()) {
            if (!artist.isEmpty()) {
                String dispArtist = artist;
                int artistMaxW = contentW - (timeStr.isEmpty() ? 0 : tr.getWidth(timeStr) + 6);
                if (tr.getWidth(artist) > artistMaxW) {
                    dispArtist = truncate(tr, artist, Math.max(20, artistMaxW));
                }
                int artistColor = isGlobalChroma() ? chromaColor(20) : ColorUtil.withAlpha(0xFF8888AA, textAlpha);
                context.drawText(tr, dispArtist, contentX, cy, artistColor, false);
            }

            if (!timeStr.isEmpty()) {
                int timeW = tr.getWidth(timeStr);
                context.drawText(tr, timeStr, contentX + contentW - timeW, cy,
                        ColorUtil.withAlpha(0xFF5A5A7A, textAlpha), false);
            }
            cy += LINE_H;
        }

        // Progress bar
        if (hasBar) {
            cy += 1;
            float targetProgress = state.getProgress();
            progressAnim = AnimationUtil.lerp(progressAnim, targetProgress, 0.12f * mo.getAnimSpeed());
            if (targetProgress < progressAnim - 0.5f) progressAnim = targetProgress;

            int barX = contentX;
            int barW = contentW;
            int barY = cy;

            // Track (background)
            RenderUtil.drawRoundedRect(context, barX, barY, barW, BAR_H, 1,
                    ColorUtil.withAlpha(0xFF1A1A2E, (int) (255 * showAnim)));

            // Fill
            if (mo.hasAccent()) {
                int fillW = Math.max(2, (int) (barW * progressAnim));
                RenderUtil.drawRoundedRect(context, barX, barY, fillW, BAR_H, 1,
                        ColorUtil.withAlpha(accent, (int) (200 * showAnim)));
            }

            // Dot indicator at current position
            if (mo.hasAccent() && progressAnim > 0.01f) {
                int dotX = barX + (int) (barW * progressAnim) - 2;
                int dotY = barY - 1;
                context.fill(dotX, dotY, dotX + 4, dotY + BAR_H + 2,
                        ColorUtil.withAlpha(0xFFFFFFFF, (int) (180 * showAnim)));
            }
        }

        // Pop scale
        if (scale != 1.0f) {
            org.joml.Matrix3x2fStack mat = ((spz.meowing.mixin.DrawContextAccessor) (Object) context).meowing_getMatrices();
            mat.popMatrix();
        }
    }

    /** Renders scrolling text within a max width by drawing characters one by one. */
    private static void renderScrollingText(DrawContext ctx, TextRenderer tr,
                                            String text, int x, int y, int maxW,
                                            int scrollPixels, int color) {
        int cx = -scrollPixels;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int charW = tr.getWidth(ch);
            if (cx + charW > 0 && cx < maxW) {
                ctx.drawText(tr, ch, x + Math.max(0, cx), y, color, true);
            }
            cx += charW;
            if (cx > maxW) break;
        }
    }

    /** Truncates text to fit within maxWidth, adding "..." at the end. */
    private static String truncate(TextRenderer tr, String text, int maxW) {
        String ellipsis = "..";
        int ellW = tr.getWidth(ellipsis);
        if (maxW <= ellW) return "";

        StringBuilder sb = new StringBuilder();
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            int cw = tr.getWidth(String.valueOf(text.charAt(i)));
            if (w + cw + ellW > maxW) break;
            sb.append(text.charAt(i));
            w += cw;
        }
        return sb + ellipsis;
    }

    private static boolean isGlobalChroma() {
        try {
            var c = ModuleManager.getInstance().getModule("ChromaHUD");
            return c != null && c.isEnabled();
        } catch (Exception e) { return false; }
    }

    private static int chromaColor(int offset) {
        float speed = 5;
        try {
            var c = ModuleManager.getInstance().getModule("ChromaHUD");
            if (c instanceof spz.meowing.module.impl.ChromaHUD ch) {
                speed = ch.getSpeed();
            }
        } catch (Exception ignored) {}
        float speedMult = (float) Math.pow(speed / 5.0, 2.5) * 0.3f;
        float time = (System.currentTimeMillis() % 360000L) * speedMult;
        float hue = (time + offset) % 360f;
        return java.awt.Color.HSBtoRGB(hue / 360f, 0.7f, 1.0f) | 0xFF000000;
    }
}
