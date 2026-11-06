package dev.bkclient.combatengine.api.events.implement;

import dev.bkclient.combatengine.api.events.Event;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class EventAttackEntity extends Event {
   private final PlayerEntity recoveredField1;
   private final Entity recoveredField2;

   @Generated
   public EventAttackEntity(PlayerEntity player, Entity target) {
      this.recoveredField1 = player;
      this.recoveredField2 = target;
   }

   @Generated
   public PlayerEntity getPlayer() {
      return this.recoveredField1;
   }

   @Generated
   public Entity getTarget() {
      return this.recoveredField2;
   }
}
