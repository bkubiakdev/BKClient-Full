package dev.bkclient.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.ArmorGlass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.*;

/** A scene copy plus an armor-only surface pass; never samples the active render target. */
public final class ArmorGlassRenderer implements AutoCloseable {
    private static final ShaderProgramKey SHADER=new ShaderProgramKey(Identifier.of("bkclient","core/armor_glass/glass"),VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,Defines.EMPTY);
    private SimpleFramebuffer scene;
    private BufferAllocator allocator;
    private volatile boolean dirty;
    private boolean failed;
    public void invalidate(){dirty=true;}
    @Override public void close(){
        if(scene!=null){
            int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            try(VisualRenderState ignored=new VisualRenderState()){scene.delete();scene=null;}
            finally{GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);}
        }
        if(allocator!=null){allocator.close();allocator=null;}
        failed=false;
    }
    public boolean render(Model model,MatrixStack matrices,VertexConsumerProvider consumers,Identifier texture,int light,int overlay,int color,ArmorGlass o){
        MinecraftClient mc=MinecraftClient.getInstance();
        // Outline/shadow capture consumers must keep their original material and depth handling.
        if(!(consumers instanceof VertexConsumerProvider.Immediate immediate))return false;
        Framebuffer target=mc.getFramebuffer();
        if(target==null||target.textureWidth<=0||target.textureHeight<=0||GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)!=target.fbo)return false;
        if(dirty){close();dirty=false;}
        if(failed)return false;
        ShaderProgram program;
        try{program=mc.getShaderLoader().getOrCreateProgram(SHADER);}
        catch(Exception error){failed=true;BKClientInit.LOGGER.warn("Armor Glass shader unavailable; keeping vanilla armor",error);return false;}
        if(program==null)return false;
        // Flush skin and earlier equipment before taking the refracted background snapshot.
        immediate.draw();
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),vao=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),vbo=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),depth=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] bindings=new int[2],slots=new int[2];
        for(int i=0;i<2;i++){RenderSystem.activeTexture(GL13.GL_TEXTURE0+i);bindings[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);slots[i]=RenderSystem.getShaderTexture(i);}
        try(VisualRenderState ignored=new VisualRenderState()){
            int w=target.textureWidth,h=target.textureHeight;
            if(scene==null||scene.textureWidth!=w||scene.textureHeight!=h){
                if(scene!=null)scene.delete();
                scene=new SimpleFramebuffer(w,h,false);scene.setTexFilter(GL11.GL_LINEAR);
            }
            // Keep Minecraft's cached framebuffer bindings in sync with the actual GL state.
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,target.fbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,scene.fbo);
            GL30.glBlitFramebuffer(0,0,w,h,0,0,w,h,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
            target.beginWrite(false);
            if(allocator==null)allocator=new BufferAllocator(1<<16);
            BufferBuilder buffer=new BufferBuilder(allocator,VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            model.render(matrices,new FaceVertices(buffer),light,overlay,color);
            BuiltBuffer built=buffer.endNullable();
            if(built==null)return true;
            RenderSystem.setShader(program);
            RenderSystem.setShaderTexture(0,mc.getTextureManager().getTexture(texture).getGlId());
            RenderSystem.setShaderTexture(1,scene.getColorAttachment());
            program.getUniformOrDefault("Viewport").set((float)w,(float)h);
            var tint=o.tint.getColor();
            program.getUniformOrDefault("Tint").set(tint.getRed()/255f,tint.getGreen()/255f,tint.getBlue()/255f);
            program.getUniformOrDefault("Material").set(o.opacity.getCurrentValue()/100,o.refraction.getCurrentValue(),o.shine.getCurrentValue(),o.edges.getCurrentValue());
            program.getUniformOrDefault("Time").set((System.nanoTime()%1_000_000_000_000L)/1_000_000_000f*o.speed.getCurrentValue());
            RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(true);
            RenderSystem.enableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
            BufferRenderer.drawWithGlobalProgram(built);
            return true;
        }finally{
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            BufferRenderer.resetCurrentVertexBuffer();GL30.glBindVertexArray(vao);GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);
            for(int i=0;i<2;i++){RenderSystem.activeTexture(GL13.GL_TEXTURE0+i);RenderSystem.bindTexture(bindings[i]);RenderSystem.setShaderTexture(i,slots[i]);}
            RenderSystem.depthFunc(depth);RenderSystem.activeTexture(active);
        }
    }
    /** UV1 carries continuous face coordinates, so edges contain no triangle diagonals. */
    private static final class FaceVertices implements VertexConsumer {
        private final VertexConsumer out;private int corner;
        FaceVertices(VertexConsumer out){this.out=out;}
        public VertexConsumer vertex(float x,float y,float z){out.vertex(x,y,z);int c=corner++&3;out.overlay(c>=2?1:0,c==1||c==2?1:0);return this;}
        public VertexConsumer color(int r,int g,int b,int a){out.color(r,g,b,a);return this;}
        public VertexConsumer texture(float u,float v){out.texture(u,v);return this;}
        public VertexConsumer overlay(int u,int v){return this;}
        public VertexConsumer light(int u,int v){out.light(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){out.normal(x,y,z);return this;}
    }
}
