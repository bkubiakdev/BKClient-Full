package dev.bkclient.mixin.minecraft.client.render.entity;

import dev.bkclient.systems.modules.modules.visuals.ItemPhysics;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;
import java.util.Map;
import com.google.common.collect.MapMaker;

/**
 * Item Physics — ported from BKClient's MixinItemEntityRenderer.
 *
 * <p>BKClient targets 1.21.11, where the renderer submits through
 * {@code SubmitNodeCollector} and the render state exposes a model bounding box.
 * 1.21.4 renders directly via {@code renderStack} and has no bounding box on
 * {@code ItemRenderState}, so the ground seating uses a fixed lift instead of
 * BKClient's {@code -box.minY} / {@code -box.getYsize() / 2} math.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    /** How far above the ground a laid-down item sits, in blocks. */
    @Unique
    private static final float BK$GROUND_LIFT = 0.0625f;

    /** Ticks a resting item keeps its "grounded" look after onGround goes false. */
    @Unique
    private static final int BK$GROUND_HOLD_TICKS = 8;

    /** Grounded flag per render state, read back during the render pass. */
    @Unique
    private static final WeakHashMap<ItemEntityRenderState, Boolean> bk$groundState = new WeakHashMap<>();

    /**
     * Debounce per entity. Items resting on the ground flicker onGround off
     * for single ticks; without this the item would snap upright and back.
     * Weak keys release despawned items and never transfer state to a reused entity id.
     */
    @Unique
    private static final Map<ItemEntity, Integer> bk$groundHold = new MapMaker().weakKeys().makeMap();

    /** True while the maps may hold entries written by an enabled pass. */
    @Unique
    private static boolean bk$hasState;

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
            at = @At("HEAD"),
            require = 1
    )
    private void bk$captureGroundState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!bk$isEnabled()) {
            // Drop the state once after the module turns off, not on every item every frame.
            if (bk$hasState) {
                bk$groundState.clear();
                bk$groundHold.clear();
                bk$hasState = false;
            }
            return;
        }
        bk$hasState = true;
        bk$groundState.put(state, bk$resolveGroundState(entity, entity.isOnGround()));
    }

    /**
     * Vanilla bobs the item up and down here. A grounded item must not bob, so
     * replace the vertical component with a flat lift.
     */
    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 0),
            require = 1
    )
    private void bk$translate(MatrixStack matrices, float x, float y, float z,
                              ItemEntityRenderState state, MatrixStack matricesArg,
                              VertexConsumerProvider vertexConsumers, int light) {
        if (bk$isEnabled() && bk$groundState.getOrDefault(state, false)) {
            matrices.translate(x, BK$GROUND_LIFT, z);
        } else {
            matrices.translate(x, y, z);
        }
    }

    /**
     * Vanilla has already applied its Y spin by this point, so cancel it and
     * apply the physics orientation instead: flat on the ground, tumbling in air.
     */
    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/ItemEntityRenderer;renderStack(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/ItemStackEntityRenderState;Lnet/minecraft/util/math/random/Random;)V"),
            require = 1
    )
    private void bk$renderPhysics(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                  ItemStackEntityRenderState stackState, Random random) {
        if (bk$isEnabled() && stackState instanceof ItemEntityRenderState state) {
            float age = state.age;
            float offset = state.uniqueOffset;
            boolean onGround = bk$groundState.getOrDefault(state, false);

            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-ItemEntity.getRotation(age, offset)));
            if (onGround) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
            } else {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((age * 15.0f + offset * 360.0f) % 360.0f));
            }
        }
        ItemEntityRenderer.renderStack(matrices, vertexConsumers, light, stackState, random);
    }

    @Unique
    private static boolean bk$isEnabled() {
        ItemPhysics module = ItemPhysics.getInstance();
        return module != null && module.isEnabled();
    }

    @Unique
    private static boolean bk$resolveGroundState(ItemEntity entity, boolean onGround) {
        if (onGround) {
            if (bk$groundHold.size() > 4096) {
                bk$groundHold.clear();
            }
            bk$groundHold.put(entity, entity.age + BK$GROUND_HOLD_TICKS);
            return true;
        }

        Integer expiresAt = bk$groundHold.get(entity);
        if (expiresAt == null) return false;
        if (entity.age >= expiresAt) {
            bk$groundHold.remove(entity);
            return false;
        }

        // A real toss cancels the hold immediately; only near-still items coast.
        Vec3d velocity = entity.getVelocity();
        boolean lowMotion = Math.abs(velocity.x) <= 0.08
                && Math.abs(velocity.y) <= 0.08
                && Math.abs(velocity.z) <= 0.08;
        if (!lowMotion) {
            bk$groundHold.remove(entity);
            return false;
        }
        return true;
    }
}
