package dev.bkclient.ui.clicksys.port.data;

import dev.bkclient.ui.clicksys.port.gui.widget.ActionRow;
import dev.bkclient.ui.clicksys.port.gui.widget.BooleanRow;
import dev.bkclient.ui.clicksys.port.gui.widget.ChoiceRow;
import dev.bkclient.ui.clicksys.port.gui.widget.ColorPickerRow;
import dev.bkclient.ui.clicksys.port.gui.widget.DropdownRow;
import dev.bkclient.ui.clicksys.port.gui.widget.FilterDropdown;
import dev.bkclient.ui.clicksys.port.gui.widget.FilterMenuRow;
import dev.bkclient.ui.clicksys.port.gui.widget.PanelWidget;
import dev.bkclient.ui.clicksys.port.gui.widget.SettingRow;
import dev.bkclient.ui.clicksys.port.gui.widget.SliderRow;
import dev.bkclient.ui.clicksys.port.gui.widget.TextInputRow;
import dev.bkclient.ui.clicksys.port.setting.ActionSetting;
import dev.bkclient.ui.clicksys.port.setting.BooleanSetting;
import dev.bkclient.ui.clicksys.port.setting.ChoiceSetting;
import dev.bkclient.ui.clicksys.port.setting.ColorSetting;
import dev.bkclient.ui.clicksys.port.setting.CompactGroupSetting;
import dev.bkclient.ui.clicksys.port.setting.FilterMenuSetting;
import dev.bkclient.ui.clicksys.port.setting.HotkeySetting;
import dev.bkclient.ui.clicksys.port.setting.InputSetting;
import dev.bkclient.ui.clicksys.port.setting.KeybindSetting;
import dev.bkclient.ui.clicksys.port.setting.ListSetting;
import dev.bkclient.ui.clicksys.port.setting.MultilistSetting;
import dev.bkclient.ui.clicksys.port.setting.Setting;
import dev.bkclient.ui.clicksys.port.setting.SliderSetting;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public class SettingWidgetFactory {
   private static final Map<Class<? extends Setting>, Function<Setting, PanelWidget>> map = new LinkedHashMap<>();

   private SettingWidgetFactory() {
   }

   static {
      onClassFunction(ChoiceSetting.class, var0 -> new ChoiceRow((ChoiceSetting)var0));
      onClassFunction(FilterMenuSetting.class, var0 -> new FilterMenuRow((FilterMenuSetting)var0));
      onClassFunction(BooleanSetting.class, var0 -> new BooleanRow((BooleanSetting)var0));
      onClassFunction(dev.bkclient.ui.clicksys.port.setting.RangeSliderSetting.class, value -> new dev.bkclient.ui.clicksys.port.gui.widget.RangeSliderRow((dev.bkclient.ui.clicksys.port.setting.RangeSliderSetting)value));
      onClassFunction(SliderSetting.class, var0 -> new SliderRow((SliderSetting)var0));
      onClassFunction(MultilistSetting.class, var0 -> new FilterDropdown((MultilistSetting)var0));
      onClassFunction(ListSetting.class, var0 -> new FilterDropdown((ListSetting)var0));
      onClassFunction(ColorSetting.class, var0 -> new ColorPickerRow((ColorSetting)var0));
      onClassFunction(InputSetting.class, var0 -> new TextInputRow((InputSetting)var0));
      onClassFunction(KeybindSetting.class, var0 -> new DropdownRow((Setting)var0));
      onClassFunction(HotkeySetting.class, var0 -> new DropdownRow((Setting)var0));
      onClassFunction(ActionSetting.class, var0 -> new ActionRow((ActionSetting)var0));
      onClassFunction(CompactGroupSetting.class, var0 -> new SettingRow((CompactGroupSetting)var0));
   }

   public static boolean supports(Setting setting) {
      return setting != null && map.keySet().stream().anyMatch(type -> type.isInstance(setting));
   }

   public static PanelWidget getPanelWidgetBySetting10(Setting setting2) {
      if (setting2 instanceof ListSetting slots && slots.getName().equals("Slots")
          && slots.getList2().equals(java.util.List.of("1","2","3","4","5","6","7","8","9")))
         return new dev.bkclient.ui.clicksys.port.gui.widget.HotbarSlotsRow(slots);
      if (setting2 == null) {
         return null;
      } else {
         for (Entry entry : map.entrySet()) {
            if (((Class)entry.getKey()).isInstance(setting2)) {
               return (PanelWidget)((Function)entry.getValue()).apply(setting2);
            }
         }

         return null;
      }
   }

   public static void onClassFunction(Class value, Function function2) {
      map.put(value, function2);
   }
}
