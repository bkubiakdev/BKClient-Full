package dev.bkclient.utility.render;

import dev.bkclient.systems.modules.modules.visuals.ChinaHat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class ChinaHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public ChinaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        ChinaHat module = ChinaHat.getInstance();
        if (module == null || !module.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (state.id != mc.player.getId()) {
            if (!module.renderOnFriends.isEnabled()) return;

            PlayerEntity found = null;
            if (mc.world != null) {
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p.getId() == state.id) {
                        found = p;
                        break;
                    }
                }
            }
            if (found == null) return;
        }

        // Halo is rendered in world-space via the module's Render3DEvent listener, not here.
        if (module.mode.is(module.modeHalo)) return;

        matrices.push();

        PlayerEntityModel playerModel = this.getContextModel();
        ModelPart head = playerModel.getHead();
        head.rotate(matrices);

        // Offset: move hat above head. If helmet equipped, offset slightly more.
        float offset = -0.48f;
        if (mc.world != null) {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p.getId() == state.id) {
                    ItemStack headItem = p.getEquippedStack(EquipmentSlot.HEAD);
                    if (!headItem.isEmpty()) offset = -0.54f;
                    break;
                }
            }
        }
        matrices.translate(0, offset, 0);

        float rotSpeed = module.rotationSpeed.getCurrentValue();
        if (rotSpeed != 0.0f) {
            float rotAngle = (System.currentTimeMillis() % 360000L) / 1000.0f * rotSpeed * 360.0f;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotAngle));
        }

        module.renderHat(matrices, vertexConsumers);

        matrices.pop();
    }
}
