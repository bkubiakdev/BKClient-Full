package dev.bkclient.combatengine.api.utils.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Local diagnostics only; player.sendMessage does not send chat to a server. */
public final class ChatUtils {
   public static void sendMessage(Object message) {
      var client = MinecraftClient.getInstance();
      if (client.player == null) System.out.println("[Aura] " + message);
      else client.player.sendMessage(Text.literal("[Aura] " + message), false);
   }
}
