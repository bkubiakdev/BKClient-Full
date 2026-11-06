package dev.bkclient.utility.render.batching.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.msdf.MsdfFont;
import dev.bkclient.framework.msdf.MsdfRenderer;
import dev.bkclient.utility.render.batching.Batching;
import net.minecraft.client.render.VertexFormats;

/** One atlas and one horizontal fade region; rows may differ in Y and color. */
public final class FadedFontBatching extends Batching {
    private final MsdfFont font;
    private final float x, width;
    public FadedFontBatching(MsdfFont font, float x, float width) {
        super(VertexFormats.POSITION_TEXTURE_COLOR);
        this.font = font; this.x = x; this.width = width;
    }
    @Override public void draw() {
        try {
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
            RenderSystem.setShaderTexture(0, font.getTextureId());
            var shader = RenderSystem.setShader(MsdfRenderer.MSDF_FONT_SHADER_KEY);
            shader.getUniform("Range").set(font.getAtlas().range());
            shader.getUniform("Thickness").set(0.05F);
            shader.getUniform("Smoothness").set(0.5F);
            shader.getUniform("Outline").set(0);
            shader.getUniform("EnableFadeout").set(1);
            shader.getUniform("FadeoutStart").set(0.85F);
            shader.getUniform("FadeoutEnd").set(1.0F);
            shader.getUniform("MaxWidth").set(width);
            shader.getUniform("TextPosX").set(x);
            build();
        } finally {
            if (active == this) active = null;
            RenderSystem.setShaderTexture(0, 0); RenderSystem.enableCull(); RenderSystem.disableBlend();
        }
    }
}
