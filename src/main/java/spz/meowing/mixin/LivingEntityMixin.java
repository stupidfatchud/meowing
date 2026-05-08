package spz.meowing.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.Animations;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    public boolean handSwinging;

    // Speed + No Effects: modify swing duration
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof ClientPlayerEntity)) return;

        Animations anim = getAnimations();
        if (anim == null) return;

        int baseDuration;
        if (anim.isNoEffects()) {
            // Ignore status effects, use vanilla base of 6
            baseDuration = 6;
        } else {
            baseDuration = cir.getReturnValue();
        }

        double speed = anim.getSpeed();
        if (speed != 1.0) {
            baseDuration = Math.max(1, (int) (baseDuration / speed));
        }

        if (baseDuration != cir.getReturnValue()) {
            cir.setReturnValue(baseDuration);
        }
    }

    // Disable Reswing: prevent swinging again while already swinging
    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true)
    private void onSwingHand(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity)) return;

        Animations anim = getAnimations();
        if (anim != null && anim.isDisableReswing() && this.handSwinging) {
            ci.cancel();
        }
    }

    private Animations getAnimations() {
        try {
            ModuleManager mgr = ModuleManager.getInstance();
            if (mgr == null) return null;
            var mod = mgr.getModule("Animations");
            if (mod instanceof Animations a && a.isEnabled()) return a;
        } catch (Exception ignored) {}
        return null;
    }
}
