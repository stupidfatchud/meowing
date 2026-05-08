package spz.meowing.module.impl.dungeons;

import spz.meowing.gui.NotificationManager;
import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.util.SkyblockDetector;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;
import spz.meowing.setting.StringSetting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MaskTimers extends Module {

    private final BooleanSetting showBonzo = addSetting(new BooleanSetting("Bonzo's Mask", true));
    private final BooleanSetting showSpirit = addSetting(new BooleanSetting("Spirit Mask", true));
    private final BooleanSetting showPhoenix = addSetting(new BooleanSetting("Phoenix Pet", true));
    private final NumberSetting cataLevel = addSetting(new NumberSetting("Cata Level", 0, 0, 50, 1));
    private final BooleanSetting procAlert = addSetting(new BooleanSetting("Proc Alert", true));
    private final ModeSetting alertStyle = addSetting(new ModeSetting("Alert Style", "Both",
            "Title", "Notification", "Both"));
    private final NumberSetting titleScale = addSetting(new NumberSetting("Title Scale", 2.0, 1.0, 5.0, 0.5));
    private final ModeSetting alertColor = addSetting(new ModeSetting("Alert Color", "White",
            "White", "Red", "Green", "Blue", "Cyan", "Purple", "Orange", "Yellow", "Pink"));

    // Editable alert text per mask
    private final StringSetting bonzoTitle = addSetting(new StringSetting("Bonzo Title", "Bonzo's Mask!", 32));
    private final StringSetting spiritTitle = addSetting(new StringSetting("Spirit Title", "Spirit Mask!", 32));
    private final StringSetting phoenixTitle = addSetting(new StringSetting("Phoenix Title", "Phoenix Pet!", 32));

    private final StringSetting resetMessage = addSetting(new StringSetting("Reset Message", "mask timers reset :3", 32));
    private final BooleanSetting hideWhenReady = addSetting(new BooleanSetting("Hide When Ready", false));

    // Title alert queue — plays alerts one after another
    private static final long ALERT_FADE_IN = 150;
    private static final long ALERT_STAY = 1200;
    private static final long ALERT_FADE_OUT = 400;
    private static final long ALERT_TOTAL = ALERT_FADE_IN + ALERT_STAY + ALERT_FADE_OUT;

    private final java.util.Queue<QueuedAlert> alertQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private QueuedAlert currentAlert = null;
    private long currentAlertStart = 0;

    private record QueuedAlert(String title, String subtitle, int color) {}

    private final Map<String, MaskEntry> masks = new LinkedHashMap<>();

    public MaskTimers() {
        super("MaskTimers", "Tracks mask/pet cooldowns", Category.DUNGEONS, -1);
        initMasks();
    }

    private void initMasks() {
        masks.put("bonzo", new MaskEntry(
                "Bonzo",
                Pattern.compile("Your( ⚚)? Bonzo's Mask saved your life!"),
                3000, 0, 0xFF5555FF
        ));
        masks.put("spirit", new MaskEntry(
                "Spirit",
                Pattern.compile("Second Wind Activated! Your Spirit Mask saved your life!"),
                3000, 30000, 0xFFFFFFFF
        ));
        masks.put("phoenix", new MaskEntry(
                "Phoenix",
                Pattern.compile("Your Phoenix Pet saved you from certain death!"),
                4000, 60000, 0xFFFF5555
        ));
    }

    private long getBonzoCooldown() {
        int level = (int) cataLevel.getValue().doubleValue();
        long base = 360000;
        long reduction = (long) (3600.0 * level);
        return Math.max(36000, base - reduction);
    }

    private boolean wasInDungeon = false;

    @Override
    public void onEnable() {}

    @Override
    public void onTick() {
        boolean inDungeon = SkyblockDetector.isInDungeon();
        if (wasInDungeon && !inDungeon) {
            // Left dungeon — reset bonzo mask cooldown
            MaskEntry bonzo = masks.get("bonzo");
            if (bonzo != null) bonzo.reset();
        }
        wasInDungeon = inDungeon;
    }

    public void resetAll() {
        for (MaskEntry entry : masks.values()) entry.reset();
        alertQueue.clear();
        currentAlert = null;
    }

    public String getResetMessage() {
        String msg = resetMessage.getValue();
        return msg.isEmpty() ? "mask timers reset :3" : msg;
    }

    public void onChatMessage(String message) {
        if (!isEnabled()) return;
        String clean = message.replaceAll("§.", "").trim();

        for (var entry : masks.entrySet()) {
            MaskEntry mask = entry.getValue();
            if (!isTrackingMask(entry.getKey())) continue;

            if (mask.pattern.matcher(clean).find()) {
                long cd = entry.getKey().equals("bonzo") ? getBonzoCooldown() : mask.baseCooldownMs;
                mask.proc(cd);

                if (procAlert.getValue()) {
                    String title = getCustomTitle(entry.getKey());
                    sendAlert(title, "");
                }
                break;
            }
        }
    }

    private String getCustomTitle(String key) {
        return switch (key) {
            case "bonzo" -> bonzoTitle.getValue().isEmpty() ? "Bonzo's Mask!" : bonzoTitle.getValue();
            case "spirit" -> spiritTitle.getValue().isEmpty() ? "Spirit Mask!" : spiritTitle.getValue();
            case "phoenix" -> phoenixTitle.getValue().isEmpty() ? "Phoenix Pet!" : phoenixTitle.getValue();
            default -> "Mask Proc!";
        };
    }

    private int getAlertColorFromSetting() {
        return switch (alertColor.getValue()) {
            case "Red" -> 0xFFE74C3C;
            case "Green" -> 0xFF2ECC71;
            case "Blue" -> 0xFF3498DB;
            case "Cyan" -> 0xFF00D2FF;
            case "Purple" -> 0xFF6C5CE7;
            case "Orange" -> 0xFFE67E22;
            case "Yellow" -> 0xFFFFD700;
            case "Pink" -> 0xFFFF69B4;
            default -> 0xFFFFFFFF;
        };
    }

    private boolean isTrackingMask(String key) {
        return switch (key) {
            case "bonzo" -> showBonzo.getValue();
            case "spirit" -> showSpirit.getValue();
            case "phoenix" -> showPhoenix.getValue();
            default -> false;
        };
    }

    private void sendAlert(String title, String subtitle) {
        String mode = alertStyle.getValue();
        String cleanSub = subtitle.replaceAll("\u00a7.", "");

        if (mode.equals("Notification") || mode.equals("Both")) {
            NotificationManager.warn(title, cleanSub);
        }

        if (mode.equals("Title") || mode.equals("Both")) {
            // Don't queue if an identical title is already queued or currently showing
            boolean duplicate = false;
            if (currentAlert != null && currentAlert.title.equals(title)) duplicate = true;
            for (QueuedAlert q : alertQueue) {
                if (q.title.equals(title)) { duplicate = true; break; }
            }
            if (!duplicate) {
                alertQueue.add(new QueuedAlert(title, cleanSub, getAlertColorFromSetting()));
            }
        }
    }

    private long lastTickFrame = 0;

    // Advance the queue once per frame
    private void tickAlertQueue() {
        long now = System.currentTimeMillis();

        // Only tick once per millisecond to avoid double-processing in the same frame
        if (now == lastTickFrame) return;
        lastTickFrame = now;

        if (currentAlert != null) {
            if (now - currentAlertStart >= ALERT_TOTAL) {
                currentAlert = null;
            } else {
                return;
            }
        }

        QueuedAlert next = alertQueue.poll();
        if (next != null) {
            currentAlert = next;
            currentAlertStart = System.currentTimeMillis();
        }
    }

    public boolean hasActiveAlert() {
        tickAlertQueue();
        return currentAlert != null;
    }

    public float getAlertAlpha() {
        if (currentAlert == null) return 0f;
        long elapsed = System.currentTimeMillis() - currentAlertStart;
        if (elapsed < ALERT_FADE_IN) return (float) elapsed / ALERT_FADE_IN;
        if (elapsed < ALERT_FADE_IN + ALERT_STAY) return 1f;
        long fadeElapsed = elapsed - ALERT_FADE_IN - ALERT_STAY;
        if (fadeElapsed < ALERT_FADE_OUT) return 1f - (float) fadeElapsed / ALERT_FADE_OUT;
        return 0f;
    }

    public String getAlertTitle() { return currentAlert != null ? currentAlert.title : ""; }
    public String getAlertSubtitle() { return currentAlert != null ? currentAlert.subtitle : ""; }
    public int getAlertColor() { return currentAlert != null ? currentAlert.color : 0xFFFFFFFF; }
    public float getTitleScale() { return (float) titleScale.getValue().doubleValue(); }

    // Visible entries for HUD
    public java.util.List<MaskEntry> getVisibleEntries() {
        java.util.List<MaskEntry> visible = new java.util.ArrayList<>();
        for (var entry : masks.entrySet()) {
            if (!isTrackingMask(entry.getKey())) continue;
            MaskEntry mask = entry.getValue();
            if (hideWhenReady.getValue() && mask.isReady()) continue;
            visible.add(mask);
        }
        return visible;
    }

    public static class MaskEntry {
        public final String displayName;
        public final Pattern pattern;
        public final long immunityMs;
        public final long baseCooldownMs;
        public final int color;
        private long procTime = -1;
        private long activeCooldownMs = 0;

        public MaskEntry(String displayName, Pattern pattern, long immunityMs, long baseCooldownMs, int color) {
            this.displayName = displayName;
            this.pattern = pattern;
            this.immunityMs = immunityMs;
            this.baseCooldownMs = baseCooldownMs;
            this.color = color;
        }

        public void proc(long cooldownMs) { procTime = System.currentTimeMillis(); activeCooldownMs = cooldownMs; }
        public void proc() { proc(baseCooldownMs); }
        public void reset() { procTime = -1; activeCooldownMs = 0; }
        public boolean isReady() { return procTime < 0 || System.currentTimeMillis() - procTime >= activeCooldownMs; }
        public boolean isImmune() { return procTime >= 0 && System.currentTimeMillis() - procTime < immunityMs; }
        public float getImmunityRemaining() { return procTime < 0 ? 0f : Math.max(0f, (immunityMs - (System.currentTimeMillis() - procTime)) / 1000f); }
        public float getCooldownRemaining() { return procTime < 0 ? 0f : Math.max(0f, (activeCooldownMs - (System.currentTimeMillis() - procTime)) / 1000f); }

        public String getLabel() { return displayName + ": "; }

        public String getStatusText() {
            if (isReady()) return "\u00a7aREADY";
            // Always show cooldown countdown, no immunity phase display
            float cd = getCooldownRemaining();
            float max = activeCooldownMs / 1000f;
            String cc;
            if (cd >= max * 0.75f) cc = "\u00a7c";
            else if (cd >= max * 0.50f) cc = "\u00a76";
            else if (cd >= max * 0.25f) cc = "\u00a7e";
            else cc = "\u00a7a";
            return String.format("%s%.1fs", cc, cd);
        }

        public float getCooldownProgress() {
            if (isReady() || procTime < 0) return 1f;
            return Math.min(1f, (float)(System.currentTimeMillis() - procTime) / activeCooldownMs);
        }
    }
}
