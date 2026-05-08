package spz.meowing.setting;

public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double increment;

    public NumberSetting(String name, double defaultValue, double min, double max, double increment) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    @Override
    public void setValue(Double value) {
        value = Math.max(min, Math.min(max, value));
        value = Math.round(value / increment) * increment;
        super.setValue(value);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getIncrement() {
        return increment;
    }

    public double getPercentage() {
        return (getValue() - min) / (max - min);
    }

    public void setFromPercentage(double pct) {
        setValue(min + pct * (max - min));
    }

    public String getDisplayValue() {
        if (increment >= 1.0) {
            return String.valueOf((int) getValue().doubleValue());
        }
        return String.format("%.1f", getValue());
    }
}
