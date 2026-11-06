package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.module.Category;
import dev.bkclient.ui.clicksys.port.module.Module;
import dev.bkclient.ui.clicksys.port.module.client.ThemeModule;
import org.joml.Matrix4f;

/** Edits the same setting objects as Appearance. HUD settings stay in the BK Interface module. */
public final class ThemesPage extends SettingsInspector {
   private final dev.bkclient.ui.clicksys.port.setting.BooleanSetting enabled;
   public ThemesPage() {
      super(() -> {});
      utilityPage = true;
      var theme = ThemeModule.getThemeModule();
      Module settings = new Module("Themes", Category.CLIENT);
      enabled = new dev.bkclient.ui.clicksys.port.setting.BooleanSetting("Enable theme", "Apply the selected GUI theme. Choosing a color enables it automatically.", theme.isEnabled());
      enabled.setOnChange(() -> theme.setEnabled(enabled.isFlag3()));
      settings.addSetting(enabled);
      for (var setting : theme.getSettingsCopy()) if (setting != theme.getKeybindSetting()) settings.addSetting(setting);
      show(settings);
   }
   @Override public void onFloatFloatFloatMatrix4f(float alpha, float my, float mx, Matrix4f matrix) {
      enabled.setBoolean(ThemeModule.getThemeModule().isEnabled());
      float x = value235, y = value236, width = value237, height = value238;
      value237 = Math.min(520, width-48);
      position(x+24,y+24,height-48);
      super.onFloatFloatFloatMatrix4f(alpha,my,mx,matrix);
   }
}
