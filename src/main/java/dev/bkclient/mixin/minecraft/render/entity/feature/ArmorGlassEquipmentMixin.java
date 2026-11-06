package dev.bkclient.mixin.minecraft.render.entity.feature;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.bkclient.systems.modules.modules.visuals.ArmorGlass;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EquipmentRenderer.class)
public abstract class ArmorGlassEquipmentMixin {
    // Replace only the base material. Vanilla still draws armor trims afterwards.
    @WrapOperation(method="render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void bkclient$glass(Model model,MatrixStack matrices,VertexConsumer vertices,int light,int overlay,int color,Operation<Void> original,
        @Local(argsOnly=true) ItemStack stack,@Local(argsOnly=true) RegistryKey<EquipmentAsset> asset,
        @Local(argsOnly=true) EquipmentModel.LayerType type,@Local(argsOnly=true) VertexConsumerProvider consumers,
        @Local(ordinal=1) Identifier texture){
        ArmorGlass module=ArmorGlass.getInstance();
        if(module!=null&&module.isEnabled()&&ArmorGlass.supports(stack,asset,type)
            &&module.renderer.render(model,matrices,consumers,texture,light,overlay,color,module))return;
        original.call(model,matrices,vertices,light,overlay,color);
    }
}
