package dev.bkclient.systems.event.handlers;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ServerConnectionEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.fabricmc.loader.api.FabricLoader;

public class ServerConnectionHandler implements IMinecraft {
   private boolean messageSent = false;
   private boolean connected;
   private final EventListener<ServerConnectionEvent> onServerConnection = event -> {
      this.connected = true;
      this.messageSent = false;
   };
   private final EventListener<ClientPlayerTickEvent> onClientPlayerTick = event -> {
      if (this.connected
         && !this.messageSent
         && mc.player != null
         && mc.player.age > 100
         && mc.getCurrentServerEntry() != null
         && FabricLoader.getInstance().isModLoaded("viafabricplus")) {
      }
   };

   public ServerConnectionHandler() {
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }
}
