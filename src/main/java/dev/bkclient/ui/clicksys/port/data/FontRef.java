package dev.bkclient.ui.clicksys.port.data;

import dev.bkclient.ui.clicksys.port.render.IconAtlas;

public record FontRef(IconAtlas font, float weight) {
   public float getWeight() {
      return this.weight;
   }

   public IconAtlas getFont() {
      return this.font;
   }
}
