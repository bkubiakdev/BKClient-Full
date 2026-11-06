package dev.bkclient.utility.render.batching.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.batching.Batching;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/** Same-size screen-AA shapes only: tracks and thumbs use separate batches. */
public final class ScreenRectBatching extends Batching {
    private boolean configured;
    private float width, height;
    private BorderRadius radius;
    public ScreenRectBatching() { super(VertexFormats.POSITION_COLOR); }

    public void add(Matrix4f matrix, float x, float y, float w, float h, BorderRadius r, int color) {
        if (configured && (width != w || height != h || radius.topLeftRadius() != r.topLeftRadius()
                || radius.topRightRadius() != r.topRightRadius() || radius.bottomLeftRadius() != r.bottomLeftRadius()
                || radius.bottomRightRadius() != r.bottomRightRadius())) {
            throw new IllegalArgumentException("Screen-AA batch requires identical dimensions and radii");
        }
        if (!configured) { width = w; height = h; radius = r; configured = true; }
        float pad = 2;
        builder.vertex(matrix, x-pad, y-pad, 0).color(color);
        builder.vertex(matrix, x-pad, y+h+pad, 0).color(color);
        builder.vertex(matrix, x+w+pad, y+h+pad, 0).color(color);
        builder.vertex(matrix, x+w+pad, y-pad, 0).color(color);
    }
    @Override public void draw() {
        try {
            var built = builder.endNullable();
            if (built == null) return;
            try {
                DrawUtility.rectangleProgram.use();
                DrawUtility.rectangleProgram.findUniform("Size").set(width + 4, height + 4);
                DrawUtility.rectangleProgram.findUniform("Radius").set(radius.topLeftRadius(), radius.bottomLeftRadius(), radius.topRightRadius(), radius.bottomRightRadius());
                DrawUtility.rectangleProgram.findUniform("Smoothness").set(-2.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            } catch (Throwable failure) { built.close(); throw failure; }
            BufferRenderer.drawWithGlobalProgram(built);
        } finally {
            if (active == this) active = null;
            RenderSystem.disableBlend();
        }
    }
}
