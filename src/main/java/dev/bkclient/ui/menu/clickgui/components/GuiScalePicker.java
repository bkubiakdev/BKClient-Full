package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.ScissorUtility;

/** BK-Client wheel-driven behavior rendered with the client's Liquid surface. */
public final class GuiScalePicker implements IMinecraft {
   private static final float[] VALUES = {
      0.0F, 60.0F, 70.0F, 80.0F, 90.0F, 100.0F, 110.0F, 120.0F, 130.0F
   };

   private final SliderSetting setting;
   private final Animation showAnimation = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation indexAnimation = new Animation(170L, Easing.FIGMA_EASE_IN_OUT);
   private int selectedIndex;
   private double wheelRemainder;
   private boolean closing;

   public GuiScalePicker(SliderSetting setting) {
      this.setting = setting;
      this.selectedIndex = nearestIndex(setting.getCurrentValue());
      this.indexAnimation.setValue(this.selectedIndex);
   }

   public void render(UIContext context, float screenAlpha) {
      float open = this.showAnimation.update(this.closing ? 0.0F : 1.0F);
      float alpha = Math.clamp(open * screenAlpha, 0.0F, 1.0F);
      if (alpha <= 0.01F) return;

      Layout layout = layout();
      float visualIndex = this.indexAnimation.update(this.selectedIndex);
      float scale = 0.98F + 0.02F * open;
      float centerX = layout.x + layout.width / 2.0F;
      float centerY = layout.y + layout.height / 2.0F;

      context.drawRect(0.0F, 0.0F, layout.screenWidth, layout.screenHeight,
         Colors.getBackgroundColor().withAlpha(20.0F * alpha));
      context.getMatrices().push();
      context.getMatrices().translate(centerX, centerY, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);
      context.getMatrices().translate(-centerX, -centerY + (1.0F - open) * 5.0F, 0.0F);

      context.drawClientRect(layout.x, layout.y, layout.width, layout.height, alpha, 0.0F, 9.0F);
      context.drawRoundedRect(
         layout.x, layout.y, layout.width, layout.headerHeight,
         new BorderRadius(layout.radius, layout.radius, 0.0F, 0.0F),
         Colors.getTextColor().withAlpha(7.0F * alpha)
      );
      context.drawRect(
         layout.x, layout.listY - 0.5F, layout.width, 0.5F,
         Colors.getTextColor().withAlpha(14.0F * alpha)
      );
      float footerY = layout.y + layout.height - layout.footerHeight;
      context.drawRect(
         layout.x, footerY, layout.width, 0.5F,
         Colors.getTextColor().withAlpha(10.0F * alpha)
      );

      Font titleFont = Fonts.SEMIBOLD.getFont(layout.titleFont);
      context.drawCenteredText(
         titleFont, Localizator.translate("gui.scale_picker.title"), centerX,
         layout.y + (layout.headerHeight - titleFont.height()) / 2.0F,
         Colors.getTextColor().withAlpha(220.0F * alpha)
      );

      float selectedY = layout.listY + layout.listHeight / 2.0F - layout.rowHeight / 2.0F;
      BorderRadius selectionRadius = BorderRadius.all(Math.min(7.0F, layout.rowHeight / 2.0F));
      context.drawRoundedRect(
         layout.x + 5.0F, selectedY, layout.width - 10.0F, layout.rowHeight,
         selectionRadius, Colors.getAccent().withAlpha(32.0F * alpha)
      );

      ScissorUtility.push(context.getMatrices(), layout.x + 3.0F, layout.listY, layout.width - 6.0F, layout.listHeight);
      for (int index = 0; index < VALUES.length; index++) {
         float offset = index - visualIndex;
         if (Math.abs(offset) > layout.visibleRows / 2.0F + 1.2F) continue;
         float rowCenter = layout.listY + layout.listHeight / 2.0F + offset * layout.rowHeight;
         float distance = Math.abs(offset);
         float focus = 1.0F - Math.clamp(distance, 0.0F, 1.0F);
         float edgeFade = 1.0F - Math.clamp(
            (distance - 0.75F) / Math.max(1.0F, layout.visibleRows / 2.0F), 0.0F, 0.82F
         );
         Font valueFont = Fonts.SEMIBOLD.getFont(layout.valueFont);
         float textAlpha = (focus > 0.45F ? 255.0F : 96.0F) * edgeFade * alpha;
         context.drawCenteredText(
            valueFont, label(index), centerX, rowCenter - valueFont.height() / 2.0F,
            (focus > 0.45F ? Colors.getAccent() : Colors.getTextColor()).withAlpha(textAlpha)
         );
      }
      ScissorUtility.pop();

      Font hintFont = Fonts.MEDIUM.getFont(layout.hintFont);
      context.drawCenteredText(
         hintFont, Localizator.translate("gui.scale_picker.hint"), centerX,
         layout.y + layout.height - layout.footerHeight + (layout.footerHeight - hintFont.height()) / 2.0F,
         Colors.getTextColor().withAlpha(82.0F * alpha)
      );
      context.getMatrices().pop();
   }

   public boolean onClick(double mouseX, double mouseY, MouseButton button) {
      if (button == MouseButton.RIGHT) {
         this.close();
         return true;
      }
      if (button != MouseButton.LEFT) return true;

      Layout layout = layout();
      if (!layout.contains(mouseX, mouseY)) {
         this.close();
         return true;
      }
      if (mouseY >= layout.listY && mouseY <= layout.listY + layout.listHeight) {
         float centerY = layout.listY + layout.listHeight / 2.0F;
         int clicked = Math.round(this.indexAnimation.getValue() + ((float)mouseY - centerY) / layout.rowHeight);
         this.select(Math.clamp(clicked, 0, VALUES.length - 1));
      }
      return true;
   }

   public boolean onScroll(double verticalAmount) {
      if (verticalAmount == 0.0) return true;
      this.wheelRemainder -= verticalAmount;
      int steps = (int)this.wheelRemainder;
      if (steps != 0) {
         this.wheelRemainder -= steps;
         this.select(Math.clamp(this.selectedIndex + steps, 0, VALUES.length - 1));
      }
      return true;
   }

   public boolean onKeyPressed(int keyCode) {
      if (keyCode == 256) {
         this.close();
         return true;
      }
      if (keyCode == 264 || keyCode == 267) {
         this.select(Math.min(VALUES.length - 1, this.selectedIndex + 1));
         return true;
      }
      if (keyCode == 265 || keyCode == 266) {
         this.select(Math.max(0, this.selectedIndex - 1));
         return true;
      }
      if (keyCode == 257 || keyCode == 335) {
         this.close();
         return true;
      }
      return true;
   }

   public void close() {
      this.closing = true;
   }

   public boolean isDead() {
      return this.closing && this.showAnimation.getValue() <= 0.01F;
   }

   private void select(int index) {
      if (index == this.selectedIndex) return;
      this.selectedIndex = index;
      this.setting.setCurrentValue(VALUES[index]);
      BKClientInit.getInstance().getFileManager().writeFile("client");
   }

   private static String label(int index) {
      return VALUES[index] <= 0.0F
         ? Localizator.translate("gui.scale_picker.auto")
         : Math.round(VALUES[index]) + "%";
   }

   private static int nearestIndex(float value) {
      int best = 0;
      float distance = Float.MAX_VALUE;
      for (int index = 0; index < VALUES.length; index++) {
         float current = Math.abs(VALUES[index] - value);
         if (current < distance) {
            distance = current;
            best = index;
         }
      }
      return best;
   }

   private static Layout layout() {
      float screenWidth = Math.max(1.0F, mc.getWindow().getScaledWidth());
      float screenHeight = Math.max(1.0F, mc.getWindow().getScaledHeight());
      int visibleRows = screenHeight < 260.0F ? 3 : 5;
      float rowHeight = Math.clamp(screenHeight * 0.038F, 14.0F, 18.0F);
      float width = Math.clamp(screenWidth * 0.145F, 86.0F, 116.0F);
      width = Math.min(width, Math.max(64.0F, screenWidth - 20.0F));
      float headerHeight = Math.clamp(rowHeight * 1.05F, 15.0F, 19.0F);
      float footerHeight = Math.clamp(rowHeight * 0.88F, 13.0F, 16.0F);
      float listHeight = visibleRows * rowHeight;
      float height = Math.min(headerHeight + listHeight + footerHeight, Math.max(70.0F, screenHeight - 20.0F));
      float x = (screenWidth - width) / 2.0F;
      float y = (screenHeight - height) / 2.0F;
      return new Layout(
         screenWidth, screenHeight, x, y, width, height, headerHeight, footerHeight,
         y + headerHeight, listHeight, rowHeight, visibleRows,
         Math.clamp(width * 0.075F, 6.0F, 8.5F),
         Math.clamp(width * 0.061F, 5.5F, 7.0F),
         Math.clamp(width * 0.049F, 4.5F, 5.5F),
         Math.clamp(width * 0.085F, 6.0F, 9.0F)
      );
   }

   private record Layout(
      float screenWidth, float screenHeight, float x, float y, float width, float height,
      float headerHeight, float footerHeight, float listY, float listHeight, float rowHeight,
      int visibleRows, float radius, float titleFont, float hintFont, float valueFont
   ) {
      boolean contains(double mouseX, double mouseY) {
         return mouseX >= this.x && mouseX <= this.x + this.width
            && mouseY >= this.y && mouseY <= this.y + this.height;
      }
   }
}
