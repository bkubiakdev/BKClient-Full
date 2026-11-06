package dev.bkclient.mixin.accessors;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ScreenHandler.class)
public interface ScreenHandlerAccessor {
   @Accessor("trackedStacks")
   DefaultedList<ItemStack> getTrackedStacks();

   @Accessor("revision")
   int getRevision();

   @Accessor("slots")
   DefaultedList<Slot> getSlots();
}
