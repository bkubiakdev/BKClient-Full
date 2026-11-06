package dev.bkclient.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.opengl.*;

/** Scope for offscreen visual passes; never leave state behind for terrain, hands or HUD. */
public final class VisualRenderState implements AutoCloseable {
    private final boolean blend=GL11.glIsEnabled(GL11.GL_BLEND), depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean depthWrite=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB), srcA=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING), draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), programId=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int texture=RenderSystem.getShaderTexture(0);
    private final int[] viewport=new int[4];
    private final float[] color=RenderSystem.getShaderColor().clone();
    private final ShaderProgram program=RenderSystem.getShader();
    public VisualRenderState(){GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);}
    @Override public void close(){
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
        RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
        RenderSystem.setShader(program); GL20.glUseProgram(programId);
        RenderSystem.setShaderTexture(0,texture); RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
        RenderSystem.depthMask(depthWrite);
        if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
        if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
        RenderSystem.blendFuncSeparate(src,dst,srcA,dstA);
        if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
    }
}
