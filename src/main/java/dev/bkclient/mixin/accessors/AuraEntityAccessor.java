package dev.bkclient.mixin.accessors;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface AuraEntityAccessor extends dev.bkclient.combatengine.mixin.IEntity {
   @Invoker("movementInputToVelocity")
   static Vec3d bkengine$movementInputToVelocity(Vec3d input, float speed, float yaw) {
      throw new AssertionError("Mixin invoker was not applied");
   }
   @org.spongepowered.asm.mixin.gen.Accessor("fluidHeight")
   it.unimi.dsi.fastutil.objects.Object2DoubleMap<net.minecraft.registry.tag.TagKey<net.minecraft.fluid.Fluid>> aura2$getFluidHeight();
   @org.spongepowered.asm.mixin.gen.Accessor("submergedFluidTag")
   java.util.Set<net.minecraft.registry.tag.TagKey<net.minecraft.fluid.Fluid>> aura2$getSubmergedFluidTags();
   @Invoker("adjustMovementForCollisions")
   Vec3d invokeAdjustMovementForCollisions(Vec3d var1);
}
