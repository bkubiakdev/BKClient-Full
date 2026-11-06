package dev.bkclient.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import java.util.ArrayList;

/** Rising, rotating cubes with an additive glow, adapted from AuraServices. */
public final class TargetCubesRenderer {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final ArrayList<CubeParticle> cubeParticles = new ArrayList<>();
   private final ArrayList<CubeParticle> visibleCubes = new ArrayList<>();
   private net.minecraft.client.world.ClientWorld world;
   private LivingEntity target;
   private long lastFrameMillis;
   private float spawnAccumulator;
   /** Incremented once per render call; marks which frame a cube's cached matrix belongs to. */
   private long frameId;
   private final Matrix4f bloomScratch = new Matrix4f();
   private final Matrix4f bloomScaled = new Matrix4f();
   private static final Identifier BLOOM_TEXTURE = Identifier.of("bkclient", "textures/target_cubes/bloom.png");
   public void clear() {
      cubeParticles.clear(); visibleCubes.clear(); target = null;
      world = null; lastFrameMillis = 0; spawnAccumulator = 0;
   }
   private static void drawBuffer(BufferBuilder builder) {
      BuiltBuffer buffer = builder.endNullable();
      if (buffer != null) BufferRenderer.drawWithGlobalProgram(buffer);
   }
   private static int replAlpha(int color, int alpha) { return color & 0xFFFFFF | MathHelper.clamp(alpha, 0, 255) << 24; }
   static final byte[][] CUBE_EDGES = new byte[][]{
      {-1, -1, -1, 1, -1, -1},
      {1, -1, -1, 1, -1, 1},
      {1, -1, 1, -1, -1, 1},
      {-1, -1, 1, -1, -1, -1},
      {-1, 1, -1, 1, 1, -1},
      {1, 1, -1, 1, 1, 1},
      {1, 1, 1, -1, 1, 1},
      {-1, 1, 1, -1, 1, -1},
      {-1, -1, -1, -1, 1, -1},
      {1, -1, -1, 1, 1, -1},
      {1, -1, 1, 1, 1, 1},
      {-1, -1, 1, -1, 1, 1}
   };
   public void render(Render3DEvent event, LivingEntity target, int color, boolean damageFlash) {
      if (world != mc.world) { clear(); world = mc.world; }
      if (mc.world == null || mc.player == null) return;
      boolean hasTarget = target != null && target.isAlive() && !target.isRemoved();
      long var4 = System.currentTimeMillis();
      if (this.lastFrameMillis == 0L) {
         this.lastFrameMillis = var4;
      }

      float var6 = Math.min((float)(var4 - this.lastFrameMillis) / 1000.0F, 0.1F);
      this.lastFrameMillis = var4;
      if (Float.isFinite(var6) && mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
         if (hasTarget && target != null) {
            this.target = target;
            this.spawnAccumulator += var6;

            while (this.spawnAccumulator >= 0.022F) {
               this.spawnAccumulator -= 0.022F;
               if (this.cubeParticles.size() >= 72) {
                  break;
               }

               for (int var7 = 0; var7 < 1; var7++) {
                  double var8 = Math.random() * 360.0;
                  double var10 = Math.cos(Math.toRadians(var8)) * 0.7;
                  double var12 = 0.02 + Math.random() * 0.1;
                  double var14 = Math.sin(Math.toRadians(var8)) * 0.7;
                  this.cubeParticles.add(new CubeParticle(target, var10, var12, var14));
               }
            }
         } else {
            this.spawnAccumulator = 0.0F;
         }

         this.visibleCubes.clear();

         for (int var30 = this.cubeParticles.size() - 1; var30 >= 0; var30--) {
            CubeParticle var32 = this.cubeParticles.get(var30);

            try {
               var32.update(var6, var4, hasTarget ? target : null);
               if (var32.shouldRemove(var4)) {
                  this.cubeParticles.remove(var30);
               } else {
                  this.visibleCubes.add(var32);
               }
            } catch (Throwable var29) {
               this.cubeParticles.remove(var30);
            }
         }

         if (!this.visibleCubes.isEmpty()) {
            long frame = ++this.frameId;
            float var31 = event.getTickDelta();
            MatrixStack var33 = event.getMatrices();
            Vec3d var9 = mc.gameRenderer.getCamera().getPos();
            LivingEntity var34 = hasTarget ? target : this.target;
            float var11 = damageFlash && var34 != null ? MathHelper.clamp(var34.hurtTime / 10.0F, 0.0F, 1.0F) : 0.0F;
            int var35 = color;
            int var13 = 0xFFFF0303;
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(770, 1);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder var36 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            boolean var15 = false;
            int var16 = 0;

            for (int var17 = this.visibleCubes.size(); var16 < var17; var16++) {
               CubeParticle var18 = this.visibleCubes.get(var16);

               try {
                  int var19 = var18.getRenderColor(var35, var13, var11, var4);
                  if ((var19 >> 24 & 0xFF) > 0 && var18.appendCubeFaces(var36, var33, var9, var31, var19, frame)) {
                     var15 = true;
                  }
               } catch (Throwable var28) {
               }
            }

            drawBuffer(var36);

            BufferBuilder var37 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            boolean var38 = false;
            int var39 = 0;

            for (int var41 = this.visibleCubes.size(); var39 < var41; var39++) {
               CubeParticle var20 = this.visibleCubes.get(var39);

               try {
                  int var21 = var20.getRenderColor(var35, var13, var11, var4);
                  if ((var21 >> 24 & 0xFF) > 0 && var20.appendCubeLines(var37, var33, var9, var31, var21, frame)) {
                     var38 = true;
                  }
               } catch (Throwable var27) {
               }
            }

            drawBuffer(var37);

            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
            BufferBuilder var40 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            boolean var42 = false;
            float var43 = mc.gameRenderer.getCamera().getYaw();
            float var44 = mc.gameRenderer.getCamera().getPitch();
            // The yaw/pitch rotation of every bloom sprite is the same (JOML's rotate reads only the 3x3
            // block and translate never touches it), so it is computed once; each sprite only scales it
            // and takes the translation the same translate call produces. Vertices are bit-identical.
            Matrix4f bloomBase = var33.peek().getPositionMatrix();
            Matrix4f bloomBillboard = new Matrix4f(bloomBase).translate(0.0F, 0.0F, 0.0F)
               .rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-var43))
               .rotate(RotationAxis.POSITIVE_X.rotationDegrees(var44));
            int var22 = 0;

            for (int var23 = this.visibleCubes.size(); var22 < var23; var22++) {
               CubeParticle var24 = this.visibleCubes.get(var22);

               try {
                  int var25 = var24.getRenderColor(var35, var13, var11, var4);
                  if (var24.appendBloom(var40, bloomBase, bloomBillboard, this.bloomScratch, this.bloomScaled, var9, var31, var25, var4)) {
                     var42 = true;
                  }
               } catch (Throwable var26) {
               }
            }

            drawBuffer(var40);

            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
         }
      }
   }
   static int overCol(int color1, int color2, float factor) {
      factor = Math.max(0.0F, Math.min(1.0F, factor));
      int var4 = color1 >> 16 & 0xFF;
      int var5 = color1 >> 8 & 0xFF;
      int var6 = color1 & 0xFF;
      int var7 = color1 >> 24 & 0xFF;
      int var8 = color2 >> 16 & 0xFF;
      int var9 = color2 >> 8 & 0xFF;
      int var10 = color2 & 0xFF;
      int var11 = color2 >> 24 & 0xFF;
      int var12 = (int)(var4 + (var8 - var4) * factor);
      int var13 = (int)(var5 + (var9 - var5) * factor);
      int var14 = (int)(var6 + (var10 - var6) * factor);
      int var15 = (int)(var7 + (var11 - var7) * factor);
      return var15 << 24 | var12 << 16 | var13 << 8 | var14;
   }
private static class CubeParticle {
   double x;
   double y;
   double z;
   double worldX;
   double worldY;
   double worldZ;
   long time;
   LivingEntity entity;
   boolean fading;
   long fadeStartTime;
   float vx;
   float vy;
   float vz;
   float rotX;
   float rotY;
   float rotZ;
   float rotSpeedX;
   float rotSpeedY;
   float rotSpeedZ;
   /** Cube matrix built by the face pass, reused by the line pass of the same frame (non-fading only). */
   final Matrix4f cubeMatrix = new Matrix4f();
   long cubeMatrixFrame = -1L;

   public CubeParticle(LivingEntity entity, double x, double y, double z) {
      this.entity = entity;
      this.x = x;
      this.y = y;
      this.z = z;
      this.time = System.currentTimeMillis();
      this.rotX = (float)(Math.random() * 360.0);
      this.rotY = (float)(Math.random() * 360.0);
      this.rotZ = (float)(Math.random() * 360.0);
      this.rotSpeedX = 1.4F + (float)Math.random() * 3.4F;
      this.rotSpeedY = 1.4F + (float)Math.random() * 3.4F;
      this.rotSpeedZ = 1.4F + (float)Math.random() * 3.4F;
      this.vx = (float)((Math.random() - 0.5) * 0.0022);
      this.vy = 0.031F + (float)Math.random() * 0.02F;
      this.vz = (float)((Math.random() - 0.5) * 0.0022);
   }

   public void update(float dt, long now, LivingEntity currentTarget) {
      float var5 = dt * 60.0F;
      this.rotX = this.rotX + this.rotSpeedX * var5;
      this.rotY = this.rotY + this.rotSpeedY * var5;
      this.rotZ = this.rotZ + this.rotSpeedZ * var5;
      if (!this.fading) {
         this.x = this.x + this.vx * var5;
         this.y = this.y + this.vy * var5;
         this.z = this.z + this.vz * var5;
         this.vx *= 0.992F;
         this.vz *= 0.992F;
         this.vy *= 0.989F;
         if (this.entity != null) {
            double var6 = Math.max(2.2, this.entity.getHeight() * 1.85);
            if (this.y >= var6) {
               this.y = var6;
               this.recoveredMethod1(now);
               return;
            }
         }

         boolean var8 = currentTarget == null || this.entity == null || !this.entity.isAlive() || this.entity != currentTarget;
         if (var8 || now - this.time >= 560L) {
            this.recoveredMethod1(now);
         }
      }
   }

   public boolean shouldRemove(long now) {
      return this.fading && now - this.fadeStartTime >= 320L;
   }

   public int getRenderColor(int baseColor, int redColor, float hurtPC, long now) {
      float var6 = this.recoveredMethod2(now);
      if (var6 <= 0.001F) {
         return 0;
      }

      int var7 = replAlpha(baseColor, (int)(var6 * 255.0F));
      int var8 = replAlpha(redColor, (int)(var6 * 255.0F));
      return overCol(var7, var8, hurtPC);
   }

   public boolean appendCubeFaces(BufferBuilder faceBuilder, MatrixStack ms, Vec3d cam, float partialTicks, int color, long frame) {
      float var6 = (color >> 24 & 0xFF) / 255.0F;
      if (var6 <= 0.001F) {
         return false;
      }

      Vec3d var7 = this.recoveredMethod3(partialTicks);
      if (var7 == null) {
         return false;
      }

      float var8 = this.fading
         ? MathHelper.lerp(MathHelper.clamp((float)(System.currentTimeMillis() - this.fadeStartTime) / 320.0F, 0.0F, 1.0F), 1.0F, 0.45F)
         : 1.0F;
      float var9 = 0.12F * var8;
      ms.push();
      ms.translate(var7.x - cam.x, var7.y - cam.y, var7.z - cam.z);
      ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotX));
      ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotY));
      ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotZ));
      ms.scale(var9, var9, var9);
      Matrix4f var10 = ms.peek().getPositionMatrix();
      if (!this.fading) {
         // Same inputs in the line pass (no clock read when not fading): keep the result for it.
         this.cubeMatrix.set(var10);
         this.cubeMatrixFrame = frame;
      }
      this.recoveredMethod4(faceBuilder, var10, color);
      ms.pop();
      return true;
   }

   public boolean appendCubeLines(BufferBuilder lineBuilder, MatrixStack ms, Vec3d cam, float partialTicks, int color, long frame) {
      float var6 = (color >> 24 & 0xFF) / 255.0F;
      if (var6 <= 0.001F) {
         return false;
      }

      if (!this.fading && this.cubeMatrixFrame == frame) {
         // Position, rotation and scale are unchanged since the face pass of this frame.
         this.recoveredMethod5(lineBuilder, this.cubeMatrix, replAlpha(color, Math.max(1, (int)((color >> 24 & 0xFF) * 0.7F))));
         return true;
      }

      Vec3d var7 = this.recoveredMethod3(partialTicks);
      if (var7 == null) {
         return false;
      }

      float var8 = this.fading
         ? MathHelper.lerp(MathHelper.clamp((float)(System.currentTimeMillis() - this.fadeStartTime) / 320.0F, 0.0F, 1.0F), 1.0F, 0.45F)
         : 1.0F;
      float var9 = 0.12F * var8;
      ms.push();
      ms.translate(var7.x - cam.x, var7.y - cam.y, var7.z - cam.z);
      ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotX));
      ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotY));
      ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotZ));
      ms.scale(var9, var9, var9);
      Matrix4f var10 = ms.peek().getPositionMatrix();
      this.recoveredMethod5(lineBuilder, var10, replAlpha(color, Math.max(1, (int)((color >> 24 & 0xFF) * 0.7F))));
      ms.pop();
      return true;
   }

   public boolean appendBloom(BufferBuilder builder, Matrix4f base, Matrix4f billboard, Matrix4f translated, Matrix4f scaled,
                              Vec3d camPos, float partialTicks, int colorInt, long now) {
      float var10 = this.recoveredMethod2(now);
      if (var10 <= 0.001F) {
         return false;
      }

      Vec3d var11 = this.recoveredMethod3(partialTicks);
      if (var11 == null) {
         return false;
      }

      float var12 = this.fading ? MathHelper.lerp(MathHelper.clamp((float)(now - this.fadeStartTime) / 320.0F, 0.0F, 1.0F), 1.0F, 0.55F) : 1.0F;
      float var13 = 0.95F * var12;
      int var14 = (int)(var10 * 0.15F * 255.0F);
      if (var14 <= 0) {
         return false;
      }

      int var15 = colorInt >> 16 & 0xFF;
      int var16 = colorInt >> 8 & 0xFF;
      int var17 = colorInt & 0xFF;
      // push / translate / rotate yaw / rotate pitch / scale: scale only rewrites the 3x3 block and
      // keeps the translation column. MatrixStack.translate(double...) narrows to float like this.
      translated.set(base).translate((float)(var11.x - camPos.x), (float)(var11.y - camPos.y), (float)(var11.z - camPos.z));
      Matrix4f var18 = scaled.set(billboard).scale(var13, var13, var13);
      var18.m30(translated.m30()).m31(translated.m31()).m32(translated.m32()).m33(translated.m33());
      builder.vertex(var18, -0.5F, 0.5F, 0.0F).texture(0.0F, 1.0F).color(var15, var16, var17, var14);
      builder.vertex(var18, 0.5F, 0.5F, 0.0F).texture(1.0F, 1.0F).color(var15, var16, var17, var14);
      builder.vertex(var18, 0.5F, -0.5F, 0.0F).texture(1.0F, 0.0F).color(var15, var16, var17, var14);
      builder.vertex(var18, -0.5F, -0.5F, 0.0F).texture(0.0F, 0.0F).color(var15, var16, var17, var14);
      return true;
   }

   private void recoveredMethod1(long now) {
      if (!this.fading) {
         Vec3d var3 = this.recoveredMethod3(1.0F);
         if (var3 != null) {
            this.worldX = var3.x;
            this.worldY = var3.y;
            this.worldZ = var3.z;
         }

         this.fadeStartTime = now;
         this.fading = true;
         this.entity = null;
      }
   }

   private float recoveredMethod2(long now) {
      if (!this.fading) {
         float var3 = MathHelper.clamp((float)(now - this.time) / 140.0F, 0.0F, 1.0F);
         float var4 = 1.0F - MathHelper.clamp((float)(now - this.time - 440L) / 120.0F, 0.0F, 0.35F);
         return var3 * var4;
      } else {
         return 1.0F - MathHelper.clamp((float)(now - this.fadeStartTime) / 320.0F, 0.0F, 1.0F);
      }
   }

   private Vec3d recoveredMethod3(float partialTicks) {
      return !this.fading && this.entity != null
         ? new Vec3d(
            MathHelper.lerp(partialTicks, this.entity.lastRenderX, this.entity.getX()) + this.x,
            MathHelper.lerp(partialTicks, this.entity.lastRenderY, this.entity.getY()) + this.y,
            MathHelper.lerp(partialTicks, this.entity.lastRenderZ, this.entity.getZ()) + this.z
         )
         : new Vec3d(this.worldX, this.worldY, this.worldZ);
   }

   private void recoveredMethod4(BufferBuilder fb, Matrix4f m, int color) {
      float var4 = -0.5F;
      float var5 = 0.5F;
      int var6 = replAlpha(color, Math.max(1, (int)((color >> 24 & 0xFF) * 0.16F)));
      this.recoveredMethod6(fb, m, var4, var4, var4, var5, var5, var5, var6);
   }

   private void recoveredMethod5(BufferBuilder buf, Matrix4f m, int color) {
      for (byte[] var7 : CUBE_EDGES) {
         buf.vertex(m, var7[0] * 0.5F, var7[1] * 0.5F, var7[2] * 0.5F).color(color);
         buf.vertex(m, var7[3] * 0.5F, var7[4] * 0.5F, var7[5] * 0.5F).color(color);
      }
   }

   private void recoveredMethod6(BufferBuilder buf, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
      buf.vertex(m, x1, y1, z1).color(color);
      buf.vertex(m, x2, y1, z1).color(color);
      buf.vertex(m, x2, y1, z2).color(color);
      buf.vertex(m, x1, y1, z2).color(color);
      buf.vertex(m, x1, y2, z1).color(color);
      buf.vertex(m, x1, y2, z2).color(color);
      buf.vertex(m, x2, y2, z2).color(color);
      buf.vertex(m, x2, y2, z1).color(color);
      buf.vertex(m, x1, y1, z1).color(color);
      buf.vertex(m, x1, y2, z1).color(color);
      buf.vertex(m, x2, y2, z1).color(color);
      buf.vertex(m, x2, y1, z1).color(color);
      buf.vertex(m, x1, y1, z2).color(color);
      buf.vertex(m, x2, y1, z2).color(color);
      buf.vertex(m, x2, y2, z2).color(color);
      buf.vertex(m, x1, y2, z2).color(color);
      buf.vertex(m, x1, y1, z1).color(color);
      buf.vertex(m, x1, y1, z2).color(color);
      buf.vertex(m, x1, y2, z2).color(color);
      buf.vertex(m, x1, y2, z1).color(color);
      buf.vertex(m, x2, y1, z1).color(color);
      buf.vertex(m, x2, y2, z1).color(color);
      buf.vertex(m, x2, y2, z2).color(color);
      buf.vertex(m, x2, y1, z2).color(color);
   }
}
}
