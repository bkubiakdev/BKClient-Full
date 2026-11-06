package dev.bkclient.systems.modules.modules.movement;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.network.SendPacketEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Air Stuck", category = ModuleCategory.MOVEMENT)
public class AirStuck extends BaseModule {
   private final BooleanSetting catchMoment = new BooleanSetting(this, "modules.settings.air_stuck.catch_moment").enable();
   private double peakY = Double.NaN;
   private boolean stuck;
   private boolean resumeStuck;
   private ClientPlayerEntity boundPlayer;
   private ClientWorld boundWorld;
   private Vec3d stuckPos = Vec3d.ZERO;
   private final EventListener<SendPacketEvent> onSendPacket = event -> {
      if (this.stuck && mc.player == this.boundPlayer && mc.world == this.boundWorld
         && mc.player != null && mc.world != null && event.getPacket() instanceof PlayerMoveC2SPacket) {
         event.cancel();
      }
   };
   private final EventListener<WorldChangeEvent> onWorldChange = event -> this.suspendForWorldChange();

   @Override
   public void onEnable() {
      this.reset();
      this.boundPlayer = mc.player;
      this.boundWorld = mc.world;
      if (mc.player == null) {
         this.peakY = Double.NaN;
      } else {
         if (this.catchMoment.isEnabled()) {
            this.peakY = mc.player.isOnGround() ? Double.NaN : mc.player.getY();
         } else {
            this.peakY = mc.player.getY();
            this.freezePlayer();
         }

         super.onEnable();
      }
   }

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null) {
         this.suspendForWorldChange();
         super.tick();
      } else {
         // Respawns and dimension changes can replace the player/world without a join event.
         if (mc.player != this.boundPlayer || mc.world != this.boundWorld) {
            this.suspendForWorldChange();
            this.boundPlayer = mc.player;
            this.boundWorld = mc.world;
         }
         // Wait for the new world's spawn position instead of freezing at the old coordinates.
         if (mc.player.age < 2 || mc.currentScreen instanceof DownloadingTerrainScreen) {
            return;
         }
         if (this.resumeStuck || (!this.catchMoment.isEnabled() && !this.stuck)) {
            this.freezePlayer();
            this.resumeStuck = false;
         }
         if (this.catchMoment.isEnabled() && !this.stuck) {
            double y = mc.player.getY();
            if (mc.player.isOnGround()) {
               this.peakY = Double.NaN;
               this.stuck = false;
            } else if (!this.stuck) {
               if (Double.isNaN(this.peakY) || y > this.peakY) {
                  this.peakY = y;
               } else if (y < this.peakY) {
                  this.freezePlayer();
               }
            }
         }

         if (this.stuck) {
            mc.player.setVelocity(Vec3d.ZERO);
            mc.player.setPosition(this.stuckPos);
            if (mc.player.input != null) {
               mc.player.input.movementForward = 0.0F;
               mc.player.input.movementSideways = 0.0F;
            }
         }

         super.tick();
      }
   }

   @Override
   public void onDisable() {
      this.reset();
      super.onDisable();
   }

   private void freezePlayer() {
      if (mc.player != null) {
         this.stuck = true;
         this.stuckPos = mc.player.getPos();
         this.peakY = mc.player.getY();
      }
   }

   private void reset() {
      this.resumeStuck = false;
      this.clearWorldState();
   }

   private void suspendForWorldChange() {
      // Keep the freeze intent across any number of ticks with no player/world.
      this.resumeStuck |= this.stuck;
      this.clearWorldState();
   }

   private void clearWorldState() {
      this.stuck = false;
      this.peakY = Double.NaN;
      this.stuckPos = Vec3d.ZERO;
      this.boundPlayer = null;
      this.boundWorld = null;
   }
}
