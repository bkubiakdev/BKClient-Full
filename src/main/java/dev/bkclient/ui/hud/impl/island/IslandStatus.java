package dev.bkclient.ui.hud.impl.island;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IScaledResolution;

public abstract class IslandStatus extends SelectSetting.Value implements IScaledResolution {
   protected IslandSize size = new IslandSize(48.0F, 15.0F);
   protected final Animation animation = new Animation(500L, Easing.BAKEK_SIZE);

   public IslandStatus(SelectSetting parent, String name) {
      super(parent, "hud.dynamic_dock.statuses." + name);
      this.select();
   }

   public void draw(CustomDrawContext context) {
   }

   public void drawWithAlpha(CustomDrawContext context) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.animation.getValue());
      this.draw(context);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void click(float mouseX, float mouseY, int button) {
   }

   public abstract boolean canShow();

   public ColorRGBA getColor() {
      return ColorRGBA.BLACK;
   }

   @Generated
   public IslandSize getSize() {
      return this.size;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }
}
