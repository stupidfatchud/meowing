package spz.meowing.setting;

public class StringSetting extends Setting<String> {

    private final int maxLength;

    public StringSetting(String name, String defaultValue, int maxLength) {
        super(name, defaultValue);
        this.maxLength = maxLength;
    }

    @Override
    public void setValue(String value) {
        if (value.length() > maxLength) {
            value = value.substring(0, maxLength);
        }
        super.setValue(value);
    }

    public int getMaxLength() {
        return maxLength;
    }
}
