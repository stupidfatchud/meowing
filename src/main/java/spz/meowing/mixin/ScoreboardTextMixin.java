package spz.meowing.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.PurseSpoofer;

@Mixin(Team.class)
public abstract class ScoreboardTextMixin {

    @ModifyReturnValue(method = "decorateName", at = @At("RETURN"))
    private static MutableText modifyTeamDecoratedName(MutableText original) {
        try {
            String text = original.getString();

            // ===== PURSE SPOOFER =====
            var psMod = ModuleManager.getInstance().getModule("PurseSpoofer");
            if (psMod instanceof PurseSpoofer ps && ps.isEnabled()) {
                if (ps.shouldSpoofPurse() && (text.contains("Purse:") || text.contains("Piggy:"))) {
                    String label = text.contains("Purse:") ? "Purse: " : "Piggy: ";
                    return Text.literal(label).styled(s -> s.withColor(0xAAAAAA))
                            .append(Text.literal(ps.getPurseAmount()).styled(s -> s.withColor(0xFFAA00)));
                }

                if (ps.shouldSpoofBits() && text.contains("Bits:")) {
                    return Text.literal("Bits: ").styled(s -> s.withColor(0xAAAAAA))
                            .append(Text.literal(ps.getBitsAmount()).styled(s -> s.withColor(0x55FFFF)));
                }
            }
        } catch (Exception ignored) {}
        return original;
    }
}
