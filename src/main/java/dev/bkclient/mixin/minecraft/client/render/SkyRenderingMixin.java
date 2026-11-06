package dev.bkclient.mixin.minecraft.client.render;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Sky;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.SkyboxRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {

    @Inject(method = "renderSky(FFF)V", at = @At("HEAD"), cancellable = true)
    private void renderCustomSkybox(float red, float green, float blue, CallbackInfo info) {
        Sky sky = this.getSky();
        if (sky != null && sky.renderSkyEffect()) {
            info.cancel();
            return;
        }
        if (sky != null && sky.isEnabled() && sky.hasCustomSkybox()) {
            ColorRGBA color = sky.shouldTintSky() ? sky.getResolvedSkyColor() : ColorRGBA.WHITE;
            if (sky.hasCustomSkybox()) {
                SkyboxRenderer.render(sky.getSkyboxTexture(), color);
            }

            if (sky.hasShaderSkybox()) {
                float time = (float)(System.currentTimeMillis() % 100000000L) / 1000.0F * sky.getShaderSpeed();
                float opacity = sky.getShaderOpacity();
                SkyboxRenderer.render(sky.getSkyShaderProgram(), color, time, opacity);
            }

            info.cancel();
        }
    }

    @Inject(method = "renderSky(FFF)V", at = @At("RETURN"))
    private void bk$shaderOverDefaultSky(float red, float green, float blue, CallbackInfo ci) {
        Sky sky = getSky();
        if (sky == null || !sky.isEnabled() || sky.hasCustomSkybox() || !sky.hasShaderSkybox()) return;
        ColorRGBA color = sky.shouldTintSky() ? sky.getResolvedSkyColor() : ColorRGBA.WHITE;
        float time = (float)(System.currentTimeMillis() % 100000000L) / 1000.0F * sky.getShaderSpeed();
        SkyboxRenderer.render(sky.getSkyShaderProgram(), color, time, sky.getShaderOpacity());
    }

    // Procedural skies provide their own stars; vanilla celestial bodies must not sit over storm clouds.
    @Inject(method = {"renderCelestialBodies", "renderGlowingSky", "renderSkyDark"}, at = @At("HEAD"), cancellable = true)
    private void bk$proceduralSky(CallbackInfo ci) {
        Sky sky = getSky();
        if (sky != null && sky.isEnabled() && java.util.Set.of("Overcast", "Starfall", "Aurora Curtains", "Stars", "Cyberspace").contains(String.valueOf(sky.getSkyEffectMode()))) ci.cancel();
    }

    @Inject(method = "close()V", at = @At("HEAD"))
    private void closeCustomSkybox(CallbackInfo info) {
        SkyboxRenderer.close();
    }

    @Redirect(
        method = "renderStars(Lnet/minecraft/client/render/Fog;FLnet/minecraft/client/util/math/MatrixStack;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", ordinal = 0)
    )
    private void redirectStarColor(float red, float green, float blue, float alpha) {
        Sky sky = this.getSky();
        if (sky != null && sky.shouldTintStars()) {
            ColorRGBA color = sky.getResolvedStarsColor();
            RenderSystem.setShaderColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
        } else {
            RenderSystem.setShaderColor(red, green, blue, alpha);
        }
    }

    private Sky getSky() {
        return BKClientInit.getInstance().getModuleManager() == null ? null : BKClientInit.getInstance().getModuleManager().getModule(Sky.class);
    }
}
