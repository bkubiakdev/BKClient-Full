package dev.bkclient.utility.render;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.ShaderESP;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.shader.Shader;
import dev.bkclient.utility.render.shader.HandShaderHelper;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.*;
import static org.lwjgl.opengl.GL14C.*;
import static org.lwjgl.opengl.GL20C.*;

/** BKEngine dual-Kawase shader math, using the 1.21.4 OpenGL framebuffer API. */
public final class BKEngineShaderEspRenderer implements IMinecraft {
    public static final class Params {
        public int color = 0xFF00FFFF;
        public int friendColor = 0xFF55FF55;
        public boolean friendEnabled = true;
        public float opacity = 1.0f;
        public float saturation = 1.0f;

        public boolean glowEnabled = true;
        public float glowRadius = 0.5f;   // 0..1
        public float glowStrength = 1.4f;
        public float glowFalloff = 1.2f;

        public boolean fillEnabled = true;
        public float fillOpacity = 0.35f;
        public float innerGlow = 0.5f;

        public boolean outlineEnabled = true;
        public int outlineWidth = 2;
        public int outlineMode = 0;       // 0 снаружи, 1 внутри, 2 обе
        public float outlineStrength = 1.5f;
        public float outlineWhite = 0.5f;

        public boolean pulseEnabled = false;
        public float pulseSpeed = 1.0f;
        public float pulseAmount = 0.3f;

        public boolean shimmerEnabled = true;
        public float shimmerWidth = 0.04f;
        public float shimmerPeriodSec = 5.0f;
        public float shimmerBrightness = 0.8f;
    }


    private static Framebuffer pendingMask;
    public static void queue(Framebuffer mask) { pendingMask = mask; }
    public static void finishFrame() {
        Framebuffer mask = pendingMask;
        pendingMask = null;
        if (mask != null && ready()) render(mask);
    }
    private static Shader down, up, combine;
    private static final SimpleFramebuffer[] mips = new SimpleFramebuffer[5];
    private static int width, height;
    private static boolean failed;
    private static final Params params = new Params();
    private static final int[] scissorBox = new int[4];
    private static final java.nio.ByteBuffer colorMask = org.lwjgl.BufferUtils.createByteBuffer(4);
    private BKEngineShaderEspRenderer() { }
    public static boolean ready() {
        if (!ShaderESP.active() || failed) return false;
        try {
            if (down == null) down = new Shader("bkengine", "esp_down");
            if (up == null) up = new Shader("bkengine", "esp_up");
            if (combine == null) combine = new Shader("bkengine", "esp_combine");
            return true;
        } catch (RuntimeException e) {
            failed = true;
            BKClientInit.LOGGER.warn("Shader ESP shader initialization failed", e);
            return false;
        }
    }
    private static void resize(int w, int h) {
        if (width == w && height == h && mips[0] != null) return;
        width = w; height = h;
        for (int i=0;i<5;i++) {
            if (mips[i]!=null) mips[i].delete();
            w=Math.max(1,w/2);h=Math.max(1,h/2);
            mips[i]=new SimpleFramebuffer(w,h,false);
            mips[i].setClearColor(0,0,0,0);
            mips[i].setTexFilter(GL_LINEAR);
        }
    }
    public static void render(Framebuffer mask) {
        if (mask == null || !ready()) return;
        var main=mc.getFramebuffer();
        int program=glGetInteger(GL_CURRENT_PROGRAM), active=glGetInteger(GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL_TEXTURE0);int tex0=glGetInteger(GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL_TEXTURE1);int tex1=glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean depth=glIsEnabled(GL_DEPTH_TEST), blend=glIsEnabled(GL_BLEND), cull=glIsEnabled(GL_CULL_FACE);
        boolean write=glGetBoolean(GL_DEPTH_WRITEMASK);
        boolean scissor=glIsEnabled(GL_SCISSOR_TEST);
        glGetIntegerv(GL_SCISSOR_BOX,scissorBox);
        glGetBooleanv(GL_COLOR_WRITEMASK,colorMask);
        int sr=glGetInteger(GL_BLEND_SRC_RGB),dr=glGetInteger(GL_BLEND_DST_RGB),sa=glGetInteger(GL_BLEND_SRC_ALPHA),da=glGetInteger(GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true,true,true,true);
            resize(mask.textureWidth,mask.textureHeight);
            ShaderESP.get().applySettings(params);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.disableBlend();
            int levels=Math.max(1,Math.min(5,1+Math.round(params.glowRadius*4)));
            Framebuffer input=mask;
            // Outline coverage and inner glow also use the blurred mask.
            boolean needsBlur = params.glowEnabled || params.outlineEnabled
                    || (params.fillEnabled && params.innerGlow != 0);
            if (needsBlur) {
                for(int i=0;i<levels;i++) { pass(down,input,mips[i],1);input=mips[i]; }
                for(int i=levels-1;i>0;i--) pass(up,mips[i],mips[i-1],.5F+params.glowRadius*3.5F);
            }
            main.beginWrite(true);
            RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(GL_SRC_ALPHA,GL_ONE,GL_ONE,GL_ZERO);
            combine.bind();
            bind(0,mask.getColorAttachment());bind(1,needsBlur ? mips[0].getColorAttachment() : mask.getColorAttachment());
            combine.setUniform1i("maskTexture",0);combine.setUniform1i("glowTexture",1);
            color("mainColor",params.color,params.opacity);color("friendColor",params.friendColor,params.friendEnabled?1:0);
            combine.setUniform4f("params1",1F/width,1F/height,params.outlineWidth,params.outlineMode);
            combine.setUniform4f("params2",params.glowStrength,params.glowFalloff,params.fillOpacity,params.innerGlow);
            combine.setUniform4f("params3",params.outlineStrength,params.outlineWhite,params.glowEnabled?1:0,params.fillEnabled?1:0);
            combine.setUniform4f("params4",phase(params.shimmerPeriodSec),params.shimmerEnabled?1:0,params.shimmerWidth,params.shimmerBrightness);
            combine.setUniform4f("params5",params.pulseEnabled?params.pulseAmount:0,phase(1F/params.pulseSpeed),params.saturation,params.outlineEnabled?1:0);
            HandShaderHelper.drawFullScreenQuad();
        } finally {
            main.beginWrite(true);glUseProgram(program);
            RenderSystem.colorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);
            if(scissor) RenderSystem.enableScissor(scissorBox[0],scissorBox[1],scissorBox[2],scissorBox[3]);
            bind(0,tex0);bind(1,tex1);RenderSystem.activeTexture(active);
            RenderSystem.depthMask(write);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(sr,dr,sa,da);
        }
    }
    private static void bind(int unit,int texture){RenderSystem.activeTexture(GL_TEXTURE0+unit);RenderSystem.bindTexture(texture);}
    private static void pass(Shader shader,Framebuffer input,Framebuffer output,float offset){
        output.beginWrite(true);shader.bind();bind(0,input.getColorAttachment());
        shader.setUniform1i("Sampler0",0);shader.setUniform4f("params",.5F/input.textureWidth,.5F/input.textureHeight,offset,0);
        HandShaderHelper.drawFullScreenQuad();
    }
    private static void color(String name,int rgb,float alpha){combine.setUniform4f(name,((rgb>>16)&255)/255F,((rgb>>8)&255)/255F,(rgb&255)/255F,alpha);}
    private static float phase(float seconds){long ms=Math.max(1,(long)(seconds*1000));return (System.currentTimeMillis()%ms)/(float)ms;}
    public static void release(){
        if(!RenderSystem.isOnRenderThread()){RenderSystem.recordRenderCall(BKEngineShaderEspRenderer::release);return;}
        for(int i=0;i<5;i++)if(mips[i]!=null){mips[i].delete();mips[i]=null;}
        if(down!=null)down.delete();if(up!=null)up.delete();if(combine!=null)combine.delete();
        down=up=combine=null;pendingMask=null;failed=false;width=height=0;
    }
}
