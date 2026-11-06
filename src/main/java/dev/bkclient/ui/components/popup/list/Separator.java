package dev.bkclient.ui.components.popup.list;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.ui.components.popup.PopupComponent;
import dev.bkclient.utility.colors.Colors;

public class Separator extends PopupComponent {
   @Override
   protected void renderComponent(UIContext context) {
      context.drawRect(this.x, this.y, this.width, this.height, Colors.getSeparatorColor());
   }

   @Override
   public float getHeight() {
      return this.height = 4.0F;
   }
}
