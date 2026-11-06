package dev.bkclient.systems.modules.modules.player.autoleave;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.player.InputEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Mine a capacity-limited batch, collect it, then deposit and verify the whole batch. */
public final class ShulkerStash implements IMinecraft {
   public enum Result { RUNNING, DONE, FAILED, CANCELLED }
   private enum Phase { PREPARE, PLACE, OPEN_SPACE, MINE, COLLECT, OPEN_DEPOSIT, TRANSFER, REOPEN, VERIFY }
   private record Contents(int syncId, List<ItemStack> stacks) {}
   private static final Set<String> CONFLICTS = Set.of("Nuker", "MineHelper", "Scaffold", "InventorySet", "TinyTask", "AutoTrap", "Surround");
   private final ArrayDeque<BlockPos> targets = new ArrayDeque<>();
   private ClientWorld world;
   private Object connection;
   private BlockPos origin, chest, target;
   private int radius, phaseTicks, originalSlot, swapSource = -1, stored, openedSyncId = -1;
   private long deadline;
   private Phase phase;
   private volatile Result result = Result.CANCELLED;
   private Vec3d walkTo;
   private boolean jump, toolPrepared;
   private float yawVelocity, pitchVelocity;
   private net.minecraft.item.Item leasedItem;
   private List<ItemStack> beforePickup = List.of();
   private ItemStack swappedHotbar = ItemStack.EMPTY;
   private final List<BlockPos> mined = new ArrayList<>();
   private final List<ItemStack> batchItems = new ArrayList<>();
   private final ArrayDeque<ItemStack> transfers = new ArrayDeque<>();
   private List<ItemStack> depositBefore = List.of();
   private int chestCapacity, batchLimit, openRetry;
   private volatile Contents contents;
   private String failure;

   public void start(int radius, int maxSeconds) {
      reset();
      this.world = mc.world;
      this.connection = mc.getNetworkHandler();
      this.radius = radius;
      this.deadline = System.nanoTime() + maxSeconds * 1_000_000_000L;
      this.result = Result.RUNNING;
      if (mc.player == null || world == null || mc.interactionManager == null) { result = Result.CANCELLED; return; }
      this.originalSlot = mc.player.getInventory().selectedSlot;
      this.origin = mc.player.getBlockPos().toImmutable();
      if (mc.player.isCreative() || mc.player.isSpectator() || mc.player.hasVehicle()) { fail("unsupported"); return; }
      var market = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.bkmods.AutoMarket.class);
      if (market != null) market.prepareForAutoLeave();
      if (!closePreviousScreen()) { fail("busy"); return; }
      for (var module : BKClientInit.getInstance().getModuleManager().getModules()) {
         if (module.isEnabled() && CONFLICTS.contains(module.getClass().getSimpleName())) { fail("conflict"); return; }
      }
      if (dev.bkclient.combatengine.bridge.AuraRuntime.auraEnabled()) { fail("conflict"); return; }
      List<BlockPos> found = new ArrayList<>();
      for (BlockPos pos : BlockPos.iterate(origin.add(-radius, -radius, -radius), origin.add(radius, radius, radius))) {
         if (world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) found.add(pos.toImmutable());
      }
      found.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(origin)));
      targets.addAll(found);
      if (targets.isEmpty()) { result = Result.DONE; return; }
      if (!hasInventorySpace()) { fail("inventory_full"); return; }
      chest = findChest();
      change(Phase.PREPARE);
      MessageUtility.info(Text.of(Localizator.translate("modules.auto_leave.stash.start", targets.size())));
   }

   public boolean isActive() { return result == Result.RUNNING; }
   public int stored() { return stored; }
   public String failure() { return failure; }

   public void onPacket(Packet<?> packet) {
      if (isActive() && packet instanceof InventoryS2CPacket inventory && inventory.getSyncId() != 0) {
         contents = new Contents(inventory.getSyncId(), inventory.getContents().stream().map(ItemStack::copy).toList());
      }
   }

   public Result tick() {
      if (!isActive()) return result;
      walkTo = null;
      jump = false;
      if (mc.world != world || mc.getNetworkHandler() != connection || mc.player == null || !mc.player.isAlive()) {
         result = Result.CANCELLED;
         return result;
      }
      if (System.nanoTime() >= deadline) { fail("timeout"); return result; }
      // A held mouse button must not mine a second box or use an item during this sequence.
      mc.options.attackKey.setPressed(false);
      mc.options.useKey.setPressed(false);
      if (!inside(mc.player.getBlockPos(), 2)) { fail("area"); return result; }
      phaseTicks++;
      if (phaseTicks > 160) { fail("blocked"); return result; }
      switch (phase) {
         case PREPARE -> prepare();
         case PLACE -> {
            if (world.getBlockState(chest).isOf(Blocks.ENDER_CHEST)) {
               restoreHeld();
               change(Phase.OPEN_SPACE);
            } else if (phaseTicks > 40) fail("no_chest");
         }
         case OPEN_SPACE -> {
            Contents server = openChest();
            if (server != null) {
               if (freeChestSlots(server) == 0) { fail("chest_full"); break; }
               chestCapacity = freeChestSlots(server);
               closeChest();
               beginBatch();
            }
         }
         case MINE -> mine();
         case COLLECT -> collect();
         case OPEN_DEPOSIT -> {
            Contents server = openChest();
            if (server != null) deposit(server);
         }
         case TRANSFER -> transfer();
         case REOPEN -> {
            // A click is predicted locally. Closing and reopening requests authoritative contents.
            if (phaseTicks >= 2) {
               closeChest();
               change(Phase.VERIFY);
            }
         }
         case VERIFY -> {
            Contents server = openChest();
            if (server != null) {
               int confirmed = confirmedBatchCount(server.stacks());
               stored += confirmed;
               if (confirmed != batchItems.size()) {
                  fail("unconfirmed");
                  break;
               }
               chestCapacity = freeChestSlots(server);
               closeChest();
               if (targets.isEmpty()) result = Result.DONE;
               else if (chestCapacity == 0) fail("chest_full");
               else beginBatch();
            }
         }
      }
      return result;
   }

   private void prepare() {
      if (chest != null) { change(Phase.OPEN_SPACE); return; }
      int item = findEnderChest();
      if (item < 0) { fail("no_chest"); return; }
      BlockHitResult placement = null;
      // Place on an ordinary solid floor, away from the player and the boxes to collect.
      for (BlockPos candidate : BlockPos.iterate(origin.add(-2, -1, -2), origin.add(2, 1, 2))) {
         if (!world.getBlockState(candidate).isAir() || targets.contains(candidate)
            || !world.getBlockState(candidate.up()).isAir()
            || !Block.isShapeFullCube(world.getBlockState(candidate.down()).getCollisionShape(world, candidate.down()))
            || !world.getOtherEntities(null, new Box(candidate)).isEmpty()) continue;
         BlockPos floor = candidate.down();
         Vec3d point = Vec3d.ofCenter(floor).add(0, 0.5, 0);
         BlockHitResult hit = ray(mc.player.getEyePos(), point.add(0, -0.01, 0));
         if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(floor)
            && hit.getSide() == Direction.UP && inReach(point)) {
            placement = new BlockHitResult(point, Direction.UP, floor, false);
            break;
         }
      }
      if (placement == null) { fail("no_chest"); return; }
      if (!aim(placement.getPos())) return;
      chest = placement.getBlockPos().up().toImmutable();
      equip(item);
      if (!mc.player.getMainHandStack().isOf(Items.ENDER_CHEST)) { fail("no_chest"); return; }
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, placement);
      mc.player.swingHand(Hand.MAIN_HAND);
      change(Phase.PLACE);
   }

   private void beginBatch() {
      mined.clear();
      batchItems.clear();
      transfers.clear();
      depositBefore = List.of();
      beforePickup = playerShulkers();
      int space = 0;
      for (int i = 0; i < 36; i++) if (mc.player.getInventory().getStack(i).isEmpty()) space++;
      batchLimit = Math.min(space, chestCapacity);
      if (batchLimit == 0) { fail(space == 0 ? "inventory_full" : "chest_full"); return; }
      nextBox();
   }

   private void nextBox() {
      // Reserve one inventory and chest slot for every mined box, including drops still on the ground.
      if (mined.size() >= batchLimit || !hasInventorySpace()) { finishMining(); return; }
      while (!targets.isEmpty()) {
         // Prefer the next reachable box near the current view instead of repeatedly turning across the area.
         BlockPos nearest = targets.stream().min(Comparator.comparingDouble(pos ->
            mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) + turnCost(pos))).orElseThrow();
         targets.remove(nearest);
         target = nearest;
         if (world.getBlockState(target).getBlock() instanceof ShulkerBoxBlock) {
            change(Phase.MINE);
            return;
         }
      }
      if (mined.isEmpty()) result = Result.DONE;
      else finishMining();
   }

   private double turnCost(BlockPos pos) {
      Vec3d delta = Vec3d.ofCenter(pos).subtract(mc.player.getEyePos());
      float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90);
      return Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw())) * 0.025;
   }

   private void finishMining() {
      mc.interactionManager.cancelBlockBreaking();
      restoreHeld();
      toolPrepared = false;
      if (mined.isEmpty()) { fail("inventory_full"); return; }
      change(Phase.COLLECT);
   }

   private void mine() {
      if (!(world.getBlockState(target).getBlock() instanceof ShulkerBoxBlock)) {
         mc.interactionManager.cancelBlockBreaking();
         mined.add(target);
         nextBox();
         return;
      }
      BlockHitResult hit = reachableHit(target);
      if (hit == null) { approachBlock(target); return; }
      if (!hasInventorySpace()) { targets.addFirst(target); finishMining(); return; }
      if (!toolPrepared) {
      float bestSpeed = 1;
      int bestSlot = originalSlot;
      ItemStack original = mc.player.getInventory().getStack(originalSlot);
      if (original.isDamageable() && original.getMaxDamage() - original.getDamage() <= 1) {
         bestSlot = -1;
         for (int slot = 0; slot < 36; slot++) {
            if (!mc.player.getInventory().getStack(slot).isDamageable()) { bestSlot = slot; break; }
         }
         if (bestSlot < 0) { fail("blocked"); return; }
      }
      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() <= 1) continue;
         float speed = stack.getMiningSpeedMultiplier(world.getBlockState(target));
         if (speed > bestSpeed) { bestSlot = slot; bestSpeed = speed; }
      }
      equip(bestSlot);
      toolPrepared = true;
      }
      if (!aim(hit.getPos())) return;
      mc.interactionManager.updateBlockBreakingProgress(target, hit.getSide());
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   private void collect() {
      batchItems.clear();
      List<ItemStack> current = playerShulkers();
      // Count differences by full item components, including multiple identical boxes.
      for (ItemStack item : current) {
         if (batchItems.stream().anyMatch(added -> same(added, item))) continue;
         int added = countSame(current, item) - countSame(beforePickup, item);
         for (int i = 0; i < added && batchItems.size() < mined.size(); i++) batchItems.add(item.copyWithCount(1));
      }
      if (batchItems.size() == mined.size()) { change(Phase.OPEN_DEPOSIT); return; }
      if (!hasInventorySpace()) { fail("inventory_full"); return; }
      List<ItemEntity> drops = world.getEntitiesByClass(ItemEntity.class,
         new Box(origin).expand(radius + 2), item -> item.isAlive() && isShulker(item.getStack())
            && mined.stream().anyMatch(pos -> item.squaredDistanceTo(Vec3d.ofCenter(pos)) < 4));
      drops.sort(Comparator.comparingDouble(item -> item.squaredDistanceTo(mc.player)));
      if (!drops.isEmpty()) {
         Vec3d drop = drops.getFirst().getPos();
         if (mc.player.squaredDistanceTo(drop) > 0.35) approach(drop);
      } else if (phaseTicks > 60) fail("pickup");
   }

   private Contents openChest() {
      if (!world.getBlockState(chest).isOf(Blocks.ENDER_CHEST)) { fail("no_chest"); return null; }
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
         if (handler.getRows() != 3 || openedSyncId == -1) { fail("busy"); return null; }
         Contents server = contents;
         if (server != null && server.syncId() == handler.syncId && server.stacks().size() == 63) return server;
         return null;
      }
      if (mc.player.currentScreenHandler != mc.player.playerScreenHandler || mc.currentScreen != null) {
         fail("busy");
         return null;
      }
      BlockHitResult hit = reachableHit(chest);
      if (hit == null) { approachBlock(chest); return null; }
      if (!aim(hit.getPos())) return null;
      if (openRetry-- <= 0) {
         openRetry = 10;
         contents = null;
         openedSyncId = 0; // An opening request, not an arbitrary chest the user happened to open.
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
         mc.player.swingHand(Hand.MAIN_HAND);
      }
      return null;
   }

   private void deposit(Contents server) {
      if (freeChestSlots(server) < batchItems.size()) { fail("chest_full"); return; }
      if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) { fail("busy"); return; }
      depositBefore = server.stacks();
      for (ItemStack item : batchItems) {
         if (countSame(depositBefore.subList(27, 63), item) < countSame(batchItems, item)) { fail("pickup"); return; }
      }
      transfers.addAll(batchItems);
      change(Phase.TRANSFER);
   }

   private void transfer() {
      if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)
         || handler.getRows() != 3 || openedSyncId < 0 || !handler.getCursorStack().isEmpty()) { fail("busy"); return; }
      if (transfers.isEmpty()) { change(Phase.REOPEN); return; }
      ItemStack item = transfers.removeFirst();
      for (int slot = 27; slot < 63; slot++) {
         if (same(handler.getSlot(slot).getStack(), item)) {
            // One ordinary shift-click each tick; the whole batch shares the same open EC.
            mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
            return;
         }
      }
      fail("pickup");
   }

   private int confirmedBatchCount(List<ItemStack> authoritative) {
      if (authoritative.size() != 63 || depositBefore.size() != 63) return 0;
      int confirmed = 0;
      List<ItemStack> seen = new ArrayList<>();
      for (ItemStack item : batchItems) {
         if (seen.stream().anyMatch(other -> same(other, item))) continue;
         seen.add(item);
         int expected = countSame(batchItems, item);
         int chestGain = countSame(authoritative.subList(0, 27), item) - countSame(depositBefore.subList(0, 27), item);
         int playerLoss = countSame(depositBefore.subList(27, 63), item) - countSame(authoritative.subList(27, 63), item);
         confirmed += Math.max(0, Math.min(expected, Math.min(chestGain, playerLoss)));
      }
      return confirmed;
   }

   public static ItemStack addedShulker(List<ItemStack> before, List<ItemStack> after) {
      for (ItemStack stack : after) {
         if (isShulker(stack) && countSame(after, stack) > countSame(before, stack)) return stack.copyWithCount(1);
      }
      return ItemStack.EMPTY;
   }

   private static int countSame(List<ItemStack> stacks, ItemStack target) {
      return stacks.stream().filter(stack -> same(stack, target)).mapToInt(ItemStack::getCount).sum();
   }

   private static boolean same(ItemStack a, ItemStack b) {
      return !a.isEmpty() && !b.isEmpty() && ItemStack.areItemsAndComponentsEqual(a, b);
   }

   private static boolean isShulker(ItemStack stack) {
      return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock;
   }

   private List<ItemStack> playerShulkers() {
      List<ItemStack> found = new ArrayList<>();
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (isShulker(stack)) found.add(stack.copy());
      }
      return found;
   }

   private boolean hasInventorySpace() {
      for (int i = 0; i < 36; i++) if (mc.player.getInventory().getStack(i).isEmpty()) return true;
      return false;
   }

   private int freeChestSlots(Contents server) {
      return (int) server.stacks().subList(0, 27).stream().filter(ItemStack::isEmpty).count();
   }

   private int findEnderChest() {
      for (int i = 0; i < 36; i++) if (mc.player.getInventory().getStack(i).isOf(Items.ENDER_CHEST)) return i;
      return -1;
   }

   private BlockPos findChest() {
      BlockPos found = null;
      for (BlockPos pos : BlockPos.iterate(origin.add(-radius - 1, -2, -radius - 1), origin.add(radius + 1, 2, radius + 1))) {
         if (world.getBlockState(pos).isOf(Blocks.ENDER_CHEST) && !world.getBlockState(pos.up()).isSolidBlock(world, pos.up())
            && (found == null || pos.getSquaredDistance(origin) < found.getSquaredDistance(origin))) found = pos.toImmutable();
      }
      return found;
   }

   private boolean inReach(Vec3d point) {
      double reach = Math.min(4.0, mc.player.getBlockInteractionRange());
      return mc.player.getEyePos().squaredDistanceTo(point) < reach * reach;
   }

   private BlockHitResult ray(Vec3d eyes, Vec3d end) {
      return world.raycast(new RaycastContext(eyes, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
   }

   private BlockHitResult reachableHit(BlockPos block) {
      return hitFrom(mc.player.getEyePos(), block);
   }

   private BlockHitResult hitFrom(Vec3d eyes, BlockPos block) {
      double reach = Math.min(4.0, mc.player.getBlockInteractionRange());
      for (Direction face : Direction.values()) {
         Vec3d point = Vec3d.ofCenter(block).add(Vec3d.of(face.getVector()).multiply(0.49));
         if (eyes.squaredDistanceTo(point) > reach * reach) continue;
         BlockHitResult hit = ray(eyes, point);
         if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(block)) return hit;
      }
      return null;
   }

   private void approachBlock(BlockPos block) {
      navigate(pos -> hitFrom(Vec3d.ofBottomCenter(pos).add(0, mc.player.getStandingEyeHeight(), 0), block) != null);
   }

   private void approach(Vec3d point) {
      if (mc.player.getPos().squaredDistanceTo(point) < 0.35) return;
      navigate(pos -> Vec3d.ofBottomCenter(pos).squaredDistanceTo(point) < 1.2);
      // Once in the destination cell, walk to the actual item rather than stopping at its boundary.
      if (walkTo == null && mc.player.getBlockPos().equals(BlockPos.ofFloored(point)) && standable(mc.player.getBlockPos())) {
         walkTo = new Vec3d(point.x, mc.player.getY(), point.z);
      }
   }

   private void navigate(Predicate<BlockPos> goal) {
      BlockPos start = mc.player.getBlockPos();
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      Map<BlockPos, BlockPos> previous = new HashMap<>();
      queue.add(start);
      previous.put(start, start);
      BlockPos end = null;
      while (!queue.isEmpty() && previous.size() < 800) {
         BlockPos pos = queue.removeFirst();
         if (goal.test(pos)) { end = pos; break; }
         for (Direction dir : Direction.Type.HORIZONTAL) {
            for (int dy : new int[]{0, 1, -1}) {
               BlockPos next = pos.offset(dir).up(dy);
               if (!inside(next, 1) || previous.containsKey(next) || !standable(next)) continue;
               // Steps require headroom over the edge as well as at the destination.
               if (dy > 0 && !world.isSpaceEmpty(mc.player, body(pos.up()))) continue;
               if (dy < 0 && !world.isSpaceEmpty(mc.player, body(pos.offset(dir)))) continue;
               previous.put(next, pos);
               queue.addLast(next);
               break;
            }
         }
      }
      if (end == null) { fail("path"); return; }
      if (end.equals(start)) {
         Vec3d center = Vec3d.ofBottomCenter(start);
         if (mc.player.getPos().squaredDistanceTo(center) > 0.04) walkTo = center;
         return;
      }
      while (!previous.get(end).equals(start)) end = previous.get(end);
      walkTo = Vec3d.ofBottomCenter(end);
      jump = end.getY() > start.getY();
   }

   private boolean inside(BlockPos pos, int margin) {
      return Math.abs(pos.getX() - origin.getX()) <= radius + margin
         && Math.abs(pos.getZ() - origin.getZ()) <= radius + margin
         && Math.abs(pos.getY() - origin.getY()) <= radius + 1;
   }

   private Box body(BlockPos pos) {
      return new Box(pos.getX() + 0.19, pos.getY(), pos.getZ() + 0.19, pos.getX() + 0.81, pos.getY() + 1.81, pos.getZ() + 0.81);
   }

   private boolean standable(BlockPos pos) {
      var floor = world.getBlockState(pos.down());
      return Block.isShapeFullCube(floor.getCollisionShape(world, pos.down()))
         && !floor.isOf(Blocks.MAGMA_BLOCK) && !floor.isOf(Blocks.CACTUS) && !floor.isOf(Blocks.CAMPFIRE)
         && !floor.isOf(Blocks.SOUL_CAMPFIRE) && !floor.isOf(Blocks.POWDER_SNOW)
         && world.getFluidState(pos).isEmpty() && world.getFluidState(pos.up()).isEmpty()
         && !world.getBlockState(pos).isOf(Blocks.FIRE) && !world.getBlockState(pos).isOf(Blocks.SOUL_FIRE)
         && world.isSpaceEmpty(mc.player, body(pos));
   }

   public void input(InputEvent event) {
      if (!isActive() || mc.player == null) return;
      event.setForward(0);
      event.setStrafe(0);
      event.setJump(false);
      event.setSneak(false);
      event.setSprint(false);
      if (walkTo != null && mc.currentScreen == null) {
         double dx = walkTo.x - mc.player.getX(), dz = walkTo.z - mc.player.getZ();
         if (dx * dx + dz * dz > 0.015) {
            float desiredYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
            turnToward(desiredYaw, 12);
            float heading = Math.abs(MathHelper.wrapDegrees(desiredYaw - mc.player.getYaw()));
            // Turn first, then ease into walking and slow down near the next waypoint.
            if (heading < 55) {
               float speed = MathHelper.clamp((float) Math.sqrt(dx * dx + dz * dz) * 1.7F, 0.35F, 1F);
               event.setForward(speed * (float) Math.cos(Math.toRadians(heading)));
               event.setJump(jump && mc.player.isOnGround());
            }
         }
      }
   }

   private boolean aim(Vec3d point) {
      Vec3d delta = point.subtract(mc.player.getEyePos());
      float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90);
      float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
      boolean aligned = turnToward(yaw, pitch);
      mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
         mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
      return aligned;
   }

   private boolean turnToward(float yaw, float pitch) {
      float yawDelta = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
      float pitchDelta = pitch - mc.player.getPitch();
      // Bounded acceleration plus easing near the target; vanilla interpolates between ticks.
      yawVelocity = smoothStep(yawDelta, yawVelocity, 24, 8);
      pitchVelocity = smoothStep(pitchDelta, pitchVelocity, 18, 6);
      mc.player.setYaw(mc.player.getYaw() + yawVelocity);
      mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + pitchVelocity, -90, 90));
      return Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw())) < 1.5F
         && Math.abs(pitch - mc.player.getPitch()) < 1.5F;
   }

   private static float smoothStep(float delta, float previous, float maxSpeed, float acceleration) {
      float desired = MathHelper.clamp(delta * 0.55F, -maxSpeed, maxSpeed);
      float next = previous + MathHelper.clamp(desired - previous, -acceleration, acceleration);
      return Math.abs(next) > Math.abs(delta) ? delta : next;
   }

   private void select(int slot) {
      if (mc.player.getInventory().selectedSlot != slot) {
         mc.player.getInventory().selectedSlot = slot;
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private boolean closePreviousScreen() {
      var handler = mc.player.currentScreenHandler;
      // Put a carried item back into a genuinely empty player slot before closing.
      if (!handler.getCursorStack().isEmpty()) {
         var empty = handler.slots.stream().filter(slot -> slot.inventory == mc.player.getInventory()
            && !slot.hasStack() && slot.canInsert(handler.getCursorStack())
            && slot.getMaxItemCount(handler.getCursorStack()) >= handler.getCursorStack().getCount()).findFirst();
         if (empty.isEmpty()) return false;
         mc.interactionManager.clickSlot(handler.syncId, empty.get().id, 0, SlotActionType.PICKUP, mc.player);
         if (!handler.getCursorStack().isEmpty()) return false;
      }
      if (handler != mc.player.playerScreenHandler) mc.player.closeHandledScreen();
      if (mc.currentScreen != null) mc.setScreen(null);
      return true;
   }

   private void equip(int slot) {
      if (slot >= 9) {
         swappedHotbar = mc.player.getInventory().getStack(originalSlot).copy();
         leasedItem = mc.player.getInventory().getStack(slot).getItem();
         swapSource = slot;
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, originalSlot, SlotActionType.SWAP, mc.player);
         select(originalSlot);
      } else select(slot);
   }

   private void restoreHeld() {
      if (mc.player == null || mc.getNetworkHandler() != connection) return;
      if (swapSource >= 0 && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
         // Never swap over an unrelated item moved by the user or another module.
         ItemStack source = mc.player.getInventory().getStack(swapSource);
         ItemStack temporary = mc.player.getInventory().getStack(originalSlot);
         if ((source.isEmpty() && swappedHotbar.isEmpty() || same(source, swappedHotbar))
            && (temporary.isEmpty() || leasedItem != null && temporary.isOf(leasedItem))) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, swapSource, originalSlot, SlotActionType.SWAP, mc.player);
         }
         swapSource = -1;
      }
      select(originalSlot);
   }

   private void closeChest() {
      if (mc.player != null && openedSyncId != -1 && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler
         && mc.player.currentScreenHandler.getCursorStack().isEmpty()) mc.player.closeHandledScreen();
      openedSyncId = -1;
      openRetry = 0;
      contents = null;
   }

   private void change(Phase next) { phase = next; phaseTicks = 0; walkTo = null; }
   private void fail(String reason) { failure = "modules.auto_leave.stash.error." + reason; result = Result.FAILED; walkTo = null; }

   public void reset() {
      if (world != null && mc.world == world && mc.getNetworkHandler() == connection && mc.player != null) {
         closeChest();
         if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
         restoreHeld();
      }
      result = Result.CANCELLED;
      world = null;
      connection = null;
      walkTo = null;
      contents = null;
      targets.clear();
      mined.clear();
      batchItems.clear();
      transfers.clear();
      depositBefore = List.of();
      chestCapacity = batchLimit = openRetry = 0;
      beforePickup = List.of();
      swappedHotbar = ItemStack.EMPTY;
      leasedItem = null;
      toolPrepared = false;
      yawVelocity = pitchVelocity = 0;
      swapSource = -1;
      openedSyncId = -1;
      stored = 0;
      failure = null;
   }
}
