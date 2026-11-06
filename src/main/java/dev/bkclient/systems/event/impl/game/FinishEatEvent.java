package dev.bkclient.systems.event.impl.game;

import lombok.Generated;
import dev.bkclient.systems.event.Event;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class FinishEatEvent extends Event {
   private final PlayerEntity user;
   private final ItemStack stack;

   @Generated
   public PlayerEntity getUser() {
      return this.user;
   }

   @Generated
   public ItemStack getStack() {
      return this.stack;
   }

   @Generated
   public FinishEatEvent(PlayerEntity user, ItemStack stack) {
      this.user = user;
      this.stack = stack;
   }
}
