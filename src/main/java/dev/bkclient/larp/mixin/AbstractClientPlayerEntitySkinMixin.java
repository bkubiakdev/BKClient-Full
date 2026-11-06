package dev.bkclient.larp.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.bkclient.larp.feature.IllusionistWand;

@Mixin(value={AbstractClientPlayerEntity.class})
public class AbstractClientPlayerEntitySkinMixin {
    @Inject(method={"getSkinTextures"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$cloneWearsMySkin(CallbackInfoReturnable<SkinTextures> cir) {
        if (!IllusionistWand.isClone((Entity)((AbstractClientPlayerEntity)(Object)this))) {
            return;
        }
        PlayerEntity source = IllusionistWand.cloneSkinSource();
        if (source instanceof AbstractClientPlayerEntity) {
            AbstractClientPlayerEntity dressed = (AbstractClientPlayerEntity)source;
            cir.setReturnValue(dressed.getSkinTextures());
        }
    }
}
