package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.systems.emotes.EmoteStateHolder;
import java.util.UUID;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Lets the player render state carry the UUID of the player it describes. */
@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEmoteStateMixin implements EmoteStateHolder {
   @Unique
   private UUID bkclient$uuid;

   @Override
   public UUID bkclient$getUuid() {
      return this.bkclient$uuid;
   }

   @Override
   public void bkclient$setUuid(UUID uuid) {
      this.bkclient$uuid = uuid;
   }
}
