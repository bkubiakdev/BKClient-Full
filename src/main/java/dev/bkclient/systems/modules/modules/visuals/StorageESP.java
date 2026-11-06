package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.BlockEntityTracker;
import dev.bkclient.utility.render.Draw3DUtility;
import dev.bkclient.utility.render.RenderUtility;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

@ModuleInfo(name = "Storage ESP", category = ModuleCategory.VISUALS)
public class StorageESP extends BaseModule {
   private static final Box FULL_BOX = new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   private static final Box EMPTY_BOX = new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   private final SelectSetting blocks = new SelectSetting(this, "modules.settings.storage_esp.blocks");
   private final SelectSetting.Value chests = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.chests").select();
   private final SelectSetting.Value enderChests = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.ender_chests").select();
   private final SelectSetting.Value trappedChests = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.trapped_chests");
   private final SelectSetting.Value furnaces = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.furnaces");
   private final SelectSetting.Value barrels = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.barrels").select();
   private final SelectSetting.Value minecart = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.minecart").select();
   private final SelectSetting.Value shulkers = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.shulkers").select();
   private final SelectSetting.Value droppers = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.droppers");
   private final SelectSetting.Value dispensers = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.dispensers");
   private final SelectSetting.Value hoppers = new SelectSetting.Value(this.blocks, "modules.settings.storage_esp.blocks.hoppers");
   private final SelectSetting renderMode = new SelectSetting(this, "modules.settings.storage_esp.render");
   private final SelectSetting.Value fill = new SelectSetting.Value(this.renderMode, "modules.settings.storage_esp.render.fill").select();
   private final SelectSetting.Value outline = new SelectSetting.Value(this.renderMode, "modules.settings.storage_esp.render.outline").select();
   private final SelectSetting.Value diagonals = new SelectSetting.Value(this.renderMode, "modules.settings.storage_esp.render.diagonals").select();
   private final SelectSetting.Value lines = new SelectSetting.Value(this.renderMode, "modules.settings.storage_esp.render.lines");
   private final SliderSetting maxDistance = new SliderSetting(
         this, "modules.settings.storage_esp.max_distance", "modules.settings.storage_esp.max_distance.description"
      )
      .min(5.0F)
      .max(128.0F)
      .step(1.0F)
      .currentValue(128.0F);
   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (mc.world != null && mc.player != null) {
         MatrixStack matrices = event.getMatrices();
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getPos();
         RenderSystem.enableBlend();
         RenderSystem.disableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder quadsBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

         // One snapshot and one bounding-box pass per frame (the second snapshot() used to return the same
         // list: the tracker is only mutated on this thread). Valid block entities and their camera-relative
         // boxes are collected once and replayed for the lines pass in the same order. Box.offset is
         // deterministic, so the boxes are the same values the old per-mode offset calls produced.
         boolean drawFill = this.fill.isSelected();
         boolean drawDiagonals = this.diagonals.isSelected();
         boolean drawOutline = this.outline.isSelected();
         boolean drawLines = this.lines.isSelected();
         boolean needBoxes = drawFill || drawDiagonals || drawOutline || drawLines;
         List<BlockEntity> validEntities = this.validScratch;
         List<Box> relativeBoxes = this.boxScratch;
         int[] boxEnds = this.boxEndScratch;
         validEntities.clear();
         relativeBoxes.clear();
         List<BlockEntity> snapshot = BlockEntityTracker.snapshot(mc.world); // also prunes, as before
         if (needBoxes) {
            for (BlockEntity blockEntity : snapshot) {
               if (this.isValidEntity(blockEntity)) {
                  this.collectRelativeBoxes(blockEntity, cameraPos, relativeBoxes);
                  if (validEntities.size() == boxEnds.length) {
                     boxEnds = this.boxEndScratch = java.util.Arrays.copyOf(boxEnds, boxEnds.length * 2);
                  }
                  boxEnds[validEntities.size()] = relativeBoxes.size();
                  validEntities.add(blockEntity);
               }
            }
         }

         if (drawFill) {
            int boxIndex = 0;
            for (int i = 0; i < validEntities.size(); i++) {
               BlockEntity blockEntity = validEntities.get(i);
               ColorRGBA fillColor = this.getBlockColor(blockEntity).withAlpha(50.0F);
               for (int end = boxEnds[i]; boxIndex < end; boxIndex++) {
                  Draw3DUtility.renderFilledBox(matrices, quadsBuffer, relativeBoxes.get(boxIndex), fillColor);
               }
            }
         }

         for (Entity entity : mc.world.getEntities()) {
            if (this.isValidCart(entity)) {
               Box boundingBoxx = entity.getBoundingBox();
               if (this.fill.isSelected()) {
                  Draw3DUtility.renderFilledBox(
                     matrices,
                     quadsBuffer,
                     boundingBoxx.offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()),
                     this.getEntityColor(entity).withAlpha(50.0F)
                  );
               }
            }
         }

         RenderUtility.buildBuffer(quadsBuffer);
         BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

         if (drawDiagonals || drawOutline || drawLines) {
            int boxIndex = 0;
            for (int i = 0; i < validEntities.size(); i++) {
               BlockEntity blockEntityx = validEntities.get(i);
               ColorRGBA lineColor = drawDiagonals || drawOutline ? this.getBlockColor(blockEntityx).withAlpha(100.0F) : null;
               for (int end = boxEnds[i]; boxIndex < end; boxIndex++) {
                  Box boundingBoxx = relativeBoxes.get(boxIndex);
                  if (drawDiagonals) {
                     Draw3DUtility.renderBoxInternalDiagonals(matrices, linesBuffer, boundingBoxx, lineColor);
                  }

                  if (drawOutline) {
                     Draw3DUtility.renderOutlinedBox(matrices, linesBuffer, boundingBoxx, lineColor);
                  }

                  if (drawLines) {
                     Vec3d entityPos = blockEntityx.getPos().toCenterPos();
                     Draw3DUtility.renderLineFromPlayer(matrices, linesBuffer, entityPos, this.getBlockColor(blockEntityx));
                  }
               }
            }
         }
         validEntities.clear();
         relativeBoxes.clear();

         for (Entity entityx : mc.world.getEntities()) {
            if (this.isValidCart(entityx)) {
               Box boundingBoxx = entityx.getBoundingBox();
               if (this.diagonals.isSelected()) {
                  Draw3DUtility.renderBoxInternalDiagonals(
                     matrices,
                     linesBuffer,
                     boundingBoxx.offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()),
                     this.getEntityColor(entityx).withAlpha(100.0F)
                  );
               }

               if (this.outline.isSelected()) {
                  Draw3DUtility.renderOutlinedBox(
                     matrices,
                     linesBuffer,
                     boundingBoxx.offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()),
                     this.getEntityColor(entityx).withAlpha(100.0F)
                  );
               }

               if (this.lines.isSelected()) {
                  Vec3d entityPos = entityx.getPos();
                  Draw3DUtility.renderLineFromPlayer(matrices, linesBuffer, entityPos, this.getEntityColor(entityx));
               }
            }
         }

         RenderUtility.buildBuffer(linesBuffer);
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
      }
   };
   private final EventListener<WorldChangeEvent> onWorldChange = event -> BlockEntityTracker.clear();

   // Render-thread scratch for on3DRender: valid block entities, their camera-relative boxes (flattened)
   // and, per entity, the exclusive end index of its boxes in boxScratch.
   private final List<BlockEntity> validScratch = new java.util.ArrayList<>();
   private final List<Box> boxScratch = new java.util.ArrayList<>();
   private int[] boxEndScratch = new int[64];

   /**
    * The former getBoundingBox(blockEntity) (outline-shape boxes offset to the block, or the full box) with each
    * box then offset by -camera, appended to out. Same order and values as the old stream + per-mode offsets.
    */
   private void collectRelativeBoxes(BlockEntity blockEntity, Vec3d cameraPos, List<Box> out) {
      double ox = -cameraPos.getX();
      double oy = -cameraPos.getY();
      double oz = -cameraPos.getZ();
      if (mc.world == null) {
         out.add(EMPTY_BOX.offset(ox, oy, oz));
         return;
      }

      BlockPos blockPos = blockEntity.getPos();
      BlockState blockState = mc.world.getBlockState(blockPos);
      VoxelShape shape = blockState.getOutlineShape(mc.world, blockPos);
      if (shape.isEmpty()) {
         out.add(FULL_BOX.offset(blockPos).offset(ox, oy, oz));
         return;
      }
      List<Box> boxes = shape.getBoundingBoxes();
      for (int i = 0; i < boxes.size(); i++) {
         out.add(boxes.get(i).offset(blockPos).offset(ox, oy, oz));
      }
   }

   private boolean isValidEntity(BlockEntity entity) {
      double maxDistSq = this.maxDistance.getCurrentValue() * this.maxDistance.getCurrentValue();
      if (mc.player == null || mc.player.squaredDistanceTo(entity.getPos().toCenterPos()) > maxDistSq) {
         return false;
      } else if (entity instanceof ChestBlockEntity && this.chests.isSelected()) {
         return true;
      } else if (entity instanceof EnderChestBlockEntity && this.enderChests.isSelected()) {
         return true;
      } else if (entity instanceof TrappedChestBlockEntity && this.trappedChests.isSelected()) {
         return true;
      } else if (entity instanceof FurnaceBlockEntity && this.furnaces.isSelected()) {
         return true;
      } else if (entity instanceof BarrelBlockEntity && this.barrels.isSelected()) {
         return true;
      } else if (entity instanceof ShulkerBoxBlockEntity && this.shulkers.isSelected()) {
         return true;
      } else if (entity instanceof DropperBlockEntity && this.droppers.isSelected()) {
         return true;
      } else {
         return entity instanceof DispenserBlockEntity && this.dispensers.isSelected() ? true : entity instanceof HopperBlockEntity && this.hoppers.isSelected();
      }
   }

   private ColorRGBA getBlockColor(BlockEntity entity) {
      if (entity instanceof ChestBlockEntity) {
         return new ColorRGBA(255.0F, 131.0F, 54.0F);
      } else if (entity instanceof EnderChestBlockEntity) {
         return new ColorRGBA(121.0F, 54.0F, 255.0F);
      } else if (entity instanceof TrappedChestBlockEntity) {
         return new ColorRGBA(255.0F, 101.0F, 54.0F);
      } else if (entity instanceof FurnaceBlockEntity) {
         return new ColorRGBA(126.0F, 126.0F, 126.0F);
      } else if (entity instanceof BarrelBlockEntity) {
         return new ColorRGBA(255.0F, 185.0F, 54.0F);
      } else if (entity instanceof ShulkerBoxBlockEntity) {
         return new ColorRGBA(181.0F, 54.0F, 255.0F);
      } else if (entity instanceof DropperBlockEntity) {
         return new ColorRGBA(100.0F, 100.0F, 100.0F);
      } else if (entity instanceof DispenserBlockEntity) {
         return new ColorRGBA(100.0F, 100.0F, 100.0F);
      } else {
         return entity instanceof HopperBlockEntity ? new ColorRGBA(100.0F, 100.0F, 100.0F) : Colors.WHITE;
      }
   }

   private ColorRGBA getEntityColor(Entity entity) {
      return entity instanceof ChestMinecartEntity ? new ColorRGBA(255.0F, 200.0F, 100.0F) : Colors.WHITE;
   }

   private boolean isValidCart(Entity entity) {
      double maxDistSq = this.maxDistance.getCurrentValue() * this.maxDistance.getCurrentValue();
      return mc.player != null && !(mc.player.squaredDistanceTo(entity.getPos()) > maxDistSq)
         ? entity instanceof ChestMinecartEntity && this.minecart.isSelected()
         : false;
   }
}
