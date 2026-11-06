package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements EntityRenderStateAddition {
   @Unique
   private Entity bkclient$entity;

   @Unique
   @Override
   public void bkclient$setEntity(Entity entity) {
      this.bkclient$entity = entity;
   }

   @Unique
   @Override
   public Entity bkclient$getEntity() {
      return this.bkclient$entity;
   }
}
