package dev.bkclient.ui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.MsdfFont;
import dev.bkclient.framework.msdf.MsdfRenderer;
import dev.bkclient.ui.menu.api.MenuCategory;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;

/** Lucide vectors encoded as MSDF; draw outside text batches because this uses its own atlas. */
public enum LucideIcons {
    SWORDS(0xE100), MOVE(0xE101), EYE(0xE102), USER(0xE103), MORE(0xE104),
    CART(0xE105), COPY(0xE106), PALETTE(0xE107), STAR(0xE108), STAR_FILLED(0xE109), CHECK(0xE10A), CROSS(0xE10B), INFO(0xE10C),
    WAVES(0xE10D), SPARKLES(0xE10E), GAUGE(0xE10F), EXPAND(0xE110),
    RULER(0xE111), DROPLETS(0xE112), PRISM(0xE113), SAVE(0xE114), BUY(0xE115), SELL(0xE116),
    CLOCK(0xE117), KEYBOARD(0xE118), SHIELD(0xE119), PACKAGE(0xE11A), FLASK(0xE11B), GLOBE(0xE11C), USERS(0xE11D), CROSSHAIR(0xE11E), HOURGLASS(0xE11F), MUSIC(0xE120), PREVIOUS(0xE121), NEXT(0xE122), PLAY(0xE123), PAUSE(0xE124), REPEAT(0xE125), REPEAT_ONE(0xE126), SMILE(0xE127), SEND(0xE128), TRASH(0xE129), RESET(0xE12A), CENTER(0xE12B), LOCK(0xE12C), PANEL(0xE12D), PLANE(0xE12E), BOOK(0xE12F), KEY(0xE130), MEDIA_PREVIOUS(0xE131), MEDIA_NEXT(0xE132), MEDIA_PLAY(0xE133), MEDIA_PAUSE(0xE134), MENU_BACK(0xE135), FOLDER(0xE136);

    private static BufferBuilder iconBatch;

    /** Scope must contain only icon draws, with one unchanged scissor rectangle. */
    public static void drawBatch(Runnable icons) {
        if (iconBatch != null) throw new IllegalStateException("Nested icon batch");
        prepare();
        iconBatch = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        try {
            icons.run();
        } finally {
            BufferBuilder builder = iconBatch;
            iconBatch = null;
            try {
                var built = builder.endNullable();
                if (built != null) BufferRenderer.drawWithGlobalProgram(built);
            } finally {
                RenderSystem.setShaderTexture(0, 0);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
            }
        }
    }

    private final String glyph;

    LucideIcons(int codepoint) {
        this.glyph = Character.toString(codepoint);
    }

    private static final class Atlas {
        private static final MsdfFont FONT = MsdfFont.builder()
            .name("Lucide GUI icons").atlas("lucide_icons").data("lucide_icons").build();
    }

    public static LucideIcons category(MenuCategory category) {
        return switch (category) {
            case COMBAT -> SWORDS;
            case MOVEMENT -> MOVE;
            case VISUALS -> EYE;
            case PLAYER -> USER;
            case OTHER -> MORE;
        };
    }

    /** x/y/size describe the original 24x24 SVG viewport, excluding distance-field padding. */
    public void draw(CustomDrawContext context, float x, float y, float size, ColorRGBA color) {
        draw(context.getMatrices().peek().getPositionMatrix(), x, y, size, color);
    }

    public void draw(org.joml.Matrix4f matrix, float x, float y, float size, ColorRGBA color) {
        if (size <= 0 || color.getAlpha() <= 0) return;
        MsdfFont font = Atlas.FONT;
        if (iconBatch != null) {
            font.applyGlyphs(matrix, iconBatch, this.glyph,
                size, 0, 0, x - size / 6.0F, y + size, 0, color.getRGB());
            return;
        }
        prepare();
        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        // MsdfGlyph does not apply planeBounds.left: account for the 8/48 em padding explicitly.
        font.applyGlyphs(matrix, builder, this.glyph,
            size, 0, 0, x - size / 6.0F, y + size, 0, color.getRGB());
        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    private static void prepare() {
        MsdfFont font = Atlas.FONT;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, font.getTextureId());
        var shader = RenderSystem.setShader(MsdfRenderer.MSDF_FONT_SHADER_KEY);
        shader.getUniform("Range").set(font.getAtlas().range());
        // Text weight compensation would thicken icon strokes; preserve the SVG's actual contour.
        shader.getUniform("Thickness").set(0.0F);
        shader.getUniform("Smoothness").set(0.5F);
        shader.getUniform("Outline").set(0);
        shader.getUniform("EnableFadeout").set(0);
    }

}
