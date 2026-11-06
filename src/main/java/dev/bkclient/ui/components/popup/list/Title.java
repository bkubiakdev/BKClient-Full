package dev.bkclient.ui.components.popup.list;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.ui.components.popup.PopupComponent;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;

public class Title extends PopupComponent {
   private final String text;
   private final boolean translate;

   public Title(String text) {
      this(text, true);
   }

   public Title(String text, boolean translate) {
      this.text = text;
      this.translate = translate;
   }

   @Override
   protected void renderComponent(UIContext context) {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float nameLeftPadding = 8.0F;
      float nameHeight = nameFont.height();
      context.drawFadeoutText(
         nameFont,
         this.translate ? Localizator.translate(this.text) : this.text,
         this.x + nameLeftPadding,
         this.y + GuiUtility.getMiddleOfBox(nameHeight, this.height),
         Colors.getTextColor().withAlpha(RenderSystem.getShaderColor()[3] * 255.0F),
         0.8F,
         1.0F,
         this.width - 12.0F
      );
   }

   @Override
   public float getHeight() {
      return this.height = 18.0F;
   }
}
