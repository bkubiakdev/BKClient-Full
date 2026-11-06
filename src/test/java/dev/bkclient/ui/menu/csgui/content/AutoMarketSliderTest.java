package dev.bkclient.ui.menu.csgui.content;

import dev.bkclient.ui.menu.csgui.widget.CsSlider;
import dev.bkclient.systems.setting.settings.SliderSetting;
import java.util.ArrayList;

public final class AutoMarketSliderTest {
    private static final class CountingSetting extends SliderSetting {
        int writes, saves;
        CountingSetting() { super(() -> new ArrayList<>(), "test"); min(0); max(1000); step(1); writes=0; }
        @Override public void setCurrentValue(float value) { super.setCurrentValue(value); writes++; }
        @Override public void setCurrentValueAndSave(float value) { setCurrentValue(value); saves++; }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var slider = new CsSlider("Test",0,1000,0,Object::toString);
        var track = CsSlider.class.getDeclaredField("trackW"); track.setAccessible(true); track.setFloat(slider,100);
        var setting = new CountingSetting();
        check(slider.onClick(0,0),"drag begins");
        for(int i=1;i<=1000;i++) {
            check(AutoMarketContent.dragSlider(slider,setting,i/10.0),"drag handled");
            slider.setValue(setting.getCurrentValue()); // render synchronization must not erase the preview
        }
        check(setting.writes==0 && setting.saves==0,"drag must not trigger setting callbacks/profile saves");
        check(slider.getValue()==1000 && setting.getCurrentValue()==0,"preview remains independent until release");
        AutoMarketContent.releaseSlider(slider,setting);
        check(setting.writes==1 && setting.saves==1 && setting.getCurrentValue()==1000,"release commits final value once");
        AutoMarketContent.releaseSlider(slider,setting);
        check(setting.saves==1,"duplicate release cannot save again");
        check(!AutoMarketContent.dragSlider(slider,setting,50),"released slider cannot mutate");
        System.out.println("AutoMarket: 1000 drag events without writes; final value committed once on release.");
    }
}
