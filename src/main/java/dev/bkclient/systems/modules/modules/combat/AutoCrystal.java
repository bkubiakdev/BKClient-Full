package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.RangeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

/**
 * Places and breaks end crystals where you look: a crystal on the obsidian/bedrock under the crosshair, and the crystal
 * under the crosshair gets hit. The pace is a random delay, so the clicks are not evenly spaced.
 */
@ModuleInfo(name = "Auto Crystal", category = ModuleCategory.COMBAT, desc = "Automatically places and explodes crystals.")
public final class AutoCrystal extends BaseModule {
   private final RangeSetting delay = new RangeSetting(this, "Delay (ms)").min(0.0F).max(300.0F).step(5.0F).firstValue(30.0F).secondValue(50.0F);
   private final BooleanSetting silentSwap = new BooleanSetting(this, "Silent Swap");
   private final BooleanSetting headBob = new BooleanSetting(this, "Head Bob");
   private final BooleanSetting inAir = new BooleanSetting(this, "In Air");
   private final BooleanSetting switchOnEnable = new BooleanSetting(this, "Switch");
   private final BooleanSetting damageTick = new BooleanSetting(this, "Damage Tick");
   private final BooleanSetting pauseOnKill = new BooleanSetting(this, "Pause On Kill");

   private final CombatKit.Aimed aimed = new CombatKit.Aimed();
   private long nextAt;
   private BlockPos placedOn;

   @Override
   public void onEnable() {
      this.nextAt = 0L;
      this.placedOn = null;
      this.aimed.clear();
      if (this.switchOnEnable.isEnabled() && CombatKit.ready()) CombatKit.selectSlot(CombatKit.hotbar(Items.END_CRYSTAL));
   }

   @Override
   public void onDisable() {
      this.placedOn = null;
      this.aimed.clear();
   }

   @Override
   public void tick() {
      if (!CombatKit.canAct()) return;
      if (this.aimed.busy()) {
         this.aimed.tick();
         return;
      }
      long now = System.currentTimeMillis();
      var player = CombatKit.mc.player;
      if (now < this.nextAt || player.isUsingItem() || (!player.isOnGround() && !this.inAir.isEnabled())) return;
      PlayerEntity enemy = CombatKit.nearestEnemy(10.0);
      if (this.pauseOnKill.isEnabled() && enemy != null && enemy.getHealth() <= 0.0F) return;

      boolean mayBreak = !this.damageTick.isEnabled() || enemy == null || enemy.hurtTime == 0;
      HitResult target = CombatKit.mc.crosshairTarget;
      if (mayBreak && target instanceof EntityHitResult entityHit && this.breakable(entityHit.getEntity())) {
         CombatKit.attack(entityHit.getEntity());
         this.placedOn = null;
         this.pace();
         return;
      }
      if (mayBreak && this.headBob.isEnabled() && !(target instanceof EntityHitResult) && this.headBob()) return;
      if (target instanceof BlockHitResult blockHit && this.place(blockHit)) this.pace();
   }

   private boolean breakable(Entity entity) {
      return entity instanceof EndCrystalEntity || entity instanceof SlimeEntity || entity instanceof MagmaCubeEntity;
   }

   /** Silently turns to a crystal that sits above the block under the crosshair (or the block we just placed on) and hits it. */
   private boolean headBob() {
      BlockPos base = this.placedOn;
      if (base == null && CombatKit.mc.crosshairTarget instanceof BlockHitResult hit) base = hit.getBlockPos();
      if (base == null) return false;
      Box area = new Box(base.up()).expand(0.5);
      for (EndCrystalEntity crystal : CombatKit.mc.world.getEntitiesByClass(EndCrystalEntity.class, area, e -> true)) {
         if (CombatKit.mc.player.distanceTo(crystal) > 4.5F) continue;
         this.aimed.request(crystal.getBoundingBox().getCenter(), () -> CombatKit.aimingAt(crystal, 4.5),
            () -> {
               if (crystal.isAlive()) CombatKit.attack(crystal);
               this.placedOn = null;
            });
         this.pace();
         return true;
      }
      return false;
   }

   private boolean place(BlockHitResult hit) {
      var world = CombatKit.mc.world;
      var state = world.getBlockState(hit.getBlockPos());
      if ((!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) || hit.getSide() != Direction.UP) return false;
      BlockPos above = hit.getBlockPos().up();
      if (!world.isAir(above) || !world.isAir(above.up())) return false;
      if (!world.getOtherEntities(null, new Box(above).stretch(0.0, 1.0, 0.0), e -> !e.isSpectator()).isEmpty()) return false;

      Hand hand;
      int restore = -1;
      if (CombatKit.mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)) {
         hand = Hand.OFF_HAND;
      } else if (CombatKit.holding(Items.END_CRYSTAL)) {
         hand = Hand.MAIN_HAND;
      } else if (this.silentSwap.isEnabled() && CombatKit.hotbar(Items.END_CRYSTAL) >= 0) {
         hand = Hand.MAIN_HAND;
         restore = CombatKit.mc.player.getInventory().selectedSlot;
         CombatKit.selectSlot(CombatKit.hotbar(Items.END_CRYSTAL));
      } else {
         return false;
      }
      try {
         if (CombatKit.mc.interactionManager.interactBlock(CombatKit.mc.player, hand, hit).isAccepted()) {
            CombatKit.mc.player.swingHand(hand);
            this.placedOn = hit.getBlockPos();
         }
      } finally {
         if (restore >= 0) CombatKit.selectSlot(restore);
      }
      return true;
   }

   private void pace() {
      this.nextAt = System.currentTimeMillis() + CombatKit.randomDelay(this.delay);
   }
}
