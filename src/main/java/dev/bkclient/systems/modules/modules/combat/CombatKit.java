package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.bridge.AuraRuntime;
import dev.bkclient.systems.setting.settings.RangeSetting;
import dev.bkclient.utility.rotations.RotationMath;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Shared helpers of the crystal / anchor / totem PvP modules: hotbar slots, silent aim, attacks and damage estimates. */
final class CombatKit {
   static final MinecraftClient mc = MinecraftClient.getInstance();

   private CombatKit() {
   }

   static boolean ready() {
      return mc.player != null && mc.world != null && mc.interactionManager != null && mc.player.isAlive() && !mc.player.isSpectator();
   }

   /** In the world with no screen open and the window focused (the modules only act when the player could act). */
   static boolean canAct() {
      return ready() && mc.currentScreen == null && mc.isWindowFocused();
   }

   static int hotbar(Item item) {
      return hotbar(stack -> stack.isOf(item));
   }

   static int hotbar(Predicate<ItemStack> predicate) {
      for (int slot = 0; slot < 9; slot++) {
         if (predicate.test(mc.player.getInventory().getStack(slot))) return slot;
      }
      return -1;
   }

   static boolean holding(Item item) {
      return mc.player.getMainHandStack().isOf(item);
   }

   /** Selects a hotbar slot for the server and the client at once, without a screen. */
   static void selectSlot(int slot) {
      if (slot < 0 || slot > 8 || mc.player.getInventory().selectedSlot == slot) return;
      mc.player.getInventory().selectedSlot = slot;
      mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      ((dev.bkclient.mixin.accessors.ClientPlayerInteractionManagerAccessor)mc.interactionManager).aura$setLastSelectedSlot(slot);
   }

   static PlayerEntity nearestEnemy(double range) {
      return mc.world.getPlayers().stream()
         .filter(player -> player != mc.player && player.isAlive() && !player.isSpectator()
            && !BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString())
            && mc.player.squaredDistanceTo(player) <= range * range)
         .min(Comparator.comparingDouble(player -> mc.player.squaredDistanceTo(player)))
         .orElse(null);
   }

   static void attack(Entity entity) {
      mc.interactionManager.attackEntity(mc.player, entity);
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   /** A random delay in milliseconds inside the chosen range. */
   static long randomDelay(RangeSetting range) {
      long low = (long)Math.min(range.getFirstValue(), range.getSecondValue());
      long high = (long)Math.max(range.getFirstValue(), range.getSecondValue());
      return low + (high > low ? ThreadLocalRandom.current().nextLong(high - low + 1L) : 0L);
   }

   /** Estimated explosion damage at a position, reduced by armor (enchantments and effects are ignored, so it errs slightly high). */
   static float explosionDamage(Vec3d source, PlayerEntity player, double power) {
      double radius = power * 2.0;
      Vec3d center = player.getBoundingBox().getCenter();
      double distance = center.distanceTo(source) / radius;
      if (distance > 1.0) return 0.0F;
      boolean blocked = mc.world.raycast(new RaycastContext(source, center, RaycastContext.ShapeType.COLLIDER,
         RaycastContext.FluidHandling.NONE, player)).getType() != net.minecraft.util.hit.HitResult.Type.MISS;
      double impact = (1.0 - distance) * (blocked ? 0.5 : 1.0);
      float raw = (float)((impact * impact + impact) / 2.0 * 7.0 * radius + 1.0);
      return afterArmor(raw, player);
   }

   /** Vanilla armor formula: armor points and toughness reduce incoming damage by up to 80 %. */
   static float afterArmor(float damage, PlayerEntity player) {
      float armor = player.getArmor();
      float toughness = (float)player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS);
      float reduction = MathHelper.clamp(armor - damage / (2.0F + toughness / 4.0F), armor * 0.2F, 20.0F);
      return damage * (1.0F - reduction / 25.0F);
   }

   static float effectiveHealth(PlayerEntity player) {
      return player.getHealth() + player.getAbsorptionAmount();
   }


   /** Rotates (silently) towards a point through whichever rotation system currently owns the camera. */
   static void aim(Vec3d point) {
      var rotation = RotationMath.getRotationTo(point);
      if (AuraRuntime.bkengineOwnsRotation()) {
         dev.bkclient.combatengine.bkengine.manager.rotation.RotationProcess.update(
            new dev.bkclient.combatengine.bkengine.manager.rotation.Rotation(rotation.getYaw(), rotation.getPitch()),
            180F, 180F, 180F, 180F, 1, 20, false);
      } else if (AuraRuntime.ownsRotation() && RotationStorage.instance != null) {
         RotationStorage.update(new dev.bkclient.combatengine.api.utils.rotate.Rotation(rotation.getYaw(), rotation.getPitch()),
            180F, 180F, 1, 20);
      } else {
         var handler = BKClientInit.getInstance().getRotationHandler();
         handler.rotate(rotation, dev.bkclient.utility.rotations.MoveCorrection.SILENT, 180F, 180F, 180F,
            dev.bkclient.utility.rotations.RotationPriority.USE_ITEM);
         handler.update();
      }
   }

   /** Whether the rotation the server currently has points at the entity within reach. */
   static boolean aimingAt(Entity entity, double reach) {
      var rotation = BKClientInit.getInstance().getRotationHandler().getServerRotation();
      Vec3d eye = mc.player.getEyePos();
      Vec3d end = eye.add(Vec3d.fromPolar(rotation.getPitch(), rotation.getYaw()).multiply(reach));
      Box box = entity.getBoundingBox().expand(entity.getTargetingMargin());
      return box.raycast(eye, end).isPresent();
   }

   /** Whether the rotation the server currently has points at this block within reach (nothing solid in between). */
   static boolean aimingAtBlock(net.minecraft.util.math.BlockPos pos, double reach) {
      var rotation = BKClientInit.getInstance().getRotationHandler().getServerRotation();
      Vec3d eye = mc.player.getEyePos();
      Vec3d end = eye.add(Vec3d.fromPolar(rotation.getPitch(), rotation.getYaw()).multiply(reach));
      var hit = mc.world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
      return hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
   }

   /**
    * One action that needs a silent rotation first: the rotation is sent in one tick, and the action runs in the next
    * tick once the server's rotation really points at the target (extra look packets in one tick trip anticheats).
    */
   static final class Aimed {
      private Vec3d point;
      private BooleanSupplier aimed;
      private Runnable action;
      private int startedAge;

      boolean busy() {
         return this.action != null;
      }

      void request(Vec3d point, BooleanSupplier aimed, Runnable action) {
         this.point = point;
         this.aimed = aimed;
         this.action = action;
         this.startedAge = mc.player.age;
         aim(point);
      }

      /** Runs the pending action when aimed, re-aims otherwise, gives up after a few ticks. */
      void tick() {
         if (this.action == null) return;
         if (mc.player == null || mc.player.age - this.startedAge > 3) {
            this.clear();
            return;
         }
         if (mc.player.age == this.startedAge) return;
         if (this.aimed.getAsBoolean()) {
            Runnable run = this.action;
            this.clear();
            run.run();
         } else {
            aim(this.point);
         }
      }

      void clear() {
         this.action = null;
         this.aimed = null;
         this.point = null;
      }
   }

   static double clampReach(double wanted) {
      return MathHelper.clamp(wanted, 1.0, 6.0);
   }
}
