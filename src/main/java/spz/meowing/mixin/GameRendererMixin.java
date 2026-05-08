package spz.meowing.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.AspectRatio;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float modifyFov(float original) {
        try {
            ModuleManager mgr = ModuleManager.getInstance();
            if (mgr == null) return original;

            // FOV / Aspect Ratio: adjust FOV based on ratio difference
            var aspect = mgr.getModule("FOV");
            if (aspect instanceof AspectRatio ar && ar.isEnabled()) {
                net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                float currentRatio = (float) mc.getWindow().getWidth() / mc.getWindow().getHeight();
                float targetRatio = ar.getTargetRatio();
                if (currentRatio > 0 && targetRatio > 0) {
                    original *= targetRatio / currentRatio;
                }
            }
        } catch (Exception ignored) {}
        return original;
    }
}
