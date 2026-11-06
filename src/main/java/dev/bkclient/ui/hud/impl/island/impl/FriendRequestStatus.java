package dev.bkclient.ui.hud.impl.island.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.friends.FriendRequestDetector;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.impl.island.IslandStatus;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.PlayerHeadSkins;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Dynamic Island notification for incoming server friend requests. */
public final class FriendRequestStatus extends IslandStatus implements IMinecraft {
   private static final ColorRGBA REQUEST_COLOR = new ColorRGBA(74.0F, 172.0F, 255.0F);
   private static FriendRequestStatus instance;
   private static volatile boolean previewRequested;
   private String sender = "";
   private long expiresAt;
   private long lastShownAt;
   private boolean preview;

   public FriendRequestStatus(SelectSetting parent) {
      super(parent, "friend_request");
      instance = this;
      ClientReceiveMessageEvents.GAME.register((message, overlay) -> this.acceptMessage(message.getString()));
      ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, parameters, receivedAt) ->
         this.acceptMessage(parameters.applyChatDecoration(message).getString()));
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
         this.expiresAt = 0L;
         this.lastShownAt = 0L;
      });
   }

   private void acceptMessage(String message) {
      if (mc.player == null) {
         return;
      }
      String found = FriendRequestDetector.findSender(message, mc.player.getGameProfile().getName());
      if (found != null) {
         this.show(found);
      }
   }

   private void show(String name) {
      long now = System.nanoTime();
      if (!this.preview && name.equalsIgnoreCase(this.sender) && now - this.lastShownAt < 750_000_000L) {
         return;
      }
      this.preview = false;
      this.sender = name;
      this.lastShownAt = now;
      this.expiresAt = now + 5_000_000_000L;
      dev.bkclient.systems.notifications.FriendRequestNotification.getInstance().show(name, false);
   }

   public static void showPreview() {
      previewRequested = true;
      if (instance != null && mc.player != null) {
         instance.activatePreview();
         previewRequested = false;
      }
   }

   private void activatePreview() {
      if (!this.preview && this.expiresAt > System.nanoTime()) return;
      this.preview = true;
      this.sender = "bkubiakdev";
      this.lastShownAt = 0L;
      this.expiresAt = System.nanoTime() + 5_000_000_000L;
      dev.bkclient.systems.notifications.FriendRequestNotification.getInstance().show(this.sender, true);
   }

   private void consumePreviewRequest() {
      if (previewRequested && mc.player != null) {
         previewRequested = false;
         this.activatePreview();
      }
   }

   public boolean isPreviewActive() {
      return this.preview && this.canShow();
   }

   @Override
   public boolean canShow() {
      this.consumePreviewRequest();
      return mc.player != null && this.expiresAt != 0L && System.nanoTime() < this.expiresAt;
   }

   @Override
   public void draw(CustomDrawContext context) {
      var font = Fonts.MEDIUM.getFont(6.5F);
      String label = Localizator.translate("hud.dynamic_dock.friend_request.received") + " " + this.sender;
      // The small circular request badge overlaps the lower corner of the skin.
      this.size.width = 27.0F + font.width(label);
      this.size.height = 18.0F;
      var dock = BKClientInit.getInstance().getHud().getIsland();
      float x = sr.getScaledWidth() / 2.0F - dock.getSize().width / 2.0F;

      var skin = PlayerHeadSkins.get(this.sender);
      DrawUtility.drawCrispPlayerHead(context.getMatrices(), skin, x + 6.0F, 5.0F, 8.0F,
         BorderRadius.all(2.0F), ColorRGBA.WHITE);
      dev.bkclient.ui.components.FriendRequestIcon.draw(context, x + 11, 9, 7, REQUEST_COLOR);
      // White matches the Dynamic Island's readable status text even when the
      // selected client theme uses a dark text palette for menus.
      context.drawText(font, label, x + 21.0F, (18.0F - font.height()) / 2.0F, ColorRGBA.WHITE);
   }
}
