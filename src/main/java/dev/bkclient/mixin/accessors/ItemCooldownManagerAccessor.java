package dev.bkclient.mixin.accessors;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

/** Reads the live cooldown entries maintained by Minecraft's item cooldown manager. */
@Mixin(ItemCooldownManager.class)
public interface ItemCooldownManagerAccessor {
   @Accessor("entries")
   Map<Identifier, Object> getEntries();

   @Accessor("tick")
   int getTick();

   @Invoker("getGroup")
   Identifier invokeGetGroup(ItemStack stack);
}
