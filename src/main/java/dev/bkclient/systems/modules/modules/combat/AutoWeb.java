package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import dev.bkclient.combatengine.bridge.AuraRuntime;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.PlacementAnimation;
import dev.bkclient.utility.rotations.RotationMath;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Zenith-style target search and visible support-face placement, adapted to BK rotations. */
@ModuleInfo(name = "Auto Web", desc = "modules.descriptions.auto_web", category = ModuleCategory.COMBAT)
public final class AutoWeb extends BaseModule {
    public final SliderSetting range = new SliderSetting(this, "Range").min(1f).max(4.5f).step(0.1f).currentValue(4f);
    public final SliderSetting delay = new SliderSetting(this, "Place Delay").min(0f).max(10f).step(1f).currentValue(0f);
    public final BooleanSetting inventory = new BooleanSetting(this, "Use Inventory").enable();
    public final ModeSetting handToUse = new ModeSetting(this, "Hand To Use");
    public final ModeSetting.Value mainHand = new ModeSetting.Value(this.handToUse, "Main Hand").select();
    public final ModeSetting.Value offHand = new ModeSetting.Value(this.handToUse, "Off Hand");
    public final ModeSetting placeOrder = new ModeSetting(this, "Place Order");
    public final ModeSetting.Value closest = new ModeSetting.Value(this.placeOrder, "Closest").select();
    public final ModeSetting.Value furthest = new ModeSetting.Value(this.placeOrder, "Furthest");
    public final ModeSetting hitPoint = new ModeSetting(this, "Place Hit Point");
    public final ModeSetting.Value closestHit = new ModeSetting.Value(this.hitPoint, "Closest").select();
    public final ModeSetting.Value randomHit = new ModeSetting.Value(this.hitPoint, "Random");
    public final SelectSetting placementOptions = new SelectSetting(this, "Placement Options").min(1);
    public final SelectSetting.Value legs = new SelectSetting.Value(this.placementOptions, "Legs").select();
    public final SelectSetting.Value head = new SelectSetting.Value(this.placementOptions, "Head").select();
    public final SelectSetting.Value surround = new SelectSetting.Value(this.placementOptions, "Surround");
    public final SelectSetting.Value upperSurround = new SelectSetting.Value(this.placementOptions, "Upper Surround");
    public final BooleanSetting upperWeb = new BooleanSetting(this, "Place Upper Web").enable();
    public final BooleanSetting prioritizeAura = new BooleanSetting(this, "Prioritize Aura Target").enable();
    public final BooleanSetting ignoreFriends = new BooleanSetting(this, "Ignore Friends").enable();
    public final BooleanSetting ignoreSpectators = new BooleanSetting(this, "Ignore Spectators").enable();
    public final BooleanSetting includeInvisibles = new BooleanSetting(this, "Include Invisibles");
    public final BooleanSetting ignoreMoving = new BooleanSetting(this, "Ignore Moving");
    public final BooleanSetting cancelInScreens = new BooleanSetting(this, "Cancel In Screens").enable();
    public final BooleanSetting predictPosition = new BooleanSetting(this, "Predict Position");
    public final SliderSetting predictionTicks = new SliderSetting(this, "Prediction Ticks")
        .min(1.0F).max(10.0F).step(1.0F).currentValue(3.0F);
    public final BooleanSetting swing = new BooleanSetting(this, "Swing").enable();
    public final BooleanSetting animation = new BooleanSetting(this, "Placement Animation").enable();
    public final ColorSetting color = new ColorSetting(this, "Placement Color").color(Colors.ACCENT);
    private final PlacementAnimation effects = new PlacementAnimation();
    private ClientWorld world;
    private PlayerEntity target;
    private BlockPos pendingPos;
    private BlockPos pendingBase;
    private BlockHitResult pendingHit;
    private int lastAttempt = -1000;
    private int pendingAge;
    public boolean isPlacingBlock() { return isEnabled() && mc.player != null && (pendingHit != null || lastAttempt == mc.player.age); }

    private final EventListener<Render3DEvent> onRender = event -> {
        if (animation.isEnabled()) effects.render(event, color.getColor());
        else effects.clear();
    };

    @Override public void onEnable() { reset(); }
    @Override public void onDisable() { reset(); }

    private void reset() {
        pendingHit = null;
        pendingPos = null;
        pendingBase = null;
        target = null;
        world = null;
        lastAttempt = -1000;
        effects.clear();
    }

    private boolean ready() {
        AutoTrap trap = BKClientInit.getInstance().getModuleManager().getModule(AutoTrap.class);
        return mc.player != null && mc.world != null && mc.interactionManager != null
            && mc.player.isAlive() && !mc.player.isSpectator()
            && (!this.cancelInScreens.isEnabled() || mc.currentScreen == null)
            && mc.player.currentScreenHandler == mc.player.playerScreenHandler && !mc.player.isUsingItem()
            && mc.player.playerScreenHandler.getCursorStack().isEmpty() && (trap == null || !trap.isPlacingBlock());
    }

    @Override public void tick() {
        if (world != mc.world) { reset(); world = mc.world; }
        // Use the rotation sent in the previous tick, then interact before
        // this tick's movement packet. Extra Look packets here trigger Grim's
        // Timer/PacketOrder/Post checks.
        if (pendingHit != null) {
            AutoTrap trap = BKClientInit.getInstance().getModuleManager().getModule(AutoTrap.class);
            if (trap != null && trap.isPlacingBlock()) { pendingHit = null; pendingPos = null; pendingBase = null; return; }
            placePending();
            return;
        }
        pendingHit = null;
        pendingPos = null;
        pendingBase = null;
        if (!ready() || RotationStorage.instance == null) return;
        if (mc.player.age - lastAttempt < delay.getCurrentValue()) return;
        // Give Aura a full tick to regain its target rotation between placements.
        if (AuraRuntime.auraEnabled() && mc.player.age - lastAttempt <= 1) return;
        if (!mc.player.getOffHandStack().isOf(Items.COBWEB) && findWebSlot() < 0) return;
        var auraTarget = AuraRuntime.currentTarget();
        target = mc.world.getPlayers().stream().filter(this::validTarget)
            .min(Comparator.comparingDouble(player -> mc.player.squaredDistanceTo(player)
                - (this.prioritizeAura.isEnabled() && player == auraTarget ? 100 : 0)))
            .orElse(null);
        if (target == null) return;
        // Feet first, then one cell above, in the same X/Z column. Each web has its own rotation.
        BlockPos base = this.predictedBase(target);
        List<BlockPos> positions = this.getPlacementPositions(base);
        positions.sort(this.placeOrder.is(furthest)
            ? Comparator.comparingDouble((BlockPos pos) -> mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))).reversed()
            : Comparator.comparingDouble(pos -> mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))));
        for (BlockPos pos : positions) {
            if (!mc.world.getBlockState(pos).isAir() || new Box(pos).intersects(mc.player.getBoundingBox())) continue;
            BlockHitResult hit = findHit(pos);
            if (hit == null) continue;
            pendingBase = base;
            pendingPos = pos;
            pendingHit = hit;
            pendingAge = mc.player.age;
            rotateToHit(hit);
            break;
        }
    }

    private void rotateToHit(BlockHitResult hit) {
        var rotation = RotationMath.getRotationTo(hit.getPos());
        if (AuraRuntime.bkengineOwnsRotation()) {
            dev.bkclient.combatengine.bkengine.manager.rotation.RotationProcess.update(
                new dev.bkclient.combatengine.bkengine.manager.rotation.Rotation(rotation.getYaw(), rotation.getPitch()),
                180F, 180F, 180F, 180F, 1, 20, false);
        } else if (AuraRuntime.ownsRotation()) {
            RotationStorage.update(new dev.bkclient.combatengine.api.utils.rotate.Rotation(rotation.getYaw(), rotation.getPitch()),
                180F, 180F, 1, 20);
        } else {
            var handler = BKClientInit.getInstance().getRotationHandler();
            handler.rotate(rotation, dev.bkclient.utility.rotations.MoveCorrection.SILENT, 180F, 180F, 180F,
                dev.bkclient.utility.rotations.RotationPriority.USE_ITEM);
            handler.update();
        }
    }

    private boolean validTarget(PlayerEntity player) {
        return player != mc.player && player.isAlive()
            && (!this.ignoreSpectators.isEnabled() || !player.isSpectator())
            && (this.includeInvisibles.isEnabled() || !player.isInvisible())
            && (!this.ignoreMoving.isEnabled() || player.getVelocity().lengthSquared() < 0.0025)
            && (!this.ignoreFriends.isEnabled() || !BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString()))
            && mc.player.squaredDistanceTo(player) <= range.getCurrentValue() * range.getCurrentValue();
    }

    private double reach() {
        return Math.min(range.getCurrentValue(), mc.player.getBlockInteractionRange());
    }

    private BlockHitResult raycast(Vec3d end) {
        return mc.world.raycast(new RaycastContext(mc.player.getEyePos(), end,
            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
    }

    private BlockHitResult findHit(BlockPos pos) {
        Vec3d eye = mc.player.getEyePos();
        List<BlockHitResult> candidates = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos support = pos.offset(direction);
            var state = mc.world.getBlockState(support);
            if (state.isReplaceable() || state.getOutlineShape(mc.world, support).isEmpty()) continue;
            Direction face = direction.getOpposite();
            Vec3d point = Vec3d.ofCenter(support).add(Vec3d.of(face.getVector()).multiply(0.5));
            double distance = eye.distanceTo(point);
            if (distance > reach() || distance < 0.001) continue;
            BlockHitResult hit = raycast(point.add(point.subtract(eye).normalize().multiply(0.01)));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(support) && hit.getSide() == face) {
                candidates.add(hit);
            }
        }
        if (candidates.isEmpty()) return null;
        if (this.hitPoint.is(randomHit)) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        return candidates.stream().min(Comparator.comparingDouble(hit -> eye.squaredDistanceTo(hit.getPos()))).orElse(null);
    }

    private BlockPos predictedBase(PlayerEntity player) {
        if (!this.predictPosition.isEnabled()) return player.getBlockPos();
        Vec3d velocity = player.getVelocity().multiply(this.predictionTicks.getCurrentValue());
        return BlockPos.ofFloored(player.getX() + velocity.x, player.getY() + velocity.y, player.getZ() + velocity.z);
    }

    private List<BlockPos> getPlacementPositions(BlockPos base) {
        List<BlockPos> positions = new ArrayList<>();
        if (this.legs.isSelected()) positions.add(base);
        if (this.head.isSelected() || this.upperWeb.isEnabled()) positions.add(base.up());
        if (this.surround.isSelected()) {
            for (Direction direction : Direction.Type.HORIZONTAL) positions.add(base.offset(direction));
        }
        if (this.upperSurround.isSelected()) {
            for (Direction direction : Direction.Type.HORIZONTAL) positions.add(base.up().offset(direction));
        }
        return new ArrayList<>(positions.stream().distinct().toList());
    }

    private int findWebSlot() {
        int limit = inventory.isEnabled() ? 36 : 9;
        for (int slot = 0; slot < limit; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(Items.COBWEB)) return slot;
        }
        return -1;
    }

    private void placePending() {
        if (pendingHit != null && ready() && mc.player.age - pendingAge < 3) {
            var rotation = BKClientInit.getInstance().getRotationHandler().getServerRotation();
            Vec3d end = mc.player.getEyePos().add(Vec3d.fromPolar(rotation.getPitch(), rotation.getYaw()).multiply(reach()));
            var actual = raycast(end);
            if (actual.getType() != HitResult.Type.BLOCK || !actual.getBlockPos().equals(pendingHit.getBlockPos())
                    || actual.getSide() != pendingHit.getSide()) {
                rotateToHit(pendingHit);
                return;
            }
        } else if (pendingHit != null && mc.player != null && mc.player.age - pendingAge >= 3) {
            lastAttempt = mc.player.age;
            pendingHit = null; pendingPos = null; pendingBase = null; return;
        }
        BlockPos pos = pendingPos;
        BlockPos base = pendingBase;
        BlockHitResult planned = pendingHit;
        pendingPos = null;
        pendingBase = null;
        pendingHit = null;
        if (planned == null || pos == null || !ready() || world != mc.world || target == null || !validTarget(target)) return;
        if (base == null || base.getSquaredDistance(this.predictedBase(target)) > 2.25
            || !this.getPlacementPositions(base).contains(pos) || !mc.world.getBlockState(pos).isAir()
            || new Box(pos).intersects(mc.player.getBoundingBox())) return;
        Hand hand = this.offHand.isSelected() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (hand == Hand.OFF_HAND && !mc.player.getOffHandStack().isOf(Items.COBWEB)) return;
        int slot = hand == Hand.MAIN_HAND ? findWebSlot() : -1;
        if (hand == Hand.MAIN_HAND && slot < 0) return;
        int originalSlot = mc.player.getInventory().selectedSlot;
        boolean swapped = false;
        lastAttempt = mc.player.age;
        try {
            if (hand == Hand.MAIN_HAND) {
                if (slot >= 9) {
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, originalSlot, SlotActionType.SWAP, mc.player);
                    swapped = true;
                } else {
                    selectSlot(slot);
                }
            }
            if (!mc.player.getStackInHand(hand).isOf(Items.COBWEB)) return;
            var result = mc.interactionManager.interactBlock(mc.player, hand, planned);
            if (result.isAccepted()) {
                if (swing.isEnabled()) mc.player.swingHand(hand);
                if (animation.isEnabled()) effects.add(pos);
            }
        } finally {
            if (swapped) mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, originalSlot, SlotActionType.SWAP, mc.player);
            if (hand == Hand.MAIN_HAND) selectSlot(originalSlot);
        }
    }

    private void selectSlot(int slot) {
        if (mc.player.getInventory().selectedSlot != slot) {
            mc.player.getInventory().selectedSlot = slot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            ((dev.bkclient.mixin.accessors.ClientPlayerInteractionManagerAccessor) mc.interactionManager).aura$setLastSelectedSlot(slot);
        }
    }

}
