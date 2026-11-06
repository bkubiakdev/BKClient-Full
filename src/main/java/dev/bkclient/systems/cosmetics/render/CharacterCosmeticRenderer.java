package dev.bkclient.systems.cosmetics.render;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.cosmetics.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import java.util.IdentityHashMap;
import java.util.Map;

public final class CharacterCosmeticRenderer {
    private static final Map<BedrockModel,CharacterPose.Fit> FITS = new IdentityHashMap<>();
    public static void reset() { FITS.clear(); }

    public static CosmeticEntry selected(EntityRenderState state) {
        var client = MinecraftClient.getInstance();
        if (!(state instanceof PlayerEntityRenderState player) || client.player == null
                || player.id != client.player.getId() || player.spectator || state.invisible
                || BKClientInit.getInstance().isPanic()) return null;
        var entry = CosmeticManager.getInstance().equipped(CosmeticSlot.CHARACTER);
        return entry != null && CosmeticRenderer.available(entry) ? entry : null;
    }

    public static void render(CosmeticEntry entry, PlayerEntityRenderState state, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, int overlay, int color) {
        var model = CosmeticRenderer.model(entry);
        var fit = FITS.computeIfAbsent(model, CharacterPose::fit);
        float seconds = state.age / 20f;
        var pose = CharacterPose.sample(model, CosmeticRenderer.animations(entry), seconds,
                state.limbAmplitudeMultiplier, state.handSwingProgress, state.yawDegrees, state.pitch);
        // Undo the vanilla feet offset and convert the model's upward Y axis.
        // Body yaw, swimming, gliding, sleeping and death rotation are already in the stack.
        Matrix4f transform = new Matrix4f(matrices.peek().getPositionMatrix())
                .translate(0, 1.501f - (state.isInSneakingPose ? .15f : 0), 0)
                .rotateZ((float)Math.PI).scale(fit.scale()).translate(0, -fit.floor(), 0);
        CosmeticRenderer.renderCharacter(entry, transform, consumers, light, overlay, color, pose, seconds);
    }
    private CharacterCosmeticRenderer() { }
}
