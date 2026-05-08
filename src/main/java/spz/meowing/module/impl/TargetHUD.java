package spz.meowing.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;
import spz.meowing.util.AnimationUtil;

public class TargetHUD extends Module {

    private final ModeSetting targetMode = addSetting(new ModeSetting("Target", "All Entities",
            "All Entities", "Terminals Only"));
    private final ModeSetting style = addSetting(new ModeSetting("Style", "Detailed", "Detailed", "Minimal"));
    private final NumberSetting animSpeed = addSetting(new NumberSetting("Anim Speed", 5.0, 1.0, 10.0, 0.5));
    private final BooleanSetting showArmor = addSetting(new BooleanSetting("Show Armor", true));
    private final BooleanSetting showDistance = addSetting(new BooleanSetting("Show Distance", true));
    private final NumberSetting opacity = addSetting(new NumberSetting("Opacity", 0.85, 0.1, 1.0, 0.05));
    private final ModeSetting colorTheme = addSetting(new ModeSetting("Color", "Purple",
            "Purple", "Blue", "Cyan", "Green", "Red", "White", "Chroma"));

    private LivingEntity currentTarget = null;
    private Entity terminalEntity = null; // For terminal mode
    private float showAnim = 0f;
    private float healthAnim = 0f;
    private long lastTargetTime = 0;
    private boolean terminalActive = false; // true = active (green), false = inactive (red)

    public TargetHUD() {
        super("TargetHUD", "Shows target info during combat", Category.RENDER, -1);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (targetMode.getValue().equals("Terminals Only")) {
            tickTerminalMode(mc);
        } else {
            tickEntityMode(mc);
        }
    }

    private void tickEntityMode(MinecraftClient mc) {
        terminalEntity = null;

        LivingEntity found = null;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            var entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (entity instanceof LivingEntity living && living != mc.player && living.isAlive()) {
                found = living;
            }
        }

        if (found == null && mc.player.getAttacking() != null) {
            var last = mc.player.getAttacking();
            if (last instanceof LivingEntity living && living.isAlive()) {
                double dist = Math.sqrt(Math.pow(mc.player.getX() - living.getX(), 2)
                        + Math.pow(mc.player.getY() - living.getY(), 2)
                        + Math.pow(mc.player.getZ() - living.getZ(), 2));
                if (dist < 10) found = living;
            }
        }

        if (found != null) {
            currentTarget = found;
            lastTargetTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastTargetTime > 2000) {
            currentTarget = null;
        }

        float targetShow = currentTarget != null ? 1f : 0f;
        float speed = (float) (animSpeed.getValue() * 0.06);
        showAnim = AnimationUtil.lerp(showAnim, targetShow, speed);

        if (currentTarget != null) {
            float targetHealth = currentTarget.getHealth() / currentTarget.getMaxHealth();
            healthAnim = AnimationUtil.lerp(healthAnim, targetHealth, speed * 0.8f);
        }
    }

    private void tickTerminalMode(MinecraftClient mc) {
        currentTarget = null;
        Entity found = null;
        boolean active = false;

        // Search nearby armor stands for terminal nametags
        for (Entity entity : mc.world.getEntitiesByClass(ArmorStandEntity.class,
                mc.player.getBoundingBox().expand(5), e -> true)) {
            String name = entity.getName().getString().replaceAll("§.", "").trim();

            if (name.contains("Inactive Terminal") || name.contains("Not Activated")) {
                found = entity;
                active = false;
                break;
            }
            if (name.contains("Active Terminal") || name.contains("CLICK HERE") || name.contains("Activated")) {
                found = entity;
                active = true;
                break;
            }
        }

        // Also check crosshair entity
        if (found == null && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            String name = entity.getName().getString().replaceAll("§.", "").trim();

            if (name.contains("Terminal") || name.contains("Activated") || name.contains("CLICK HERE")) {
                found = entity;
                active = name.contains("Active") || name.contains("CLICK HERE") || name.contains("Activated");
                if (name.contains("Inactive") || name.contains("Not Activated")) active = false;
            }
        }

        if (found != null) {
            terminalEntity = found;
            terminalActive = active;
            lastTargetTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastTargetTime > 2000) {
            terminalEntity = null;
        }

        float targetShow = terminalEntity != null ? 1f : 0f;
        float speed = (float) (animSpeed.getValue() * 0.06);
        showAnim = AnimationUtil.lerp(showAnim, targetShow, speed);
    }

    // ==================== GETTERS ====================

    public boolean isTerminalMode() { return targetMode.getValue().equals("Terminals Only"); }
    public Entity getTerminalEntity() { return terminalEntity; }
    public boolean isTerminalActive() { return terminalActive; }

    /** Background color for terminal mode: green if active, red if inactive */
    public int getTerminalBgColor() {
        int alpha = getBgAlpha();
        if (terminalActive) {
            return (alpha << 24) | 0x1B5E20; // Dark green
        } else {
            return (alpha << 24) | 0x8B0000; // Dark red
        }
    }

    public String getTerminalName() {
        return "Terminal";
    }

    public String getTerminalStatus() {
        return terminalActive ? "\u00a7aACTIVE" : "\u00a7cINACTIVE";
    }

    public double getDistToTerminal() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || terminalEntity == null) return 0;
        return Math.sqrt(Math.pow(mc.player.getX() - terminalEntity.getX(), 2)
                + Math.pow(mc.player.getY() - terminalEntity.getY(), 2)
                + Math.pow(mc.player.getZ() - terminalEntity.getZ(), 2));
    }

    public LivingEntity getTarget() { return currentTarget; }
    public float getShowAnim() { return showAnim; }
    public float getHealthAnim() { return healthAnim; }
    public String getStyle() { return style.getValue(); }
    public boolean showArmor() { return showArmor.getValue(); }
    public boolean showDistance() { return showDistance.getValue(); }
    public int getBgAlpha() { return (int) (opacity.getValue() * 255); }
    public boolean isChroma() { return colorTheme.getValue().equals("Chroma"); }
    public int getAccentColor() {
        return switch (colorTheme.getValue()) {
            case "Blue" -> 0xFF3498DB;
            case "Cyan" -> 0xFF00D2FF;
            case "Green" -> 0xFF2ECC71;
            case "Red" -> 0xFFE74C3C;
            case "White" -> 0xFFFFFFFF;
            case "Chroma" -> 0xFFFFFFFF;
            default -> 0xFF6C5CE7;
        };
    }

    public double getDistToTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || currentTarget == null) return 0;
        return Math.sqrt(Math.pow(mc.player.getX() - currentTarget.getX(), 2)
                + Math.pow(mc.player.getY() - currentTarget.getY(), 2)
                + Math.pow(mc.player.getZ() - currentTarget.getZ(), 2));
    }
}
