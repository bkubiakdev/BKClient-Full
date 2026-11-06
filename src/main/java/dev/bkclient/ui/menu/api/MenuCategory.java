package dev.bkclient.ui.menu.api;

import lombok.Generated;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.utility.render.obj.CustomSprite;
import dev.bkclient.utility.render.sprite.SpriteAnimationPlayer;

public enum MenuCategory {
   COMBAT("Combat", ModuleCategory.COMBAT, CustomSprite.COMBAT, CustomSprite.BIG_COMBAT),
   MOVEMENT("Movement", ModuleCategory.MOVEMENT, CustomSprite.MOVEMENT, CustomSprite.BIG_MOVEMENT),
   VISUALS("Visuals", ModuleCategory.VISUALS, CustomSprite.VISUALS, CustomSprite.BIG_VISUALS),
   PLAYER("Player", ModuleCategory.PLAYER, CustomSprite.PLAYER, CustomSprite.BIG_PLAYER),
   OTHER("Other", ModuleCategory.OTHER, CustomSprite.OTHER, CustomSprite.BIG_OTHER);

   private final String name;
   private final ModuleCategory category;
   private final CustomSprite menuSprite;
   private final CustomSprite bigMenuSprite;
   private SpriteAnimationPlayer spriteAnimationPlayer;

   public void initSpriteAnimation() {
      try {
         this.spriteAnimationPlayer = new SpriteAnimationPlayer(BKClientInit.id("animations/" + this.name.toLowerCase() + ".bkanim"));
         this.spriteAnimationPlayer.stop();
      } catch (RuntimeException e) {
         this.spriteAnimationPlayer = null;
      }
   }

   public void playOnce() {
      if (this.spriteAnimationPlayer != null) {
         this.spriteAnimationPlayer.playOnce();
      }
   }

   public void stopSpriteAnimation() {
      if (this.spriteAnimationPlayer != null) {
         this.spriteAnimationPlayer.stop();
      }
   }

   public SpriteAnimationPlayer getSpriteAnimationPlayer() {
      return this.spriteAnimationPlayer;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public ModuleCategory getCategory() {
      return this.category;
   }

   @Generated
   public CustomSprite getMenuSprite() {
      return this.menuSprite;
   }

   @Generated
   public CustomSprite getBigMenuSprite() {
      return this.bigMenuSprite;
   }

   @Generated
   MenuCategory(String name, ModuleCategory category, CustomSprite menuSprite, CustomSprite bigMenuSprite) {
      this.name = name;
      this.category = category;
      this.menuSprite = menuSprite;
      this.bigMenuSprite = bigMenuSprite;
   }
}
