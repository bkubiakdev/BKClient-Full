package dev.bkclient.mixin.minecraft.client.render;

import dev.bkclient.utility.render.ambience.WindWaveTagger;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(BlockModelRenderer.class)
public class VegetationTagMixin {
    @ModifyVariable(method="render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V",at=@At("HEAD"),argsOnly=true)
    private VertexConsumer bk$tag(VertexConsumer original,BlockRenderView world,BakedModel model,BlockState state,BlockPos pos,MatrixStack matrices,VertexConsumer consumer,boolean cull,Random random,long seed,int overlay){
        return WindWaveTagger.wrap(original,state,pos,world);
    }
}
