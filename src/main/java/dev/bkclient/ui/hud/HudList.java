package dev.bkclient.ui.hud;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.ScissorUtility;

public abstract class HudList extends HudElement {
   protected final HudHeaderOptions headerOptions;
   private net.minecraft.util.Identifier iconId;
   private net.minecraft.util.Identifier iconId() {
      if (iconId == null) iconId = BKClientInit.id(this.icon);
      return iconId;
   }
   protected String headerTitle() { return Localizator.translate(this.name); }
   protected float listTop(float normal) { return headerOptions == null ? normal : headerOptions.top(normal); }
   protected float listStride(float normal) { return headerOptions == null ? normal : headerOptions.stride(normal); }
   protected void drawListHeader(UIContext context) {
      if (headerOptions != null) headerOptions.draw(context, x, y, width, headerTitle(), icon);
   }
   @Override public void update(UIContext context) {
      if (headerOptions != null && headerOptions.visible.isEnabled()) width = Math.max(width, headerOptions.minimumWidth(headerTitle()));
      super.update(context);
   }
   public HudList(String name, String icon) {
      super(name, icon);
      headerOptions = switch (name) {
         case "hud.keybinds", "hud.staff_list", "hud.friends", "hud.inventory", "hud.effects", "hud.automarket" -> new HudHeaderOptions(this, name);
         default -> null;
      };
   }

   @Override
   public void render(UIContext context) {
      if (!this.showing && this.animation.getValue() <= 0.001F) {
         return;
      }
      this.update(context);
      float anim = this.animation.getValue() * this.visible.getValue();
      if (anim != 0.0F) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, anim));
         float press = this.pressAnim.getValue();
         float scale = (0.5F + anim * 0.5F - 0.05F * this.selecting.getValue()) * press * this.getScale();
         float bounceVal = this.bounceAnim.getValue();
         float bounce = 1.0F + 0.08F * (float)Math.sin(bounceVal * Math.PI * 2.5F) * (1.0F - bounceVal);
         float sx = this.currentStretchX;
         float sy = this.currentStretchY;
         float cx = this.x + this.width / 2.0F;
         float cy = this.y + this.height / 2.0F;
         context.getMatrices().push();
         context.getMatrices().translate(cx, cy, 0.0F);
         float angleDeg = (float)Math.toDegrees(this.dragAngle);
         context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(angleDeg));
         context.getMatrices().scale(scale * bounce * sx, scale * bounce * sy, 1.0F);
         context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-angleDeg));
          context.getMatrices().translate(-cx, -cy, 0.0F);
          float stretchPad = Math.max(1.0F, (this.currentStretchX - 1.0F) * this.width * 0.5F);
          ScissorUtility.push(context.getMatrices(), this.x - stretchPad, this.y - stretchPad, this.width + stretchPad * 2.0F, Math.max(20.0F, this.height) + stretchPad * 2.0F);
         this.renderContent(context);
         ScissorUtility.pop();
         context.getMatrices().pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   protected void renderContent(UIContext context) {
      if (headerOptions != null && headerOptions.tabbed()) {
         headerOptions.drawPanels(context,x,y,width,height,headerTitle(),animation.getValue(),dragAnim.getValue());
         drawListHeader(context);
         return;
      }
      Font font = Fonts.REGULAR.getFont(7.0F);
      HudTheme.drawPanel(context, this.x, this.y, this.width, Math.max(20.0F, this.height), this.animation.getValue(), this.dragAnim.getValue(), 7.0F);
      if (headerOptions != null) { drawListHeader(context); return; }
      float iconSize = 8.0F;
      if (HudTheme.isOld()) {
         Font headerFont = Fonts.SEMIBOLD.getFont(7.0F);
         context.drawText(
            headerFont,
            Localizator.translate(this.name),
            this.x + 8.0F,
            this.y + GuiUtility.getMiddleOfBox(headerFont.height(), 16.0F) + 0.5F,
            HudTheme.textColor()
         );
         context.drawTexture(iconId(), this.x + this.width - iconSize - 8.0F,
            this.y + 4.0F, iconSize, iconSize, dev.bkclient.utility.colors.Colors.getAccent());
         return;
      }
      context.drawText(
         Fonts.MEDIUM.getFont(7.0F),
         Localizator.translate(this.name),
         this.x + 7.0F,
         this.y + GuiUtility.getMiddleOfBox(font.height(), 18.0F) + 0.5F,
         HudTheme.textColor()
      );
      context.drawTexture(iconId(), this.x + this.width - iconSize - 7.0F, this.y + 6.0F, iconSize, iconSize, HudTheme.textColor());
   }

   @Override
   protected void renderComponent(UIContext context) {
   }
}
