package dev.bkclient.ui.hud.inline.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.other.StreamerMode;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.ui.hud.inline.InlineElement;
import dev.bkclient.ui.hud.inline.InlineValue;
import dev.bkclient.ui.hud.old.OldHudPill;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.game.server.ServerUtility;
import java.util.ArrayList;
import java.util.List;

public class WorldElement extends InlineElement {
   private static final long DATA_UPDATE_INTERVAL_NS = 50_000_000L;
   private static final float LOW_TPS_THRESHOLD = 18.0F;
   private final InlineValue cords = new InlineValue(this.elements, "coords");
   private final InlineValue server = new InlineValue(this.elements, "server").texture("sf_symbold/sf/server.png");
   private final InlineValue tps = new InlineValue(this.elements, "TPS", "TPS");
   private final BooleanSetting shortName = new BooleanSetting(this, "hud.world.compact_server").enable();
   private final SelectSetting oldElements = new SelectSetting(this, "hud.old.world.elements", () -> !HudTheme.isOld()).draggable().min(1);
   private final SelectSetting.Value oldInfo = new SelectSetting.Value(this.oldElements, "Info").select();
   private final SelectSetting.Value oldCoords = new SelectSetting.Value(this.oldElements, "coords").select();
   private final SelectSetting.Value oldBps = new SelectSetting.Value(this.oldElements, "BPS").select();
   private String coordX = "0";
   private String coordY = "0";
   private String coordZ = "0";
   private String bps = "0";
   private long lastDataUpdate;
   private boolean coordinatesHidden;

   public WorldElement() {
      super("hud.world", "sf_symbold/sf/cord.png");
   }

   @Override
   public void update(UIContext context) {
      boolean hidden = StreamerMode.isHideCoords();
      if (hidden != this.coordinatesHidden) {
         this.coordinatesHidden = hidden;
         this.lastDataUpdate = 0L;
      }
      if (hidden) {
         this.cords.update("Hidden", "");
         this.cords.graySegments((String[]) null);
      }
      if (HudTheme.isOld()) {
         this.updateOldData();
         List<OldHudPill.Token> tokens = this.oldTokens();
         this.width = OldHudPill.width(tokens);
         this.height = OldHudPill.HEIGHT;
         super.update(context);
         return;
      }

      super.update(context);
      if (mc.player == null) return;
      long now = System.nanoTime();
      if (now - this.lastDataUpdate < DATA_UPDATE_INTERVAL_NS) return;
      this.lastDataUpdate = now;
      long cx = Math.round(mc.player.getX());
      long cy = Math.round(mc.player.getY());
      long cz = Math.round(mc.player.getZ());
      if (!hidden) {
         this.cords.update(String.format("%sx %sy %sz", cx, cy, cz));
         this.cords.graySegments(String.valueOf(cx), "x", " " + cy, "y", " " + cz, "z");
      }
      this.server.update(ServerUtility.getServerName(this.shortName.isEnabled()), ServerUtility.getIP());
      float tpsValue = BKClientInit.getInstance().getTpsHandler().getTPS();
      float displayedTps = Math.round(tpsValue * 10.0F) / 10.0F;
      this.tps.texture(displayedTps <= LOW_TPS_THRESHOLD ? "sf_symbold/sf/lowtps.png" : "sf_symbold/sf/tps.png");
      this.tps.update(TextUtility.formatNumber(tpsValue).replace(",", ".").replace(".0", ""));
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (!HudTheme.isOld()) {
         super.renderComponent(context);
         return;
      }
      OldHudPill.draw(context, this.x, this.y, this.width, this.getAnimation().getValue(), this.oldTokens());
   }

   private void updateOldData() {
      if (mc.player == null) return;
      long now = System.nanoTime();
      if (now - this.lastDataUpdate < DATA_UPDATE_INTERVAL_NS) return;
      this.lastDataUpdate = now;
      this.coordX = String.valueOf(Math.round(mc.player.getX()));
      this.coordY = String.valueOf(Math.round(mc.player.getY()));
      this.coordZ = String.valueOf(Math.round(mc.player.getZ()));
      double motion = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);
      this.bps = String.format("%.1f", motion * 20.0).replace(",", ".").replace(".0", "");
   }

   private List<OldHudPill.Token> oldTokens() {
      List<OldHudPill.Token> tokens = new ArrayList<>();
      for (SelectSetting.Value value : this.oldElements.getValues()) {
         if (!value.isSelected()) continue;
         if (!tokens.isEmpty()) tokens.add(OldHudPill.Token.divider());
         if (value == this.oldInfo) {
            tokens.add(OldHudPill.Token.icon('Y', Colors.getAccent(), 8.5F, 0.0F, -0.2F));
         } else if (value == this.oldCoords) {
            tokens.add(OldHudPill.Token.icon('Z', Colors.getAccent(), 9.5F, 0.0F, -0.2F));
            if (StreamerMode.isHideCoords()) {
               tokens.add(OldHudPill.Token.text("Hidden", OldHudPill.TEXT, false));
               continue;
            }
            tokens.add(OldHudPill.Token.text(this.coordX, OldHudPill.TEXT, false));
            tokens.add(OldHudPill.Token.text("x", Colors.getAccent(), true));
            tokens.add(OldHudPill.Token.text(" ; ", ColorRGBA.WHITE.withAlpha(64.0F), true));
            tokens.add(OldHudPill.Token.text(this.coordY, OldHudPill.TEXT, true));
            tokens.add(OldHudPill.Token.text("y", Colors.getAccent(), true));
            tokens.add(OldHudPill.Token.text(" ; ", ColorRGBA.WHITE.withAlpha(64.0F), true));
            tokens.add(OldHudPill.Token.text(this.coordZ, OldHudPill.TEXT, true));
            tokens.add(OldHudPill.Token.text("z", Colors.getAccent(), true));
         } else if (value == this.oldBps) {
            tokens.add(OldHudPill.Token.icon('a', Colors.getAccent(), 9.0F, 0.0F, 0.5F));
            tokens.add(OldHudPill.Token.text(this.bps, OldHudPill.TEXT, false));
            tokens.add(OldHudPill.Token.text("bps", Colors.getAccent(), true));
         }
      }
      return tokens;
   }
}
