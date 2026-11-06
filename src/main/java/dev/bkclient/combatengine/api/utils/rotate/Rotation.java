package dev.bkclient.combatengine.api.utils.rotate;

import dev.bkclient.combatengine.api.QClient;
import lombok.Generated;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Rotation implements QClient {
   private float recoveredField1;
   private float recoveredField2;

   public Rotation(Entity entity) {
      this.recoveredField1 = entity.getYaw();
      this.recoveredField2 = entity.getPitch();
   }

   public float getDelta(Rotation target) {
      float var2 = MathHelper.wrapDegrees(target.getYaw() - this.recoveredField1);
      float var3 = target.getPitch() - this.recoveredField2;
      return (float)Math.hypot(Math.abs(var2), Math.abs(var3));
   }

   public double getDeltaDouble(Rotation target) {
      double var2 = MathHelper.wrapDegrees(target.getYaw() - this.recoveredField1);
      double var4 = MathHelper.wrapDegrees(target.getPitch() - this.recoveredField2);
      return Math.hypot(var2, var4);
   }

   public static Vector2f camera() {
      return new Vector2f(cameraYaw(), cameraPitch());
   }

   public static float cameraYaw() {
      return MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() + (mc.gameRenderer.getCamera().isThirdPerson() ? 180 : 0));
   }

   public static float cameraPitch() {
      return (mc.gameRenderer.getCamera().isThirdPerson() ? -1 : 1) * mc.gameRenderer.getCamera().getPitch();
   }

   public static Rotation from(PlayerEntity player, Entity target) {
      Vec3d var2 = player.getCameraPosVec(0.0F);
      Vec3d var3 = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
      double var4 = var3.x - var2.x;
      double var6 = var3.y - var2.y;
      double var8 = var3.z - var2.z;
      double var10 = Math.sqrt(var4 * var4 + var8 * var8);
      float var12 = (float)Math.toDegrees(Math.atan2(var8, var4)) - 90.0F;
      float var13 = (float)(-Math.toDegrees(Math.atan2(var6, var10)));
      return new Rotation(var12, var13);
   }

   public final Vec3d toVector() {
      float var1 = this.recoveredField2 * (float) (Math.PI / 180.0);
      float var2 = -this.recoveredField1 * (float) (Math.PI / 180.0);
      float var3 = MathHelper.cos(var2);
      float var4 = MathHelper.sin(var2);
      float var5 = MathHelper.cos(var1);
      float var6 = MathHelper.sin(var1);
      return new Vec3d(var4 * var5, -var6, var3 * var5);
   }

   @Generated
   public float getYaw() {
      return this.recoveredField1;
   }

   @Generated
   public float getPitch() {
      return this.recoveredField2;
   }

   @Generated
   public void setYaw(float yaw) {
      this.recoveredField1 = yaw;
   }

   @Generated
   public void setPitch(float pitch) {
      this.recoveredField2 = pitch;
   }

   @Generated
   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Rotation var2)) {
         return false;
      } else if (!var2.canEqual(this)) {
         return false;
      } else {
         return Float.compare(this.getYaw(), var2.getYaw()) != 0 ? false : Float.compare(this.getPitch(), var2.getPitch()) == 0;
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof Rotation;
   }

   @Generated
   @Override
   public int hashCode() {
      byte var1 = 59;
      int var2 = 1;
      var2 = var2 * 59 + Float.floatToIntBits(this.getYaw());
      return var2 * 59 + Float.floatToIntBits(this.getPitch());
   }

   @Generated
   @Override
   public String toString() {
      return "Rotation(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }

   @Generated
   public Rotation() {
   }

   @Generated
   public Rotation(float yaw, float pitch) {
      this.recoveredField1 = yaw;
      this.recoveredField2 = pitch;
   }
}
