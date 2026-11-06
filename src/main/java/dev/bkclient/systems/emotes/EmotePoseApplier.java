package dev.bkclient.systems.emotes;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Puts an emote pose on a player model and on the whole body transform. Called from the render mixins. */
public final class EmotePoseApplier {
   private static final float DEGREES = (float)(Math.PI / 180.0);

   private EmotePoseApplier() {
   }

   /** The body bone moves and turns the whole player around the hips (0.7 blocks up). */
   public static void applyBody(EmoteManager.Active active, MatrixStack matrices) {
      EmoteAnimation.Pose body = active.animation().sample("body", active.time());
      float w = active.weight();
      float x = body.hasPosition() ? -body.px() * w / 16.0F : 0.0F;
      float y = body.hasPosition() ? body.py() * w / 16.0F : 0.0F;
      float z = body.hasPosition() ? body.pz() * w / 16.0F : 0.0F;
      float rx = body.hasRotation() ? -body.rx() * w : 0.0F;
      float ry = body.hasRotation() ? -body.ry() * w : 0.0F;
      float rz = body.hasRotation() ? body.rz() * w : 0.0F;
      matrices.translate(x, y + 0.7F, z);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
      matrices.translate(0.0F, -0.7F, 0.0F);
   }

   /** Head, torso, arms and legs follow their bones; the outer skin layers are children of those parts. */
   public static void applyModel(BipedEntityModel<?> model, EmoteManager.Active active) {
      bone(model.head, active, "head", 0.0F, 0.0F, 0.0F);
      bone(model.body, active, "torso", 0.0F, 0.0F, 0.0F);
      bone(model.rightArm, active, "right_arm", -5.0F, 2.0F, 0.0F);
      bone(model.leftArm, active, "left_arm", 5.0F, 2.0F, 0.0F);
      bone(model.rightLeg, active, "right_leg", -1.9F, 12.0F, 0.1F);
      bone(model.leftLeg, active, "left_leg", 1.9F, 12.0F, 0.1F);
      // Hat, jacket, sleeves and pants are children of head, body, arms and legs in this model, so they follow on their own;
      // copying a parent's transform onto them would apply it twice and tear the skin's outer layer away.
   }

   private static void bone(ModelPart part, EmoteManager.Active active, String name, float baseX, float baseY, float baseZ) {
      EmoteAnimation.Pose pose = active.animation().sample(name, active.time());
      float w = active.weight();
      if (pose.hasRotation()) {
         part.pitch = MathHelper.lerp(w, part.pitch, pose.rx() * DEGREES);
         part.yaw = MathHelper.lerp(w, part.yaw, pose.ry() * DEGREES);
         part.roll = MathHelper.lerp(w, part.roll, pose.rz() * DEGREES);
      }
      if (pose.hasPosition()) {
         part.pivotX = MathHelper.lerp(w, part.pivotX, baseX + pose.px());
         part.pivotY = MathHelper.lerp(w, part.pivotY, baseY - pose.py());
         part.pivotZ = MathHelper.lerp(w, part.pivotZ, baseZ + pose.pz());
      }
   }
}
