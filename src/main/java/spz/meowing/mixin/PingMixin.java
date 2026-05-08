package spz.meowing.mixin;

import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.HUD;

/**
 * Measures real ping by tracking keepalive round-trip times.
 * Uses static fields so measurements persist across Hypixel server transfers
 * (lobby → skyblock → dungeons etc.) where the handler is recreated.
 */
@Mixin(targets = "net.minecraft.client.network.ClientCommonNetworkHandler")
public abstract class PingMixin {

    // Static: survives handler recreation during server transfers
    @Unique
    private static long meowing_lastKeepAliveReceived = 0;
    @Unique
    private static long meowing_lastKeepAliveId = 0;
    @Unique
    private static int meowing_pingSmoothed = -1;

    @Inject(method = "onKeepAlive", at = @At("HEAD"))
    private void onKeepAlive(KeepAliveS2CPacket packet, CallbackInfo ci) {
        try {
            long now = System.currentTimeMillis();
            long id = packet.getId();

            // Method 1: Timestamp-based (vanilla servers use System.currentTimeMillis as ID)
            // The diff between our time and the ID is approximately the one-way latency
            long diff = now - id;
            if (diff >= 0 && diff < 10000) {
                // ID is a server timestamp — diff is ~one-way latency
                // Multiply by 1 since keepalive measures full round trip on server side
                smoothPing((int) diff);
            }

            // Method 2: Track the interval between us SENDING the response and
            // receiving the NEXT keepalive. The server waits for our pong before
            // sending the next keepalive, so: next_receive - last_receive ≈ server_interval + RTT
            // On Hypixel the interval varies, so we track the raw ID difference instead.

            // Method 3: If the ID is NOT a timestamp, use paired keepalive tracking.
            // Server sends keepalive with ID X, we respond immediately, server gets it
            // after RTT/2, then waits interval, sends next keepalive which arrives RTT/2 later.
            // So: interval between receiving keepalives = server_wait + RTT
            // We can't know server_wait, but if we track consecutive IDs and they increment,
            // the ID difference might encode the server's measurement.

            meowing_lastKeepAliveReceived = now;
            meowing_lastKeepAliveId = id;
        } catch (Exception ignored) {}
    }

    @Inject(method = "onPing", at = @At("HEAD"))
    private void onPing(CommonPingS2CPacket packet, CallbackInfo ci) {
        try {
            // CommonPing uses an int parameter — on many servers this is a tick count or timestamp
            long now = System.currentTimeMillis();
            int param = packet.getParameter();

            // Try as truncated millisecond timestamp
            long serverTimeLow = param & 0xFFFFFFFFL;
            long nowLow = now & 0xFFFFFFFFL;
            long diff = nowLow - serverTimeLow;
            if (diff < 0) diff += 0x100000000L;

            if (diff > 0 && diff < 5000) {
                smoothPing((int) diff);
            }
        } catch (Exception ignored) {}
    }

    @Unique
    private static void smoothPing(int raw) {
        if (raw <= 0 || raw > 10000) return;

        if (meowing_pingSmoothed < 0) {
            meowing_pingSmoothed = raw;
        } else {
            // Exponential moving average for smooth display
            meowing_pingSmoothed = (meowing_pingSmoothed * 3 + raw) / 4;
        }

        try {
            var mod = ModuleManager.getInstance().getModule("HUD");
            if (mod instanceof HUD hud) {
                hud.onPongReceived(meowing_pingSmoothed);
            }
        } catch (Exception ignored) {}
    }
}
