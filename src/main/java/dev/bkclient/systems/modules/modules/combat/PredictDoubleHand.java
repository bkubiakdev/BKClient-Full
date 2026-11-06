package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.SendPacketEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.RangeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Takes a totem into the main hand when something is about to kill you: a charged anchor or a crystal in range, obsidian
 * an enemy could put a crystal on, a sword in an enemy's hand, or right after an ender pearl throw. Damage is estimated
 * without armor, so it warns a little early; Sensitivity scales the estimate.
 */
@ModuleInfo(name = "Predict Double Hand", category = ModuleCategory.COMBAT, desc = "Predicts when you will pop and mainhand totems for you")
public final class PredictDoubleHand extends BaseModule {
   private final RangeSetting delay = new RangeSetting(this, "Delay (ms)").min(0.0F).max(300.0F).step(5.0F).firstValue(30.0F).secondValue(50.0F);
   private final SelectSetting triggers = new SelectSetting(this, "Triggers");
   private final SelectSetting.Value anchors = new SelectSetting.Value(this.triggers, "Anchors").select();
   private final SelectSetting.Value crystals = new SelectSetting.Value(this.triggers, "Crystals").select();
   private final SelectSetting.Value obsidian = new SelectSetting.Value(this.triggers, "Obsidian").select();
   private final SelectSetting.Value swordPop = new SelectSetting.Value(this.triggers, "Sword Pop").select();
   private final SelectSetting.Value pearl = new SelectSetting.Value(this.triggers, "Pearl").select();
   private final SliderSetting sensitivity = new SliderSetting(this, "Sensitivity").min(0.5F).max(1.5F).step(0.05F).currentValue(1.0F);
   private final BooleanSetting ignoreWhenShield = new BooleanSetting(this, "Ignore When Shield").enabled(true);

   private long nextAt;
   private volatile boolean pearlThrown;

   private final EventListener<SendPacketEvent> onSend = event -> {
      if (this.pearl.isSelected() && event.getPacket() instanceof PlayerInteractItemC2SPacket
         && CombatKit.mc.player != null && CombatKit.mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
         this.pearlThrown = true;
      }
   };

   @Override
   public void onDisable() {
      this.pearlThrown = false;
   }

   @Override
   public void tick() {
      if (!CombatKit.ready() || System.currentTimeMillis() < this.nextAt) return;
      int slot = CombatKit.hotbar(Items.TOTEM_OF_UNDYING);
      if (slot < 0 || CombatKit.holding(Items.TOTEM_OF_UNDYING)) {
         this.pearlThrown = false;
         return;
      }
      if (this.ignoreWhenShield.isEnabled() && CombatKit.holding(Items.SHIELD) && CombatKit.mc.player.isUsingItem()) return;
      this.nextAt = System.currentTimeMillis() + CombatKit.randomDelay(this.delay);

      boolean danger = this.pearlThrown;
      PlayerEntity enemy = CombatKit.nearestEnemy(10.0);
      if (!danger && enemy != null) danger = this.threatened(enemy);
      this.pearlThrown = false;
      if (danger) CombatKit.selectSlot(slot);
   }

   private boolean threatened(PlayerEntity enemy) {
      var me = CombatKit.mc.player;
      var world = CombatKit.mc.world;
      float health = CombatKit.effectiveHealth(me);
      float scale = this.sensitivity.getCurrentValue();
      if (this.anchors.isSelected()) {
         BlockPos center = me.getBlockPos();
         for (BlockPos pos : BlockPos.iterate(center.add(-6, -4, -6), center.add(6, 4, 6))) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.RESPAWN_ANCHOR) && state.get(Properties.CHARGES) > 0
               && CombatKit.explosionDamage(Vec3d.ofCenter(pos), me, 5.0) * scale >= health) return true;
         }
      }
      if (this.crystals.isSelected()) {
         for (EndCrystalEntity crystal : world.getEntitiesByClass(EndCrystalEntity.class, me.getBoundingBox().expand(12.0), e -> true)) {
            if (CombatKit.explosionDamage(crystal.getPos(), me, 6.0) * scale >= health) return true;
         }
      }
      if (this.obsidian.isSelected()) {
         BlockPos center = me.getBlockPos();
         for (BlockPos pos : BlockPos.iterate(center.add(-5, -3, -5), center.add(5, 0, 5))) {
            var state = world.getBlockState(pos);
            if ((!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) || pos.getY() >= me.getY()) continue;
            if (!world.isAir(pos.up()) || !world.isAir(pos.up(2))) continue;
            if (Math.sqrt(enemy.squaredDistanceTo(Vec3d.ofCenter(pos))) >= 5.0) continue;
            if (CombatKit.explosionDamage(Vec3d.ofCenter(pos.up()), me, 6.0) * scale >= health) return true;
         }
      }
      if (this.swordPop.isSelected() && me.distanceTo(enemy) <= 4.5F
         && (enemy.getMainHandStack().getItem() instanceof SwordItem || enemy.getMainHandStack().getItem() instanceof AxeItem)) {
         float hit = (float)enemy.getAttributeValue(EntityAttributes.ATTACK_DAMAGE) * scale;
         if (hit >= health) return true;
      }
      return false;
   }
}
