package dev.bkclient.utility.game;

import dev.bkclient.utility.BlockEntityTracker;
import lombok.Generated;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import java.util.List;

public final class WorldUtility {
   @Deprecated
   public static List<BlockEntity> getBlockEntities() {
      return BlockEntityTracker.snapshot(MinecraftClient.getInstance().world);
   }

   @Generated
   private WorldUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
