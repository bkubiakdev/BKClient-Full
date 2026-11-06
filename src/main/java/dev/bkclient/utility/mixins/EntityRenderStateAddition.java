package dev.bkclient.utility.mixins;

import net.minecraft.entity.Entity;

public interface EntityRenderStateAddition {
   void bkclient$setEntity(Entity var1);

   Entity bkclient$getEntity();
}
