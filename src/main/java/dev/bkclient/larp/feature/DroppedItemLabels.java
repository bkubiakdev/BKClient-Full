package dev.bkclient.larp.feature;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.StringVisitable;
import org.joml.Matrix4f;
import dev.bkclient.larp.feature.Disguise;

public final class DroppedItemLabels {
    private static final double MAX_DISTANCE = 24.0;
    private static final double HEIGHT = 0.9;
    private static final float SCALE = 0.025f;

    private DroppedItemLabels() {
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }
        Vec3d camera = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(false);
        boolean drewAnything = false;
        for (Entity entity : client.world.getEntities()) {
            ItemEntity item;
            Text name;
            if (!(entity instanceof ItemEntity) || (name = Disguise.name((item = (ItemEntity)entity).getStack())) == null || item.squaredDistanceTo((Entity)client.player) > 576.0) continue;
            DroppedItemLabels.drawLabel(client, matrices, consumers, camera, item.getLerpedPos(tickDelta), name);
            drewAnything = true;
        }
        if (drewAnything && consumers instanceof VertexConsumerProvider.Immediate) {
            VertexConsumerProvider.Immediate immediate = (VertexConsumerProvider.Immediate)consumers;
            immediate.draw();
        }
    }

    private static void drawLabel(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera, Vec3d at, Text name) {
        TextRenderer font = client.textRenderer;
        matrices.push();
        matrices.translate(at.x - camera.x, at.y + 0.9 - camera.y, at.z - camera.z);
        matrices.multiply(client.getEntityRenderDispatcher().getRotation());
        matrices.scale(0.025f, -0.025f, 0.025f);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float left = (float)(-font.getWidth((StringVisitable)name)) / 2.0f;
        int background = (int)(client.options.getTextBackgroundOpacity(0.25f) * 255.0f) << 24;
        font.draw(name, left, 0.0f, -1, false, matrix, consumers, TextRenderer.TextLayerType.SEE_THROUGH, background, 0xF000F0);
        matrices.pop();
    }
}
