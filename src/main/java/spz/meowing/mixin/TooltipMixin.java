package spz.meowing.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import spz.meowing.gui.tooltip.SkyblockTooltipRenderer;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.CustomTooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(DrawContext.class)
public class TooltipMixin {

    /**
     * Capture raw Text objects before conversion to TooltipComponents.
     * Preserves formatting codes for colored stat rendering and rarity detection.
     */
    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V",
            at = @At("HEAD"))
    private void captureTooltipText(TextRenderer textRenderer, List<Text> text, Optional<TooltipData> data,
                                    int x, int y, Identifier texture, CallbackInfo ci) {
        var mod = ModuleManager.getInstance().getModule("CustomTooltip");
        if (!(mod instanceof CustomTooltip ct) || !ct.isEnabled()) return;

        SkyblockTooltipRenderer.setRawText(new ArrayList<>(text));
    }

    @Inject(method = "drawTooltipImmediately", at = @At("HEAD"), cancellable = true)
    private void onDrawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y,
                               TooltipPositioner positioner, Identifier texture, CallbackInfo ci) {
        var mod = ModuleManager.getInstance().getModule("CustomTooltip");
        if (!(mod instanceof CustomTooltip ct) || !ct.isEnabled()) return;
        if (components.isEmpty()) return;

        DrawContext self = (DrawContext) (Object) this;

        int maxWidth = 0;
        int totalHeight = components.size() == 1 ? -2 : 0;
        for (TooltipComponent comp : components) {
            int w = comp.getWidth(textRenderer);
            if (w > maxWidth) maxWidth = w;
            totalHeight += comp.getHeight(textRenderer);
        }

        Vector2ic pos = positioner.getPosition(
                self.getScaledWindowWidth(), self.getScaledWindowHeight(),
                x, y, maxWidth, totalHeight
        );

        SkyblockTooltipRenderer.render(self, textRenderer, components, pos.x(), pos.y(), maxWidth, totalHeight);
        ci.cancel();
    }
}
