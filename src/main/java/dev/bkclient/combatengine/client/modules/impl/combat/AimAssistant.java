package dev.bkclient.combatengine.client.modules.impl.combat;

import dev.bkclient.combatengine.AuraServices;
import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventRender;
import dev.bkclient.combatengine.api.events.implement.EventUpdate;
import dev.bkclient.combatengine.client.modules.Module;
import dev.bkclient.combatengine.client.modules.settings.implement.BooleanSetting;
import dev.bkclient.combatengine.client.modules.settings.implement.FloatSetting;
import dev.bkclient.combatengine.client.modules.settings.implement.ListSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AimAssistant extends Module {
   public static final AimAssistant INSTANCE = new AimAssistant();
   private final ListSetting recoveredField1 = new ListSetting(
      "Цели для наведения",
      new BooleanSetting("Игроки", true),
      new BooleanSetting("Животные", false),
      new BooleanSetting("Мобы", false),
      new BooleanSetting("Друзья", true)
   );
   private final BooleanSetting recoveredField2 = new BooleanSetting("Наводить за стеной", false);
   private final FloatSetting recoveredField3 = new FloatSetting("Порог", 5.0F, 1.0F, 5.0F, 0.25F);
   private final BooleanSetting recoveredField4 = new BooleanSetting("Только с оружием", true);
   private LivingEntity recoveredField5;
   private Vec3d recoveredField6 = null;
   private long recoveredField7 = 0L;

   public AimAssistant() {
      super("Aim Assistant", "Доводит прицел до цели", Module.ModuleCategory.COMBAT);
      this.addSettings(this.recoveredField1, this.recoveredField2, this.recoveredField3, this.recoveredField4);
   }

   @Override
   public void onEnable() {
      this.recoveredField5 = null;
      this.recoveredField6 = null;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.recoveredField5 = null;
      this.recoveredField6 = null;
      super.onDisable();
   }

   @EventLink
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.world != null) {
         LivingEntity var2 = null;
         if (TriggerBot.INSTANCE.isEnable()) {
            var2 = TriggerBot.INSTANCE.getTarget();
         }

         if (var2 == null) {
            var2 = this.recoveredMethod2();
         }

         if (var2 != this.recoveredField5) {
            this.recoveredField6 = null;
         }

         this.recoveredField5 = var2;
      } else {
         this.recoveredField5 = null;
         this.recoveredField6 = null;
      }
   }

   @EventLink
   public void onRender(EventRender.Default event) {
      if (mc.player == null || mc.world == null) {
         this.recoveredField7 = 0L;
      } else if (this.recoveredMethod3(this.recoveredField5) && !mc.player.isUsingItem()) {
         if (!this.recoveredField4.getValue() || this.recoveredMethod5()) {
            Vec3d var2 = this.recoveredMethod9(mc.player.getEyePos(), this.recoveredField5, 3.0, this.recoveredField2.getValue());
            if (var2 == Vec3d.ZERO) {
               return;
            }

            float var3 = this.recoveredMethod1();
            double var4 = 1.0 - Math.exp(-var3 / 0.06);
            this.recoveredField6 = this.recoveredField6 == null ? var2 : this.recoveredField6.lerp(var2, var4);
            float var6 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(this.recoveredField6.z, this.recoveredField6.x)) - 90.0);
            float var7 = (float)(-Math.toDegrees(Math.atan2(this.recoveredField6.y, Math.hypot(this.recoveredField6.x, this.recoveredField6.z))));
            float var8 = MathHelper.wrapDegrees(var6 - mc.player.getYaw());
            float var9 = var7 - mc.player.getPitch();
            if (Math.abs(var9) <= 13.0F
               && Math.abs(var8) < 8.0F
               && this.recoveredMethod7(mc.player.getYaw(), mc.player.getPitch(), 3.0, this.recoveredField5, this.recoveredField2.getValue())) {
               var9 = 0.0F;
            }

            if (Math.abs(var8) > 45.0F || Math.abs(var9) > 30.0F) {
               return;
            }

            float var10 = (float)Math.hypot(var8, var9);
            if (var10 < 0.35F) {
               return;
            }

            float var11 = this.recoveredField3.getValue().floatValue() * 3.5F;
            float var12 = 1.0F - (float)Math.exp(-var11 * var3);
            float var13 = 1.0F - (float)Math.exp(-var11 * 0.65F * var3);
            mc.player.setYaw(mc.player.getYaw() + var8 * var12);
            if (var9 != 0.0F) {
               mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + var9 * var13, -90.0F, 90.0F));
            }
         }
      }
   }

   private float recoveredMethod1() {
      long var1 = System.nanoTime();
      if (this.recoveredField7 == 0L) {
         this.recoveredField7 = var1;
         return 0.016666668F;
      } else {
         float var3 = (float)(var1 - this.recoveredField7) / 1.0E9F;
         this.recoveredField7 = var1;
         return MathHelper.clamp(var3, 0.001F, 0.1F);
      }
   }

   private LivingEntity recoveredMethod2() {
      Vec3d var1 = mc.player.getEyePos();
      Vec3d var2 = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
      return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
         .filter(LivingEntity.class::isInstance)
         .map(LivingEntity.class::cast)
         .filter(entity -> this.recoveredMethod3(entity) && (this.recoveredField2.getValue() || this.recoveredMethod8(var1, entity, 4.0)))
         .min(
            Comparator.comparingDouble(
               entity -> Math.acos(MathHelper.clamp(var2.dotProduct(entity.getBoundingBox().getCenter().subtract(var1).normalize()), -1.0, 1.0))
            )
         )
         .orElse(null);
   }

   private boolean recoveredMethod3(LivingEntity entity) {
      if (entity == null || entity == mc.player) {
         return false;
      } else if (entity.isAlive() && !entity.isRemoved()) {
         double var2 = 4.0 + mc.player.getVelocity().length() * 3.0;
         return this.recoveredMethod6(entity) > var2 * var2 ? false : this.recoveredMethod4(entity);
      } else {
         return false;
      }
   }

   private boolean recoveredMethod4(LivingEntity entity) {
      if (entity instanceof PlayerEntity var2) {
         return this.recoveredField1.is("Игроки")
            && (this.recoveredField1.is("Друзья") || !AuraServices.INSTANCE.friendStorage.isFriend(var2.getName().getString()));
      } else {
         return entity instanceof MobEntity ? this.recoveredField1.is("Мобы") : entity instanceof AnimalEntity && this.recoveredField1.is("Животные");
      }
   }

   private boolean recoveredMethod5() {
      Item var1 = mc.player.getMainHandStack().getItem();
      return var1 instanceof SwordItem || var1 instanceof AxeItem || var1 instanceof MaceItem;
   }

   private double recoveredMethod6(Entity entity) {
      Vec3d var2 = mc.player.getEyePos();
      Box var3 = entity.getBoundingBox();
      double var4 = MathHelper.clamp(var2.x, var3.minX, var3.maxX);
      double var6 = MathHelper.clamp(var2.y, var3.minY, var3.maxY);
      double var8 = MathHelper.clamp(var2.z, var3.minZ, var3.maxZ);
      double var10 = var4 - var2.x;
      double var12 = var6 - var2.y;
      double var14 = var8 - var2.z;
      return var10 * var10 + var12 * var12 + var14 * var14;
   }

   private boolean recoveredMethod7(float yaw, float pitch, double distance, Entity entity, boolean ignoreWalls) {
      if (mc.player != null && mc.world != null) {
         Vec3d var7 = mc.player.getEyePos();
         Vec3d var8 = Vec3d.fromPolar(pitch, yaw).multiply(distance);
         Optional var9 = entity.getBoundingBox().contains(var7) ? Optional.of(var7) : entity.getBoundingBox().raycast(var7, var7.add(var8));
         return var9.isEmpty()
            ? false
            : ignoreWalls
               || mc.world.raycast(new RaycastContext(var7, (Vec3d)var9.get(), ShapeType.OUTLINE, FluidHandling.NONE, mc.player)).getType() == Type.MISS;
      } else {
         return false;
      }
   }

   private boolean recoveredMethod8(Vec3d from, LivingEntity entity, double reach) {
      Box var5 = entity.getBoundingBox();
      double[] var6 = new double[]{0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0};
      int var7 = var6.length - 1;
      double var8 = reach * reach;

      for (int var10 = 0; var10 <= var7; var10++) {
         for (int var11 = 0; var11 <= var7; var11++) {
            for (int var12 = 0; var12 <= var7; var12++) {
               if (var10 <= 0 || var10 >= var7 || var11 <= 0 || var11 >= var7 || var12 <= 0 || var12 >= var7) {
                  Vec3d var13 = new Vec3d(
                     MathHelper.lerp(var6[var10], var5.minX, var5.maxX),
                     MathHelper.lerp(var6[var11], var5.minY, var5.maxY),
                     MathHelper.lerp(var6[var12], var5.minZ, var5.maxZ)
                  );
                  double var14 = from.squaredDistanceTo(var13);
                  if (!(var14 > var8)) {
                     Vec3d var16 = var13.add(from.subtract(var13).multiply(0.05 / Math.sqrt(var14)));
                     if (mc.world.raycast(new RaycastContext(from, var16, ShapeType.OUTLINE, FluidHandling.NONE, mc.player)).getType() == Type.MISS) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private Vec3d recoveredMethod9(Vec3d eye, LivingEntity target, double reach, boolean throughWalls) {
      Box var6 = target.getBoundingBox();
      double var7 = (var6.minX + var6.maxX) * 0.5;
      double var9 = (var6.minZ + var6.maxZ) * 0.5;
      Vec3d var11 = target.getPos().add(0.0, target.getStandingEyeHeight(), 0.0);
      double var12 = eye.distanceTo(var11);
      double var14 = MathHelper.lerp(MathHelper.clamp(var12 / 3.0, 0.0, 1.0), var6.minY, MathHelper.clamp(eye.y, var6.minY, var6.maxY));
      Vec3d var16 = new Vec3d(var7, var14, var9);
      ArrayList<Vec3d> var17 = new ArrayList<>();
      var17.add(var16);
      double[] var18 = new double[]{0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0};
      int var19 = var18.length - 1;

      for (int var20 = 0; var20 < var18.length; var20++) {
         for (int var21 = 0; var21 < var18.length; var21++) {
            for (int var22 = 0; var22 < var18.length; var22++) {
               if (var20 == 0 || var20 == var19 || var21 == 0 || var21 == var19 || var22 == 0 || var22 == var19) {
                  var17.add(
                     new Vec3d(
                        MathHelper.lerp(var18[var20], var6.minX, var6.maxX),
                        MathHelper.lerp(var18[var21], var6.minY, var6.maxY),
                        MathHelper.lerp(var18[var22], var6.minZ, var6.maxZ)
                     )
                  );
               }
            }
         }
      }

      for (double var23 : new double[]{0.0, 0.2}) {
         ArrayList<Vec3d> var25 = new ArrayList<>();

         for (Vec3d var27 : var17) {
            Vec3d var28 = var27.subtract(eye);
            double var29 = var28.length();
            if (var29 <= reach + var23) {
               float var31 = (float)(reach + var23);
               if (this.recoveredMethod7(
                  (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var28.z, var28.x)) - 90.0),
                  (float)(-Math.toDegrees(Math.atan2(var28.y, Math.hypot(var28.x, var28.z)))),
                  var31,
                  target,
                  false
               )) {
                  var25.add(var27);
               }
            }
         }

         if (!var25.isEmpty()) {
            Vec3d var37 = var25.stream().reduce(Vec3d.ZERO, (v0, v1) -> v0.add(v1)).multiply(1.0 / var25.size());
            return var25.stream().min(Comparator.comparingDouble(pt -> pt.squaredDistanceTo(var37))).get().subtract(eye);
         }

         if (throughWalls) {
            ArrayList<Vec3d> var36 = new ArrayList<>();

            for (Vec3d var40 : var17) {
               Vec3d var41 = var40.subtract(eye);
               double var30 = var41.length();
               if (var30 <= reach + var23) {
                  float var32 = (float)(reach + var23);
                  if (this.recoveredMethod7(
                     (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var41.z, var41.x)) - 90.0),
                     (float)(-Math.toDegrees(Math.atan2(var41.y, Math.hypot(var41.x, var41.z)))),
                     var32,
                     target,
                     true
                  )) {
                     var36.add(var40);
                  }
               }
            }

            if (!var36.isEmpty()) {
               Vec3d var39 = var36.stream().reduce(Vec3d.ZERO, (v0, v1) -> v0.add(v1)).multiply(1.0 / var36.size());
               return var36.stream().min(Comparator.comparingDouble(pt -> pt.squaredDistanceTo(var39))).get().subtract(eye);
            }
         }
      }

      return Vec3d.ZERO;
   }

   @Generated
   public LivingEntity getCurrentTarget() {
      return this.recoveredField5;
   }
}
