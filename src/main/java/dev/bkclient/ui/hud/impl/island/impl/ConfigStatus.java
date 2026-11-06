package dev.bkclient.ui.hud.impl.island.impl;

import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.impl.island.IslandTokenStatus;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.time.Timer;
import java.util.List;

/** Short BK-style status shown after a local config is loaded. */
public class ConfigStatus extends IslandTokenStatus {
   private static final long DURATION_MS = 3000L;
   private static final char ICON_CONFIG = 'U';
   private static final Timer TIMER = new Timer();
   private static String configName = "";
   private static boolean triggered;

   public ConfigStatus(SelectSetting setting) {
      super(setting, "config");
   }

   public static void show(String name) {
      configName = name == null || name.isBlank() ? "Config" : name;
      triggered = true;
      TIMER.reset();
   }

   @Override
   protected List<Token> buildTokens() {
      String name = configName;
      float remaining = 1.0F - Math.min(1.0F, TIMER.getElapsedTime() / (float)DURATION_MS);

      return this.tokens(
         Token.icon(ICON_CONFIG, Colors.getAccent()),
         Token.divider(),
         Token.text("Config ", ColorRGBA.WHITE, false),
         Token.text(name, Colors.getAccent(), true),
         Token.text(" Loaded", ColorRGBA.WHITE, true),
         Token.divider(),
         Token.ring(remaining)
      );
   }

   @Override
   public boolean canShow() {
      return triggered && !TIMER.finished(DURATION_MS);
   }
}
