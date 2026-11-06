package dev.bkclient.ui.mainmenu;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.render.obj.Rect;

public class CustomButton extends Rect {
   private final String icon;
   private final float iconSize;
   private final Runnable onClick;
   private final ColorRGBA backgroundColor = new ColorRGBA(58.0F, 58.0F, 58.0F);
   private final Animation activeAnim = new Animation(400L, 0.0F, Easing.BAKEK);
   private final Animation hoverAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   public void draw(UIContext context) {
      if (this.hovered(context.getMouseX(), context.getMouseY()) && this.activeAnim.getValue() == 1.0F) {
         CursorUtility.set(CursorType.HAND);
      }

      this.hoverAnim.update(this.hovered(context.getMouseX(), context.getMouseY()) && this.activeAnim.getValue() == 1.0F);

      float alpha = this.activeAnim.getValue();
      float hover = this.hoverAnim.getValue();
      float radius = Math.min(this.width, this.height) / 2.0F;
      BorderRadius br = BorderRadius.all(radius);
      drawMenuSurface(context, this.x, this.y, this.width, this.height, br, alpha, hover);

      context.drawTexture(
         BKClientInit.id(this.icon),
         this.x + (this.width - this.iconSize) / 2.0F,
         this.y + (this.height - this.iconSize) / 2.0F,
         this.iconSize,
         this.iconSize,
         ColorRGBA.WHITE.withAlpha(255.0F * alpha)
      );
   }

   /** Shared material for the main menu buttons and carousel tiles. */
   static void drawMenuSurface(UIContext context, float x, float y, float width, float height,
                               BorderRadius radius, float alpha, float hover) {
      ColorRGBA background = new ColorRGBA(58.0F,58.0F,58.0F);
      if (Interface.showGlass()) {
         context.drawLiquidGlass(x-1,y-1,width+2,height+2,7.0F,.08F,radius,ColorRGBA.WHITE.withAlpha(255*alpha));
         context.drawRoundedRect(x,y,width,height,radius,background.withAlpha(255*(.15F*alpha+.15F*hover)));
      } else {
         context.drawRoundedRect(x,y,width,height,radius,background.withAlpha(255*(.33F*alpha+.2F*hover)));
      }
   }

   public void click(double mouseX, double mouseY, int button) {
      if (this.hovered(mouseX, mouseY) && button == 0 && this.activeAnim.getValue() == 1.0F) {
         this.onClick.run();
      }
   }

   @Generated
   public CustomButton(String icon, float iconSize, Runnable onClick) {
      this.icon = icon;
      this.iconSize = iconSize;
      this.onClick = onClick;
   }

   @Generated
   public Animation getActiveAnim() {
      return this.activeAnim;
   }
}
