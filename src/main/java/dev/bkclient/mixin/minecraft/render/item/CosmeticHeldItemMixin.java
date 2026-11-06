package dev.bkclient.mixin.minecraft.render.item;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.bkclient.systems.cosmetics.render.MeleeCosmeticRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;

// Runs before the existing hand event's push/pop pair, preserving its stack when a cosmetic takes over.
@Mixin(value=HeldItemRenderer.class,priority=1200)
public abstract class CosmeticHeldItemMixin {
    @WrapMethod(method="renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V")
    private void bkclient$cosmeticFrame(float delta,MatrixStack matrices,VertexConsumerProvider.Immediate consumers,net.minecraft.client.network.ClientPlayerEntity player,int light,Operation<Void> original){
        MeleeCosmeticRenderer.beginFrame();
        try{original.call(delta,matrices,consumers,player,light);}
        finally{MeleeCosmeticRenderer.endFrame(player,consumers,light);}
    }
    @WrapMethod(method="renderFirstPersonItem")
    private void bkclient$firstPersonCosmetic(AbstractClientPlayerEntity player,float tickDelta,float pitch,Hand hand,
            float swing,ItemStack stack,float equip,MatrixStack matrices,VertexConsumerProvider consumers,int light,Operation<Void> original) {
        if (!MeleeCosmeticRenderer.firstPerson(player,hand,stack,swing,equip,matrices,consumers,light))
            original.call(player,tickDelta,pitch,hand,swing,stack,equip,matrices,consumers,light);
    }
}
