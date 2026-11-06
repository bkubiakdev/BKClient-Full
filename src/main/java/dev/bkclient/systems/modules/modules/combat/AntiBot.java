package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import com.mojang.authlib.GameProfile;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;

@ModuleInfo(name = "Anti Bot", category = ModuleCategory.COMBAT, desc = "Detects bots created by anti-cheat systems")
public class AntiBot extends BaseModule {
   private final ModeSetting modeSetting = new ModeSetting(this, "Mode");
   private final ModeSetting.Value rw = new ModeSetting.Value(this.modeSetting, "RW");
   private final ModeSetting.Value defaults = new ModeSetting.Value(this.modeSetting, "Default");
   private final Set<UUID> bots = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private final Set<UUID> warnPlayers = ConcurrentHashMap.newKeySet();
   private final EventListener<ReceivePacketEvent> onPacket = event -> {
      if (event.getPacket() instanceof PlayerListS2CPacket packet && this.rw.isSelected() && packet.getActions().contains(Action.ADD_PLAYER)) {
         for (Entry entry : packet.getEntries()) {
            GameProfile profile = entry.profile();
            if (!this.warnPlayers.contains(profile.getId())
               && !this.bots.contains(profile.getId())
               && profile.getProperties().isEmpty()
               && entry.latency() != 0) {
               this.warnPlayers.add(profile.getId());
            }
         }
      }
   };

   @Override
   public void onDisable() {
      this.bots.clear();
      super.onDisable();
   }

   @Override
   public void tick() {
      if (mc.world != null && mc.player != null) {
         if (this.rw.isSelected()) {
            this.checkPlayersForFakes();
            this.checkWarnedPlayers();
            this.cleanupBots();
         } else {
            for (PlayerEntity player : mc.world.getPlayers()) {
               if (player.getHealth() <= 0.0F || player.noClip) {
                  this.bots.add(player.getUuid());
               }
            }
         }

         super.tick();
      }
   }

   private void checkPlayersForFakes() {
      Entity currentTarget = BKClientInit.getInstance().getTargetManager().getCurrentTarget();

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (!this.isInvalidPlayer(player) && this.isSuspiciousTargetFake(player, currentTarget) && player.age < 30) {
            this.bots.add(player.getUuid());
         }
      }
   }

   private void checkWarnedPlayers() {
      for (UUID uuid : this.warnPlayers) {
         PlayerEntity player = mc.world.getPlayerByUuid(uuid);
         if (player != null && (this.hasFullArmor(player) || this.hasSuspiciousUUID(player))) {
            this.bots.add(player.getUuid());
         }
      }
   }

   private void cleanupBots() {
      if (mc.player.age % 100 == 0) {
         this.bots.removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
      }
   }

   private boolean isInvalidPlayer(PlayerEntity player) {
      return player == mc.player || this.bots.contains(player.getUuid());
   }

   private boolean isSuspiciousTargetFake(PlayerEntity player, Entity target) {
      if (!(target instanceof PlayerEntity realTarget)) {
         return false;
      } else {
         boolean sameIdentity = player.getGameProfile().getName().equals(realTarget.getGameProfile().getName()) || player.getId() == realTarget.getId();
         return sameIdentity && !player.getInventory().equals(realTarget.getInventory());
      }
   }

   private boolean hasFullArmor(PlayerEntity player) {
      int armorCount = 0;

      for (ItemStack stack : player.getArmorItems()) {
         if (!stack.isEmpty()) {
            armorCount++;
         }
      }

      return armorCount == 4;
   }

   private boolean hasSuspiciousUUID(PlayerEntity player) {
      try {
         UUID nameAsUUID = UUID.fromString(player.getGameProfile().getName());
         return !player.getUuid().equals(nameAsUUID);
      } catch (IllegalArgumentException var3) {
         return false;
      }
   }

   public boolean isRWBot(PlayerEntity player) {
      return this.bots.contains(player.getUuid());
   }
}
