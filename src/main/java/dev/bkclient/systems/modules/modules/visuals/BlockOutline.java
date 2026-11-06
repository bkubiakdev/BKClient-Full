package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.Draw3DUtility;
import dev.bkclient.utility.render.RenderUtility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

@ModuleInfo(name = "Block Outline", category = ModuleCategory.VISUALS)
public class BlockOutline extends BaseModule {
    private final BooleanSetting outline = new BooleanSetting(this, "Outline").enabled(true);
    private final BooleanSetting fill = new BooleanSetting(this, "Fill").enabled(true);
    private final ColorSetting color = new ColorSetting(this, "Color").color(Colors.ACCENT);
    private final SliderSetting fillAlpha = new SliderSetting(this, "Fill Alpha", () -> !this.fill.isEnabled())
            .min(0.05f).max(1.0f).step(0.05f).currentValue(0.2f);
    private final BooleanSetting smooth = new BooleanSetting(this, "Smooth").enabled(true);
    private final SliderSetting transitionTime = new SliderSetting(this, "Transition Time",
            () -> !this.smooth.isEnabled()).min(0.05f).max(1.0f).step(0.05f).currentValue(0.18f);
    private final SliderSetting reach = new SliderSetting(this, "Reach").min(3.0f).max(12.0f).step(0.5f)
            .currentValue(6.0f);

    private BlockPos lastTargetPos;
    private List<Box> animationFromBoxes = List.of();
    private List<Box> animationToBoxes = List.of();
    private long animationStartedAt;

    private final EventListener<Render3DEvent> on3DRender = event -> {
        if (BlockOutline.mc.player == null || BlockOutline.mc.world == null) {
            this.resetAnimation();
            return;
        }

        BlockPos blockPos = this.findBlockToDisplay();
        if (blockPos == null) {
            this.resetAnimation();
            return;
        }

        List<Box> targetBoxes = this.getWorldBoxes(blockPos);
        if (targetBoxes.isEmpty()) {
            this.resetAnimation();
            return;
        }

        this.updateAnimationTarget(blockPos, targetBoxes);
        List<Box> currentBoxes = this.getCurrentBoxes(System.currentTimeMillis());
        if (!currentBoxes.isEmpty()) {
            this.renderBlockOutline(event, currentBoxes);
        }
    };

    @Override
    public void onEnable() {
        this.resetAnimation();
    }

    @Override
    public void onDisable() {
        this.resetAnimation();
    }

    private void renderBlockOutline(Render3DEvent event, List<Box> boxes) {
        MatrixStack matrices = event.getMatrices();
        Camera camera = event.getCamera();
        ColorRGBA baseColor = this.color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        if (this.fill.isEnabled()) {
            BufferBuilder quadsBuffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_COLOR);
            ColorRGBA fillColor = baseColor.withAlpha(255.0f * this.fillAlpha.getCurrentValue());

            for (Box box : boxes) {
                Vec3d center = box.getCenter();
                matrices.push();
                RenderUtility.prepareMatrices(matrices, center);
                Draw3DUtility.renderFilledBox(matrices, quadsBuffer, this.toLocalBox(box, center), fillColor);
                matrices.pop();
            }

            RenderUtility.buildBuffer(quadsBuffer);
        }

        if (this.outline.isEnabled()) {
            BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR);
            ColorRGBA outlineColor = baseColor.withAlpha(180.0f);

            for (Box box : boxes) {
                Vec3d center = box.getCenter();
                matrices.push();
                RenderUtility.prepareMatrices(matrices, center);
                Draw3DUtility.renderOutlinedBox(matrices, linesBuffer, this.toLocalBox(box, center), outlineColor);
                matrices.pop();
            }

            RenderUtility.buildBuffer(linesBuffer);
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private Box toLocalBox(Box box, Vec3d center) {
        return box.offset(-center.x, -center.y, -center.z);
    }

    private BlockPos findBlockToDisplay() {
        if (BlockOutline.mc.crosshairTarget == null
                || BlockOutline.mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockHitResult hit = (BlockHitResult) BlockOutline.mc.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        double reachDistance = this.reach.getCurrentValue();
        if (BlockOutline.mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > reachDistance * reachDistance) {
            return null;
        }

        return pos;
    }

    private List<Box> getWorldBoxes(BlockPos blockPos) {
        BlockState state = BlockOutline.mc.world.getBlockState(blockPos);
        if (state.isAir() || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA
                || state.getBlock() == Blocks.CAVE_AIR || state.getBlock() == Blocks.VOID_AIR) {
            return List.of();
        }

        VoxelShape shape = state.getOutlineShape((BlockView) BlockOutline.mc.world, blockPos);
        if (shape.isEmpty()) {
            shape = VoxelShapes.fullCube();
        }

        List<Box> worldBoxes = new ArrayList<>(shape.getBoundingBoxes().size());
        for (Box box : shape.getBoundingBoxes()) {
            worldBoxes.add(box.offset(blockPos));
        }

        return worldBoxes;
    }

    private void updateAnimationTarget(BlockPos blockPos, List<Box> targetBoxes) {
        long now = System.currentTimeMillis();
        boolean targetChanged = this.lastTargetPos == null || !this.lastTargetPos.equals(blockPos);
        boolean shapeChanged = !this.sameBoxes(this.animationToBoxes, targetBoxes);

        if (!this.smooth.isEnabled()) {
            this.lastTargetPos = blockPos.toImmutable();
            this.animationFromBoxes = this.copyBoxes(targetBoxes);
            this.animationToBoxes = this.copyBoxes(targetBoxes);
            this.animationStartedAt = now;
            return;
        }

        if (!targetChanged && !shapeChanged) {
            return;
        }

        this.animationFromBoxes = this.animationToBoxes.isEmpty() ? this.copyBoxes(targetBoxes)
                : this.getCurrentBoxes(now);
        this.animationToBoxes = this.copyBoxes(targetBoxes);
        this.animationStartedAt = now;
        this.lastTargetPos = blockPos.toImmutable();
    }

    private List<Box> getCurrentBoxes(long now) {
        if (this.animationToBoxes.isEmpty()) {
            return List.of();
        }
        if (!this.smooth.isEnabled()) {
            return this.animationToBoxes;
        }

        float durationSeconds = this.transitionTime.getCurrentValue();
        if (durationSeconds <= 0.0f || this.animationFromBoxes.isEmpty()) {
            return this.animationToBoxes;
        }

        float progress = MathHelper.clamp((float) (now - this.animationStartedAt) / (durationSeconds * 1000.0f),
                0.0f, 1.0f);
        if (progress >= 1.0f) {
            this.animationFromBoxes = this.animationToBoxes;
            return this.animationToBoxes;
        }

        int size = Math.max(this.animationFromBoxes.size(), this.animationToBoxes.size());
        List<Box> interpolated = new ArrayList<>(size);
        Box fromFallback = this.getCombinedBox(this.animationFromBoxes);
        Box toFallback = this.getCombinedBox(this.animationToBoxes);

        for (int i = 0; i < size; ++i) {
            Box from = this.getBoxAt(this.animationFromBoxes, i, fromFallback != null ? fromFallback : toFallback);
            Box to = this.getBoxAt(this.animationToBoxes, i, toFallback != null ? toFallback : fromFallback);
            if (from != null && to != null) {
                interpolated.add(this.lerpBox(from, to, progress));
            }
        }

        return interpolated;
    }

    private Box getBoxAt(List<Box> boxes, int index, Box fallback) {
        if (index < boxes.size()) {
            return boxes.get(index);
        }
        return fallback;
    }

    private Box getCombinedBox(List<Box> boxes) {
        if (boxes.isEmpty()) {
            return null;
        }

        Box first = boxes.get(0);
        double minX = first.minX;
        double minY = first.minY;
        double minZ = first.minZ;
        double maxX = first.maxX;
        double maxY = first.maxY;
        double maxZ = first.maxZ;

        for (int i = 1; i < boxes.size(); ++i) {
            Box box = boxes.get(i);
            minX = Math.min(minX, box.minX);
            minY = Math.min(minY, box.minY);
            minZ = Math.min(minZ, box.minZ);
            maxX = Math.max(maxX, box.maxX);
            maxY = Math.max(maxY, box.maxY);
            maxZ = Math.max(maxZ, box.maxZ);
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private Box lerpBox(Box from, Box to, float progress) {
        return new Box(
                MathHelper.lerp(progress, from.minX, to.minX),
                MathHelper.lerp(progress, from.minY, to.minY),
                MathHelper.lerp(progress, from.minZ, to.minZ),
                MathHelper.lerp(progress, from.maxX, to.maxX),
                MathHelper.lerp(progress, from.maxY, to.maxY),
                MathHelper.lerp(progress, from.maxZ, to.maxZ));
    }

    private List<Box> copyBoxes(List<Box> boxes) {
        return new ArrayList<>(boxes);
    }

    private boolean sameBoxes(List<Box> first, List<Box> second) {
        if (first.size() != second.size()) {
            return false;
        }

        for (int i = 0; i < first.size(); ++i) {
            Box a = first.get(i);
            Box b = second.get(i);
            if (Double.compare(a.minX, b.minX) != 0 || Double.compare(a.minY, b.minY) != 0
                    || Double.compare(a.minZ, b.minZ) != 0 || Double.compare(a.maxX, b.maxX) != 0
                    || Double.compare(a.maxY, b.maxY) != 0 || Double.compare(a.maxZ, b.maxZ) != 0) {
                return false;
            }
        }

        return true;
    }

    private void resetAnimation() {
        this.lastTargetPos = null;
        this.animationFromBoxes = List.of();
        this.animationToBoxes = List.of();
        this.animationStartedAt = 0L;
    }
}
