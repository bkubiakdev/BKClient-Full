package dev.bkclient.combatengine.client.bots.core;

/** The imported runtime is bound exclusively to BKClient's main Minecraft client. */
public final class BotContext {
   private BotContext() {}
   public static boolean isBot() { return false; }
}
