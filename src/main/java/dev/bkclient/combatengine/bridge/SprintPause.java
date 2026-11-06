package dev.bkclient.combatengine.bridge;

import net.minecraft.client.MinecraftClient;

/** Shared immediate sprint interruption used by Aura and AutoEat. */
public final class SprintPause {
    private SprintPause() {}
    public static void pushPause(long delayMs) {
        var client = MinecraftClient.getInstance();
        if (client.options != null) client.options.sprintKey.setPressed(false);
        if (client.player != null) client.player.setSprinting(false);
    }
    // Sprint resumption is owned by vanilla input / AutoSprint, as before with AuraSprint off.
    public static void popPause() {}
}
