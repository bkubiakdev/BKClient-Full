package dev.bkclient.ui.hud.inline.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.ui.hud.inline.InlineElement;
import dev.bkclient.ui.hud.inline.InlineValue;
import dev.bkclient.ui.hud.old.OldHudPill;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.TextUtility;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlayerElement extends InlineElement {
   private static final long DATA_UPDATE_INTERVAL_NS = 100_000_000L;
   private final InlineValue fps = new InlineValue(this.elements, "FPS", "FPS");
   private final InlineValue speed = new InlineValue(this.elements, "speed", "BPS").texture("sf_symbold/sf/bps.png");
   private final BooleanSetting ySpeed = new BooleanSetting(this, "hud.player.speedY").enable();
   private final Animation animation = new Animation(300L, 0.0F, Easing.SMOOTH_STEP);
   private final SelectSetting oldElements = new SelectSetting(this, "hud.old.player.elements", () -> !HudTheme.isOld()).draggable().min(1);
   private final SelectSetting.Value oldLogo = new SelectSetting.Value(this.oldElements, "Logo").select();
   private final SelectSetting.Value oldNick = new SelectSetting.Value(this.oldElements, "Nick").select();
   private final SelectSetting.Value oldFps = new SelectSetting.Value(this.oldElements, "FPS").select();
   private final SelectSetting.Value oldPing = new SelectSetting.Value(this.oldElements, "Ping").select();
   private final SelectSetting.Value oldClock = new SelectSetting.Value(this.oldElements, "Clock").select();
   private final SelectSetting.Value oldTps = new SelectSetting.Value(this.oldElements, "TPS").select();
   private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm");
   private String nick = "?";
   private String fpsText = "0";
   private String ping = "0";
   private String time = "00:00";
   private String tps = "0";
   private long lastDataUpdate;

   public PlayerElement() {
      super("hud.player", "sf_symbold/sf/stats.png");
   }

   @Override
   public void update(UIContext context) {
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
      double motion = !this.ySpeed.isEnabled()
         ? Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ)
         : Math.hypot(
            mc.player.getY() - mc.player.prevY,
            Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ)
         );
      this.speed.update(String.format("%.2f", motion * 20.0).replace(",", "."));
      this.fps.update(Math.round(this.animation.update(mc.getCurrentFps())) + "");
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
      if (mc.player != null) {
         this.nick = mc.player.getName().getString();
         if (mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) this.ping = String.valueOf(entry.getLatency());
         }
      }
      this.fpsText = String.valueOf(Math.round(this.animation.update(mc.getCurrentFps())));
      this.time = this.clockFormat.format(new Date());
      if (BKClientInit.getInstance().getTpsHandler() != null) {
         this.tps = TextUtility.formatNumber(BKClientInit.getInstance().getTpsHandler().getTPS()).replace(",", ".").replace(".0", "");
      }
   }

   private List<OldHudPill.Token> oldTokens() {
      List<OldHudPill.Token> tokens = new ArrayList<>();
      for (SelectSetting.Value value : this.oldElements.getValues()) {
         if (!value.isSelected()) continue;
         if (!tokens.isEmpty()) tokens.add(OldHudPill.Token.divider());
         if (value == this.oldLogo) {
            tokens.add(OldHudPill.Token.icon('D', Colors.getAccent(), 15.5F, -1.0F, -0.2F));
         } else if (value == this.oldNick) {
            tokens.add(OldHudPill.Token.icon('F', Colors.getAccent(), 9.5F, 0.0F, -0.2F));
            tokens.add(OldHudPill.Token.text(this.nick, OldHudPill.TEXT, false));
         } else if (value == this.oldFps) {
            tokens.add(OldHudPill.Token.icon('G', Colors.getAccent(), 9.5F, 0.0F, 0.2F));
            tokens.add(OldHudPill.Token.text(this.fpsText, OldHudPill.TEXT, false));
            tokens.add(OldHudPill.Token.text("fps", Colors.getAccent(), true));
         } else if (value == this.oldPing) {
            tokens.add(OldHudPill.Token.icon('H', Colors.getAccent(), 9.5F, 0.0F, 0.2F));
            tokens.add(OldHudPill.Token.text(this.ping, OldHudPill.TEXT, false));
            tokens.add(OldHudPill.Token.text("Ms", Colors.getAccent(), true));
         } else if (value == this.oldClock) {
            tokens.add(OldHudPill.Token.icon('d', Colors.getAccent(), 9.5F, 0.0F, 0.2F));
            String[] parts = this.time.split(":", 2);
            tokens.add(OldHudPill.Token.text(parts[0], OldHudPill.TEXT, false));
            tokens.add(OldHudPill.Token.text(":", Colors.getAccent(), true));
            tokens.add(OldHudPill.Token.text(parts.length > 1 ? parts[1] : "00", OldHudPill.TEXT, true));
         } else if (value == this.oldTps) {
            tokens.add(OldHudPill.Token.icon('I', Colors.getAccent(), 9.5F, 0.0F, 0.0F));
            tokens.add(OldHudPill.Token.text(this.tps, OldHudPill.TEXT, false));
            tokens.add(OldHudPill.Token.text("tps", Colors.getAccent(), true));
         }
      }
      return tokens;
   }
}
