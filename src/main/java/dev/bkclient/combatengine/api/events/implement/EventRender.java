package dev.bkclient.combatengine.api.events.implement;

import dev.bkclient.combatengine.api.events.Event;
import lombok.Generated;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class EventRender extends Event {
   public static class Default extends Event {
      private final DrawContext recoveredField1;
      private final float recoveredField2;

      @Generated
      public DrawContext getContext() {
         return this.recoveredField1;
      }

      @Generated
      public float getPartialTicks() {
         return this.recoveredField2;
      }

      @Generated
      public Default(DrawContext context, float partialTicks) {
         this.recoveredField1 = context;
         this.recoveredField2 = partialTicks;
      }
   }

   public static class Game extends Event {
      private final WorldRenderer recoveredField1;
      private final MatrixStack recoveredField2;
      private final Matrix4f recoveredField3;
      private final Camera recoveredField4;
      private final float recoveredField5;
      private final long recoveredField6;

      @Generated
      public WorldRenderer getContext() {
         return this.recoveredField1;
      }

      @Generated
      public MatrixStack getMatrix() {
         return this.recoveredField2;
      }

      @Generated
      public Matrix4f getProjectionMatrix() {
         return this.recoveredField3;
      }

      @Generated
      public Camera getCamera() {
         return this.recoveredField4;
      }

      @Generated
      public float getPartialTicks() {
         return this.recoveredField5;
      }

      @Generated
      public long getFinishTimeNano() {
         return this.recoveredField6;
      }

      @Generated
      public Game(WorldRenderer context, MatrixStack matrix, Matrix4f projectionMatrix, Camera camera, float partialTicks, long finishTimeNano) {
         this.recoveredField1 = context;
         this.recoveredField2 = matrix;
         this.recoveredField3 = projectionMatrix;
         this.recoveredField4 = camera;
         this.recoveredField5 = partialTicks;
         this.recoveredField6 = finishTimeNano;
      }
   }

   public static class World extends Event {
      private final Window recoveredField1;
      private final float recoveredField2;
      private final Matrix4f recoveredField3;
      private final MatrixStack recoveredField4;

      @Generated
      public Window getScaledResolution() {
         return this.recoveredField1;
      }

      @Generated
      public float getPartialTicks() {
         return this.recoveredField2;
      }

      @Generated
      public Matrix4f getMatrix() {
         return this.recoveredField3;
      }

      @Generated
      public MatrixStack getMatrixStack() {
         return this.recoveredField4;
      }

      @Generated
      public World(Window scaledResolution, float partialTicks, Matrix4f matrix, MatrixStack matrixStack) {
         this.recoveredField1 = scaledResolution;
         this.recoveredField2 = partialTicks;
         this.recoveredField3 = matrix;
         this.recoveredField4 = matrixStack;
      }
   }
}
