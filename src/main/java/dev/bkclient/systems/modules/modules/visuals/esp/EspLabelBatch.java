package dev.bkclient.systems.modules.modules.visuals.esp;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.MsdfFont;
import dev.bkclient.framework.msdf.MsdfRenderer;
import dev.bkclient.systems.modules.modules.other.NameProtect;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.DrawUtility;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import org.joml.Matrix4f;

/**
 * Collects the mob/animal ESP labels of one frame and draws them as three draw calls (all
 * backgrounds, all spawn-egg icons, all names) instead of three per label. Each piece uses the
 * same geometry, shader math and GL state as the unbatched DrawUtility/MsdfRenderer path; only
 * the order across different labels changes (visible where labels overlap). Render thread only.
 */
public final class EspLabelBatch {
    private static final ShaderProgramKey RECT_SHADER = new ShaderProgramKey(
        BKClientInit.id("core/rectangle_batch/data"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    /** DrawUtility.drawRoundedRect smoothness. */
    private static final float SMOOTHNESS = 0.5F;

    private final BufferAllocator rectMemory = new BufferAllocator(1 << 14);
    private final BufferAllocator textMemory = new BufferAllocator(1 << 16);
    private BufferBuilder rects;
    private BufferBuilder text;
    private MsdfFont textFont;
    private boolean active;

    public boolean active() { return active; }

    public void begin() {
        rects = new BufferBuilder(rectMemory, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        text = new BufferBuilder(textMemory, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        textFont = null;
        active = true;
    }

    /** Pill background (radius = height / 2), same quad padding as DrawUtility.drawRoundedRect. */
    public void pill(Matrix4f m, float x, float y, float width, float height, ColorRGBA color) {
        float horizontalPadding = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
        float verticalPadding = SMOOTHNESS / 2.0F + SMOOTHNESS;
        float ax = x - horizontalPadding / 2.0F;
        float ay = y - verticalPadding / 2.0F;
        float aw = width + horizontalPadding;
        float ah = height + verticalPadding;
        int rgba = color.getRGB();
        rects.vertex(m, ax, ay, 0.0F).texture(width, height).color(rgba);
        rects.vertex(m, ax, ay + ah, 0.0F).texture(width, height).color(rgba);
        rects.vertex(m, ax + aw, ay + ah, 0.0F).texture(width, height).color(rgba);
        rects.vertex(m, ax + aw, ay, 0.0F).texture(width, height).color(rgba);
    }

    /** Same glyph placement and text preprocessing as MsdfRenderer.renderText. Returns false if not batchable. */
    public boolean text(Matrix4f m, Font font, String string, float x, float y, int color) {
        MsdfFont msdf = font.getFont();
        if (textFont != null && textFont != msdf) return false;
        textFont = msdf;
        string = string.replace("і", "i").replace("І", "I");
        NameProtect nameProtect = BKClientInit.getInstance().getModuleManager().getModule(NameProtect.class);
        if (nameProtect.isEnabled()) string = nameProtect.patchName(string);
        float size = font.getSize();
        msdf.applyGlyphs(m, text, string, size, 0.05F * 0.5F * size, 0.0F, x - 0.75F, y + size * 0.7F, 0.0F, color);
        return true;
    }

    /** Draws backgrounds, then the queued item icons, then names. */
    public void flush(CustomDrawContext context) {
        if (!active) return;
        active = false;
        BuiltBuffer builtRects = rects.endNullable();
        if (builtRects != null) {
            ShaderProgram shader = RenderSystem.setShader(RECT_SHADER);
            if (shader != null) {
                var smoothness = shader.getUniform("Smoothness");
                if (smoothness != null) smoothness.set(SMOOTHNESS);
            }
            DrawUtility.drawSetup();
            BufferRenderer.drawWithGlobalProgram(builtRects);
            DrawUtility.drawEnd();
        }
        context.flushBatchItems();
        BuiltBuffer builtText = text.endNullable();
        if (builtText != null && textFont != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShaderTexture(0, textFont.getTextureId());
            ShaderProgram shader = RenderSystem.setShader(MsdfRenderer.MSDF_FONT_SHADER_KEY);
            shader.getUniform("Range").set(textFont.getAtlas().range());
            shader.getUniform("Thickness").set(0.05F);
            shader.getUniform("Smoothness").set(0.5F);
            shader.getUniform("EnableFadeout").set(0);
            shader.getUniform("FadeoutStart").set(0.0F);
            shader.getUniform("FadeoutEnd").set(1.0F);
            shader.getUniform("MaxWidth").set(0.0F);
            shader.getUniform("TextPosX").set(0.0F);
            BufferRenderer.drawWithGlobalProgram(builtText);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        } else if (builtText != null) {
            builtText.close();
        }
    }
}
