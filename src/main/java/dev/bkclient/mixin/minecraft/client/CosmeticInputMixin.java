package dev.bkclient.mixin.minecraft.client;
import dev.bkclient.systems.cosmetics.render.MeleeCosmeticRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(MinecraftClient.class)
public abstract class CosmeticInputMixin {
 @Inject(method="doAttack",at=@At("HEAD")) private void bkclient$primary(CallbackInfoReturnable<Boolean> ci){MeleeCosmeticRenderer.attack(true);}
 @Inject(method="doItemUse",at=@At("RETURN")) private void bkclient$secondary(CallbackInfo ci){MeleeCosmeticRenderer.attack(false);}
}
