package dev.bkclient.systems.modules.constructions.swinganim;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

public record SwingTransformations(
   float anchorX, float anchorY, float anchorZ, float moveX, float moveY, float moveZ, float rotateX, float rotateY, float rotateZ
) {
   public float[] values() {
      return new float[]{anchorX, anchorY, anchorZ, moveX, moveY, moveZ, rotateX, rotateY, rotateZ};
   }

   public static SwingTransformations fromValues(float[] values) {
      if (values.length != 9) throw new IllegalArgumentException("A swing pose needs nine values");
      return new SwingTransformations(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8]);
   }

   public void apply(MatrixStack matrices, boolean leftHand) {
      float side = leftHand ? -1 : 1;
      matrices.translate(anchorX * side, anchorY, anchorZ);
      matrices.translate(moveX * side, moveY, moveZ);
      matrices.multiply(new Quaternionf().rotationXYZ((float)Math.toRadians(rotateX),
         (float)Math.toRadians(rotateY * side), (float)Math.toRadians(rotateZ * side)));
      matrices.translate(-anchorX * side, -anchorY, -anchorZ);
   }
}
