package dev.bkclient.mixin.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses the package-private Entry record held by ItemCooldownManager. */
@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownEntryAccessor {
   @Accessor("endTick")
   int getEndTick();

   @Accessor("startTick")
   int getStartTick();
}
