package spz.meowing.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import org.lwjgl.glfw.GLFW;
import spz.meowing.module.Module;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.*;
import spz.meowing.util.AnimationUtil;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.RenderUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HudRenderer {

    private static final int PADDING = 4;
    private static final Map<String, Float> moduleListAnims = new HashMap<>();
    private static final Map<String, Float> keyAnims = new HashMap<>();
    private static boolean lastLmb = false, lastRmb = false;

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null && !HudEditorScreen.isEditing) return;

        renderInfoHud(context, mc);
        renderModuleList(context, mc);
        renderKeystrokes(context, mc);
        renderInventoryHud(context, mc);
        renderTargetHud(context, mc);
        renderWatcherTitle(context, mc);
        renderMaskTimers(context, mc);
        renderMaskAlert(context, mc);
        NotificationManager.render(context);
    }

    // ==================== CHROMA HELPERS ====================
    private static boolean isGlobalChroma() {
        var mod = ModuleManager.getInstance().getModule("ChromaHUD");
        return mod instanceof ChromaHUD && mod.isEnabled();
    }

    private static ChromaHUD getChromaModule() {
        var mod = ModuleManager.getInstance().getModule("ChromaHUD");
        return (mod instanceof ChromaHUD c && c.isEnabled()) ? c : null;
    }

    private static int chromaColor(int offset) {
        ChromaHUD c = getChromaModule();
        float speed = c != null ? c.getSpeed() : 5;
        float sat = c != null ? c.getSaturation() : 0.7f;
        // Exponential curve: slow at low speeds, fast at high speeds
        // speed 1 = very slow, 10 = moderate, 20 = fast
        float speedMult = (float) Math.pow(speed / 5.0, 2.5) * 0.3f;
        float time = (System.currentTimeMillis() % 360000L) * speedMult;
        float hue = (time + offset) % 360f;
        return java.awt.Color.HSBtoRGB(hue / 360f, sat, 1.0f) | 0xFF000000;
    }

    private static void drawChromaText(DrawContext context, TextRenderer tr, String text, int x, int y, boolean shadow) {
        ChromaHUD c = getChromaModule();
        int spread = c != null ? c.getSpread() : 8;
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int color = chromaColor(i * spread + y);
            context.drawText(tr, ch, cx, y, color, shadow);
            cx += tr.getWidth(ch);
        }
    }

    private static void drawText(DrawContext context, TextRenderer tr, String text, int x, int y, int normalColor, boolean shadow) {
        if (isGlobalChroma()) {
            drawChromaText(context, tr, text, x, y, shadow);
        } else {
            context.drawText(tr, text, x, y, normalColor, shadow);
        }
    }

    // ==================== SCALE HELPERS ====================
    private static void pushScale(DrawContext context, String element, int pivotX, int pivotY) {
        float scale = HudPositions.getScale(element);
        if (scale != 1.0f) {
            org.joml.Matrix3x2fStack mat = ((spz.meowing.mixin.DrawContextAccessor) (Object) context).meowing_getMatrices();
            mat.pushMatrix();
            mat.translate(pivotX, pivotY);
            mat.scale(scale, scale);
            mat.translate(-pivotX, -pivotY);
        }
    }

    private static void popScale(DrawContext context, String element) {
        if (HudPositions.getScale(element) != 1.0f) {
            org.joml.Matrix3x2fStack mat = ((spz.meowing.mixin.DrawContextAccessor) (Object) context).meowing_getMatrices();
            mat.popMatrix();
        }
    }

    // ==================== INFO HUD ====================
    private static void renderInfoHud(DrawContext context, MinecraftClient mc) {
        HUD hud = (HUD) ModuleManager.getInstance().getModule("HUD");
        if (hud == null || !hud.isEnabled()) return;

        int hudPosX = HudPositions.getX("HUD", 4);
        int hudPosY = HudPositions.getY("HUD", 4);
        pushScale(context, "HUD", hudPosX, hudPosY);
        TextRenderer tr = mc.textRenderer;
        boolean shadow = hud.hasShadow();
        int lineSpacing = hud.getLineSpacing();

        int x = hudPosX + PADDING;
        int y = HudPositions.getY("HUD", 4) + PADDING;
        String[] labels = new String[10];
        String[] values = new String[10];
        int count = 0;

        if (hud.shouldShowFps()) { labels[count] = hud.formatLabel("FPS "); values[count] = String.valueOf(mc.getCurrentFps()); count++; }
        if (hud.shouldShowTps()) { labels[count] = hud.formatLabel("TPS "); values[count] = String.format("%.1f", hud.getCurrentTps()); count++; }
        if (hud.shouldShowPing()) { int p = hud.getPing(); if (p >= 0) { labels[count] = hud.formatLabel("Ping "); values[count] = p + "ms"; count++; } }
        if (hud.shouldShowSpeed()) { labels[count] = hud.formatLabel("Speed "); values[count] = String.format("%.1f", hud.getCurrentSpeed()) + " b/s"; count++; }
        if (hud.shouldShowCoords()) { labels[count] = hud.formatLabel("XYZ "); values[count] = String.format("%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()); count++; }
        if (hud.shouldShowDirection()) { labels[count] = hud.formatLabel("Dir "); values[count] = hud.getDirection() + String.format(" (%.1f)", mc.player.getYaw() % 360); count++; }
        if (hud.shouldShowBiome()) { String b = getBiomeName(mc); if (b != null) { labels[count] = hud.formatLabel("Biome "); values[count] = b; count++; } }
        if (hud.shouldShowTime()) { labels[count] = hud.formatLabel("Time "); values[count] = LocalTime.now().format(DateTimeFormatter.ofPattern(hud.is12h() ? "h:mm:ss a" : "HH:mm:ss")); count++; }

        if (count == 0) return;
        int maxWidth = 0;
        for (int i = 0; i < count; i++) maxWidth = Math.max(maxWidth, tr.getWidth(labels[i]) + tr.getWidth(values[i]));

        int bgX = HudPositions.getX("HUD", 4);
        int bgY = HudPositions.getY("HUD", 4);
        if (hud.hasBackground()) {
            int bgColor = (hud.getBgAlpha() << 24) | 0x101020;
            RenderUtil.drawRoundedRect(context, bgX, bgY, maxWidth + PADDING * 2, count * lineSpacing + PADDING * 2, 4, bgColor);
        }

        for (int i = 0; i < count; i++) {
            int ly = y + i * lineSpacing;
            int labelColor = hud.getLabelColor();
            int accentColor = hud.isChroma() ? chromaColor(i * 20) : hud.getAccentColor();
            drawText(context, tr, labels[i], x, ly, labelColor, shadow);
            drawText(context, tr, values[i], x + tr.getWidth(labels[i]), ly, accentColor, shadow);
        }
        popScale(context, "HUD");
    }

    // ==================== MODULE LIST ====================
    private static void renderModuleList(DrawContext context, MinecraftClient mc) {
        ModuleList ml = (ModuleList) ModuleManager.getInstance().getModule("ModuleList");
        if (ml == null || !ml.isEnabled()) return;

        TextRenderer tr = mc.textRenderer;
        int screenW = mc.getWindow().getScaledWidth();
        int LINE_HEIGHT = 11;

        List<Module> enabled = new ArrayList<>();
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (m.isEnabled() && !(m instanceof ModuleList) && !(m instanceof ClickGUIModule) && !(m instanceof HudEditor)) {
                enabled.add(m);
            }
        }

        if (ml.getSorting().equals("Length")) {
            enabled.sort((a, b) -> tr.getWidth(b.getName()) - tr.getWidth(a.getName()));
        } else {
            enabled.sort(Comparator.comparing(Module::getName));
        }

        Set<String> enabledNames = new HashSet<>();
        for (Module m : enabled) enabledNames.add(m.getName());
        for (Module m : ModuleManager.getInstance().getModules()) {
            String name = m.getName();
            float current = moduleListAnims.getOrDefault(name, 0f);
            float target = enabledNames.contains(name) ? 1f : 0f;
            moduleListAnims.put(name, AnimationUtil.lerp(current, target, 0.15f));
        }

        int mlBaseX = HudPositions.getX("ModuleList", screenW - 110);
        int y = HudPositions.getY("ModuleList", 2);
        pushScale(context, "ModuleList", mlBaseX, y);
        long time = System.currentTimeMillis();
        int speed = ml.getRainbowSpeed();
        int idx = 0;

        for (Module m : enabled) {
            float anim = moduleListAnims.getOrDefault(m.getName(), 0f);
            if (anim < 0.01f) continue;

            String name = ml.isUppercase() ? m.getName().toUpperCase() : m.getName();
            int textW = tr.getWidth(name);
            int entryW = textW + 6;
            int x = (int) (mlBaseX + (screenW - mlBaseX) - entryW * anim);

            int color;
            String mode = ml.getColorMode();
            if (mode.equals("Rainbow") || mode.equals("Chroma")) {
                float speedMult = (float) Math.pow(speed / 5.0, 2.5) * 0.3f;
                float hue = ((time % 360000L) * speedMult + idx * 12) % 360;
                color = java.awt.Color.HSBtoRGB(hue / 360f, 0.6f, 1.0f) | 0xFF000000;
            } else if (mode.equals("Category")) {
                color = getCategoryColor(m.getCategory());
            } else {
                color = 0xFF6C5CE7;
            }

            if (ml.hasBackground()) {
                int bgColor = (ml.getBgAlpha() << 24) | 0x101020;
                RenderUtil.drawRect(context, x, y, entryW, LINE_HEIGHT, bgColor);
            }
            if (ml.hasAccentBar()) {
                context.fill(x + entryW - 2, y, x + entryW, y + LINE_HEIGHT, color);
            }
            drawText(context, tr, name, x + 2, y + 1, color, ml.hasShadow());
            y += LINE_HEIGHT;
            idx++;
        }
        popScale(context, "ModuleList");
    }

    // ==================== KEYSTROKES ====================
    private static void renderKeystrokes(DrawContext context, MinecraftClient mc) {
        Keystrokes ks = (Keystrokes) ModuleManager.getInstance().getModule("Keystrokes");
        if (ks == null || !ks.isEnabled()) return;

        int baseX = HudPositions.getX("Keystrokes", ks.getPosX() + 4);
        int baseY = HudPositions.getY("Keystrokes", ks.getPosY() + 4);
        pushScale(context, "Keystrokes", baseX, baseY);
        TextRenderer tr = mc.textRenderer;
        long handle = mc.getWindow().getHandle();
        int keySize = ks.getKeySize();
        int gap = 2;
        int accent = ks.getAccentColor();
        boolean chroma = ks.isChroma();
        String style = ks.getStyle();
        int bgAlpha = ks.getBgAlpha();

        boolean lmbNow = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmbNow = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (lmbNow && !lastLmb) ks.registerLeftClick();
        if (rmbNow && !lastRmb) ks.registerRightClick();
        lastLmb = lmbNow;
        lastRmb = rmbNow;

        int ci = 0;
        renderKey(context, tr, "W", baseX + keySize + gap, baseY, keySize, keySize,
                GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS, chroma ? chromaColor(ci++ * 30) : accent, style, bgAlpha);
        renderKey(context, tr, "A", baseX, baseY + keySize + gap, keySize, keySize,
                GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS, chroma ? chromaColor(ci++ * 30) : accent, style, bgAlpha);
        renderKey(context, tr, "S", baseX + keySize + gap, baseY + keySize + gap, keySize, keySize,
                GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS, chroma ? chromaColor(ci++ * 30) : accent, style, bgAlpha);
        renderKey(context, tr, "D", baseX + (keySize + gap) * 2, baseY + keySize + gap, keySize, keySize,
                GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS, chroma ? chromaColor(ci++ * 30) : accent, style, bgAlpha);

        int nextY = baseY + (keySize + gap) * 2;
        int totalW = keySize * 3 + gap * 2;

        if (ks.showSpace()) {
            renderKey(context, tr, "---", baseX, nextY, totalW, 10,
                    GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS, chroma ? chromaColor(ci++ * 30) : accent, style, bgAlpha);
            nextY += 10 + gap;
        }

        if (ks.showSneak()) {
            renderKey(context, tr, "SNEAK", baseX, nextY, totalW, 10,
                    GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS, chroma ? chromaColor(ci++ * 30) : accent, style, bgAlpha);
            nextY += 10 + gap;
        }

        if (ks.showMouse()) {
            boolean lmb = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmb = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            int halfW = (totalW - gap) / 2;

            String lmbText = ks.showCps() ? "LMB " + ks.getLeftCps() : "LMB";
            String rmbText = ks.showCps() ? "RMB " + ks.getRightCps() : "RMB";

            renderKey(context, tr, lmbText, baseX, nextY, halfW, keySize, lmb, chroma ? chromaColor(ci++ * 30) : accent, style, bgAlpha);
            renderKey(context, tr, rmbText, baseX + halfW + gap, nextY, halfW, keySize, rmb, chroma ? chromaColor(ci++ * 30) : accent, style, bgAlpha);
        }
        popScale(context, "Keystrokes");
    }

    private static void renderKey(DrawContext ctx, TextRenderer tr, String label, int x, int y, int w, int h, boolean pressed, int accent, String style, int bgAlpha) {
        String animKey = label + x + y;
        float current = keyAnims.getOrDefault(animKey, 0f);
        current = AnimationUtil.lerp(current, pressed ? 1f : 0f, 0.25f);
        keyAnims.put(animKey, current);

        int baseBg = (bgAlpha << 24) | 0x111118;
        int bg = ColorUtil.interpolate(baseBg, accent, current * 0.6f);
        int radius = style.equals("Square") ? 0 : 3;

        if (style.equals("Outline")) {
            ctx.fill(x, y, x + w, y + 1, bg);
            ctx.fill(x, y + h - 1, x + w, y + h, bg);
            ctx.fill(x, y, x + 1, y + h, bg);
            ctx.fill(x + w - 1, y, x + w, y + h, bg);
            if (pressed) RenderUtil.drawRoundedRect(ctx, x + 1, y + 1, w - 2, h - 2, 0, ColorUtil.withAlpha(accent, 40));
        } else {
            RenderUtil.drawRoundedRect(ctx, x, y, w, h, radius, bg);
        }

        int textColor = ColorUtil.interpolate(0xFF888899, 0xFFFFFFFF, current);
        int textW = tr.getWidth(label);
        drawText(ctx, tr, label, x + (w - textW) / 2, y + (h - 8) / 2, textColor, false);
    }

    // ==================== INVENTORY HUD ====================
    private static void renderInventoryHud(DrawContext context, MinecraftClient mc) {
        InventoryHUD inv = (InventoryHUD) ModuleManager.getInstance().getModule("InventoryHUD");
        if (inv == null || !inv.isEnabled()) return;

        TextRenderer tr = mc.textRenderer;
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        int bgColor = (inv.getBgAlpha() << 24) | 0x101020;

        // Armor + Offhand (separate draggable element)
        if (inv.showArmor()) {
            int armorX = HudPositions.getX("ArmorHUD", screenW / 2 + 95);
            pushScale(context, "ArmorHUD", armorX, HudPositions.getY("ArmorHUD", screenH - 75));
            int armorY = HudPositions.getY("ArmorHUD", screenH - 75);

            ItemStack[] armor = {
                    mc.player.getEquippedStack(EquipmentSlot.HEAD),
                    mc.player.getEquippedStack(EquipmentSlot.CHEST),
                    mc.player.getEquippedStack(EquipmentSlot.LEGS),
                    mc.player.getEquippedStack(EquipmentSlot.FEET)
            };

            // Render armor horizontally
            int armorCount = 4 + (inv.showOffhand() ? 1 : 0);
            if (inv.hasBackground()) {
                RenderUtil.drawRoundedRect(context, armorX - 2, armorY - 2, armorCount * 18 + 4, 20, 3, bgColor);
            }
            for (int i = 0; i < 4; i++) {
                if (!armor[i].isEmpty()) {
                    context.drawItem(armor[i], armorX + i * 18, armorY);
                    if (inv.showDurability() && armor[i].isDamageable()) {
                        float dur = 1f - (float) armor[i].getDamage() / armor[i].getMaxDamage();
                        int durColor = dur > 0.5f ? 0xFF2ECC71 : (dur > 0.25f ? 0xFFE67E22 : 0xFFE74C3C);
                        int barW = (int) (16 * dur);
                        context.fill(armorX + i * 18, armorY + 15, armorX + i * 18 + barW, armorY + 16, durColor);
                    }
                }
            }

            // Offhand next to armor
            if (inv.showOffhand()) {
                ItemStack offhand = mc.player.getOffHandStack();
                if (!offhand.isEmpty()) {
                    context.drawItem(offhand, armorX + 4 * 18, armorY);
                }
            }
            popScale(context, "ArmorHUD");
        }

        // Inventory grid (separate draggable element, always 9x3 horizontal)
        if (inv.showInventory()) {
            int invX = HudPositions.getX("Inventory", screenW / 2 - 82);
            pushScale(context, "Inventory", invX, HudPositions.getY("Inventory", screenH - 80));
            int invY = HudPositions.getY("Inventory", screenH - 80);

            if (inv.hasBackground()) {
                RenderUtil.drawRoundedRect(context, invX - 2, invY - 2, 9 * 18 + 4, 3 * 18 + 4, 3, bgColor);
            }

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int slot = 9 + row * 9 + col;
                    ItemStack stack = mc.player.getInventory().getStack(slot);
                    if (!stack.isEmpty()) {
                        int ix = invX + col * 18;
                        int iy = invY + row * 18;
                        context.drawItem(stack, ix, iy);
                        if (inv.showItemCount() && stack.getCount() > 1) {
                            String ct = String.valueOf(stack.getCount());
                            context.drawText(tr, ct, ix + 17 - tr.getWidth(ct), iy + 9, 0xFFFFFFFF, true);
                        }
                        if (inv.showDurability() && stack.isDamageable()) {
                            float dur = 1f - (float) stack.getDamage() / stack.getMaxDamage();
                            int durColor = dur > 0.5f ? 0xFF2ECC71 : (dur > 0.25f ? 0xFFE67E22 : 0xFFE74C3C);
                            int barW = (int) (16 * dur);
                            context.fill(ix, iy + 15, ix + barW, iy + 16, durColor);
                        }
                    }
                }
            }
            popScale(context, "Inventory");
        }
    }


    // ==================== TARGET HUD ====================
    private static void renderTargetHud(DrawContext context, MinecraftClient mc) {
        TargetHUD th = (TargetHUD) ModuleManager.getInstance().getModule("TargetHUD");
        if (th == null || !th.isEnabled()) return;
        if (th.getShowAnim() < 0.01f) return;

        // Terminal mode: separate rendering path
        if (th.isTerminalMode()) {
            renderTerminalHud(context, mc, th);
            return;
        }

        TextRenderer tr = mc.textRenderer;
        var target = th.getTarget();
        if (target == null && th.getShowAnim() < 0.05f) return;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        boolean detailed = th.getStyle().equals("Detailed");

        int boxW = detailed ? 150 : 120;
        int boxH = detailed ? 44 : 28;

        int baseX = HudPositions.getX("TargetHUD", screenW / 2 + 10);
        int baseY = HudPositions.getY("TargetHUD", screenH / 2 + 10);
        pushScale(context, "TargetHUD", baseX, baseY);

        float anim = th.getShowAnim();
        int bgAlpha = (int) (th.getBgAlpha() * anim);
        int bgColor = (bgAlpha << 24) | 0x111118;

        int animW = (int) (boxW * anim);
        int animH = (int) (boxH * anim);
        int x = baseX + (boxW - animW) / 2;
        int y = baseY + (boxH - animH) / 2;

        RenderUtil.drawRoundedRect(context, x, y, animW, animH, 4, bgColor);

        if (anim < 0.5f || target == null) return;

        int accent = th.isChroma() ? chromaColor(0) : th.getAccentColor();

        // Accent bar
        context.fill(x, y, x + 3, y + animH, accent);

        // Name
        String name = target.getName().getString();
        drawText(context, tr, name, x + 8, y + 4, 0xFFE8E8EE, true);

        // Health bar
        int barX = x + 8;
        int barY = y + 16;
        int barW = animW - 16;
        int barH = 4;

        RenderUtil.drawRoundedRect(context, barX, barY, barW, barH, 2, 0xFF2A2A3A);
        int healthW = Math.max(1, (int) (barW * th.getHealthAnim()));
        int healthColor = th.getHealthAnim() > 0.5f ? 0xFF2ECC71 : (th.getHealthAnim() > 0.25f ? 0xFFE67E22 : 0xFFE74C3C);
        RenderUtil.drawRoundedRect(context, barX, barY, healthW, barH, 2, healthColor);

        // Health text
        String healthText = String.format("%.1f", target.getHealth()) + " / " + String.format("%.0f", target.getMaxHealth());
        context.drawText(tr, healthText, barX + barW - tr.getWidth(healthText), barY - 1, 0xFF888899, false);

        if (detailed) {
            int detailY = barY + 8;

            // Distance
            if (th.showDistance()) {
                String dist = String.format("%.1fm", th.getDistToTarget());
                context.drawText(tr, dist, x + 8, detailY, 0xFF888899, false);
            }

            // Armor
            if (th.showArmor() && target instanceof net.minecraft.entity.player.PlayerEntity player) {
                int armorX = x + animW - 72;
                net.minecraft.entity.EquipmentSlot[] slots = {
                        net.minecraft.entity.EquipmentSlot.HEAD,
                        net.minecraft.entity.EquipmentSlot.CHEST,
                        net.minecraft.entity.EquipmentSlot.LEGS,
                        net.minecraft.entity.EquipmentSlot.FEET
                };
                for (int i = 0; i < 4; i++) {
                    var stack = player.getEquippedStack(slots[i]);
                    if (!stack.isEmpty()) {
                        context.drawItem(stack, armorX + i * 16, detailY - 2);
                    }
                }
            }
        }
        popScale(context, "TargetHUD");
    }

    private static void renderTerminalHud(DrawContext context, MinecraftClient mc, TargetHUD th) {
        if (th.getTerminalEntity() == null && th.getShowAnim() < 0.05f) return;

        TextRenderer tr = mc.textRenderer;
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        int boxW = 130;
        int boxH = 32;

        int baseX = HudPositions.getX("TargetHUD", screenW / 2 + 10);
        int baseY = HudPositions.getY("TargetHUD", screenH / 2 + 10);
        pushScale(context, "TargetHUD", baseX, baseY);

        float anim = th.getShowAnim();
        int animW = (int) (boxW * anim);
        int animH = (int) (boxH * anim);
        int x = baseX + (boxW - animW) / 2;
        int y = baseY + (boxH - animH) / 2;

        // Background: green if active, red if inactive
        int bgColor = th.getTerminalEntity() != null
                ? ColorUtil.withAlpha(th.isTerminalActive() ? 0x1B5E20 : 0x8B0000, (int)(th.getBgAlpha() * anim))
                : (((int)(th.getBgAlpha() * anim)) << 24) | 0x111118;

        RenderUtil.drawRoundedRect(context, x, y, animW, animH, 5, bgColor);

        if (anim < 0.5f || th.getTerminalEntity() == null) {
            popScale(context, "TargetHUD");
            return;
        }

        // Accent bar: green or red
        int accentColor = th.isTerminalActive() ? 0xFF2ECC71 : 0xFFE74C3C;
        context.fill(x, y, x + 3, y + animH, accentColor);

        // Terminal name
        String name = th.getTerminalName();
        drawText(context, tr, name, x + 8, y + 4, 0xFFE8E8EE, true);

        // Status text
        String status = th.getTerminalStatus();
        String cleanStatus = status.replaceAll("\u00a7.", "");
        int statusColor = th.isTerminalActive() ? 0xFF55FF55 : 0xFFFF5555;
        context.drawText(tr, cleanStatus, x + 8, y + 16, statusColor, true);

        // Distance
        if (th.showDistance()) {
            String dist = String.format("%.1fm", th.getDistToTerminal());
            int distW = tr.getWidth(dist);
            context.drawText(tr, dist, x + animW - distW - 6, y + 16, 0xFF888899, false);
        }

        popScale(context, "TargetHUD");
    }


    // ==================== MASK TIMER ALERT ====================
    private static void renderMaskAlert(DrawContext context, MinecraftClient mc) {
        var mod = ModuleManager.getInstance().getModule("MaskTimers");
        if (!(mod instanceof spz.meowing.module.impl.dungeons.MaskTimers mt) || !mt.isEnabled()) return;
        if (!mt.hasActiveAlert()) return;

        TextRenderer tr = mc.textRenderer;
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        float alpha = mt.getAlertAlpha();
        float scale = mt.getTitleScale();

        int defX = screenW / 2;
        int defY = screenH / 3 + 30;
        int x = HudPositions.getX("MaskAlert", defX);
        int y = HudPositions.getY("MaskAlert", defY);
        pushScale(context, "MaskAlert", x, y);

        int a = (int) (255 * alpha);
        int titleColor = (a << 24) | (mt.getAlertColor() & 0x00FFFFFF);
        int subColor = (a << 24) | 0xBBBBBB;

        org.joml.Matrix3x2fStack mat = ((spz.meowing.mixin.DrawContextAccessor) (Object) context).meowing_getMatrices();
        mat.pushMatrix();
        mat.translate(x, y);
        mat.scale(scale, scale);
        mat.translate(-x, -y);

        String title = mt.getAlertTitle();
        String subtitle = mt.getAlertSubtitle();

        int titleW = tr.getWidth(title);
        context.drawText(tr, title, x - titleW / 2, y, titleColor, true);

        if (!subtitle.isEmpty()) {
            int subW = tr.getWidth(subtitle);
            context.drawText(tr, subtitle, x - subW / 2, y + 12, subColor, true);
        }

        mat.popMatrix();
        popScale(context, "MaskAlert");
    }

    // ==================== MASK TIMERS ====================
    private static void renderMaskTimers(DrawContext context, MinecraftClient mc) {
        var mod = ModuleManager.getInstance().getModule("MaskTimers");
        if (!(mod instanceof spz.meowing.module.impl.dungeons.MaskTimers mt) || !mt.isEnabled()) return;

        var entries = mt.getVisibleEntries();
        if (entries.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        int screenH = mc.getWindow().getScaledHeight();

        int baseX = HudPositions.getX("MaskTimers", 4);
        int baseY = HudPositions.getY("MaskTimers", screenH / 2);
        pushScale(context, "MaskTimers", baseX, baseY);

        int y = baseY;
        int maxW = 0;

        // Pre-calculate widths
        for (var entry : entries) {
            String line = entry.displayName + ": " + entry.getStatusText().replaceAll("\u00a7.", "");
            maxW = Math.max(maxW, tr.getWidth(line));
        }

        int bgW = maxW + 14;
        int bgH = entries.size() * 14 + 6;

        // Background
        RenderUtil.drawRoundedRect(context, baseX, y - 2, bgW, bgH, 4, 0xC0101020);

        for (var entry : entries) {
            int textY = y + 2;

            // Cooldown progress bar behind text
            if (!entry.isReady()) {
                float progress = entry.getCooldownProgress();
                int barW = (int) ((bgW - 4) * progress);
                int barColor = entry.isImmune() ? 0x3055FFFF : 0x20FFFFFF;
                context.fill(baseX + 2, textY - 1, baseX + 2 + barW, textY + 10, barColor);
            }

            // Name
            String name = entry.displayName;
            context.drawText(tr, name, baseX + 4, textY, entry.color, true);

            // Status
            String status = entry.getStatusText();
            // Parse section sign color codes for rendering
            int statusX = baseX + 4 + tr.getWidth(name + ": ");
            context.drawText(tr, ": ", baseX + 4 + tr.getWidth(name), textY, 0xFF888888, false);

            // Render status with color codes
            int cx = statusX;
            int currentColor = 0xFFFFFFFF;
            String[] parts = status.split("\u00a7");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (i > 0 && !part.isEmpty()) {
                    currentColor = sectionColor(part.charAt(0));
                    part = part.substring(1);
                }
                if (!part.isEmpty()) {
                    context.drawText(tr, part, cx, textY, currentColor, true);
                    cx += tr.getWidth(part);
                }
            }

            y += 14;
        }

        popScale(context, "MaskTimers");
    }

    private static int sectionColor(char code) {
        return switch (code) {
            case '0' -> 0xFF000000;
            case '1' -> 0xFF0000AA;
            case '2' -> 0xFF00AA00;
            case '3' -> 0xFF00AAAA;
            case '4' -> 0xFFAA0000;
            case '5' -> 0xFFAA00AA;
            case '6' -> 0xFFFFAA00;
            case '7' -> 0xFFAAAAAA;
            case '8' -> 0xFF555555;
            case '9' -> 0xFF5555FF;
            case 'a' -> 0xFF55FF55;
            case 'b' -> 0xFF55FFFF;
            case 'c' -> 0xFFFF5555;
            case 'd' -> 0xFFFF55FF;
            case 'e' -> 0xFFFFFF55;
            default -> 0xFFFFFFFF;
        };
    }

    // ==================== WATCHER ALERT TITLE ====================
    private static void renderWatcherTitle(DrawContext context, MinecraftClient mc) {
        var mod = ModuleManager.getInstance().getModule("WatcherAlert");
        if (!(mod instanceof spz.meowing.module.impl.dungeons.WatcherAlert wa) || !wa.isEnabled()) return;
        if (!wa.hasActiveTitle()) return;

        TextRenderer tr = mc.textRenderer;
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        float alpha = wa.getTitleAlpha();
        float scale = wa.getTitleScale();

        int defX = screenW / 2;
        int defY = screenH / 3;
        int x = HudPositions.getX("WatcherAlert", defX);
        int y = HudPositions.getY("WatcherAlert", defY);
        pushScale(context, "WatcherAlert", x, y);

        int color = wa.getCurrentColor();
        int a = (int) (255 * alpha);
        int textColor = (a << 24) | (color & 0x00FFFFFF);
        int subColor = (a << 24) | 0xBBBBBB;

        String title = wa.getCurrentTitle();
        String subtitle = wa.getCurrentSubtitle();

        // Scale text by drawing with matrix scale
        org.joml.Matrix3x2fStack mat = ((spz.meowing.mixin.DrawContextAccessor) (Object) context).meowing_getMatrices();
        mat.pushMatrix();
        mat.translate(x, y);
        mat.scale(scale, scale);
        mat.translate(-x, -y);

        // Title
        int titleW = tr.getWidth(title);
        context.drawText(tr, title, x - titleW / 2, y, textColor, true);

        // Subtitle (smaller, below)
        if (!subtitle.isEmpty()) {
            int subW = tr.getWidth(subtitle);
            context.drawText(tr, subtitle, x - subW / 2, y + (int)(12 * scale / scale), subColor, true);
        }

        mat.popMatrix();
        popScale(context, "WatcherAlert");
    }

    private static int getCategoryColor(spz.meowing.module.Category cat) {
        return switch (cat) {
            case DUNGEONS -> 0xFF9B59B6;
            case RENDER -> 0xFF2ECC71;
            case MISC -> 0xFFE67E22;
        };
    }

    private static String getBiomeName(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return null;
        try {
            RegistryEntry<Biome> biome = mc.world.getBiome(mc.player.getBlockPos());
            return biome.getKey()
                    .map(key -> key.getValue().getPath().substring(0, 1).toUpperCase() + key.getValue().getPath().substring(1).replace('_', ' '))
                    .orElse("Unknown");
        } catch (Exception e) { return null; }
    }
}
