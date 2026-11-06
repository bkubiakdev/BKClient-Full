package dev.bkclient.systems.event.impl.render;

import lombok.Generated;
import dev.bkclient.systems.event.EventCancellable;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.joml.Matrix4f;

public class HandRenderEvent extends EventCancellable {
   private final Arm arm;
   private final float swingProgress;
   private final ItemStack itemStack;
   private final float equipProgress;
   private final MatrixStack matrices;
   private final Matrix4f positionMatrix;

   @Generated
   public Arm getArm() {
      return this.arm;
   }

   @Generated
   public float getSwingProgress() {
      return this.swingProgress;
   }

   @Generated
   public ItemStack getItemStack() {
      return this.itemStack;
   }

   @Generated
   public float getEquipProgress() {
      return this.equipProgress;
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
   }

   @Generated
   public Matrix4f getPositionMatrix() {
      return this.positionMatrix;
   }

   @Generated
   public HandRenderEvent(Arm arm, float swingProgress, ItemStack itemStack, float equipProgress, MatrixStack matrices, Matrix4f positionMatrix) {
      this.arm = arm;
      this.swingProgress = swingProgress;
      this.itemStack = itemStack;
      this.equipProgress = equipProgress;
      this.matrices = matrices;
      this.positionMatrix = positionMatrix;
   }
}
