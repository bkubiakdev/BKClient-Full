package dev.bkclient.ui.menu.modern;

import java.util.List;
import lombok.Generated;
import dev.bkclient.ui.menu.api.MenuCategory;
import dev.bkclient.ui.menu.modern.components.ModernModule;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.render.sprite.SpriteAnimationPlayer;

public class ModernCategory {
   private final MenuCategory category;
   private final List<ModernModule> modules;
   private final Animation selected = new Animation(500L, Easing.BAKEK);
   private SpriteAnimationPlayer spriteAnimation;
   private float y;

   @Generated
   public MenuCategory getCategory() {
      return this.category;
   }

   @Generated
   public List<ModernModule> getModules() {
      return this.modules;
   }

   @Generated
   public Animation getSelected() {
      return this.selected;
   }

   @Generated
   public SpriteAnimationPlayer getSpriteAnimation() {
      return this.spriteAnimation;
   }

   @Generated
   public float getY() {
      return this.y;
   }

   @Generated
   public ModernCategory(MenuCategory category, List<ModernModule> modules) {
      this.category = category;
      this.modules = modules;
   }

   public List<ModernModule> getVisibleModules() {
      return this.modules.stream().filter(m -> !m.getModule().isHidden()).toList();
   }

   @Generated
   public void setSpriteAnimation(SpriteAnimationPlayer spriteAnimation) {
      this.spriteAnimation = spriteAnimation;
   }

   @Generated
   public void setY(float y) {
      this.y = y;
   }
}
