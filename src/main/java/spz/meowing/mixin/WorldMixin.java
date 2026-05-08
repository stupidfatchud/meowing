package spz.meowing.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.WorldModule;

@Mixin(World.class)
public abstract class WorldMixin {

    @Shadow public abstract boolean isClient();

    @ModifyReturnValue(method = "getTimeOfDay", at = @At("RETURN"))
    private long overrideTimeOfDay(long original) {
        if (!isClient()) return original;
        try {
            var mod = ModuleManager.getInstance().getModule("World");
            if (mod instanceof WorldModule w && w.isEnabled() && w.shouldOverrideTime()) {
                return w.getWorldTime();
            }
        } catch (Exception ignored) {}
        return original;
    }

    @ModifyReturnValue(method = "getRainGradient", at = @At("RETURN"))
    private float overrideRainGradient(float original) {
        if (!isClient()) return original;
        try {
            var mod = ModuleManager.getInstance().getModule("World");
            if (mod instanceof WorldModule w && w.isEnabled() && w.shouldOverrideWeather()) {
                return w.shouldRain() ? 1.0f : 0.0f;
            }
        } catch (Exception ignored) {}
        return original;
    }

    @ModifyReturnValue(method = "getThunderGradient", at = @At("RETURN"))
    private float overrideThunderGradient(float original) {
        if (!isClient()) return original;
        try {
            var mod = ModuleManager.getInstance().getModule("World");
            if (mod instanceof WorldModule w && w.isEnabled() && w.shouldOverrideWeather()) {
                return w.shouldThunder() ? 1.0f : 0.0f;
            }
        } catch (Exception ignored) {}
        return original;
    }
}
