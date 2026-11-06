package dev.bkclient.ui.clicksys.port.setting;
public final class RangeSliderSetting extends SliderSetting {
    public final SliderSetting upper;
    public RangeSliderSetting(String name, String description, double low, double high, double min, double max, double step) {
        super(name, description, low, min, max, step);
        upper = new SliderSetting(name, description, high, min, max, step);
    }
    @Override public void reset() {
        super.reset();
        upper.reset();
    }
}
