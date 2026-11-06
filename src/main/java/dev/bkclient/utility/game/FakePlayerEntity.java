package dev.bkclient.utility.game;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public class FakePlayerEntity extends OtherClientPlayerEntity {
   public FakePlayerEntity(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   public void spawn() {
      this.unsetRemoved();
      this.clientWorld.addEntity(this);
   }

   public void remove() {
      this.clientWorld.removeEntity(this.getId(), RemovalReason.DISCARDED);
      this.onRemoved();
   }

   public void takeKnockback(double strength, double x, double z) {
   }
}
