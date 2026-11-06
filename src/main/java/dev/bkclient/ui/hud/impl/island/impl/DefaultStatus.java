package dev.bkclient.ui.hud.impl.island.impl;

import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.impl.island.IslandStatus;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.utility.interfaces.IMinecraft;

public class DefaultStatus extends IslandStatus implements IMinecraft {
   private static final char LOGO_GLYPH = 'D';
   private static final float LOGO_SIZE = 13.0F;
   private static final float LOGO_OFFSET_X = -1.0F;
   private static final float LOGO_OFFSET_Y = -0.2F;

   public DefaultStatus(SelectSetting setting) {
      super(setting, "default");
   }

   @Override
   public void draw(CustomDrawContext context) {
      String label = "BK-Client";
      Font font = Fonts.MEDIUM.getFont(7.0F);
      float padding = 8.0F;
      float gap = 5.0F;
      this.size.width = padding * 2.0F + LOGO_SIZE + gap + 1.0F + gap + font.width(label);
      this.size.height = 15.0F;
      float left = sr.getScaledWidth() / 2.0F - this.size.width / 2.0F;
      float cursor = left + padding;
      float centerY = this.size.height / 2.0F;
      context.drawIcon(
         Fonts.ICONS,
         LOGO_GLYPH,
         cursor + LOGO_OFFSET_X,
         centerY - LOGO_SIZE / 2.0F + LOGO_OFFSET_Y,
         LOGO_SIZE,
         ColorRGBA.WHITE
      );
      cursor += LOGO_SIZE + gap;
      context.drawRoundedRect(cursor, centerY - 2.5F, 1.0F, 5.0F, BorderRadius.all(0.5F), ColorRGBA.WHITE.withAlpha(64.0F));
      cursor += 1.0F + gap;
      context.drawText(
         font,
         label,
         cursor,
         centerY - font.height() / 2.0F,
         ColorRGBA.WHITE.withAlpha(220.0F)
      );
   }

   @Override
   public boolean canShow() {
      return mc.world != null && mc.player != null;
   }

   @Override
   public boolean isHidden() {
      return true;
   }

   @Override
   public ColorRGBA getColor() {
      return ColorRGBA.BLACK;
   }
}
