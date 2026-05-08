package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.StringSetting;

public class PurseSpoofer extends Module {

    private final BooleanSetting spoofPurse = addSetting(new BooleanSetting("Spoof Purse", true));
    private final StringSetting purseAmount = addSetting(new StringSetting("Purse Amount", "1,000,000,000", 20));
    private final BooleanSetting spoofBits = addSetting(new BooleanSetting("Spoof Bits", false));
    private final StringSetting bitsAmount = addSetting(new StringSetting("Bits Amount", "999,999", 20));

    public PurseSpoofer() {
        super("PurseSpoofer", "Spoofs purse/bits on scoreboard", Category.MISC, -1);
    }

    public boolean shouldSpoofPurse() { return spoofPurse.getValue(); }
    public String getPurseAmount() { return purseAmount.getValue(); }
    public boolean shouldSpoofBits() { return spoofBits.getValue(); }
    public String getBitsAmount() { return bitsAmount.getValue(); }
}
