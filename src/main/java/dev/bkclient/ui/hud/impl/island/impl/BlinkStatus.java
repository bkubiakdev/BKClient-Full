package dev.bkclient.ui.hud.impl.island.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.modules.modules.player.Blink;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.impl.island.DynamicDock;
import dev.bkclient.ui.hud.impl.island.IslandStatus;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

public class BlinkStatus extends IslandStatus {
   public BlinkStatus(SelectSetting setting) {
      super(setting, "blink");
   }

   @Override
   public void draw(CustomDrawContext context) {
      DynamicDock island = BKClientInit.getInstance().getHud().getIsland();
      Blink blink = this.blink();
      Font font = Fonts.MEDIUM.getFont(7.0F);
      float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
      float y = 0.0F;
      float width = this.size.width = 80.0F;
      float height = this.size.height = 15.0F;
      context.drawText(font, "FakeLag", x - 4.0F + 10.0F * this.animation.getValue(), y + 5.0F, ColorRGBA.WHITE);
      {
         float blinkWidth = width - font.width("FakeLag") - 14.0F;
         float delayMs = (float)blink.getHoldMillis();
         float progress = blinkWidth * blink.getRemainingFraction();
         context.drawRoundedRect(x + width - 5.0F - blinkWidth, y + 4.5F, blinkWidth, 6.0F, BorderRadius.all(2.5F), Colors.getAdditionalColor());
         context.drawRoundedRect(x + width - 5.0F - progress, y + 4.5F, progress, 6.0F, BorderRadius.all(2.5F), Colors.getAccent());
      }
   }

   @Override
   public boolean canShow() {
      return this.blink().isEnabled();
   }

   private Blink blink() {
      return BKClientInit.getInstance().getModuleManager().getModule(Blink.class);
   }
}
