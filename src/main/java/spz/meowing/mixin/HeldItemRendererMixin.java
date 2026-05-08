package spz.meowing.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.Animations;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Unique
    private float meowing_capturedSwing = 0f;

    // Capture the original swing progress, only zero for No Swing
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private float captureSwing(float swingProgress) {
        meowing_capturedSwing = swingProgress;
        Animations anim = getAnimations();
        if (anim != null && anim.isNoSwing()) return 0f;
        return swingProgress;
    }

    // No Equip Reset: force equip progress to 1.0
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private float modifyEquipProgress(float equipProgress) {
        Animations anim = getAnimations();
        if (anim != null && anim.isNoEquipReset()) return 1.0f;
        return equipProgress;
    }

    // Apply user custom transforms (size, position, rotation) after push()
    @WrapOperation(
            method = "renderFirstPersonItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V")
    )
    private void applyUserTransforms(MatrixStack matrices, Operation<Void> original) {
        original.call(matrices);

        Animations anim = getAnimations();
        if (anim == null) return;

        float px = (float) anim.getPosX();
        float py = (float) anim.getPosY();
        float pz = (float) anim.getPosZ();
        float size = (float) anim.getSize();
        float yaw = (float) anim.getYaw();
        float pitch = (float) anim.getPitch();
        float roll = (float) anim.getRoll();

        if (px != 0 || py != 0 || pz != 0) matrices.translate(px, py, pz);
        if (size != 1.0f) matrices.scale(size, size, size);
        if (yaw != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        if (pitch != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        if (roll != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
    }

    // Replace vanilla swing animation with custom mode
    // swingArm is called from renderFirstPersonItem after applyEquipOffset
    // Signature: swingArm(float swingProgress, MatrixStack matrices, int light, Arm arm)
    @WrapOperation(
            method = "renderFirstPersonItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V")
    )
    private void replaceSwingAnimation(HeldItemRenderer instance, float swingProgress, MatrixStack matrices, int light, Arm arm, Operation<Void> original) {
        Animations anim = getAnimations();

        // No module or Normal mode: use vanilla swing
        if (anim == null || anim.getSwingMode().equals("Normal") || anim.isNoSwing()) {
            original.call(instance, swingProgress, matrices, light, arm);
            return;
        }

        // Custom mode: skip vanilla, apply our own using the captured swing value
        if (meowing_capturedSwing > 0f) {
            int side = (arm == Arm.RIGHT) ? 1 : -1;
            applySwingMode(matrices, anim.getSwingMode(), meowing_capturedSwing, side);
        }
    }

    // Skip equip animation on item swap
    @Inject(method = "shouldSkipHandAnimationOnSwap", at = @At("HEAD"), cancellable = true)
    private static void onShouldSkipSwap(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
        Animations anim = getAnimations();
        if (anim != null && anim.isNoEquipReset()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static void applySwingMode(MatrixStack matrices, String mode, float swing, int side) {
        float sin = MathHelper.sin(swing * (float) Math.PI);
        float sinSq = MathHelper.sin(MathHelper.square(swing) * (float) Math.PI);

        switch (mode) {
            case "1.7" -> {
                // Classic 1.7 blockhit: item tilts and rotates down
                float progress = MathHelper.sin((float) (swing * swing * Math.PI));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * progress * -20.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * sin * -20.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(progress * -80.0f));
                matrices.translate(side * -0.15f * sinSq, 0.15f * sin, 0.0f);
            }
            case "Tap" -> {
                // Quick sharp downward tap
                float tap = (float) Math.pow(sin, 4);
                matrices.translate(side * -0.02f * tap, -0.18f * tap, -0.08f * tap);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0f * tap));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -5.0f * tap));
            }
            case "Slide" -> {
                // Horizontal sweep across screen
                float ease = MathHelper.sin(swing * (float) Math.PI * 0.5f);
                matrices.translate(side * 0.35f * ease, -0.08f * sinSq, -0.15f * sinSq);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 40.0f * ease));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -25.0f * ease));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0f * sinSq));
            }
            case "Smooth" -> {
                // Smooth fluid circular arc
                float cos = MathHelper.cos(swing * (float) Math.PI);
                float eased = sin * sin;
                matrices.translate(side * 0.08f * sin, 0.06f * cos, -0.2f * eased);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0f * eased));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -25.0f * sin));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 10.0f * cos));
            }
            case "Spin" -> {
                // Full spin on swing
                float angle = swing * 360.0f;
                matrices.translate(0.0f, -0.08f * sinSq, -0.12f * sinSq);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * angle));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0f * sinSq));
            }
            case "Sigma" -> {
                // Sharp aggressive pullback and twist
                float sharp = (float) Math.pow(sin, 3);
                float twist = MathHelper.sin(swing * (float) Math.PI * 2.0f);
                matrices.translate(side * -0.08f * sharp, -0.05f * sharp, -0.35f * sharp);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-75.0f * sharp));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -50.0f * sharp));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 20.0f * twist));
            }
            case "Punch" -> {
                // Forward thrust
                float thrust = sin * sin;
                matrices.translate(side * -0.03f * sin, 0.04f * sin, -0.45f * thrust);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-45.0f * thrust));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -15.0f * sin));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 5.0f * sin));
            }
            case "Down" -> {
                // Hard slam downward
                float slam = (float) Math.pow(sinSq, 2);
                matrices.translate(side * -0.05f * sin, -0.4f * slam, -0.08f * slam);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(65.0f * slam));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -25.0f * sin));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 10.0f * sin));
            }
        }
    }

    @Unique
    private static Animations getAnimations() {
        try {
            ModuleManager mgr = ModuleManager.getInstance();
            if (mgr == null) return null;
            var mod = mgr.getModule("Animations");
            if (mod instanceof Animations anim && anim.isEnabled()) return anim;
        } catch (Exception ignored) {}
        return null;
    }
}
