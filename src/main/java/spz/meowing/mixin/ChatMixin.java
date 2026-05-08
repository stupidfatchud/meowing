package spz.meowing.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.dungeons.MaskTimers;
import spz.meowing.module.impl.dungeons.WatcherAlert;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ChatMixin {

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onChatMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        try {
            String message = packet.content().getString();
            var mod = ModuleManager.getInstance().getModule("WatcherAlert");
            if (mod instanceof WatcherAlert wa) {
                wa.onChatMessage(message);
            }

            var maskMod = ModuleManager.getInstance().getModule("MaskTimers");
            if (maskMod instanceof MaskTimers mt) {
                mt.onChatMessage(message);
            }
        } catch (Exception ignored) {}
    }
}
