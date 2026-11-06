package dev.bkclient.systems.modules.modules.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.player.InputEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.player.autoleave.ShulkerStash;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.game.server.ServerUtility;
import dev.bkclient.utility.time.Timer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import java.util.function.BooleanSupplier;

@ModuleInfo(name = "Auto Leave", category = ModuleCategory.PLAYER)
public class AutoLeave extends BaseModule {
   private final SelectSetting leave = new SelectSetting(this, "modules.settings.auto_leave.leave") {
      @Override
      public void load(JsonElement element) {
         // Before 3.0.6 this setting stored a single ModeSetting option as a string.
         if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            JsonArray selected = new JsonArray();
            selected.add(element);
            super.load(selected);
         } else {
            super.load(element);
         }
      }
   }.min(1);
   private final SelectSetting.Value distLeave = new SelectSetting.Value(this.leave, "modules.settings.auto_leave.leave.distance").select();
   private final SelectSetting.Value healthLeave = new SelectSetting.Value(this.leave, "modules.settings.auto_leave.leave.health");
   private final SelectSetting.Value banLeave = new SelectSetting.Value(this.leave, "modules.settings.auto_leave.leave.ban");
   private final SliderSetting dist = new SliderSetting(
         this, "modules.settings.auto_leave.distance", () -> !this.distLeave.isSelected()
      )
      .suffix(number -> " %s".formatted(Localizator.translate("block")) + TextUtility.makeCountTranslated(number))
      .step(1.0F)
      .min(1.0F)
      .max(150.0F)
      .currentValue(30.0F);
   private final SliderSetting health = new SliderSetting(
         this, "modules.settings.auto_leave.health", () -> !this.healthLeave.isSelected()
      )
      .step(1.0F)
      .min(1.0F)
      .max(20.0F)
      .currentValue(10.0F);
   private final SliderSetting delay = new SliderSetting(
         this, "modules.settings.auto_leave.delay", () -> !this.banLeave.isSelected()
      )
      .suffix(Localizator.translate("sec") + ".")
      .step(1.0F)
      .min(1.0F)
      .max(60.0F)
      .currentValue(40.0F);
   private final Timer timer = new Timer();
   private boolean waiting;
   private volatile boolean banRequested;
   private final BooleanSetting stashBoxes = new BooleanSetting(this, "modules.settings.auto_leave.stash");
   private final SliderSetting stashRadius = new SliderSetting(this, "modules.settings.auto_leave.stash_radius", () -> !this.stashBoxes.isEnabled())
      .min(1).max(5).step(1).currentValue(3);
   private final SliderSetting stashTimeout = new SliderSetting(this, "modules.settings.auto_leave.stash_timeout", () -> !this.stashBoxes.isEnabled())
      .min(5).max(60).step(1).currentValue(20).suffix(Localizator.translate("sec") + ".");
   private final ShulkerStash stash = new ShulkerStash();
   private String pendingReason;
   private boolean pendingBan;
   private int lastStored;
   private boolean stashAttempted;
   private String stashFailure;
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.auto_leave.mode");
   private final ModeSetting.Value hub = new ModeSetting.Value(this.mode, "modules.settings.auto_leave.mode.hub");
   private final ModeSetting.Value serverLeave = new ModeSetting.Value(this.mode, "modules.settings.auto_leave.mode.server");
   private final ModeSetting.Value spawn = new ModeSetting.Value(this.mode, "modules.settings.auto_leave.mode.spawn");
   private final EventListener<ClientPlayerTickEvent> onClientPlayerTickEvent = event -> {
      if (mc.player == null || mc.world == null) {
         this.waiting = false;
         this.banRequested = false;
         this.pendingReason = null;
         this.stash.reset();
         return;
      }

      if (this.pendingReason != null) {
         this.tickStash();
         return;
      }
      String reason = this.leaveReason(mc.player.getHealth() + mc.player.getAbsorptionAmount(), this::hasNearbyPlayer);
      if (reason != null) {
         this.beginLeave(reason, false);
         // Two matching conditions must never send two actions or toggle the module back on.
         return;
      }

      if (this.banRequested) {
         this.banRequested = false;
         if (this.banLeave.isSelected() && !this.waiting) {
            this.beginLeave("modules.auto_leave.banned_word", true);
            return;
         }
      }
      if (!this.banLeave.isSelected()) this.waiting = false;
      if (this.waiting && this.timer.finished((long)this.delay.getCurrentValue() * 1000L)) {
         mc.player.networkHandler.sendChatCommand("an" + ServerUtility.ftAn);
         this.waiting = false;
      }
   };
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      this.stash.onPacket(event.getPacket());
      if (mc.player != null && mc.world != null
         && event.getPacket() instanceof GameMessageS2CPacket packet
         && packet.content().getString().contains(Localizator.translate("modules.auto_leave.banned_word"))
         && this.banLeave.isSelected()) {
         this.banRequested = true;
      }
   };
   private final EventListener<InputEvent> onInput = new EventListener<>() {
      @Override public void onEvent(InputEvent event) { stash.input(event); }
      @Override public int getPriority() { return Integer.MIN_VALUE; }
   };

   public static boolean isStashing() {
      var client = BKClientInit.getInstance();
      if (client == null || client.getModuleManager() == null) return false;
      var module = client.getModuleManager().getModule(AutoLeave.class);
      return module != null && module.isEnabled() && module.stash.isActive();
   }

   private void beginLeave(String reason, boolean ban) {
      this.lastStored = 0;
      this.stashFailure = null;
      this.stashAttempted = this.stashBoxes.isEnabled();
      var market = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.bkmods.AutoMarket.class);
      if (market != null) market.prepareForAutoLeave();
      this.waiting = false;
      this.banRequested = false;
      if (!this.stashBoxes.isEnabled()) {
         this.finishLeave(reason, ban);
         return;
      }
      this.pendingReason = reason;
      this.pendingBan = ban;
      this.stash.start((int) this.stashRadius.getCurrentValue(), (int) this.stashTimeout.getCurrentValue());
   }

   private void tickStash() {
      ShulkerStash.Result result = this.stashBoxes.isEnabled() ? this.stash.tick() : ShulkerStash.Result.FAILED;
      if (result == ShulkerStash.Result.RUNNING) return;
      String reason = this.pendingReason;
      boolean ban = this.pendingBan;
      if (result == ShulkerStash.Result.DONE && this.stash.stored() > 0) {
         MessageUtility.info(Text.of(Localizator.translate("modules.auto_leave.stash.done", this.stash.stored())));
      } else if (result == ShulkerStash.Result.FAILED) {
         String failure = this.stash.failure();
         MessageUtility.warn(Text.of(Localizator.translate("modules.auto_leave.stash.failed",
            failure == null ? Localizator.translate("modules.auto_leave.stash.error.cancelled") : Localizator.translate(failure), this.stash.stored())));
      }
      this.lastStored = this.stash.stored();
      this.stashFailure = result == ShulkerStash.Result.FAILED
         ? (this.stash.failure() == null ? "modules.auto_leave.stash.error.cancelled" : this.stash.failure()) : null;
      this.pendingReason = null;
      this.stash.reset();
      if (result == ShulkerStash.Result.CANCELLED) {
         this.disable(); // Do not send the old action after a world/server change or death.
      } else {
         this.finishLeave(reason, ban);
      }
   }

   private void finishLeave(String reason, boolean ban) {
      if (ban) {
         mc.player.networkHandler.sendChatCommand("hub");
         this.timer.reset();
         this.waiting = true;
      } else {
         this.performLeave(reason);
      }
   }

   String leaveReason(float healthWithAbsorption, BooleanSupplier nearbyPlayer) {
      if (this.distLeave.isSelected() && nearbyPlayer.getAsBoolean()) {
         return "modules.auto_leave.near_player";
      }
      if (this.healthLeave.isSelected() && healthWithAbsorption <= this.health.getCurrentValue()) {
         return "modules.auto_leave.low_health";
      }
      return null;
   }

   boolean isWithinLeaveDistance(float distance) {
      return distance <= this.dist.getCurrentValue();
   }

   private boolean hasNearbyPlayer() {
      if (ServerUtility.hasCT) return false;
      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player != null && player != mc.player
            && this.isWithinLeaveDistance(player.distanceTo(mc.player))
            && !BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString())) {
            return true;
         }
      }
      return false;
   }

   private void performLeave(String reason) {
      this.waiting = false;
      if (this.hub.isSelected()) {
         mc.player.networkHandler.sendChatCommand("hub");
      } else if (this.serverLeave.isSelected()) {
         var market = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.bkmods.AutoMarket.class);
         if (market != null) market.getReconnectController().pauseAfterAutoLeave();
         var message = Text.literal("BK-Client saved ur ass").append("\n\n" + Localizator.translate(reason));
         if (stashAttempted) message.append("\n" + Localizator.translate("modules.auto_leave.saved.count", lastStored));
         if (stashFailure != null) message.append("\n" + Localizator.translate(stashFailure));
         mc.player.networkHandler.getConnection().disconnect(message);
      } else if (this.spawn.isSelected()) {
         mc.player.networkHandler.sendChatCommand("spawn");
      }
      this.disable();
   }

   @Override
   public void onEnable() {
      this.waiting = false;
      this.banRequested = false;
      this.pendingReason = null;
      this.stash.reset();
   }

   @Override
   public void onDisable() {
      this.waiting = false;
      this.banRequested = false;
      this.pendingReason = null;
      this.stash.reset();
   }
}
