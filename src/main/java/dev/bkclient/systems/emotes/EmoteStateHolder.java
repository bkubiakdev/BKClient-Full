package dev.bkclient.systems.emotes;

import java.util.UUID;

/** Implemented by the player render state (through a mixin) so the model knows which player it is posing. */
public interface EmoteStateHolder {
   UUID bkclient$getUuid();

   void bkclient$setUuid(UUID uuid);
}
