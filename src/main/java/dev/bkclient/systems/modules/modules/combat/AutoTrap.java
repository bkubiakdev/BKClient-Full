package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import dev.bkclient.utility.game.TrapPlacementGeometry;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.GameTickEvent;
import dev.bkclient.mixin.accessors.ClientPlayerInteractionManagerAccessor;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.PlacementAnimation;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.Rotation;
import dev.bkclient.utility.rotations.RotationHandler;
import dev.bkclient.utility.rotations.RotationMath;
import dev.bkclient.utility.rotations.RotationPriority;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;

/**
 * SoulClient's AutoTrap placement logic, adapted to BKClient's module and
 * rotation systems. The module builds the sides first and closes the roof last.
 */
@ModuleInfo(name = "Auto Trap", desc = "modules.descriptions.auto_trap", category = ModuleCategory.COMBAT)
public class AutoTrap extends BaseModule {
   public final SelectSetting blocks = new SelectSetting(this, "modules.settings.auto_trap.blocks");

   public AutoTrap() {
      Registries.BLOCK.getIds().stream().sorted(java.util.Comparator.comparing(Object::toString)).forEach(id -> {
         SelectSetting.Value value = new SelectSetting.Value(this.blocks, id.toString());
         if (id.toString().equals("minecraft:obsidian")) value.select();
      });
      this.trapMode.exclusive("Box", "Head", "Legs");
   }

   public final SliderSetting range = new SliderSetting(this, "Range")
      .min(2.0F)
      .max(8.0F)
      .step(0.5F)
      .currentValue(5.5F);
   public final SliderSetting blocksPerTick = new SliderSetting(this, "Blocks Per Tick")
      .min(1.0F)
      .max(6.0F)
      .step(1.0F)
      .currentValue(4.0F);
   public final SliderSetting placeDelay = new SliderSetting(this, "Place Delay")
      .min(0.0F).max(10.0F).step(1.0F).currentValue(0.0F);
   public final ModeSetting placeOrder = new ModeSetting(this, "Place Order");
   public final ModeSetting.Value closest = new ModeSetting.Value(this.placeOrder, "Closest").select();
   public final ModeSetting.Value furthest = new ModeSetting.Value(this.placeOrder, "Furthest");
   public final ModeSetting hitPoint = new ModeSetting(this, "Place Hit Point");
   public final ModeSetting.Value closestHit = new ModeSetting.Value(this.hitPoint, "Closest").select();
   public final ModeSetting.Value randomHit = new ModeSetting.Value(this.hitPoint, "Random");
   public final SelectSetting trapMode = new SelectSetting(this, "Trap Mode").min(1);
   public final SelectSetting.Value box = new SelectSetting.Value(this.trapMode, "Box").select();
   public final SelectSetting.Value head = new SelectSetting.Value(this.trapMode, "Head");
   public final SelectSetting.Value legs = new SelectSetting.Value(this.trapMode, "Legs");
   public final BooleanSetting ignoreFriends = new BooleanSetting(this, "Ignore Friends").enable();
   public final BooleanSetting ignoreSpectators = new BooleanSetting(this, "Ignore Spectators").enable();
   public final BooleanSetting includeInvisibles = new BooleanSetting(this, "Include Invisibles");
   public final BooleanSetting ignoreMoving = new BooleanSetting(this, "Ignore Moving");
   public final BooleanSetting cancelInScreens = new BooleanSetting(this, "Cancel In Screens").enable();
   public final BooleanSetting throughWalls = new BooleanSetting(this, "Through walls").enable();
   public final BooleanSetting instant = new BooleanSetting(this, "Instant");
   public final BooleanSetting swing = new BooleanSetting(this, "Swing").enable();
   public final BooleanSetting placementAnimation = new BooleanSetting(this, "Placement Animation").enable();
   public final ColorSetting placementColor = new ColorSetting(this, "Placement Color").color(Colors.ACCENT);
   private final PlacementAnimation placementEffects = new PlacementAnimation(true);
   private final EventListener<Render3DEvent> onPlacementRender = event -> {
      if (this.placementAnimation.isEnabled()) this.placementEffects.render(event, this.placementColor.getColor());
      else this.placementEffects.clear();
   };

   private final java.util.Map<BlockPos, Integer> retryAfter = new java.util.HashMap<>();
   private int pendingRetries;
   private BlockHitResult pendingHit;
   private Integer scheduledSlot;
   private BlockPos pendingPos;
   private PlayerEntity pendingTarget;
   private net.minecraft.client.world.ClientWorld pendingWorld;
   private BlockHitResult heldHit;
   private Rotation pendingRotation;
   private Rotation heldRotation;
   private Rotation buildRotation;
   private net.minecraft.client.world.ClientWorld buildWorld;
   private int diagnosticTick = -1000;
   private int rejectedRange, rejectedSight, rejectedRotation;
   private int lastPlaceTick;
   private BlockPos targetBase;
   private Direction roofPreferredFace = Direction.NORTH;
   private boolean hasJumpedForRoof;
   private int ticksSinceRoofStart;

   private final EventListener<GameTickEvent> onInputTick = new EventListener<>() {
      @Override public int getPriority() { return 1000; }
      @Override public void onEvent(GameTickEvent event) {
         heldHit = null;
         heldRotation = null;
         if (mc.player == null || mc.world == null || mc.player.isSpectator()
            || (cancelInScreens.isEnabled() && mc.currentScreen != null)) {
            retryAfter.clear();
            resetPending();
            return;
         }
         if (pendingHit != null) executePendingPlacement();
      }
   };

   @Override
   public void onEnable() {
      this.placementEffects.clear();
      this.retryAfter.clear();
      this.buildRotation = null;
      this.buildWorld = null;
      this.resetPending();
      this.heldHit = null;
      this.diagnosticTick = -1000;
      this.rejectedRange = this.rejectedSight = this.rejectedRotation = 0;
      this.lastPlaceTick = mc.player != null ? mc.player.age : -1000;
      this.targetBase = null;
      this.roofPreferredFace = Direction.NORTH;
      this.hasJumpedForRoof = false;
      this.ticksSinceRoofStart = 0;
   }

   @Override
   public void onDisable() {
      this.placementEffects.clear();
      this.retryAfter.clear();
      this.buildRotation = null;
      this.buildWorld = null;
      this.resetPending();
      this.heldHit = null;
      this.lastPlaceTick = -1000;
      this.targetBase = null;
      this.hasJumpedForRoof = false;
      this.ticksSinceRoofStart = 0;
   }

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.player.isSpectator()) {
         this.resetPending();
         return;
      }

      if (this.cancelInScreens.isEnabled() && mc.currentScreen != null) {
         this.resetPending();
         return;
      }

      // Preserve the previous aim as a fallback, but prepare the next placement
      // before this cycle's movement instead of inserting a whole idle tick.
      if (this.heldHit != null) {
         this.applyRotation(this.heldRotation);
      }

      if (this.pendingHit != null) {
         this.rotateToHit(this.pendingHit);
         return;
      }

      PlayerEntity target = this.getClosestPlayer();
      if (target == null) {
         this.targetBase = null;
         return;
      }

      this.updateTargetBase(target);
      if (this.hasJumpedForRoof) {
         ++this.ticksSinceRoofStart;
      }

      Integer slot = this.findBlockSlot();
      if (slot == null) {
         return;
      }

      if (mc.player.age - this.lastPlaceTick < this.placeDelay.getCurrentValue()) {
         return;
      }

      this.retryAfter.entrySet().removeIf(entry -> entry.getValue() <= mc.player.age);
      List<BlockPos> planned = this.getPlacePositions(target);
      // Keep the build aim between placement steps. Returning
      // to camera yaw in every short gap creates repeated mirrored turns.
      if (this.buildRotation != null && this.buildWorld == mc.world
         && planned.stream().anyMatch(pos -> mc.world.getBlockState(pos).isReplaceable())) {
         this.applyRotation(this.buildRotation);
      }
      boolean prepared = false;
      for (BlockPos pos : planned) {
         if (this.retryAfter.containsKey(pos) || !this.canPlaceAt(pos)) {
            continue;
         }

         BlockHitResult hit = this.findHit(pos);
         if (hit == null) {
            continue;
         }

         this.rotateToHit(hit);
         this.pendingRotation = this.placementRotation(hit);
         this.pendingHit = hit;
         this.pendingPos = pos;
         this.scheduledSlot = slot;
         this.pendingTarget = target;
         this.pendingWorld = mc.world;
         // Synchronize the selected stack in the preparation cycle, before movement.
         this.selectSlot(slot);
         prepared = true;
         break;
      }
      if ((!prepared || this.rejectedRotation > 0) && mc.player.age - this.diagnosticTick >= 100) {
         this.diagnosticTick = mc.player.age;
         org.slf4j.LoggerFactory.getLogger("BKClient-AutoTrap").info(
            "targetDistance={} blockReach={} prepared={} rangeRejected={} sightRejected={} rotationRejected={}",
            mc.player.distanceTo(target), mc.player.getBlockInteractionRange(), prepared,
            this.rejectedRange, this.rejectedSight, this.rejectedRotation);
         this.rejectedRange = this.rejectedSight = this.rejectedRotation = 0;
      }
   }

   private void updateTargetBase(PlayerEntity target) {
      if (this.targetBase == null) {
         this.targetBase = target.getBlockPos();
         this.hasJumpedForRoof = false;
         this.ticksSinceRoofStart = 0;
         this.updateRoofPreferredFace();
         return;
      }

      Vec3d baseCenter = Vec3d.ofCenter(this.targetBase);
      Vec3d targetPos = target.getPos();
      double dx = targetPos.x - baseCenter.x;
      double dz = targetPos.z - baseCenter.z;
      if (dx * dx + dz * dz > 1.0) {
         BlockPos newBase = target.getBlockPos();
         this.targetBase = new BlockPos(newBase.getX(), newBase.getY(), newBase.getZ());
         this.hasJumpedForRoof = false;
         this.ticksSinceRoofStart = 0;
         this.updateRoofPreferredFace();
      }
   }

   private void executePendingPlacement() {
      if (this.pendingHit == null || mc.player == null || mc.world == null || mc.interactionManager == null) {
         this.resetPending();
         return;
      }

      BlockPos checkPos = this.pendingPos;
      if (this.pendingWorld != mc.world || this.pendingTarget == null || !this.pendingTarget.isAlive()
         || mc.player.squaredDistanceTo(this.pendingTarget) > this.range.getCurrentValue() * this.range.getCurrentValue()
         || checkPos == null || !this.getPlacePositions(this.pendingTarget).contains(checkPos)
         || !this.canPlaceAt(checkPos) || !this.isHitValid(this.pendingHit, checkPos)) {
         this.resetPending();
         return;
      }
      if (!this.serverLooksAt(this.pendingHit)) {
         // Keep the same candidate/aim during convergence instead of picking a
         // fresh random hit point each tick and restarting rotation forever.
         if (++this.pendingRetries >= 3) {
            this.retryAfter.put(checkPos, mc.player.age + 4);
            this.resetPending();
         }
         return;
      }
      Integer slot = this.scheduledSlot;
      if (slot == null || mc.player.getInventory().selectedSlot != slot
         || !this.isValidBlock(mc.player.getInventory().getStack(slot))) {
         this.resetPending();
         return;
      }
      this.heldHit = this.pendingHit;
      this.heldRotation = this.pendingRotation;
      ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, this.pendingHit);
      if (result.isAccepted()) {
         this.buildRotation = this.pendingRotation;
         this.buildWorld = mc.world;
         if (this.placementAnimation.isEnabled()) this.placementEffects.add(checkPos);
         if (this.swing.isEnabled()) mc.player.swingHand(Hand.MAIN_HAND);
         this.lastPlaceTick = mc.player.age;
         if (this.instant.isEnabled()) this.handleInstantMode(this.pendingTarget, slot);
      } else {
         // An unusable support or denied block must not starve the rest of the box.
         this.retryAfter.put(checkPos, mc.player.age + 4);
      }
      this.resetPending();
   }

   private void handleInstantMode(PlayerEntity target, int blockSlot) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }
      if (!this.isValidBlock(mc.player.getInventory().getStack(blockSlot))) {
         return;
      }
      int cap = Math.max(1, (int)this.blocksPerTick.getCurrentValue());
      int placed = 1; // The prepared placement already consumed one position in this cycle.
      for (BlockPos pos : this.getPlacePositions(target)) {
         if (placed >= cap) {
            break;
         }
         if (this.retryAfter.containsKey(pos) || !this.canPlaceAt(pos)) {
            continue;
         }

         BlockHitResult hit = this.findHit(pos);
         if (hit == null || !this.serverLooksAt(hit)) {
            continue;
         }

         // A previous placement may have consumed the stack; only selected blocks may replace it.
         if (!this.isValidBlock(mc.player.getInventory().getStack(blockSlot))) {
            return; // Prepare a new stack in the next cycle instead of swapping mid-burst.
         }
         this.rotateToHit(hit);
         ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
         if (!result.isAccepted()) {
            continue;
         }
         if (this.placementAnimation.isEnabled()) this.placementEffects.add(pos);
         if (this.swing.isEnabled()) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }
         ++placed;
      }
      if (placed > 0) {
         this.lastPlaceTick = mc.player.age;
      }
   }

   private void selectSlot(int slot) {
      if (mc.player.getInventory().selectedSlot != slot) {
         mc.player.getInventory().selectedSlot = slot;
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).aura$setLastSelectedSlot(slot);
      }
   }

   private void rotateToHit(BlockHitResult hit) {
      this.applyRotation(this.placementRotation(hit));
   }

   private Rotation placementRotation(BlockHitResult hit) {
      // Aim before movement/input correction, accounting for the vertical motion
      // already scheduled by vanilla. Changing yaw after movement breaks its basis.
      double verticalMotion = mc.player.isOnGround() ? 0.0 : mc.player.getVelocity().y;
      return RotationMath.getRotationTo(hit.getPos().add(0, -verticalMotion, 0));
   }

   private void applyRotation(Rotation requested) {
      if (requested == null) return;
      Rotation rotation = new Rotation(requested.getYaw(), requested.getPitch());
      if (dev.bkclient.combatengine.bridge.AuraRuntime.bkengineOwnsRotation()) {
         dev.bkclient.combatengine.bkengine.manager.rotation.RotationProcess.update(
            new dev.bkclient.combatengine.bkengine.manager.rotation.Rotation(rotation.getYaw(), rotation.getPitch()),
            180F, 180F, 180F, 180F, 1, 20, false);
         return;
      }
      if (dev.bkclient.combatengine.bridge.AuraRuntime.ownsRotation()) {
         dev.bkclient.combatengine.api.storages.implement.RotationStorage.update(
            new dev.bkclient.combatengine.api.utils.rotate.Rotation(rotation.getYaw(), rotation.getPitch()),
            180F, 180F, 1, 20);
         return;
      }
      RotationHandler handler = BKClientInit.getInstance().getRotationHandler();
      handler.rotate(rotation, MoveCorrection.SILENT, 180.0F, 180.0F, 180.0F, RotationPriority.USE_ITEM);
      // Use one rotation owner. Mutating player yaw through RotationStorage as
      // well made movement correction and rendered/server angles disagree.
      handler.update();
   }

   private void resetPending() {
      this.pendingRetries = 0;
      this.pendingHit = null;
      this.scheduledSlot = null;
      this.pendingPos = null;
      this.pendingTarget = null;
      this.pendingWorld = null;
      this.pendingRotation = null;
   }

   /** Lets other placement modules yield without changing the trap sequence. */
   public boolean isPlacingBlock() {
      return this.isEnabled() && (this.pendingHit != null || mc.player != null && this.lastPlaceTick == mc.player.age);
   }

   private List<BlockPos> getPlacePositions(PlayerEntity target) {
      List<BlockPos> result = new ArrayList<>();
      List<BlockPos> playerBlocks = this.getPlayerBlocks(target);

      for (BlockPos playerBlock : playerBlocks) {
         for (Direction direction : Direction.values()) {
            if (!direction.getAxis().isHorizontal()) {
               continue;
            }
            BlockPos side = playerBlock.offset(direction);
            if (!playerBlocks.contains(side) && !result.contains(side)) {
               result.add(side);
               BlockPos sideUp = side.up();
               if (!playerBlocks.contains(sideUp) && !result.contains(sideUp)) {
                  result.add(sideUp);
               }
            }
         }
      }

      int minX = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxZ = Integer.MIN_VALUE;
      int baseY = Integer.MAX_VALUE;
      for (BlockPos pos : playerBlocks) {
         minX = Math.min(minX, pos.getX());
         maxX = Math.max(maxX, pos.getX());
         minZ = Math.min(minZ, pos.getZ());
         maxZ = Math.max(maxZ, pos.getZ());
         baseY = Math.min(baseY, pos.getY());
      }

      int centerX = Math.floorDiv(minX + maxX, 2);
      int centerZ = Math.floorDiv(minZ + maxZ, 2);
      int helperX = centerX;
      int helperZ = centerZ;
      double dx = centerX + 0.5 - mc.player.getX();
      double dz = centerZ + 0.5 - mc.player.getZ();
      if (Math.abs(dx) > Math.abs(dz)) helperX = dx > 0.0 ? maxX + 1 : minX - 1;
      else helperZ = dz > 0.0 ? maxZ + 1 : minZ - 1;

      // Every roof row needs a support on its far end. A single bridge left
      // half of a 2x2 footprint open because its other face points away from us.
      if (Math.abs(dx) > Math.abs(dz)) {
         for (int z = minZ; z <= maxZ; ++z) this.addUnique(result, new BlockPos(helperX, baseY + 2, z));
      } else {
         for (int x = minX; x <= maxX; ++x) this.addUnique(result, new BlockPos(x, baseY + 2, helperZ));
      }
      this.addUnique(result, new BlockPos(centerX, baseY + 2, centerZ));
      for (int x = minX; x <= maxX; ++x) {
         for (int z = minZ; z <= maxZ; ++z) {
            if (x != centerX || z != centerZ) {
               this.addUnique(result, new BlockPos(x, baseY + 2, z));
            }
         }
      }
      final int trapBaseY = baseY;
      result.removeIf(pos -> this.legs.isSelected() && pos.getY() > trapBaseY
         || this.head.isSelected() && pos.getY() < trapBaseY + 1);
      Comparator<BlockPos> distanceOrder = this.furthest.isSelected()
         ? Comparator.comparingDouble((BlockPos pos) -> mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))).reversed()
         : Comparator.comparingDouble(pos -> mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)));
      List<BlockPos> unfinished = result.stream().filter(this::canPlaceAt).toList();
      java.util.Map<BlockPos, Integer> obstruction = new java.util.HashMap<>();
      for (BlockPos pos : result) {
         obstruction.put(pos, TrapPlacementGeometry.obstructionCost(pos, unfinished, mc.player.getEyePos()));
      }
      result.sort(Comparator
         .comparingInt((BlockPos pos) -> obstruction.get(pos))
         .thenComparingInt(pos -> pos.getY() - trapBaseY)
         .thenComparing(distanceOrder));
      return result;
   }

   private void addUnique(List<BlockPos> positions, BlockPos pos) {
      if (!positions.contains(pos)) {
         positions.add(pos);
      }
   }

   private List<BlockPos> getPlayerBlocks(PlayerEntity entity) {
      return TrapPlacementGeometry.footprint(entity.getBoundingBox());
   }

   private BlockHitResult findHit(BlockPos pos) {
      Vec3d eye = mc.player.getEyePos();
      boolean roof = this.targetBase != null && pos.getY() - this.targetBase.getY() >= 2;
      Direction[] order = roof
         ? new Direction[]{this.roofPreferredFace, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN, Direction.UP}
         : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN, Direction.UP};

      BlockHitResult best = null;
      double bestDistance = Double.POSITIVE_INFINITY;
      for (Direction face : order) {
         BlockPos neighbor = pos.offset(face.getOpposite());
         if (mc.world.getBlockState(neighbor).isReplaceable()
            || !this.isFaceVisible(neighbor, face, eye)) {
            continue;
         }
         Vec3d point = this.randomHit.isSelected()
            ? this.placementPoint(neighbor, face, eye) : this.faceCenter(neighbor, face);
         BlockHitResult hit = new BlockHitResult(point, face, neighbor, false);
         if (this.isHitValid(hit, pos) && eye.squaredDistanceTo(hit.getPos()) < bestDistance) {
            return hit;
         }
         // A visible edge can remain reachable when both the nearest point and
         // center are occluded. Sample the actual outline, including slabs.
         for (Box shape : mc.world.getBlockState(neighbor).getOutlineShape(mc.world, neighbor).getBoundingBoxes()) {
            for (Vec3d sample : TrapPlacementGeometry.faceSamples(shape.offset(neighbor), face)) {
               hit = new BlockHitResult(sample, face, neighbor, false);
               double distance = eye.squaredDistanceTo(sample);
               if (distance < bestDistance && this.isHitValid(hit, pos)) {
                  best = hit;
                  bestDistance = distance;
               }
            }
         }
         // A partial support can occlude the nearest point; also try its center.
         hit = new BlockHitResult(this.faceCenter(neighbor, face), face, neighbor, false);
         if (this.isHitValid(hit, pos) && eye.squaredDistanceTo(hit.getPos()) < bestDistance) {
            best = hit;
            bestDistance = eye.squaredDistanceTo(hit.getPos());
         }
      }
      return best;
   }

   private Vec3d placementPoint(BlockPos support, Direction face, Vec3d eye) {
      Vec3d center = this.faceCenter(support, face);
      double x = Math.max(support.getX() + 0.05, Math.min(support.getX() + 0.95, eye.x));
      double y = Math.max(support.getY() + 0.05, Math.min(support.getY() + 0.95, eye.y));
      double z = Math.max(support.getZ() + 0.05, Math.min(support.getZ() + 0.95, eye.z));
      if (this.randomHit.isSelected()) {
         x = support.getX() + ThreadLocalRandom.current().nextDouble(0.15, 0.85);
         y = support.getY() + ThreadLocalRandom.current().nextDouble(0.15, 0.85);
         z = support.getZ() + ThreadLocalRandom.current().nextDouble(0.15, 0.85);
      }
      return new Vec3d(face.getAxis() == Direction.Axis.X ? center.x : x,
         face.getAxis() == Direction.Axis.Y ? center.y : y,
         face.getAxis() == Direction.Axis.Z ? center.z : z);
   }

   private boolean isHitValid(BlockHitResult hit, BlockPos destination) {
      double reach = mc.player.getBlockInteractionRange();
      if (!hit.getBlockPos().offset(hit.getSide()).equals(destination)
         || mc.world.getBlockState(hit.getBlockPos()).isReplaceable()
         || !this.isFaceVisible(hit.getBlockPos(), hit.getSide(), mc.player.getEyePos())) return false;
      if (mc.player.getEyePos().squaredDistanceTo(hit.getPos()) > reach * reach) {
         this.rejectedRange++;
         return false;
      }
      if (this.throughWalls.isEnabled()) return true;
      BlockHitResult visible = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), hit.getPos(),
         RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
      boolean valid = visible.getType() == HitResult.Type.BLOCK && visible.getBlockPos().equals(hit.getBlockPos())
         && visible.getSide() == hit.getSide();
      if (!valid) this.rejectedSight++;
      return valid;
   }

   private boolean serverLooksAt(BlockHitResult hit) {
      Rotation rotation = BKClientInit.getInstance().getRotationHandler().getServerRotation();
      Vec3d eye = mc.player.getEyePos();
      Vec3d end = eye.add(Vec3d.fromPolar(rotation.getPitch(), rotation.getYaw())
         .multiply(mc.player.getBlockInteractionRange()));
      if (this.throughWalls.isEnabled()) {
         // A placement may hit anywhere on the requested face. Requiring a
         // two-degree cone around one point stalled placement during walking.
         BlockHitResult faceHit = mc.world.getBlockState(hit.getBlockPos())
            .getOutlineShape(mc.world, hit.getBlockPos()).raycast(eye, end, hit.getBlockPos());
         boolean aligned = faceHit != null && faceHit.getSide() == hit.getSide();
         if (!aligned) this.rejectedRotation++;
         return aligned;
      }
      BlockHitResult seen = mc.world.raycast(new RaycastContext(eye, end,
         RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
      boolean valid = seen.getType() == HitResult.Type.BLOCK && seen.getBlockPos().equals(hit.getBlockPos())
         && seen.getSide() == hit.getSide();
      if (!valid) this.rejectedRotation++;
      return valid;
   }

   private Vec3d faceCenter(BlockPos neighbor, Direction face) {
      return Vec3d.ofCenter((Vec3i)neighbor).add(
         face.getOffsetX() * 0.499,
         face.getOffsetY() * 0.499,
         face.getOffsetZ() * 0.499
      );
   }

   private boolean isFaceVisible(BlockPos neighbor, Direction face, Vec3d eye) {
      double epsilon = 0.0311;
      return switch (face) {
         case DOWN -> eye.y <= neighbor.getY() + epsilon;
         case UP -> eye.y >= neighbor.getY() + 1 - epsilon;
         case NORTH -> eye.z <= neighbor.getZ() + epsilon;
         case SOUTH -> eye.z >= neighbor.getZ() + 1 - epsilon;
         case WEST -> eye.x <= neighbor.getX() + epsilon;
         case EAST -> eye.x >= neighbor.getX() + 1 - epsilon;
      };
   }

   private boolean canPlaceAt(BlockPos pos) {
      if (mc.world == null || !mc.world.getBlockState(pos).isReplaceable()) {
         return false;
      }
      Integer slot = this.findBlockSlot();
      if (slot == null) return false;
      BlockItem item = (BlockItem) mc.player.getInventory().getStack(slot).getItem();
      return mc.world.canPlace(item.getBlock().getDefaultState(), pos, net.minecraft.block.ShapeContext.absent());
   }

   private void updateRoofPreferredFace() {
      if (mc.player == null || this.targetBase == null) {
         return;
      }
      Vec3d eye = mc.player.getEyePos();
      Vec3d center = Vec3d.ofCenter(this.targetBase);
      double dx = eye.x - center.x;
      double dz = eye.z - center.z;
      if (Math.abs(dx) > Math.abs(dz)) {
         this.roofPreferredFace = dx > 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         this.roofPreferredFace = dz > 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private boolean isValidBlock(ItemStack stack) {
      if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
         return false;
      }
      String id = Registries.BLOCK.getId(blockItem.getBlock()).toString();
      return this.blocks.getSelectedValues().stream().anyMatch(value -> value.getName().equals(id));
   }

   private Integer findBlockSlot() {
      for (int slot = 0; slot < 9; ++slot) {
         if (this.isValidBlock(mc.player.getInventory().getStack(slot))) {
            return slot;
         }
      }
      return null;
   }

   private PlayerEntity getClosestPlayer() {
      double closestDistance = this.range.getCurrentValue() * this.range.getCurrentValue();
      PlayerEntity closest = null;
      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player == mc.player || !player.isAlive()
            || this.ignoreSpectators.isEnabled() && player.isSpectator()
            || !this.includeInvisibles.isEnabled() && player.isInvisible()
            || this.ignoreMoving.isEnabled() && player.getVelocity().lengthSquared() >= 0.0025
            || this.ignoreFriends.isEnabled() && BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString())) {
            continue;
         }
         double distance = mc.player.squaredDistanceTo((Entity)player);
         if (distance < closestDistance) {
            closestDistance = distance;
            closest = player;
         }
      }
      return closest;
   }
}
