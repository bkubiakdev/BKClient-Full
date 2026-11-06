package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.RangeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Explodes the respawn anchors around you without having to look at them: the nearest anchor in reach that you can see is
 * turned to silently and used. With Auto Charge an empty anchor (one you just placed) is first charged with glowstone from
 * the hotbar. An anchor only explodes when it is used with something other than glowstone, so after charging the hand goes
 * back to the item held before. Does nothing in dimensions where anchors work as spawn points (the Nether).
 */
@ModuleInfo(name = "Anchor Exploder", category = ModuleCategory.COMBAT, desc = "Explodes anchors with glowstone in them when looking at them")
public final class AnchorExploder extends BaseModule {
   private final RangeSetting delay = new RangeSetting(this, "Delay (ms)").min(0.0F).max(300.0F).step(5.0F).firstValue(30.0F).secondValue(50.0F);
   private final SliderSetting range = new SliderSetting(this, "Range", () -> !this.silentAim.isEnabled()).min(2.0F).max(5.5F).step(0.1F).currentValue(4.5F).suffix("blocks");
   private final BooleanSetting silentAim = new BooleanSetting(this, "Silent Aim").enabled(true);
   private final BooleanSetting autoCharge = new BooleanSetting(this, "Auto Charge").enabled(true);

   private final CombatKit.Aimed aimed = new CombatKit.Aimed();
   private long nextAt;
   /** The slot that was held before glowstone was taken, so the hand goes back to it. */
   private int returnSlot = -1;

   @Override
   public void onDisable() {
      this.aimed.clear();
   }

   @Override
   public void tick() {
      if (!CombatKit.canAct()) return;
      if (this.aimed.busy()) {
         this.aimed.tick();
         return;
      }
      if (System.currentTimeMillis() < this.nextAt) return;
      var player = CombatKit.mc.player;
      if (player.isUsingItem() && CombatKit.holding(Items.SHIELD)) return;
      if (CombatKit.mc.world.getDimension().respawnAnchorWorks()) return;
      BlockHitResult hit = this.silentAim.isEnabled() ? this.nearestVisibleAnchor() : this.crosshairAnchor();
      if (hit == null) return;

      boolean empty = CombatKit.mc.world.getBlockState(hit.getBlockPos()).get(Properties.CHARGES) == 0;
      if (empty && (!this.autoCharge.isEnabled() || CombatKit.hotbar(Items.GLOWSTONE) < 0)) return;
      if (!empty && CombatKit.holding(Items.GLOWSTONE)) {
         // Glowstone would only charge it further: take a different item first, then explode on the next pass.
         CombatKit.selectSlot(this.explodeSlot());
         this.pace();
         return;
      }
      if (!this.silentAim.isEnabled()) {
         // Without Silent Aim only the anchor under the crosshair is used, straight away.
         this.interact(hit, empty);
         this.pace();
         return;
      }
      BlockPos pos = hit.getBlockPos();
      this.aimed.request(hit.getPos(), () -> CombatKit.aimingAtBlock(pos, this.range.getCurrentValue() + 0.5), () -> this.use(pos, empty));
      this.pace();
   }

   private void use(BlockPos pos, boolean empty) {
      if (!CombatKit.ready() || !CombatKit.mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) return;
      var player = CombatKit.mc.player;
      // Use the block face the rotation sent to the server really points at.
      var rotation = BKClientInit.getInstance().getRotationHandler().getServerRotation();
      Vec3d eye = player.getEyePos();
      BlockHitResult hit = CombatKit.mc.world.raycast(new RaycastContext(eye,
         eye.add(Vec3d.fromPolar(rotation.getPitch(), rotation.getYaw()).multiply(this.range.getCurrentValue() + 0.5)),
         RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
      if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) return;
      this.interact(hit, empty);
   }

   /** Charges an empty anchor with glowstone (then goes back to the earlier item) or explodes a charged one. */
   private void interact(BlockHitResult hit, boolean empty) {
      var player = CombatKit.mc.player;
      if (empty) {
         int glowstone = CombatKit.hotbar(Items.GLOWSTONE);
         if (glowstone < 0) return;
         if (!CombatKit.holding(Items.GLOWSTONE)) this.returnSlot = player.getInventory().selectedSlot;
         CombatKit.selectSlot(glowstone);
         CombatKit.mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
         player.swingHand(Hand.MAIN_HAND);
         CombatKit.selectSlot(this.explodeSlot());
      } else {
         CombatKit.mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
         player.swingHand(Hand.MAIN_HAND);
      }
   }

   /** The anchor the crosshair is on, if any. */
   private BlockHitResult crosshairAnchor() {
      return CombatKit.mc.crosshairTarget instanceof BlockHitResult hit
         && CombatKit.mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR) ? hit : null;
   }

   /** The closest anchor in reach with a face you can see, as the hit to aim at. */
   private BlockHitResult nearestVisibleAnchor() {
      var player = CombatKit.mc.player;
      var world = CombatKit.mc.world;
      Vec3d eye = player.getEyePos();
      double reach = this.range.getCurrentValue();
      int r = (int)Math.ceil(reach);
      BlockPos center = player.getBlockPos();
      BlockHitResult best = null;
      double bestDistance = Double.MAX_VALUE;
      for (BlockPos pos : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
         if (!world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) continue;
         for (Direction face : Direction.values()) {
            Vec3d point = Vec3d.ofCenter(pos).add(Vec3d.of(face.getVector()).multiply(0.5));
            double distance = eye.distanceTo(point);
            if (distance > reach || distance >= bestDistance) continue;
            Vec3d beyond = point.add(point.subtract(eye).normalize().multiply(0.01));
            BlockHitResult seen = world.raycast(new RaycastContext(eye, beyond, RaycastContext.ShapeType.OUTLINE,
               RaycastContext.FluidHandling.NONE, player));
            if (seen.getType() == HitResult.Type.BLOCK && seen.getBlockPos().equals(pos) && seen.getSide() == face) {
               best = seen;
               bestDistance = distance;
            }
         }
      }
      return best;
   }

   private void pace() {
      this.nextAt = System.currentTimeMillis() + CombatKit.randomDelay(this.delay);
   }

   /** The slot to explode with: the one held before the glowstone, or the first hotbar slot that is not glowstone. */
   private int explodeSlot() {
      var inventory = CombatKit.mc.player.getInventory();
      if (this.returnSlot >= 0 && !inventory.getStack(this.returnSlot).isOf(Items.GLOWSTONE)) return this.returnSlot;
      int other = CombatKit.hotbar(stack -> !stack.isOf(Items.GLOWSTONE));
      return other >= 0 ? other : inventory.selectedSlot;
   }
}
