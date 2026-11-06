package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import dev.bkclient.framework.shader.impl.ParticleLightPost;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.LinkedModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.utility.render.ambience.SkyWorldStreakRenderer;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.render.Draw3DUtility;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.Utils;
import dev.bkclient.utility.time.Timer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

@ModuleInfo(name = "World", category = ModuleCategory.VISUALS, desc = "modules.descriptions.world")
public class World extends BaseModule {

    public World() {
        LinkedModeSetting.toggle(this, "color.source", this.themeSync, "Theme", "Custom");
    }

   private final List<World.Particle> particles = new ArrayList<>();
   private final ModeSetting effect = new ModeSetting(this, "modules.settings.world.effect");
   private final ModeSetting.Value classic = new ModeSetting.Value(effect, "modules.settings.world.effect.classic").select();
   private final ModeSetting.Value shootingStars = new ModeSetting.Value(effect, "modules.settings.world.effect.shooting_stars");
   private final BooleanSetting themeSync = new BooleanSetting(this, "theme.sync").enable();
   private final ColorSetting color = new ColorSetting(this, "color", this.themeSync::isEnabled).color(Colors.ACCENT);
   private final ModeSetting starShape = new ModeSetting(this, "modules.settings.world.stars.shape", () -> !shootingStars.isSelected());
   private final ModeSetting.Value cubeHead = new ModeSetting.Value(starShape, "modules.settings.world.stars.shape.cube").select();
   private final ModeSetting.Value roundHead = new ModeSetting.Value(starShape, "modules.settings.world.stars.shape.round");
   private final SliderSetting starCount = new SliderSetting(this, "modules.settings.world.stars.count", () -> !shootingStars.isSelected())
       .min(4).max(32).step(2).currentValue(24);
   private final SliderSetting starSpeed = new SliderSetting(this, "modules.settings.world.stars.speed", () -> !shootingStars.isSelected())
       .min(0).max(200).step(5).currentValue(80).suffix("%");
   private final SliderSetting starLength = new SliderSetting(this, "modules.settings.world.stars.length", () -> !shootingStars.isSelected())
       .min(10).max(100).step(5).currentValue(60).suffix("%");
   private final SliderSetting starSize = new SliderSetting(this, "modules.settings.world.stars.size", () -> !shootingStars.isSelected())
       .min(50).max(200).step(5).currentValue(100).suffix("%");
   private final SkyWorldStreakRenderer starRenderer = new SkyWorldStreakRenderer();
   private final BooleanSetting lighting = new BooleanSetting(this, "modules.settings.world.lighting", shootingStars::isSelected);
   private final SliderSetting lightRadius = new SliderSetting(this, "modules.settings.world.lighting.radius", () -> shootingStars.isSelected() || !lighting.isEnabled())
       .min(2).max(7).step(0.5F).currentValue(4);
   private final SliderSetting lightStrength = new SliderSetting(this, "modules.settings.world.lighting.strength", () -> shootingStars.isSelected() || !lighting.isEnabled())
       .min(0).max(150).step(10).currentValue(100);
   private int visibilityCursor;
   private ClientWorld particleWorld;
   public boolean rendersShootingStars() { return isEnabled() && shootingStars.isSelected(); }
   private final EventListener<dev.bkclient.systems.event.impl.game.WorldChangeEvent> onWorldChange = event -> onWorldChanged();
   public void onWorldChanged() { clearParticles(); starRenderer.reset(); }
   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (mc.world == null || mc.player == null) return;
      ColorRGBA tint = themeSync.isEnabled() ? Colors.getAccent() : color.getColor();
      if (shootingStars.isSelected()) {
         starRenderer.renderWorld(event, Math.round(starCount.getCurrentValue()), starSpeed.getCurrentValue()/100,
             starLength.getCurrentValue(), starSize.getCurrentValue(), cubeHead.isSelected(), tint, tint.getAlpha()/255f, 1);
         return;
      }
      starRenderer.reset();
      if (mc.world != particleWorld || particles.isEmpty()) return;
      for (Particle particle : particles) {
         particle.alpha.update(!particle.expired());
         particle.visibility.update(!particle.occluded);
      }
      renderLighting(event, tint);
      MatrixStack ms = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      ms.push();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      // Every particle used to push, translate, rotate by the camera and pop. The rotation part is the
      // same for all of them (JOML's rotate reads only the 3x3 block and translate never touches it), so
      // it is computed once; each particle only takes its translation. Vertices are bit-identical.
      Matrix4f base = ms.peek().getPositionMatrix();
      Matrix4f billboard = new Matrix4f(base).translate(0.0F, 0.0F, 0.0F).rotate(camera.getRotation());
      Matrix4f translated = this.billboardScratch;
      for (World.Particle particle : this.particles) {
         // alpha <= 0 gives an alpha byte of 0 (ColorRGBA clamps); with blend SRC_ALPHA/ONE, depth writes
         // off and the shaders' alpha == 0 discard such a quad changes nothing on screen.
         if (particle.alpha.getValue() <= 0.0F) continue;
         Vec3d pos = Utils.getInterpolatedPos(particle.prev, particle.pos, event.getTickDelta());
         float bigSize = 4.0F * particle.size;
         // RenderUtility.prepareMatrices: pos.subtract(cameraPos), MatrixStack.translate(double...) narrows to float.
         translated.set(base).translate((float)(pos.x - cameraPos.x), (float)(pos.y - cameraPos.y), (float)(pos.z - cameraPos.z));
         billboard.m30(translated.m30()).m31(translated.m31()).m32(translated.m32()).m33(translated.m33());
         int rgb = tint.withAlpha(255.0F * particle.alpha.getValue() * 0.4F).getRGB();
         // DrawUtility.drawImage(-bigSize / 2, -bigSize / 2, 0, bigSize, bigSize) on the billboard matrix.
         double x = -bigSize / 2.0F;
         double y = -bigSize / 2.0F;
         builder.vertex(billboard, (float)x, (float)(y + bigSize), 0.0F).texture(0.0F, 1.0F).color(rgb);
         builder.vertex(billboard, (float)(x + bigSize), (float)(y + bigSize), 0.0F).texture(1.0F, 1.0F).color(rgb);
         builder.vertex(billboard, (float)(x + bigSize), (float)y, 0.0F).texture(1.0F, 0.0F).color(rgb);
         builder.vertex(billboard, (float)x, (float)y, 0.0F).texture(0.0F, 0.0F).color(rgb);
      }

      BuiltBuffer builtLinesBuffer1 = builder.endNullable();
      if (builtLinesBuffer1 != null) {
         BufferRenderer.drawWithGlobalProgram(builtLinesBuffer1);
      }

      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.disableDepthTest();
      ms.pop();
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (World.Particle particle : this.particles) {
         // Same invisible case as the glow pass: every line colour would have alpha 0.
         if (particle.alpha.getValue() <= 0.0F) continue;
         Vec3d pos = Utils.getInterpolatedPos(particle.prev, particle.pos, event.getTickDelta());
         Vec3d rot = Utils.getInterpolatedPos(particle.prevRot, particle.rotate, event.getTickDelta());
         ms.push();
         ms.translate(pos.add(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()));
         ms.multiply(new Quaternionf().rotationXYZ((float)rot.x, (float)rot.y, (float)rot.z));
         ms.scale(particle.size, particle.size, particle.size);
         Draw3DUtility.renderBoxInternalDiagonals(
            ms, linesBuffer, UNIT_BOX, tint.withAlpha(255.0F * particle.alpha.getValue() * 0.4F)
         );
         Draw3DUtility.renderOutlinedBox(
            ms, linesBuffer, UNIT_BOX, tint.withAlpha(205.0F * particle.alpha.getValue())
         );
         ms.pop();
      }

      BuiltBuffer builtLinesBuffer = linesBuffer.endNullable();
      if (builtLinesBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtLinesBuffer);
      }

      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   };

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null) {
         clearParticles();
         starRenderer.reset();
         return;
      }
      if (particleWorld != mc.world) {
         clearParticles();
         starRenderer.reset();
         particleWorld = mc.world;
      }
      if (shootingStars.isSelected()) {
         particles.clear(); visibilityCursor=0;
         return;
      }
      starRenderer.reset();
      particles.removeIf(p -> p.alpha.getValue() == 0 && p.expired());
      for (Particle particle : particles) {
         particle.tick();
      }
      int limit = 100;
      int active = (int) particles.stream().filter(p -> !p.retiring).count();
      for (int i = particles.size() - 1; i >= 0 && active > limit; i--) {
         Particle particle = particles.get(i);
         if (!particle.retiring) { particle.retiring = true; active--; }
      }
      for (int i = 0; i < 4 && active < limit; i++) {
         Vec3d pos = spawnPosition();
         if (pos == null) break;
         Particle particle = new Particle(pos, Vec3d.ZERO,
             new Vec3d(MathUtility.random(-1.0, 1.0), MathUtility.random(0.0, 2.0), MathUtility.random(-1.0, 1.0)),
             new Vec3d(MathUtility.random(-1.0, 1.0), MathUtility.random(-1.0, 1.0), MathUtility.random(-1.0, 1.0)),
             (long) MathUtility.random(1500.0, 4500.0),
             MathUtility.random(0.1F, 0.3F));
         if (lighting.isEnabled()) {
            particle.occluded = isOccluded(mc.gameRenderer.getCamera().getPos(), pos);
            particle.visibility.setValue(particle.occluded ? 0 : 1);
         }
         particles.add(particle);
         active++;
      }
      updateVisibility();
   }

   @Override
   public void onDisable() {
      onWorldChanged();
   }

   private void clearParticles() {
      particles.clear();
      particleWorld = null;
      visibilityCursor = 0;
   }

   private Vec3d spawnPosition() {
      Vec3d fallback = null;
      for (int attempt = 0; attempt < 8; attempt++) {
         Vec3d pos = mc.player.getPos().add(MathUtility.random(-20.0, 20.0),
             MathUtility.random(1.6, 6.0), MathUtility.random(-20.0, 20.0));
         if (insideBlock(pos)) continue;
         if (fallback == null) fallback = pos;
         if (!insideBlock(pos.subtract(0, 1.2, 0))) return pos;
      }
      return fallback;
   }

   private boolean insideBlock(Vec3d pos) {
      BlockPos blockPos = BlockPos.ofFloored(pos);
      if (mc.world.isOutOfHeightLimit(blockPos.getY())) return true;
      BlockState state = mc.world.getBlockState(blockPos);
      if (state.isAir()) return false;
      Vec3d local = pos.subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      for (Box bounds : state.getCollisionShape(mc.world, blockPos).getBoundingBoxes()) {
         if (bounds.expand(0.01).contains(local)) return true;
      }
      return false;
   }

   private boolean isOccluded(Vec3d camera, Vec3d pos) {
      return camera.squaredDistanceTo(pos) > 48 * 48 || mc.world.raycast(new RaycastContext(
          camera, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player))
          .getType() != HitResult.Type.MISS;
   }

   private void updateVisibility() {
      if (!lighting.isEnabled()) {
         for (Particle particle : particles) particle.occluded = false;
         return;
      }
      int size = particles.size();
      if (size == 0) return;
      Vec3d camera = mc.gameRenderer.getCamera().getPos();
      int budget = Math.min(size, 40);
      for (Particle particle : particles) {
         if (particle.selectedLight && budget > 0) {
            particle.occluded = isOccluded(camera, particle.pos);
            budget--;
         }
      }
      int visited = 0;
      while (visited < size && budget > 0) {
         Particle particle = particles.get((visibilityCursor + visited++) % size);
         if (!particle.selectedLight) {
            particle.occluded = isOccluded(camera, particle.pos);
            budget--;
         }
      }
      visibilityCursor = (visibilityCursor + visited) % size;
   }

   private void renderLighting(Render3DEvent event, ColorRGBA tint) {
      if (!lighting.isEnabled() || DrawUtility.particleLightPost == null) return;
      float radius = lightRadius.getCurrentValue();
      float strength = lightStrength.getCurrentValue() / 100.0F;
      if (strength <= 0) return;
      Vec3d camera = event.getCamera().getPos();
      Matrix4f viewProjection = new Matrix4f(event.getProjectionMatrix()).mul(event.getPositionMatrix());
      FrustumIntersection frustum = new FrustumIntersection(viewProjection);
      List<LightCandidate> candidates = new ArrayList<>();
      for (Particle particle : particles) {
         float alpha = particle.alpha.getValue() * particle.visibility.getValue();
         Vec3d pos = Utils.getInterpolatedPos(particle.prev, particle.pos, event.getTickDelta()).subtract(camera);
         double distance = pos.length();
         if (alpha <= 0.01F || distance - radius > 48 ||
             !frustum.testSphere((float) pos.x, (float) pos.y, (float) pos.z, radius)) {
            particle.selectedLight = false;
            particle.lightFade.update(false);
         } else {
            candidates.add(new LightCandidate(particle, pos, alpha * strength, distance));
         }
      }
      // Prefer previously selected lights to avoid flicker when distances are similar.
      candidates.sort(Comparator.comparingDouble(c -> c.distance * (c.particle.selectedLight ? 0.5 : 1)));
      for (LightCandidate candidate : candidates) candidate.particle.selectedLight = false;
      List<LightCandidate> selected = new ArrayList<>(32);
      List<LightCandidate> fading = new ArrayList<>();
      float mergeDistance = radius * 0.35F;
      for (LightCandidate candidate : candidates) {
         if (selected.size() >= 32) { fading.add(candidate); continue; }
         LightCandidate nearby = null;
         for (LightCandidate current : selected) {
            if (candidate.pos.distanceTo(current.pos) < mergeDistance) { nearby = current; break; }
         }
         if (nearby == null) {
            selected.add(candidate);
         } else {
            float contribution = candidate.intensity * 0.35F *
                (1 - (float) candidate.pos.distanceTo(nearby.pos) / mergeDistance) *
                (1 - candidate.particle.lightFade.getValue());
            nearby.intensity = Math.min(strength * 2, nearby.intensity + contribution);
            fading.add(candidate);
         }
      }
      List<ParticleLightPost.Light> lights = new ArrayList<>(48);
      for (LightCandidate candidate : selected) {
         candidate.particle.selectedLight = true;
         addLight(lights, candidate, tint, radius, candidate.particle.lightFade.update(1));
      }
      int fadeCount = 0;
      for (LightCandidate candidate : fading) {
         float fade = candidate.particle.lightFade.update(0);
         if (fade > 0.001F && fadeCount < 16 && addLight(lights, candidate, tint, radius, fade)) fadeCount++;
      }
      if (!lights.isEmpty()) DrawUtility.particleLightPost.apply(mc.getFramebuffer(), viewProjection.invert(), lights);
   }

   private boolean addLight(List<ParticleLightPost.Light> lights, LightCandidate candidate, ColorRGBA tint, float radius, float fade) {
      float intensity = candidate.intensity * fade;
      if (intensity <= 0.001F) return false;
      lights.add(new ParticleLightPost.Light((float) candidate.pos.x, (float) candidate.pos.y, (float) candidate.pos.z,
          radius, tint.getRed() / 255.0F, tint.getGreen() / 255.0F, tint.getBlue() / 255.0F, intensity));
      return true;
   }

   private static final Identifier BLOOM_TEXTURE = BKClientInit.id("textures/bloom.png");
   private static final Box UNIT_BOX = new Box(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
   private final Matrix4f billboardScratch = new Matrix4f();

   private static final class LightCandidate {
      final Particle particle;
      final Vec3d pos;
      float intensity;
      final double distance;
      LightCandidate(Particle particle, Vec3d pos, float intensity, double distance) {
         this.particle = particle;
         this.pos = pos;
         this.intensity = intensity;
         this.distance = distance;
      }
   }

   static class Particle {
      Vec3d prev;
      Vec3d prevRot;
      Vec3d pos;
      Vec3d rotate;
      Vec3d motion;
      Vec3d rotateMotion;
      final long liveTicks;
      float size;
      final Timer timer = new Timer();
      final Animation alpha = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
      final Animation visibility = new Animation(200L, 1, Easing.FIGMA_EASE_IN_OUT);
      final Animation lightFade = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
      boolean retiring;
      boolean occluded;
      boolean selectedLight;
      boolean expired() { return retiring || timer.finished(liveTicks); }

      public Particle(Vec3d pos, Vec3d rotate, Vec3d motion, Vec3d rotateMotion, long liveTicks, float size) {
         this.pos = pos;
         this.rotate = rotate;
         this.motion = motion.multiply(0.04F);
         this.rotateMotion = rotateMotion.multiply(0.04F);
         this.liveTicks = liveTicks;
         this.size = size;
         this.prevRot = rotate;
         this.prev = pos;
         this.alpha.setDuration(1000L);
      }

      void tick() {
         this.prev = this.pos;
         this.prevRot = this.rotate;
         this.pos = this.pos.add(this.motion);
         this.rotate = this.rotate.add(this.rotateMotion);
         this.motion = this.motion.multiply(0.98);
         this.rotateMotion = this.rotateMotion.multiply(0.98);
      }
   }
}
