package dev.bkclient.systems.modules.modules.visuals;

import lombok.Generated;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.ui.menu.api.MenuCloseListener;
import dev.bkclient.ui.menu.csgui.CsGuiScreen;
import dev.bkclient.ui.menu.spotlight.SpotlightScreen;

@ModuleInfo(name = "Menu", category = ModuleCategory.VISUALS, key = 344, desc = "modules.descriptions.menu")
public class MenuModule extends BaseModule {
   public final dev.bkclient.systems.setting.settings.BooleanSetting modifierSymbols =
      new dev.bkclient.systems.setting.settings.BooleanSetting(this,"Menu modifier symbols",()->true).enabled(true);
   public final dev.bkclient.systems.setting.settings.SliderSetting keyModifiers =
      new dev.bkclient.systems.setting.settings.SliderSetting(this,"Menu key modifiers",()->true).min(0).max(7).currentValue(0).step(1);
   public void setMenuBind(int key,int modifiers) {
      setKey(key);
      keyModifiers.setCurrentValue(dev.bkclient.ui.clicksys.MenuKeybind.normalizeModifiers(key,modifiers));
   }
   public boolean acceptsModifiers(int held) {
      return dev.bkclient.ui.clicksys.MenuKeybind.accepts((int)keyModifiers.getCurrentValue(),held);
   }
   public boolean acceptsCurrentModifiers() {
      int held=0;
      if(net.minecraft.client.gui.screen.Screen.hasShiftDown())held|=1;
      if(net.minecraft.client.gui.screen.Screen.hasControlDown())held|=2;
      if(net.minecraft.client.gui.screen.Screen.hasAltDown())held|=4;
      return acceptsModifiers(held);
   }
   public String bindLabel() {
      String label=dev.bkclient.ui.clicksys.port.setting.KeybindSetting.getStringByIntInt(getKey(),(int)keyModifiers.getCurrentValue());
      return dev.bkclient.ui.clicksys.MenuKeybind.format(label,modifierSymbols.isEnabled());
   }
   private static final MenuCloseListener menuCloseListener = new MenuCloseListener();
    private final ModeSetting mode = new ModeSetting(this, "modules.settings.menu.mode") {
       @Override public void load(com.google.gson.JsonElement element) {
          String saved = element.getAsString().replace(" ", "");
          if (saved.equalsIgnoreCase("ClickGUIv2")
                || saved.equalsIgnoreCase("ClickGUIv3")
                || saved.equalsIgnoreCase("ClickGUI")
                || saved.equalsIgnoreCase("clicksys")
                || saved.equalsIgnoreCase("clickSYS2")) {
             super.load(new com.google.gson.JsonPrimitive("modules.settings.menu.mode.clickgui"));
          } else if (saved.equalsIgnoreCase("Old")) {
             super.load(new com.google.gson.JsonPrimitive("modules.settings.menu.mode.old"));
          } else if (saved.equalsIgnoreCase("Spotlight")) {
             super.load(new com.google.gson.JsonPrimitive("modules.settings.menu.mode.spotlight"));
          } else {
             super.load(element);
          }
       }
    };
    private final ModeSetting.Value clickGui = new ModeSetting.Value(this.mode, "modules.settings.menu.mode.clickgui").select();
    private final ModeSetting.Value old = new ModeSetting.Value(this.mode, "modules.settings.menu.mode.old");
    private final ModeSetting.Value spotlight = new ModeSetting.Value(this.mode, "modules.settings.menu.mode.spotlight");

   private final ModeSetting v2Appearance = new ModeSetting(this, "ClickGUIv2 theme", () -> !this.isClickGuiV2());
   private final ModeSetting.Value v2Light = new ModeSetting.Value(this.v2Appearance, "Light");
   private final ModeSetting.Value v2Dark = new ModeSetting.Value(this.v2Appearance, "Dark").select();
   private final dev.bkclient.systems.setting.settings.ColorSetting v2Accent =
      new dev.bkclient.systems.setting.settings.ColorSetting(this, "V2 accent", () -> !this.isClickGuiV2())
         .alpha(false).color(new dev.bkclient.utility.colors.ColorRGBA(36, 125, 218));

   public boolean isV2Dark() { return this.v2Dark.isSelected(); }
   public void setV2Dark(boolean dark) {
      (dark ? this.v2Dark : this.v2Light).select();
      dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();
   }
   public dev.bkclient.systems.setting.settings.ColorSetting getV2Accent() { return this.v2Accent; }

   @Override
   public void onEnable() {
       if (!(mc.currentScreen instanceof MenuScreen)) {
          MenuScreen menuScreen = this.createMenuScreen();
          BKClientInit.getInstance().setMenuScreen(menuScreen);
          mc.setScreen(menuScreen);

          super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (mc.currentScreen instanceof MenuScreen) {
         mc.setScreen(null);
         BKClientInit.getInstance().getMenuScreen().setClosing(true);
      }

      super.onDisable();
   }

   @Generated
   public ModeSetting.Value getSpotlight() {
      return this.spotlight;
   }

   public ModeSetting.Value getClickGui() {
      return this.clickGui;
   }


   public boolean isClickGui() {
      return this.clickGui.isSelected();
   }


   public boolean isClickGuiV3() { return false; }

   public boolean isClickGuiV2() { return false; }

   public boolean isClickSys() { return this.isClickGui(); }

   public boolean isOld() {
      return this.old.isSelected();
   }

   public ModeSetting.Value getOld() {
      return this.old;
   }

   public MenuScreen createMenuScreen() {
      if (this.isClickGui()) return new dev.bkclient.ui.clicksys.ClickSysScreen();
      if (this.isOld()) return new CsGuiScreen();
      return new SpotlightScreen();
   }
}
