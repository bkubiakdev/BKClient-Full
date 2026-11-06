package dev.bkclient.ui.hud.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.hud.preview.HudPreviewData;
import dev.bkclient.ui.hud.preview.PreviewListHud;
import dev.bkclient.utility.game.ClientTelemetryUtility;
import dev.bkclient.utility.game.server.ServerUtility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.Items;

/** HUD backed by the persisted friend list and the current network/world state. */
public class FriendsElement extends PreviewListHud {
   private final dev.bkclient.systems.setting.settings.BooleanSetting showCount = new dev.bkclient.systems.setting.settings.BooleanSetting(this, "hud.list.count");
   private final dev.bkclient.systems.setting.settings.BooleanSetting showStatus = new dev.bkclient.systems.setting.settings.BooleanSetting(this, "hud.list.status").enable();
   private final dev.bkclient.systems.setting.settings.BooleanSetting onlineOnly = new dev.bkclient.systems.setting.settings.BooleanSetting(this, "hud.list.online_only");
   @Override protected String headerTitle() { return super.headerTitle() + (showCount.isEnabled() ? " (" + rows().size() + ")" : ""); }

   private final dev.bkclient.systems.setting.settings.BooleanSetting hidden =
         new dev.bkclient.systems.setting.settings.BooleanSetting(this, "Hidden") {
            @Override public boolean isEnabled() { return dev.bkclient.systems.friends.PresencePrivacy.isHidden(); }
            @Override public com.google.gson.JsonElement save() { return new com.google.gson.JsonPrimitive(isEnabled()); }
            @Override public void toggle() { setEnabled(!isEnabled()); }
            @Override public void setEnabled(boolean value) {
               super.setEnabled(value);
               dev.bkclient.systems.friends.PresencePrivacy.setHidden(value);
               if (!dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave) {
                  BKClientInit.getInstance().getFileManager().writeFile("client");
                  ClientTelemetryUtility.privacyChanged();
               }
            }
         };
   public FriendsElement() {
      super("hud.friends", "icons/hud/who.png");
   }

   /** Rows and pointer angles of the current render pass: update, header and rows all read one
    *  snapshot instead of rescanning friends x (tab list + world players) several times a frame. */
   private List<HudPreviewData.Row> frameRows;
   private final java.util.IdentityHashMap<HudPreviewData.Row, Float> frameDirections = new java.util.IdentityHashMap<>();

   @Override
   public void render(dev.bkclient.framework.base.UIContext context) {
      try {
         super.render(context);
      } finally {
         this.frameRows = null;
         this.frameDirections.clear();
      }
   }

   @Override
   public void update(dev.bkclient.framework.base.UIContext context) {
      this.frameDirections.clear();
      this.frameRows = this.readLiveRows();
      super.update(context);
   }

   @Override
   protected List<HudPreviewData.Row> rows() {
      return this.frameRows != null ? this.frameRows : this.readLiveRows();
   }

   private List<HudPreviewData.Row> readLiveRows() {
      List<HudPreviewData.Row> rows = new ArrayList<>();
      String currentServer = ServerUtility.getCurrentServerAddress("Offline");
      for (String friendName : BKClientInit.getInstance().getFriendManager().listFriends()) {
         PlayerListEntry listEntry = this.findPlayerListEntry(friendName);
         AbstractClientPlayerEntity worldPlayer = this.findWorldPlayer(friendName);
         ClientTelemetryUtility.FriendPresence presence = ClientTelemetryUtility.getFriendPresence(friendName);
         boolean online = listEntry != null || worldPlayer != null || presence != null;
         if (onlineOnly.isEnabled() && !online) continue;
         String displayName = listEntry != null ? listEntry.getProfile().getName()
               : worldPlayer != null ? worldPlayer.getName().getString()
               : presence != null ? presence.name() : friendName;
         String status;
         if (worldPlayer != null && mc.player != null) {
            status = Math.round(mc.player.distanceTo(worldPlayer)) + "m";
         } else if (online) {
            status = listEntry != null || worldPlayer != null ? currentServer : presence.server();
         } else {
            status = "Offline";
         }
         rows.add(new HudPreviewData.Row(Items.PLAYER_HEAD, displayName, showStatus.isEnabled() ? status : ""));
      }
      return rows;
   }

   private Float direction(HudPreviewData.Row row) {
      if (this.frameRows == null) return computeDirection(row);
      if (this.frameDirections.containsKey(row)) return this.frameDirections.get(row);
      Float angle = computeDirection(row);
      this.frameDirections.put(row, angle);
      return angle;
   }

   private Float computeDirection(HudPreviewData.Row row) {
      AbstractClientPlayerEntity friend = findWorldPlayer(row.label());
      if (friend == null || mc.player == null) return null;
      double dx = friend.getX() - mc.player.getX();
      double dz = friend.getZ() - mc.player.getZ();
      return (float)Math.toDegrees(Math.atan2(-dx, dz)) - mc.player.getYaw();
   }

   @Override
   protected float extraRowWidth(HudPreviewData.Row row) {
      return direction(row) == null ? 0 : 12;
   }

   @Override
   protected void renderRow(dev.bkclient.framework.base.UIContext context, HudPreviewData.Row row, float rowY) {
      Float angle = direction(row);
      boolean old = dev.bkclient.ui.hud.HudTheme.isOld();
      dev.bkclient.ui.hud.components.HudValueRow.draw(context, this.x, rowY, this.width - (angle == null ? 0 : 12),
            row.label(), row.value(), dev.bkclient.ui.hud.HudTheme.textColor(), 1,
            (iconX, iconY, size) -> {
               var skin = dev.bkclient.utility.render.PlayerHeadSkins.get(row.label());
               dev.bkclient.utility.render.DrawUtility.drawCrispPlayerHead(context.getMatrices(), skin,
                     iconX, iconY, size, dev.bkclient.framework.objects.BorderRadius.all(1.0F), dev.bkclient.utility.colors.ColorRGBA.WHITE);
            });
      if (angle == null) return;
      float size = (old ? 6.5F : 10) * 0.75F;
      float centerX = this.x + this.width - (old ? 8 : 6) - size / 2;
      float centerY = rowY + (old ? 4.5F : 9);
      context.getMatrices().push();
      context.getMatrices().translate(centerX, centerY, 0);
      context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(angle));
      // Solid navigation pointer: a broad head with a short notch at the base.
      var matrix = context.getMatrices().peek().getPositionMatrix();
      int color = dev.bkclient.ui.hud.HudTheme.textColor().getRGB();
      com.mojang.blaze3d.systems.RenderSystem.enableBlend();
      com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
      com.mojang.blaze3d.systems.RenderSystem.disableCull();
      com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
      var buffer = net.minecraft.client.render.Tessellator.getInstance().begin(
            net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLES, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, 0, -size * .5F, 0).color(color);
      buffer.vertex(matrix, size * .43F, size * .4F, 0).color(color);
      buffer.vertex(matrix, 0, size * .18F, 0).color(color);
      buffer.vertex(matrix, 0, -size * .5F, 0).color(color);
      buffer.vertex(matrix, 0, size * .18F, 0).color(color);
      buffer.vertex(matrix, -size * .43F, size * .4F, 0).color(color);
      net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());
      com.mojang.blaze3d.systems.RenderSystem.enableCull();
      com.mojang.blaze3d.systems.RenderSystem.disableBlend();
      context.getMatrices().pop();
   }

   private PlayerListEntry findPlayerListEntry(String name) {
      if (name == null || mc.getNetworkHandler() == null) {
         return null;
      }
      for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
         if (entry.getProfile().getName().equalsIgnoreCase(name)) {
            return entry;
         }
      }
      return null;
   }

   private AbstractClientPlayerEntity findWorldPlayer(String name) {
      if (name == null || mc.world == null) {
         return null;
      }
      for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
         if (player.getName().getString().equalsIgnoreCase(name)) {
            return player;
         }
      }
      return null;
   }

   @Override
   public boolean show() {
      return !BKClientInit.getInstance().getFriendManager().listFriends().isEmpty()
            || mc.currentScreen instanceof ChatScreen
            || this.alwaysDisplay.isEnabled();
   }
}
