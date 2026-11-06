package dev.bkclient.mixin.minecraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface IMinecraftClient {
   @Invoker("doItemUse")
   void idoItemUse();

   @Accessor("itemUseCooldown")
   void setUseCooldown(int var1);

   @Accessor("session")
   Session getSession();

   @Accessor("session")
   void setSession(Session session);

   @Accessor("crosshairTarget")
   void setCrosshairTarget(net.minecraft.util.hit.HitResult target);

   @Accessor("targetedEntity")
   void setTargetedEntity(net.minecraft.entity.Entity entity);

}
