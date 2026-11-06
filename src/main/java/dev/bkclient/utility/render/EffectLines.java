package dev.bkclient.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;

public final class EffectLines implements AutoCloseable {
    private final BufferBuilder buffer;
    private final Matrix4f matrix;
    private final Vec3d camera;
    public EffectLines(Render3DEvent event){
        matrix=event.getMatrices().peek().getPositionMatrix();camera=event.getCamera().getPos();
        RenderSystem.enableBlend();RenderSystem.blendFunc(770,1);RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        buffer=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_COLOR);
    }
    public void line(Vec3d from,Vec3d to,int color,float width,boolean ignored){
        Vec3d a=from.subtract(camera),b=to.subtract(camera);
        Vec3d side=b.subtract(a).crossProduct(a.add(b)).normalize().multiply(width*.004);
        vertex(a.add(side),color);vertex(b.add(side),color);vertex(b.subtract(side),color);vertex(a.subtract(side),color);
    }
    private void vertex(Vec3d p,int color){buffer.vertex(matrix,(float)p.x,(float)p.y,(float)p.z).color(color);}
    public void close(){try{BuiltBuffer built=buffer.endNullable();if(built!=null)BufferRenderer.drawWithGlobalProgram(built);}finally{RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableDepthTest();}}
    public static int alpha(int c,float a){return c&0xFFFFFF | MathHelper.clamp((int)((c>>>24)*a),0,255)<<24;}
    public static int mix(int a,int b,float t){t=MathHelper.clamp(t,0,1);int out=0;for(int shift=0;shift<=24;shift+=8)out|=(int)(((a>>>shift)&255)*(1-t)+((b>>>shift)&255)*t)<<shift;return out;}
}
