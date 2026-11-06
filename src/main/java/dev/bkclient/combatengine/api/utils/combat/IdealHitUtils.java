package dev.bkclient.combatengine.api.utils.combat;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import dev.bkclient.combatengine.mixin.IEntity;
import lombok.Generated;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class IdealHitUtils implements QClient {
   private static final int recoveredField1 = 8;
   private static final int recoveredField2 = 10;
   private static final double recoveredField3 = 0.05;
   private static int recoveredField4 = Integer.MIN_VALUE;
   private static int recoveredField5 = Integer.MIN_VALUE;

   public static float getAICooldown() {
      if (mc.player.getMainHandStack().getItem() == Items.AIR) {
         return 0.9F;
      } else {
         return !(mc.player.getMainHandStack().getItem() instanceof AxeItem) && !(mc.player.getMainHandStack().getItem() instanceof ShovelItem) ? 0.93F : 0.95F;
      }
   }

   public static boolean canAIFall() {
      BlockPos var0 = BlockPos.ofFloored(mc.player.getPos().add(0.0, -0.4F, 0.0));
      return mc.world.getBlockState(var0).isOf(Blocks.WATER)
         ? true
         : getBlock(0.0, 3.0, 0.0) == Blocks.AIR && getBlock(0.0, 2.0, 0.0) == Blocks.AIR && getBlock(0.0, 1.0, 0.0) == Blocks.AIR
            || mc.player.fallDistance < (getBlock(0.0, 2.0, 0.0) != Blocks.AIR ? 0.08F : 0.6F)
            || mc.player.fallDistance > 1.2F;
   }

   public static boolean canCritical(LivingEntity target) {
      recoveredMethod6();
      boolean var1 = mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
      boolean var2 = isInCobweb();
      boolean var3 = ModuleClass.aura.smartCrit.isState();
      boolean var4 = ModuleClass.packetCriticals.isEnable();
      if (recoveredMethod8()) {
         return recoveredMethod9();
      } else if (var4 && var2) {
         return true;
      } else if (var4 && var1) {
         return mc.player.getVelocity().y < 0.0 && mc.player.fallDistance > 0.0F;
      } else {
         boolean var5 = !mc.player.isOnGround() && mc.player.getVelocity().y < 0.0 && mc.player.fallDistance > 0.0F;
         if (recoveredMethod1()) {
            return recoveredMethod3();
         } else if (recoveredMethod2()) {
            return isNoJumpDelayJumpCritWindow();
         } else if (recoveredMethod11()) {
            return true;
         } else {
            return !var3 ? var5 : mc.player.isOnGround() || var5;
         }
      }
   }

   private static boolean recoveredMethod1() {
      return ModuleClass.noJumpDelay.isEnable() && mc.options != null && mc.options.jumpKey.isPressed() && recoveredMethod5();
   }

   private static boolean recoveredMethod2() {
      return ModuleClass.noJumpDelay.isEnable() && mc.options != null && mc.options.jumpKey.isPressed();
   }

   private static boolean recoveredMethod3() {
      return recoveredMethod4();
   }

   public static boolean isNoJumpDelayJumpCritWindow() {
      return mc.player != null
         && mc.world != null
         && ModuleClass.noJumpDelay.isEnable()
         && mc.options != null
         && mc.options.jumpKey.isPressed()
         && recoveredMethod4();
   }

   private static boolean recoveredMethod4() {
      return mc.player != null
         && mc.world != null
         && !mc.player.isOnGround()
         && mc.player.fallDistance > 0.0F
         && mc.player.getVelocity().y <= 0.0
         && !mc.player.isTouchingWater()
         && !mc.player.isSubmergedInWater()
         && !mc.player.isInLava()
         && !mc.player.isClimbing()
         && !mc.player.hasVehicle()
         && !mc.player.getAbilities().flying
         && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
         && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
         && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         && !mc.player.isGliding()
         && !isInCobweb();
   }

   private static boolean recoveredMethod5() {
      if (mc.player != null && mc.world != null) {
         Box var0 = mc.player.getBoundingBox().contract(0.03);
         Box var1 = new Box(var0.minX, var0.maxY, var0.minZ, var0.maxX, var0.maxY + 0.32, var0.maxZ);

         for (BlockPos var3 : BlockPos.iterate(
            MathHelper.floor(var1.minX),
            MathHelper.floor(var1.minY),
            MathHelper.floor(var1.minZ),
            MathHelper.floor(var1.maxX),
            MathHelper.floor(var1.maxY),
            MathHelper.floor(var1.maxZ)
         )) {
            BlockState var4 = mc.world.getBlockState(var3);
            if (!var4.isAir() && !var4.getCollisionShape(mc.world, var3).isEmpty()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean canPacketCrit() {
      return isInCobweb() || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
   }

   private static void recoveredMethod6() {
      if (mc.player != null && mc.world != null) {
         boolean var0 = recoveredMethod10();
         if (var0) {
            recoveredField4 = mc.player.age;
            if (recoveredMethod7()) {
               recoveredField5 = mc.player.age;
            }
         }
      } else {
         recoveredField4 = Integer.MIN_VALUE;
         recoveredField5 = Integer.MIN_VALUE;
      }
   }

   private static boolean recoveredMethod7() {
      return mc.player != null && mc.options != null
         ? mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && !mc.player.isSubmergedInWater() && mc.player.getVelocity().y > 0.05
         : false;
   }

   private static boolean recoveredMethod8() {
      return mc.player != null && mc.options != null && mc.options.jumpKey.isPressed()
         ? mc.player.age - recoveredField5 <= 8 && mc.player.age - recoveredField4 <= 10
         : false;
   }

   private static boolean recoveredMethod9() {
      return mc.player != null
         && !mc.player.isOnGround()
         && !mc.player.isTouchingWater()
         && !mc.player.isSubmergedInWater()
         && mc.player.fallDistance > 0.0F
         && mc.player.getVelocity().y < 0.0;
   }

   private static boolean recoveredMethod10() {
      if (mc.player != null && mc.world != null) {
         BlockPos var0 = BlockPos.ofFloored(mc.player.getPos().add(0.0, -0.4F, 0.0));
         return mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.world.getBlockState(var0).isOf(Blocks.WATER);
      } else {
         return false;
      }
   }

   private static boolean recoveredMethod11() {
      double var0 = mc.player.getStepHeight();
      Vec3d var2 = new Vec3d(0.0, var0, 0.0);
      Vec3d var3 = ((IEntity)mc.player).invokeAdjustMovementForCollisions(var2);
      boolean var4 = isInCobweb();
      BlockPos var5 = BlockPos.ofFloored(mc.player.getPos().add(0.0, mc.player.getHeight() / 2.0F, 0.0));
      return mc.player.isInLava()
         || mc.player.isClimbing()
         || mc.world.getBlockState(var5).isOf(Blocks.WATER)
         || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
         || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
         || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         || var4
         || mc.player.isGliding()
         || mc.player.hasVehicle()
         || mc.player.getAbilities().flying
         || mc.player.isTouchingWater()
         || var3.y < mc.player.getStepHeight() - 0.5 && mc.player.isOnGround();
   }

   public static boolean isInCobweb() {
      Box var0 = mc.player.getBoundingBox();

      for (BlockPos var2 : BlockPos.iterate(
         MathHelper.floor(var0.minX),
         MathHelper.floor(var0.minY),
         MathHelper.floor(var0.minZ),
         MathHelper.floor(var0.maxX),
         MathHelper.floor(var0.maxY),
         MathHelper.floor(var0.maxZ)
      )) {
         if (mc.world.getBlockState(var2).isOf(Blocks.COBWEB)) {
            return true;
         }
      }

      return false;
   }

   public static Block getBlock(double x, double y, double z) {
      return mc.world.getBlockState(mc.player.getBlockPos().add((int)x, (int)y, (int)z)).getBlock();
   }

   public static boolean findFall(float fallDistance) {
      Vec3d var1 = mc.player.getRotationVector();
      double var2 = mc.player.getVelocity().x;
      double var4 = mc.player.getVelocity().y;
      double var6 = mc.player.getVelocity().z;
      float var8 = MathHelper.cos(mc.player.getPitch() * (float) (Math.PI / 180.0));
      var8 = (float)(var8 * var8 * Math.min(var1.length() / 0.4, 1.0));
      Vec3d var9 = new Vec3d(var2, var4, var6).add(0.0, 0.08 * (-1.0 + var8 * 0.75), 0.0);
      var4 = var9.y * 0.98F;
      return var4 < fallDistance;
   }

   @Generated
   private IdealHitUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
