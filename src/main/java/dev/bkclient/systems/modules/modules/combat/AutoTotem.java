package dev.bkclient.systems.modules.modules.combat;

import java.util.Comparator;
import dev.bkclient.combatengine.api.utils.player.SwapManager;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.game.ItemUtility;
import dev.bkclient.utility.game.prediction.EntityPredictor;
import dev.bkclient.utility.game.prediction.FallPredictor;
import dev.bkclient.utility.inventory.ItemSlot;
import dev.bkclient.utility.inventory.group.SlotGroup;
import dev.bkclient.utility.inventory.group.SlotGroups;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "Auto Totem", category = ModuleCategory.COMBAT, desc = "modules.descriptions.auto_totem")
public class AutoTotem extends BaseModule {
   private final BooleanSetting takeFromHotbar = new BooleanSetting(this, "Take From Hotbar");
   private final SliderSetting swapCooldown = new SliderSetting(this, "Swap Cooldown")
      .min(0.0F).max(1000.0F).step(10.0F).currentValue(0.0F);
   private final BooleanSetting randomDelay = new BooleanSetting(this, "Random Delay");
   private final SliderSetting maxSwapDelay = new SliderSetting(this, "Max Swap Delay", () -> !this.randomDelay.isEnabled())
      .min(0.0F).max(1000.0F).step(10.0F).currentValue(150.0F);
   private final BooleanSetting fastCheck = new BooleanSetting(this, "Fast Check").enable();
   private final BooleanSetting retotemOnPop = new BooleanSetting(this, "Re-totem On Pop").enable();
   private final BooleanSetting healthRequirement = new BooleanSetting(this, "Health Requirement").enable();
   private final SliderSetting minimumHealthPercent = new SliderSetting(this, "Minimal Health %")
      .min(1.0F).max(100.0F).step(1.0F).currentValue(50.0F);
   private final BooleanSetting pickRandomTotem = new BooleanSetting(this, "Pick Random Totem");
   private final SliderSetting health = new SliderSetting(this, "modules.settings.auto_totem.health").min(1.0F).max(20.0F).step(0.5F).currentValue(6.0F);
   private final SliderSetting healthElytra = new SliderSetting(this, "modules.settings.auto_totem.elytra_health")
      .min(1.0F)
      .max(20.0F)
      .step(0.5F)
      .currentValue(6.0F);
   private final SliderSetting crystalDistance = new SliderSetting(this, "modules.settings.auto_totem.crystal_distance")
      .min(1.0F).max(12.0F).step(0.5F).currentValue(6.0F);
   private final SliderSetting tntDistance = new SliderSetting(this, "modules.settings.auto_totem.tnt_distance")
      .min(1.0F).max(12.0F).step(0.5F).currentValue(6.0F);
   private final SliderSetting tridentDistance = new SliderSetting(this, "modules.settings.auto_totem.trident_distance")
      .min(1.0F).max(12.0F).step(0.5F).currentValue(6.0F);
   private final SelectSetting get = new SelectSetting(this, "modules.settings.auto_totem.select_with");
   private final SelectSetting.Value fall = new SelectSetting.Value(this.get, "modules.settings.auto_totem.select_with.fall");
   private final SelectSetting.Value crystal = new SelectSetting.Value(this.get, "modules.settings.auto_totem.select_with.crystal");
   private final SelectSetting.Value tnt = new SelectSetting.Value(this.get, "modules.settings.auto_totem.select_with.tnt");
   private final SelectSetting.Value trident = new SelectSetting.Value(this.get, "modules.settings.auto_totem.select_with.trident");
   private final BooleanSetting deathTNT = new BooleanSetting(this, "modules.settings.auto_totem.death_tnt", () -> !this.tnt.isSelected());
   private final BooleanSetting returnItem = new BooleanSetting(this, "Return Item").enable();
   private int returnSlot = -1;
   private long lastSwapTime;
   private int totemCount = 0;
   // Set by the pop packet (network thread): when the replacement totem may be equipped, and when the request lapses.
   private volatile long popReadyAt;
   private volatile long popExpiresAt;
   private long nextDelay;
   private final EventListener<ReceivePacketEvent> onPacket = event -> {
      if (!this.retotemOnPop.isEnabled() || mc.player == null || mc.world == null) return;
      if (event.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 35
         && status.getEntity(mc.world) == mc.player) {
         long now = System.currentTimeMillis();
         this.popReadyAt = now + this.pickDelay();
         this.popExpiresAt = now + 3000L;
         // Without a delay do not wait for the next client tick (up to 50 ms): swap as soon as the main thread is free.
         if (this.popReadyAt <= now) mc.execute(this::equipAfterPop);
      }
   };
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player != null && mc.world != null) {
         this.updateTotemCount();
         this.update();
      }
   };

   /** The whole decision; runs every tick and, with Fast Check, every rendered frame too (the swap cooldown prevents doubles). */
   private void update() {
      if (mc.player == null || mc.world == null) return;
      boolean hasTotemInOffhand = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
      long now = System.currentTimeMillis();
      boolean popPending = this.popExpiresAt > now;
      if (popPending && hasTotemInOffhand) this.popExpiresAt = 0L;
      boolean popReady = popPending && !hasTotemInOffhand && now >= this.popReadyAt;
      boolean needsTotem = popReady || this.checkTotemCondition();
      if (needsTotem && !hasTotemInOffhand && now - this.lastSwapTime >= (this.randomDelay.isEnabled() ? this.nextDelay : (long)this.swapCooldown.getCurrentValue())) {
         this.equipTotem();
      }
      if (!needsTotem && !popPending && this.returnItem.isEnabled() && this.returnSlot != -1) {
         this.returnPreviousItem();
      }
   }
   private final EventListener<HudRenderEvent> onHud = event -> {
      if (this.fastCheck.isEnabled()) this.update();
      if (this.totemCount > 0) {
         CustomDrawContext ctx = event.getContext();
         Font font = Fonts.BOLD.getFont(7.0F);
         boolean isLeftHanded = mc.options.getMainArm().getValue() == Arm.LEFT;
         float x = isLeftHanded ? sr.getScaledWidth() / 2.0F - 103.0F : sr.getScaledWidth() / 2.0F + 103.0F;
         float y = sr.getScaledHeight() - 21.5F;
         ctx.pushMatrix();
         ctx.drawText(
            font,
            Text.of(String.valueOf(this.totemCount)),
            x - font.width(Text.of(String.valueOf(this.totemCount))) / 2.0F + 8.1F,
            y + 15.3F
         );
         ctx.drawItem(Items.TOTEM_OF_UNDYING, x - 8.0F, y + 3.0F, 1.0F);
         ctx.popMatrix();
      }
   };

   private boolean checkTotemCondition() {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen != null || mc.player.isSpectator() || !mc.player.isAlive()
            || (mc.player.isUsingItem() && mc.player.getActiveHand() == net.minecraft.util.Hand.OFF_HAND)
            || !mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
            return false;
         }

         if (mc.player.getItemCooldownManager().isCoolingDown(Items.TOTEM_OF_UNDYING.getDefaultStack())) {
            return false;
         }

         if (this.healthRequirement.isEnabled()) {
            float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            float maxHealth = Math.max(1.0F, mc.player.getMaxHealth());
            if (currentHealth / maxHealth * 100.0F <= this.minimumHealthPercent.getCurrentValue()
               || currentHealth <= (
                  mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
                     ? this.healthElytra.getCurrentValue()
                     : this.health.getCurrentValue()
               )) {
               return true;
            }
         }

         if (this.crystal.isSelected()) {
            float maxDmg = 0.0F;

            for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
               mc.player.getBoundingBox().expand(this.crystalDistance.getCurrentValue()), e -> true)) {
               maxDmg = Math.max(maxDmg, CombatKit.explosionDamage(c.getPos(), mc.player, 6.0));
            }

            if (maxDmg >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
               return true;
            }
         }

         if (this.tnt.isSelected()) {
            if (!this.deathTNT.isEnabled()) {
               return !mc.world.getEntitiesByClass(TntEntity.class,
                  mc.player.getBoundingBox().expand(this.tntDistance.getCurrentValue()), e -> true).isEmpty()
                  || !mc.world.getEntitiesByClass(TntMinecartEntity.class,
                     mc.player.getBoundingBox().expand(this.tntDistance.getCurrentValue()), e -> true).isEmpty();
            }

            float maxDMG = 0.0F;

            for (TntEntity tntEntity : mc.world.getEntitiesByClass(TntEntity.class,
               mc.player.getBoundingBox().expand(this.tntDistance.getCurrentValue()), e -> true)) {
               maxDMG = Math.max(maxDMG, CombatKit.explosionDamage(tntEntity.getPos(), mc.player, 4.0));
            }
            for (TntMinecartEntity minecart : mc.world.getEntitiesByClass(TntMinecartEntity.class,
               mc.player.getBoundingBox().expand(this.tntDistance.getCurrentValue()), e -> true)) {
               maxDMG = Math.max(maxDMG, CombatKit.explosionDamage(minecart.getPos(), mc.player, 4.0));
            }

            if (maxDMG >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
               return true;
            }
         }

         if (this.trident.isSelected()) {
            for (TridentEntity tridentEntity : mc.world.getEntitiesByClass(
               TridentEntity.class, mc.player.getBoundingBox().expand(this.tridentDistance.getCurrentValue()), e -> true)) {
               if (EntityPredictor.predictDamage(tridentEntity, mc.player)
                  >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                  return true;
               }
            }
         }

         if (this.fall.isSelected()) {
            float predicted = FallPredictor.predictFallDamage(mc.player, 120);
            return predicted >= mc.player.getHealth() + mc.player.getAbsorptionAmount();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void equipTotem() {
      ItemSlot totemSlot = this.findBestTotemSlot();
      if (totemSlot != null && totemSlot.getIdForServer() != 45) {
         int inventoryIndex = this.toInventoryIndex(totemSlot.getIdForServer());
         if (SwapManager.swapToOffhand(inventoryIndex)) {
            this.returnSlot = inventoryIndex;
            this.lastSwapTime = System.currentTimeMillis();
            this.nextDelay = this.pickDelay();
            this.popExpiresAt = 0L;
         } else if (!mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            // The offhand swap is not possible right now (screen, item on the cursor): hold a hotbar totem instead.
            for (int slot = 0; slot < 9; slot++) {
               if (mc.player.getInventory().getStack(slot).isOf(Items.TOTEM_OF_UNDYING)) {
                  CombatKit.selectSlot(slot);
                  this.lastSwapTime = System.currentTimeMillis();
                  break;
               }
            }
         }
      }
   }

   private void equipAfterPop() {
      if (mc.player == null || mc.world == null || this.popExpiresAt <= System.currentTimeMillis()) return;
      if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
         && System.currentTimeMillis() - this.lastSwapTime >= (this.randomDelay.isEnabled() ? this.nextDelay : (long)this.swapCooldown.getCurrentValue())
         && System.currentTimeMillis() >= this.popReadyAt) {
         this.equipTotem();
      }
   }

   /** The swap delay: the fixed cooldown, or a random value between it and the maximum when Random Delay is on. */
   private long pickDelay() {
      long min = (long)this.swapCooldown.getCurrentValue();
      if (!this.randomDelay.isEnabled()) return min;
      long max = Math.max(min, (long)this.maxSwapDelay.getCurrentValue());
      return min + (max > min ? ThreadLocalRandom.current().nextLong(max - min + 1L) : 0L);
   }

   private void returnPreviousItem() {
      if (this.returnSlot != -1) {
         SwapManager.swapToOffhand(this.returnSlot);
         this.returnSlot = -1;
         this.lastSwapTime = System.currentTimeMillis();
      }
   }

   private int toInventoryIndex(int serverSlot) {
      return serverSlot >= 36 && serverSlot <= 44 ? serverSlot - 36 : serverSlot;
   }

   @Nullable
   private ItemSlot findBestTotemSlot() {
      SlotGroup<ItemSlot> slotsToSearch = SlotGroups.offhand().and(SlotGroups.inventory());
      if (this.takeFromHotbar.isEnabled()) slotsToSearch = slotsToSearch.and(SlotGroups.hotbar());
      var candidates = slotsToSearch.findItems(Items.TOTEM_OF_UNDYING).stream().toList();
      if (candidates.isEmpty()) return null;
      if (this.pickRandomTotem.isEnabled()) {
         return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
      }
      return candidates.stream().min(Comparator.comparingInt(s -> ItemUtility.totemFactor(s.itemStack()))).orElse(null);
   }

   private void updateTotemCount() {
      this.totemCount = SlotGroups.inventory()
         .and(SlotGroups.hotbar())
         .and(SlotGroups.offhand())
         .and(SlotGroups.armor())
         .findItems(Items.TOTEM_OF_UNDYING)
         .size();
   }
}
