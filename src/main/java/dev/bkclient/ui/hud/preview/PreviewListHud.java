package dev.bkclient.ui.hud.preview;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.ui.hud.HudList;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.ui.hud.components.HudValueRow;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;

public abstract class PreviewListHud extends HudList {
   protected final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");
   private float cachedWidth = -1.0F;

   protected PreviewListHud(String name, String icon) {
      super(name, icon);
   }

   protected abstract List<HudPreviewData.Row> rows();

   protected float extraRowWidth(HudPreviewData.Row row) { return 0; }

   protected void renderRow(UIContext context, HudPreviewData.Row row, float rowY) {
      HudValueRow.draw(context, this.x, rowY, this.width, row.label(), row.value(), HudTheme.textColor(), 1,
         (iconX, iconY, size) -> context.drawItem(row.item(), iconX, iconY, size / 16));
   }

   @Override
   public void update(UIContext context) {
      List<HudPreviewData.Row> currentRows = rows();
      if (HudTheme.isOld()) {
         Font nameFont = Fonts.MEDIUM.getFont(5.5F);
         Font valueFont = Fonts.SEMIBOLD.getFont(5.5F);
         this.width = 70.0F;
         for (HudPreviewData.Row row : currentRows) {
            this.width = Math.max(this.width, 8.0F + 7.0F + 2.0F + nameFont.width(row.label()) + 4.0F + valueFont.width(row.value()) + 5.0F + 8.0F + extraRowWidth(row));
         }
         this.height = currentRows.isEmpty() ? listTop(16.0F) : listTop(18.0F) + currentRows.size() * listStride(9.0F) + 6.0F;
         super.update(context);
         return;
      }

      Font font = Fonts.REGULAR.getFont(7.0F);
      this.cachedWidth = 104.0F;
      for (HudPreviewData.Row row : currentRows) {
         this.cachedWidth = Math.max(this.cachedWidth, 37.0F + font.width(row.label()) + font.width(row.value()) + extraRowWidth(row));
      }
      this.width = this.cachedWidth;
      this.height = listTop(22.0F) + currentRows.size() * listStride(18.0F);
      super.update(context);
   }

   @Override
   protected void renderContent(UIContext context) {
      if (HudTheme.isOld()) {
         this.renderOldContent(context);
         return;
      }

      super.renderContent(context);
      Font font = Fonts.REGULAR.getFont(7.0F);
      Font valueFont = Fonts.MEDIUM.getFont(6.5F);
      float rowY = this.y + listTop(22.0F);
      for (HudPreviewData.Row row : rows()) {
         renderRow(context, row, rowY);
         rowY += listStride(18.0F);
      }
   }

   private void renderOldContent(UIContext context) {
      super.renderContent(context);
      float rowY = this.y + listTop(18.0F);
      for (HudPreviewData.Row row : rows()) {
         renderRow(context, row, rowY);
         rowY += listStride(9.0F);
      }
   }

   @Override
   public boolean show() {
      return mc.currentScreen instanceof ChatScreen || this.alwaysDisplay.isEnabled();
   }
}
