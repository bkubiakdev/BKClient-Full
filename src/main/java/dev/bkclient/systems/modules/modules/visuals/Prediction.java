package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.shared.PredicateValue;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.PotionUtility;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.inventory.EnchantmentUtility;
import dev.bkclient.utility.render.Draw3DUtility;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.Utils;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;

@ModuleInfo(name = "Prediction", category = ModuleCategory.VISUALS)
public class Prediction extends BaseModule {
   private static final Identifier BLOOM_TEXTURE = BKClientInit.id("textures/bloom.png");
   private static final Identifier HIT_TEXTURE = BKClientInit.id("textures/hit.png");
   private final List<Prediction.Predicted> predicted = new ArrayList<>();
   private final List<Prediction.Landed> landed = new ArrayList<>();
   private final SelectSetting entities = new SelectSetting(this, "modules.settings.prediction.entities");
   private final ModeSetting renderMode = new ModeSetting(this, "modules.settings.prediction.render_mode");
   private final ModeSetting.Value defaultMode = new ModeSetting.Value(this.renderMode, "modules.settings.prediction.render_mode.default");
   private final ModeSetting.Value glowMode = new ModeSetting.Value(this.renderMode, "modules.settings.prediction.render_mode.glow").select();
   private final BooleanSetting inHand = new BooleanSetting(this, "modules.settings.prediction.hand").enable();
   private final BooleanSetting walls = new BooleanSetting(this, "modules.settings.prediction.walls").enable();
   private final BooleanSetting hud = new BooleanSetting(this, "modules.settings.prediction.hud");
   private final EventListener<HudRenderEvent> onRender2D = event -> {
      CustomDrawContext context = event.getContext();
      MatrixStack ms = context.getMatrices();

      for (Prediction.Predicted predict : this.predicted) {
         Vec2f screenPos = Utils.worldToScreen((Vec3d)predict.vectors.getLast());
         if (screenPos != null) {
            float x = screenPos.x;
            float y = screenPos.y;
            Font font = Fonts.MEDIUM.getFont(13.0F);
            float height = font.height() + 6.0F;
            float yOff = -height;
            String name = predict.entity.getName().getString().replace("Брошенный эндер-жемчуг", "Ender Pearl");
            if (predict.entity instanceof PotionEntity potion) {
               name = potion.getStack().getFormattedName().getString();
            }

            name = name.replace("] ", "").replace("[", "") + String.format(" (%ss)", TextUtility.formatNumber(predict.ticks / 20.0F));

            ItemStack stack = switch (predict.entity) {
               case ThrownItemEntity item -> item.getStack();
               case PersistentProjectileEntity itemx -> itemx.getItemStack();
               case ItemEntity itemxx -> itemxx.getStack();
               default -> Items.ARROW.getDefaultStack();
            };
            float distance = (float)((Vec3d)predict.vectors.getLast()).distanceTo(mc.player.getEyePos());
            float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
            ms.push();
            ms.translate(x, y, 0.0F);
            ms.scale(scale, scale, 1.0F);
            float firstWidth = font.width(name) + 20.0F;
            context.drawRect(-firstWidth / 2.0F, yOff, firstWidth, height, new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F));
            context.drawItem(stack, -firstWidth / 2.0F, yOff, 1.0F);
            context.drawText(font, name, -firstWidth / 2.0F + 17.0F, yOff + 3.0F, Colors.WHITE);
            yOff += height;
            if (predict.entity instanceof ProjectileEntity projectile && projectile.getOwner() instanceof AbstractClientPlayerEntity player) {
               String owner = "From " + (projectile.getOwner() == mc.player ? "You" : projectile.getOwner().getName().getString());
               float secondWidth = font.width(owner) + 22.0F;
               context.drawRect(-secondWidth / 2.0F, yOff, secondWidth, height, new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F));
               context.drawHead(player, -secondWidth / 2.0F, yOff, height, BorderRadius.ZERO, Colors.WHITE);
               context.drawText(font, owner, -secondWidth / 2.0F + 19.0F, yOff + 3.0F, Colors.WHITE);
               yOff += height;
            }

            if (predict.entity instanceof PotionEntity potion) {
               for (StatusEffectInstance effect : PotionUtility.effects(potion.getStack())) {
                  String potionName = ((StatusEffect)effect.getEffectType().value()).getName().getString();
                  int amplifier = effect.getAmplifier();
                  int duration = effect.getDuration();
                  String potionLevel = amplifier > 0 ? " " + (amplifier + 1) : "";
                  String potionTime = this.formatDuration(duration);
                  String fullPotionText = potionName + potionLevel + " (" + potionTime + ")";
                  float potionWidth = font.width(fullPotionText) + 6.0F;
                  context.drawRect(-potionWidth / 2.0F, yOff + 5.0F, potionWidth, height, new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F));
                  context.drawText(
                     font,
                     fullPotionText,
                     -potionWidth / 2.0F + 3.0F,
                     yOff + 8.0F,
                     ColorRGBA.fromInt(((StatusEffect)effect.getEffectType().value()).getColor()).withAlpha(255.0F)
                  );
                  yOff += height;
               }
            }

            ms.pop();
         }
      }

      if (this.hud.isEnabled()) {
         Font fontx = Fonts.MEDIUM.getFont(10.0F);
         float yOffx = 0.0F;

         for (Prediction.Predicted predictx : this.predicted) {
            if (predictx.collidedEntity == mc.player && !(predictx.entity instanceof EnderPearlEntity)) {
               String namex = predictx.entity.getName().getString().replace("Брошенный эндер-жемчуг", "Ender Pearl")
                  + String.format(" (%ss)", TextUtility.formatNumber(predictx.ticks / 20.0F));
               context.drawCenteredText(fontx, "Incoming " + namex, sr.getScaledWidth() / 2.0F, sr.getScaledHeight() / 2.0F + 20.0F + yOffx, Colors.WHITE);
               yOffx += fontx.height() + 3.0F;
            }
         }
      }
   };
   private final EventListener<Render3DEvent> onRender3D = event -> {
      MatrixStack ms = event.getMatrices();
      ms.push();
      RenderUtility.setupRender3D(true);
      RenderUtility.prepareMatrices(ms);
      RenderSystem.enableDepthTest();
      if (this.walls.isEnabled()) {
         RenderSystem.disableDepthTest();
      }

      if (this.defaultMode.isSelected()) {
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

         for (Prediction.Predicted predict : this.predicted) {
            Vec3d prevPos = (Vec3d)predict.vectors.getFirst();
            Draw3DUtility.drawLine(ms, builder, Utils.getInterpolatedPos(predict.entity, event.getTickDelta()), prevPos, Colors.ACCENT);

            for (Vec3d pos : predict.vectors) {
               Draw3DUtility.drawLine(ms, builder, prevPos, pos, Colors.ACCENT);
               prevPos = pos;
            }
         }

         RenderUtility.buildBuffer(builder);
      } else {
         RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         // Each glow used to push the stack, translate, rotate by the camera and pop (20 per segment,
         // up to 3000 per projectile). The rotation part is the same for all of them (JOML's rotate reads
         // only the 3x3 block, translate never touches it), so it is computed once; each glow only takes
         // the translation that the same translate call produces. Vertices come out bit for bit as before.
         Matrix4f base = ms.peek().getPositionMatrix();
         Matrix4f billboard = new Matrix4f(base).translate(0.0F, 0.0F, 0.0F).rotate(mc.gameRenderer.getCamera().getRotation());
         Matrix4f translated = this.glowScratch;
         Vec3d eyePos = mc.player.getEyePos();
         int coreRgb = Colors.ACCENT.withAlpha(255.0F * 1.0F).getRGB();
         int haloRgb = Colors.ACCENT.withAlpha(255.0F * 0.05F).getRGB();

         for (Prediction.Predicted predict : this.predicted) {
            Vec3d prevPos = (Vec3d)predict.vectors.getFirst();
            Vec3d entityPos = Utils.getInterpolatedPos(predict.entity, event.getTickDelta());
            if (entityPos.distanceTo(eyePos) > 2.0) {
               float distance = (float)prevPos.distanceTo(entityPos);
               float coreSize = distance / 3.0F;
               float haloSize = distance * 2.0F;
               // entityPos.add(prevPos.subtract(entityPos).multiply(t)), component by component.
               double dx = prevPos.x - entityPos.x, dy = prevPos.y - entityPos.y, dz = prevPos.z - entityPos.z;
               for (int i = 0; i < 10; i++) {
                  double t = i / 10.0F;
                  double x = entityPos.x + dx * t, y = entityPos.y + dy * t, z = entityPos.z + dz * t;
                  drawGlow(buffer, base, billboard, translated, x, y, z, coreSize, coreRgb);
                  drawGlow(buffer, base, billboard, translated, x, y, z, haloSize, haloRgb);
               }
            }

            for (Vec3d pos : predict.vectors) {
               if (pos.distanceTo(eyePos) > 2.0) {
                  float distance = (float)pos.distanceTo(prevPos);
                  float coreSize = distance / 3.0F;
                  float haloSize = distance * 2.0F;
                  // prevPos.add(pos.subtract(prevPos).multiply(t)), component by component.
                  double dx = pos.x - prevPos.x, dy = pos.y - prevPos.y, dz = pos.z - prevPos.z;
                  for (int i = 0; i < 10; i++) {
                     double t = i / 10.0F;
                     double x = prevPos.x + dx * t, y = prevPos.y + dy * t, z = prevPos.z + dz * t;
                     drawGlow(buffer, base, billboard, translated, x, y, z, coreSize, coreRgb);
                     drawGlow(buffer, base, billboard, translated, x, y, z, haloSize, haloRgb);
                  }
               }

               prevPos = pos;
            }

            float size = 9.0F;
            if (predict.entity instanceof PotionEntity) {
               ms.push();
               ms.translate((Vec3d)predict.vectors.getLast());
               ms.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-90.0F));
               DrawUtility.drawImage(ms, buffer, -size / 2.0F, -size / 2.0F, 0.0, size, size, Colors.ACCENT.withAlpha(255.0F));
               ms.pop();
            }
         }

         RenderUtility.buildBuffer(buffer);
      }

      float size = 1.0F;
      RenderSystem.setShaderTexture(0, HIT_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (Prediction.Landed landed : this.landed) {
         if (landed.collidedEntity == null) {
            ms.push();
            ms.translate(landed.hitResult.getPos());
            ms.multiply(landed.hitResult.getSide().getRotationQuaternion());
            ms.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-90.0F));
            DrawUtility.drawImage(ms, buffer, -size / 2.0F, -size / 2.0F, 0.0, size, size, Colors.ACCENT.withAlpha(255.0F));
            ms.pop();
         }
      }

      RenderUtility.buildBuffer(buffer);
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      BufferBuilder quadsBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

      for (Prediction.Landed landedx : this.landed) {
         if (landedx.collidedEntity != null) {
            Draw3DUtility.renderFilledBox(ms, quadsBuffer, landedx.collidedEntity.getBoundingBox(), Colors.ACCENT.mulAlpha(0.5F));
         }
      }

      RenderUtility.buildBuffer(quadsBuffer);
      BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (Prediction.Landed landedxx : this.landed) {
         if (landedxx.collidedEntity != null) {
            Draw3DUtility.renderOutlinedBox(ms, linesBuffer, landedxx.collidedEntity.getBoundingBox(), Colors.ACCENT);
         }
      }

      RenderUtility.buildBuffer(linesBuffer);
      RenderUtility.endRender3D();
      ms.pop();
   };

   public Prediction() {
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.pearls", entity -> entity instanceof EnderPearlEntity).select();
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.tridents", entity -> entity instanceof TridentEntity).select();
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.snowballs", entity -> entity instanceof SnowballEntity).select();
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.arrows", entity -> entity instanceof ArrowEntity).select();
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.potions", entity -> entity instanceof PotionEntity).select();
      new PredicateValue<>(this.entities, "modules.settings.prediction.entities.items", entity -> entity instanceof ItemEntity);
   }

   @Override
   public void tick() {
      this.predicted.clear();
      this.landed.clear();
      List<ProjectileEntity> projectiles = new ArrayList<>();
      if (this.inHand.isEnabled()) {
         ItemStack handStack = mc.player.getMainHandStack();
         Entity inHand = null;
         if (handStack.getItem() instanceof EnderPearlItem) {
            inHand = new EnderPearlEntity(mc.world, mc.player, handStack);
         } else if (handStack.getItem() instanceof TridentItem && mc.player.isUsingItem()) {
            inHand = new TridentEntity(mc.world, mc.player, handStack);
         } else if (handStack.getItem() instanceof BowItem && mc.player.isUsingItem()) {
            ItemStack arrowStack = new ItemStack(Items.ARROW);
            inHand = new ArrowEntity(mc.world, mc.player, arrowStack, handStack);
         } else if (handStack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(handStack)) {
            boolean hasMultishot = EnchantmentUtility.getEnchantmentLevel(handStack, Enchantments.MULTISHOT) > 0;
            ItemStack arrowStack = new ItemStack(Items.ARROW);
            if (hasMultishot) {
               for (int i = 0; i < 3; i++) {
                  ArrowEntity arrow = new ArrowEntity(mc.world, mc.player, arrowStack, handStack);
                  projectiles.add(arrow);
               }
            } else {
               inHand = new ArrowEntity(mc.world, mc.player, arrowStack, handStack);
            }
         }

         if (inHand instanceof ProjectileEntity projectile) {
            float speed = 1.5F;
            if (inHand instanceof TridentEntity) {
               speed = 2.5F;
            } else if (inHand instanceof ArrowEntity) {
               speed = 3.0F;
            }

            this.setVelocity(projectile, mc.player, mc.player.getPitch(), mc.player.getYaw(), 0.0F, speed, 1.0F);
            this.predict(projectile, true);
         }
      }

      if (!projectiles.isEmpty()) {
         float speed = 3.15F;
         float spreadAngle = 10.0F;

         for (int i = 0; i < projectiles.size(); i++) {
            ProjectileEntity projectile = projectiles.get(i);
            float yawOffset = 0.0F;
            if (i == 0) {
               yawOffset = -spreadAngle;
            } else if (i == 2) {
               yawOffset = spreadAngle;
            }

            this.setVelocity(projectile, mc.player, mc.player.getPitch(), mc.player.getYaw() + yawOffset, 0.0F, speed, 1.0F);
            this.predict(projectile, true);
         }
      }

      for (Entity entity : mc.world.getEntities()) {
         this.predict(entity, false);
      }
   }

   private void predict(Entity entity, boolean inHand) {
      if (this.isValid(entity)) {
         if (entity instanceof ProjectileEntity pearl && pearl.getOwner() == null) {
            // Nearest player as the owner. Sorting mc.world.getPlayers() in place reordered the
            // world's own player list; pick the minimum without touching it.
            AbstractClientPlayerEntity nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
               double distance = player.distanceTo(pearl);
               if (nearest == null || distance < nearestDistance) {
                  nearest = player;
                  nearestDistance = distance;
               }
            }
            if (nearest != null) {
               pearl.setOwner((Entity)nearest);
            }
         }

         List<Vec3d> positions = new ArrayList<>();
         Vec3d lastPos = entity.getPos();
         Vec3d lastMotion = entity.getVelocity();
         Entity collidedEntity = null;
         int ticks = 0;
         BlockHitResult blockHitResult = null;

         for (int i = 0; i < 150; i++) {
            Vec3d motion = this.predictMotion(entity, lastMotion);
            Vec3d pos = lastPos.add(motion);
            ticks = i;
            blockHitResult = mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.COLLIDER, FluidHandling.NONE, entity));
            Entity collided = this.checkEntityCollision(entity, lastPos, pos);
            if (collided != null) {
               positions.add(pos);
               collidedEntity = collided;
               break;
            }

            if (blockHitResult.getType() != Type.MISS) {
               positions.add(blockHitResult.getPos());
               break;
            }

            positions.add(pos);
            lastPos = pos;
            lastMotion = motion;
         }

         if (!positions.isEmpty()) {
            if (inHand) {
               this.landed.add(new Prediction.Landed(entity, (Vec3d)positions.getLast(), ticks, collidedEntity, blockHitResult));
            } else {
               this.predicted.add(new Prediction.Predicted(entity, positions, ticks, collidedEntity));
            }
         }
      }
   }

   private final Matrix4f glowScratch = new Matrix4f();

   /**
    * Same vertices as push / translate(pos) / multiply(camera rotation) / DrawUtility.drawImage / pop.
    * MatrixStack.translate(double...) narrows to float exactly like this.
    */
   private static void drawGlow(BufferBuilder buffer, Matrix4f base, Matrix4f billboard, Matrix4f translated,
                                double x, double y, double z, float size, int rgb) {
      translated.set(base).translate((float)x, (float)y, (float)z);
      billboard.m30(translated.m30()).m31(translated.m31()).m32(translated.m32()).m33(translated.m33());
      double left = -size / 2.0F;
      double top = -size / 2.0F;
      buffer.vertex(billboard, (float)left, (float)(top + size), 0.0F).texture(0.0F, 1.0F).color(rgb);
      buffer.vertex(billboard, (float)(left + size), (float)(top + size), 0.0F).texture(1.0F, 1.0F).color(rgb);
      buffer.vertex(billboard, (float)(left + size), (float)top, 0.0F).texture(1.0F, 0.0F).color(rgb);
      buffer.vertex(billboard, (float)left, (float)top, 0.0F).texture(0.0F, 0.0F).color(rgb);
   }

   private boolean isValid(Entity entity) {
      boolean valid = false;

      for (SelectSetting.Value selectedValue : this.entities.getSelectedValues()) {
         PredicateValue<Entity> predicateValue = (PredicateValue<Entity>)selectedValue;
         if (predicateValue.predicated(entity)) {
            valid = true;
         }
      }

      return entity instanceof TridentEntity trident && trident.returnTimer > 0
         ? false
         : valid
            && (Math.abs(entity.getVelocity().x + entity.getVelocity().z) > 0.01F || Math.abs(entity.getVelocity().y) > 0.2F);
   }

   /**
    * Entity hit on one simulated step, {@code segmentStart} to {@code predictedPos}. It used to cast
    * from the projectile's current position on every step, so the search box and the straight ray
    * grew with the whole flight instead of following the arc.
    */
   private Entity checkEntityCollision(Entity movingEntity, Vec3d segmentStart, Vec3d predictedPos) {
      Vec3d direction = predictedPos.subtract(segmentStart);
      if (direction.lengthSquared() == 0.0) {
         return null;
      }

      EntityHitResult hitResult = ProjectileUtil.raycast(
         movingEntity,
         segmentStart,
         predictedPos,
         movingEntity.getBoundingBox().offset(segmentStart.subtract(movingEntity.getPos())).stretch(direction).expand(0.5),
         entity -> mc.player != entity
            && entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && entity != movingEntity,
         direction.lengthSquared()
      );
      return hitResult != null ? hitResult.getEntity() : null;
   }

   private void setVelocity(ProjectileEntity entity, double x, double y, double z, float power) {
      Vec3d vec3d = this.calculateVelocity(entity, x, y, z, power);
      entity.setVelocity(vec3d);
      entity.velocityDirty = true;
      double d = vec3d.horizontalLength();
      entity.setYaw((float)(MathHelper.atan2(vec3d.x, vec3d.z) * 180.0 / (float) Math.PI));
      entity.setPitch((float)(MathHelper.atan2(vec3d.y, d) * 180.0 / (float) Math.PI));
      entity.prevYaw = entity.getYaw();
      entity.prevPitch = entity.getPitch();
   }

   private void setVelocity(ProjectileEntity entity, Entity shooter, float pitch, float yaw, float roll, float speed, float divergence) {
      float f = -MathHelper.sin(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
      float g = -MathHelper.sin((pitch + roll) * (float) (Math.PI / 180.0));
      float h = MathHelper.cos(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
      this.setVelocity(entity, f, g, h, speed);
      Vec3d vec3d = shooter.getMovement();
      entity.setVelocity(entity.getVelocity().add(vec3d.x, shooter.isOnGround() ? 0.0 : vec3d.y, vec3d.z));
   }

   private Vec3d calculateVelocity(ProjectileEntity entity, double x, double y, double z, float power) {
      return new Vec3d(x, y, z).normalize().multiply(power);
   }

   private Vec3d predictMotion(Entity entity, Vec3d motion) {
      return motion.multiply(0.99).add(0.0, -entity.getFinalGravity(), 0.0);
   }

   private String formatDuration(int ticks) {
      int seconds = ticks / 20;
      int minutes = seconds / 60;
      int remainingSeconds = seconds % 60;
      return minutes > 0 ? String.format("%d:%02d", minutes, remainingSeconds) : String.format("0:%02d", remainingSeconds);
   }

   record Landed(Entity entity, Vec3d pos, int ticks, Entity collidedEntity, BlockHitResult hitResult) {
   }

   record Predicted(Entity entity, List<Vec3d> vectors, int ticks, Entity collidedEntity) {
   }
}
