package dev.bkclient.utility.render.ambience;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.render.VisualRenderState;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Batched world ribbons, local glow and emissive cubes; no fullscreen particle loop. */
public final class SkyWorldStreakRenderer {
    private static final ShaderProgramKey KEY=new ShaderProgramKey(Identifier.of("bkclient","core/sky_effects/world_streak"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    private final SkyStreakTimeline timeline=new SkyStreakTimeline();
    private final float[] heads=new float[SkyStreakTimeline.LIMIT*4], tails=new float[SkyStreakTimeline.LIMIT*4];
    private final Vector3f right=new Vector3f(), up=new Vector3f(), side=new Vector3f();
    private static final float[][] CORNERS={{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},{-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{0,1,5,4},{3,7,6,2}};
    private static final float[] FACE_LIGHT={.8f,1,.65f,.85f,.6f,1};
    private long lastFrame, retryAt;
    private double seconds;

    public void reset() { timeline.reset(); seconds=0; lastFrame=0; retryAt=0; }

    public void render(Render3DEvent event, CyberspaceSkySettings settings, float speed, float opacity, float brightness) {
        if (!settings.streaks.isEnabled() || opacity<=0) { reset(); return; }
        renderWorld(event, Math.round(settings.streakCount.getCurrentValue()),
            speed*settings.flow.getCurrentValue()/25*settings.streakSpeed.getCurrentValue()/100,
            settings.streakLength.getCurrentValue(), settings.streakSize.getCurrentValue(),
            settings.squareHead.isSelected(), settings.starColor.getColor(), opacity, brightness);
    }

    /** Shared by Sky and World; each owner keeps its own clock, anchors and settings. */
    public void renderWorld(Render3DEvent event, int count, float playbackSpeed, float length, float size,
                            boolean squareHead, ColorRGBA color, float opacity, float brightness) {
        count=Math.max(0,Math.min(SkyStreakTimeline.LIMIT,count));
        if(count==0 || opacity<=0) { reset(); return; }
        long now=System.nanoTime();
        // Integrating the clock makes speed changes continuous, including freeze/resume.
        if (lastFrame!=0) seconds+=(now-lastFrame)/1_000_000_000.0*playbackSpeed;
        lastFrame=now;
        if (now<retryAt) return;
        ShaderProgram program;
        try { program=MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(KEY); }
        catch (Exception error) {
            retryAt=now+5_000_000_000L;
            BKClientInit.LOGGER.warn("Unable to load world streak shader",error); return;
        }
        if (program==null) return;
        var camera=event.getCamera(); var pos=camera.getPos();
        timeline.sample(seconds,count,length,size,pos.x,pos.y,pos.z,heads,tails);
        right.set(1,0,0).rotate(camera.getRotation()); up.set(0,1,0).rotate(camera.getRotation());
        int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        try (var state=new VisualRenderState()) {
            RenderSystem.enableBlend(); RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO,GL11.GL_ONE);
            RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL); RenderSystem.depthMask(false); RenderSystem.disableCull();
            RenderSystem.setShader(program);
            program.getUniform("WorldView").set(event.getPositionMatrix());
            program.getUniform("WorldProjection").set(event.getProjectionMatrix());
            program.getUniform("SquareHead").set(squareHead?1f:0f);
            program.getUniform("Brightness").set(brightness);
            for (int pass=0;pass<(squareHead?3:2);pass++) {
                program.getUniform("Part").set((float)pass);
                BufferBuilder b=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE_COLOR);
                for (int i=0;i<count;i++) {
                    int p=i*4;
                    int alpha=Math.round(heads[p+3]*opacity*255);
                    if(alpha<1) continue;
                    int rgba=alpha<<24|color.getRGB()&0xffffff;
                    float x=heads[p],y=heads[p+1],z=heads[p+2],r=tails[p+3];
                    if(pass==0) {
                        // Ribbon lies along the flight in world space and faces the eye.
                        side.set((float)SkyStreakTimeline.DX,(float)SkyStreakTimeline.DY,(float)SkyStreakTimeline.DZ).cross(x,y,z);
                        if(side.lengthSquared()<1e-6f) side.set(up);
                        side.normalize().mul(r*5);
                        vertex(b,tails[p]-side.x,tails[p+1]-side.y,tails[p+2]-side.z,0,-5,rgba);
                        vertex(b,x-side.x,y-side.y,z-side.z,1,-5,rgba);
                        vertex(b,x+side.x,y+side.y,z+side.z,1,5,rgba);
                        vertex(b,tails[p]+side.x,tails[p+1]+side.y,tails[p+2]+side.z,0,5,rgba);
                    } else if(pass==1) {
                        float w=r*8;
                        vertex(b,x-right.x*w-up.x*w,y-right.y*w-up.y*w,z-right.z*w-up.z*w,-8,-8,rgba);
                        vertex(b,x+right.x*w-up.x*w,y+right.y*w-up.y*w,z+right.z*w-up.z*w,8,-8,rgba);
                        vertex(b,x+right.x*w+up.x*w,y+right.y*w+up.y*w,z+right.z*w+up.z*w,8,8,rgba);
                        vertex(b,x-right.x*w+up.x*w,y-right.y*w+up.y*w,z-right.z*w+up.z*w,-8,8,rgba);
                    } else {
                        float c=(float)Math.cos(i*.7+.35),s=(float)Math.sin(i*.7+.35);
                        for(int face=0;face<6;face++) {
                            // Only outward faces: overlapping rear faces would wash out the cube.
                            float nx=face==2?-c:face==3?c:face==0?-s:face==1?s:0;
                            float ny=face==4?-1:face==5?1:0;
                            float nz=face==2?s:face==3?-s:face==0?-c:face==1?c:0;
                            if(nx*x+ny*y+nz*z>=0)continue;
                            float light=FACE_LIGHT[face];
                            int shaded=alpha<<24|Math.round(color.getRed()*light)<<16|Math.round(color.getGreen()*light)<<8|Math.round(color.getBlue()*light);
                            for(int corner=0;corner<4;corner++) {
                                float[] v=CORNERS[FACES[face][corner]];
                                vertex(b,x+(v[0]*c+v[2]*s)*r,y+v[1]*r,z+(v[2]*c-v[0]*s)*r,corner==0||corner==3?-1:1,corner<2?-1:1,shaded);
                            }
                        }
                    }
                }
                BuiltBuffer built=b.endNullable(); if(built!=null) BufferRenderer.drawWithGlobalProgram(built);
            }
        } finally { RenderSystem.depthFunc(depthFunc); }
    }
    private static void vertex(BufferBuilder b,float x,float y,float z,float u,float v,int color) { b.vertex(x,y,z).texture(u,v).color(color); }
}
