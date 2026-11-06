package dev.bkclient.mixin.minecraft.client.network;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.other.NameProtect;
import dev.bkclient.systems.modules.modules.visuals.Capes;
import dev.bkclient.systems.modules.modules.visuals.SkinChanger;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

            // Only apply to the local player
            if (player == BKClientInit.mc.player) {

                SkinTextures original = cir.getReturnValue();
                Identifier overriddenTexture = original.texture();
                Identifier overriddenCape = original.capeTexture();

                NameProtect nameProtect = BKClientInit.getInstance().getModuleManager().getModule(NameProtect.class);
                if (nameProtect.isEnabled() && nameProtect.hideSkin.isEnabled()) {
                    overriddenTexture = BKClientInit.id("textures/skin/anon_skin.png");
                }

                // SkinChanger Module
                SkinChanger skinChanger = BKClientInit.getInstance().getModuleManager().getModule(SkinChanger.class);
                if (skinChanger.isEnabled() && skinChanger.getSkinIdentifier() != null) {
                    cir.setReturnValue(new SkinTextures(
                            skinChanger.getSkinIdentifier(),
                            original.textureUrl(),
                            original.capeTexture(),
                            original.elytraTexture(),
                            skinChanger.getSkinModel(),
                            original.secure()));
                    return;
                }

                // Capes Module
                Capes capes = BKClientInit.getInstance().getModuleManager().getModule(Capes.class);
                if (capes.isEnabled()) {
                    overriddenCape = capes.getCapeIdentifier();
                }

                if (overriddenTexture != original.texture() || overriddenCape != original.capeTexture()) {
                    SkinTextures modified = new SkinTextures(
                            overriddenTexture,
                            original.textureUrl(),
                            overriddenCape,
                            original.elytraTexture(),
                            original.model(),
                            original.secure());
                    cir.setReturnValue(modified);
                }
            }
        } catch (Exception e) {
            // Fail silently to avoid crashing the game renderer
        }
    }
}
