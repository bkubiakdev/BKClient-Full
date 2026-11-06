package dev.bkclient.utility.render.lyrics;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.VisualRenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import java.util.*;

/** Direct world rendering with a dedicated MSDF shader; never draws through GUI batching. */
public final class LyricsRenderer implements AutoCloseable {
    private static final ShaderProgramKey SHADER=new ShaderProgramKey(Identifier.of("bkclient","core/lyrics/font"),VertexFormats.POSITION_TEXTURE_COLOR,Defines.EMPTY);
    public final LyricsFonts fonts=new LyricsFonts();
    private static volatile long resourceRevision;
    private long seenRevision=-1;
    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate consumers;
    public void close() { consumers=null;if(allocator!=null){allocator.close();allocator=null;}fonts.clear(); }
    public static void resourcesReloaded(){resourceRevision++;}
    public boolean prepare(){if(seenRevision==resourceRevision)return false;close();seenRevision=resourceRevision;return true;}
    public void render(Render3DEvent event,List<Draw> lines,LyricsOptions o,double seconds) {
        if(lines.isEmpty())return;
        renderPass(()->{
            var camera=event.getCamera().getPos();
            for(Draw draw:lines){
                var v=draw.verse;var effect=draw.transform;
                Matrix4f base=new Matrix4f(event.getMatrices().peek().getPositionMatrix())
                    .translate((float)(v.pose.position.x-camera.x),(float)(v.pose.position.y-camera.y+draw.lift),(float)(v.pose.position.z-camera.z))
                    .rotate(v.pose.rotation).scale(v.scale,-v.scale,v.scale).translate(0,effect.y(),0).rotateX(effect.flip()).scale(effect.scale());
                draw(base,draw,o,seconds,o.throughWalls.isEnabled());
            }
        });
    }
    /** Same glyphs, shader and effects as the world pass, in a clipped editor viewport. */
    public void renderPreview(Matrix4f matrix,List<Draw> lines,LyricsOptions o,double seconds){
        if(lines.isEmpty())return;
        renderPass(()->{
            for(Draw draw:lines){
                var effect=draw.transform;
                Matrix4f base=new Matrix4f(matrix).translate(0,-draw.lift,0)
                    .translate(0,effect.y(),0).rotateX(effect.flip()).scale(effect.scale());
                draw(base,draw,o,seconds,true);
            }
        });
    }
    private void renderPass(Runnable action) {
        prepare();
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),vao=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),vbo=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),depth=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int[] bindings=new int[3],slots=new int[3];
        for(int i=0;i<3;i++){RenderSystem.activeTexture(GL13.GL_TEXTURE0+i);bindings[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);slots[i]=RenderSystem.getShaderTexture(i);}
        try(VisualRenderState ignored=new VisualRenderState()) {
            if(allocator==null){allocator=new BufferAllocator(1<<16);consumers=VertexConsumerProvider.immediate(allocator);}
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(1,1,1,1);
            action.run();
            consumers.draw();
        } finally {
            BufferRenderer.resetCurrentVertexBuffer();GL30.glBindVertexArray(vao);GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);
            for(int i=0;i<3;i++){RenderSystem.activeTexture(GL13.GL_TEXTURE0+i);RenderSystem.bindTexture(bindings[i]);RenderSystem.setShaderTexture(i,slots[i]);}
            RenderSystem.depthFunc(depth);RenderSystem.activeTexture(active);
        }
    }
    public record Draw(LyricsVerse verse,LyricsAnimation.Transform transform,float alpha,float lift) { }
    private void draw(Matrix4f base,Draw draw,LyricsOptions o,double seconds,boolean through) {
        if(through)RenderSystem.disableDepthTest();else RenderSystem.enableDepthTest();
        LyricsVerse v=draw.verse;LyricsLayout layout=v.layout;var effect=draw.transform;
        var wordEffect=o.wordAt(v.wordProgress);
        float alpha=draw.alpha*effect.alpha()*o.opacity.getCurrentValue()/100;
        if(alpha<.02f)return;
        String font=LyricsOptions.id(o.font),colorMode=LyricsOptions.id(o.colors);
        int first=colorMode.equals("theme")?Colors.getAccentColor().getRGB():o.color.getColor().getRGB();
        LyricsPalette palette=new LyricsPalette(first,o.color2.getColor().getRGB(),o.color3.getColor().getRGB(),colorMode.equals("gradient3")?3:colorMode.equals("gradient2")?2:1,o.angle.getCurrentValue(),seconds/o.period.getCurrentValue(),o.flow.isEnabled());
        // Emphasised words of the Scattered display use the second colour, like a highlighted keyword.
        int accent=o.color2.getColor().getRGB();
        LyricsPalette emphasis=new LyricsPalette(accent,accent,accent,1,0,0,false);
        v.trackPieces(seconds);
        Map<LyricsFonts.Face,List<LyricsLayout.Glyph>> batches=new LinkedHashMap<>();
        for(var g:layout.glyphs())if(g.end()<=v.visibleEnd)batches.computeIfAbsent(fonts.resolve(font,g.codepoint()).face(),f->new ArrayList<>()).add(g);
        // Render glow and shadow before crisp fill. No global blur or depth writes.
        for(int pass=0;pass<3;pass++) {
            if(pass==0&&!o.glow.isEnabled()||pass==1&&!o.shadow.isEnabled())continue;
            for(var batch:batches.entrySet()) {
                if(batch.getKey()==null) { vanilla(batch.getValue(),v,base,palette,emphasis,alpha,o,pass,wordEffect,effect,seconds,through);continue; }
                consumers.draw();
                RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);
                if(through)RenderSystem.disableDepthTest();else RenderSystem.enableDepthTest();
                var face=batch.getKey();
                var texture=MinecraftClient.getInstance().getTextureManager().getTexture(face.texture());texture.setFilter(true,false);
                RenderSystem.setShaderTexture(0,texture.getGlId());
                ShaderProgram shader=RenderSystem.setShader(SHADER);
                shader.getUniform("Range").set(face.data().atlas().range());
                shader.getUniform("OutlineWidth").set(pass==2&&o.outline.isEnabled()?o.outlineWidth.getCurrentValue():0);
                int oc=o.outlineColor.getColor().getRGB();shader.getUniform("OutlineColor").set(((oc>>16)&255)/255f,((oc>>8)&255)/255f,(oc&255)/255f,(oc>>>24)/255f);
                shader.getUniform("Softness").set(pass==0?1.5f:.5f);
                BufferBuilder buffer=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE_COLOR);
                for(var g:batch.getValue()) {
                    var resolved=fonts.resolve(font,g.codepoint());var data=resolved.glyph();if(data.planeBounds()==null)continue;
                    float shatter=glyphShatter(g,v,effect,seconds);
                    Matrix4f matrix=glyphMatrix(base,g,v,wordEffect,shatter,seconds);
                    float a=alpha*wordAlpha(g,v,wordEffect)*shatterAlpha(g,v,shatter)*pieceIn(g,v,seconds);
                    if(a<.004f)continue;
                    LyricsPalette tint=emphasized(g,v)?emphasis:palette;
                    if(pass==0) {
                        for(int j=0;j<8;j++) {double rad=j*Math.PI/4;emit(buffer,new Matrix4f(matrix).translate((float)Math.cos(rad)*o.glowRadius.getCurrentValue(),(float)Math.sin(rad)*o.glowRadius.getCurrentValue(),0),g,resolved,tint,a*o.glowPower.getCurrentValue()/100*.16f,o,pass,layout);}
                    } else emit(buffer,pass==1?new Matrix4f(matrix).translate(1,1,0):matrix,g,resolved,tint,a*(pass==1?.5f:1),o,pass,layout);
                }
                BuiltBuffer built=buffer.endNullable();if(built!=null)BufferRenderer.drawWithGlobalProgram(built);
            }
        }
    }
    /** Full per-glyph transform: Scattered placement (or the word reveal), then the Disintegrate exit. */
    private Matrix4f glyphMatrix(Matrix4f base,LyricsLayout.Glyph g,LyricsVerse v,LyricsAnimation.Transform word,float shatter,double seconds) {
        Matrix4f m;
        LyricsScatter s=v.scatter;
        if(s!=null&&g.word()>=0&&g.word()<s.words.length) {
            var w=s.words[g.word()];var c=s.chunks[w.chunk()];
            float xn=c.halfWidth()>0?Math.clamp(w.center()/c.halfWidth(),-1,1):0;
            // A new piece pops in slightly larger and settles to its size.
            float pop=1+.35f*(1-pieceIn(g,v,seconds));
            // Piece position and tilt, then the word's spot on the (optionally bent) piece line.
            m=new Matrix4f(base).translate(c.x(),c.y(),c.z()).rotateY(c.yaw()).rotateZ(c.roll()).scale(pop,pop,1)
                .translate(w.center(),-c.arc()*(1-xn*xn),0).rotateZ((float)Math.atan(2*c.arc()*xn/Math.max(1,c.halfWidth())));
            if(g.end()>v.settledEnd)m.translate(0,word.y(),0).rotateZ(word.roll()).scale(word.scale(),word.scale(),1);
            m.scale(w.scale(),w.scale(),1).translate(-(w.start()+w.width()/2),0,0);
        } else m=wordMatrix(base,g,v,word);
        if(shatter>0)shatter(m,g,v,shatter);
        return m;
    }
    private static final double PIECE_IN_SECONDS=.28;
    /** Entry progress (0..1, eased) of the glyph's piece; 1 outside the Scattered display. */
    private static float pieceIn(LyricsLayout.Glyph g,LyricsVerse v,double seconds) {
        if(v.scatter==null||v.pieceShown==null||g.word()<0||g.word()>=v.scatter.words.length)return 1;
        double shown=v.pieceShown[v.scatter.words[g.word()].chunk()];
        return Double.isNaN(shown)?1:LyricsAnimation.ease((float)((seconds-shown)/PIECE_IN_SECONDS));
    }
    /** Disintegrate progress for a glyph (the verse exit). */
    private static float glyphShatter(LyricsLayout.Glyph g,LyricsVerse v,LyricsAnimation.Transform verse,double seconds) {
        return verse.shatter();
    }
    private static boolean emphasized(LyricsLayout.Glyph g,LyricsVerse v) {
        return v.scatter!=null&&g.word()>=0&&g.word()<v.scatter.words.length&&v.scatter.words[g.word()].emphasis();
    }
    /** Glyphs leave one after another (left to right with jitter); each flies off, spins, shrinks and fades. */
    private static float shatterProgress(LyricsLayout.Glyph g,LyricsVerse v,float shatter) {
        float order=g.start()/(float)Math.max(1,v.cue.text().length());
        if(v.scatter!=null&&g.word()>=0&&g.word()<v.scatter.words.length) {
            // Scattered: pieces break up one after another (first piece first), letters left to right inside a piece.
            var w=v.scatter.words[g.word()];var c=v.scatter.chunks[w.chunk()];
            float x=w.center()+(g.x()+g.advance()/2-w.start()-w.width()/2)*w.scale();
            float within=Math.clamp((x+c.halfWidth())/Math.max(1,2*c.halfWidth()),0,1);
            float piece=Math.clamp((w.chunk()+.35f*within)/Math.max(1,v.scatter.chunks.length),0,1);
            float delay=.55f*piece+.06f*random(v,g,0);
            return Math.clamp((shatter-delay)/.39f,0,1);
        }
        float delay=.45f*(.6f*order+.4f*random(v,g,0));
        return Math.clamp((shatter-delay)/.55f,0,1);
    }
    private static float shatterAlpha(LyricsLayout.Glyph g,LyricsVerse v,float shatter) {
        if(shatter<=0)return 1;
        return (float)Math.pow(1-shatterProgress(g,v,shatter),1.4);
    }
    private static void shatter(Matrix4f m,LyricsLayout.Glyph g,LyricsVerse v,float shatter) {
        float e=LyricsAnimation.ease(shatterProgress(g,v,shatter));
        if(e<=0)return;
        double angle=random(v,g,1)*Math.PI*2;float distance=(14+random(v,g,2)*22)*e;
        float dx=(float)Math.cos(angle)*distance,dy=(float)Math.sin(angle)*distance*.6f-(10+random(v,g,3)*14)*e,dz=(random(v,g,4)-.5f)*30*e;
        float cx=g.x()+g.advance()/2,cy=g.y()+5;
        m.translate(cx+dx,cy+dy,dz).rotateZ((random(v,g,5)-.5f)*5*e).rotateX((random(v,g,6)-.5f)*3*e).scale(Math.max(.05f,1-.6f*e)).translate(-cx,-cy,0);
    }
    /** Stable pseudo-random value in [0,1) for a glyph of a verse. */
    private static float random(LyricsVerse v,LyricsLayout.Glyph g,int salt) {
        long h=v.cue.startMs()*0x9E3779B97F4A7C15L+g.start()*0xBF58476D1CE4E5B9L+salt*0x94D049BB133111EBL;
        h^=h>>>33;h*=0xff51afd7ed558ccdL;h^=h>>>33;h*=0xc4ceb9fe1a85ec53L;h^=h>>>33;
        return (h>>>40)/(float)(1L<<24);
    }
    private Matrix4f wordMatrix(Matrix4f base,LyricsLayout.Glyph g,LyricsVerse v,LyricsAnimation.Transform t) {
        if(g.end()<=v.settledEnd)return new Matrix4f(base);
        float start=g.x(),end=g.x()+g.advance();
        for(var p:v.layout.glyphs())if(p.word()==g.word()&&p.y()==g.y()){start=Math.min(start,p.x());end=Math.max(end,p.x()+p.advance());}
        float center=(start+end)/2;
        return new Matrix4f(base).translate(center,g.y()+5+t.y(),0).rotateZ(t.roll()).scale(t.scale(),t.scale(),1).translate(-center,-g.y()-5,0);
    }
    private float wordAlpha(LyricsLayout.Glyph g,LyricsVerse v,LyricsAnimation.Transform effect) {
        return g.end()<=v.settledEnd?1:effect.alpha();
    }
    private void emit(BufferBuilder b,Matrix4f m,LyricsLayout.Glyph g,LyricsFonts.Resolved r,LyricsPalette palette,float alpha,LyricsOptions o,int pass,LyricsLayout layout) {
        var p=r.glyph().planeBounds();var uv=r.glyph().atlasBounds();var atlas=r.face().data().atlas();
        float x=g.x()+p.left()*LyricsFonts.EM,y=g.y()+8-p.top()*LyricsFonts.EM,w=(p.right()-p.left())*LyricsFonts.EM,h=(p.top()-p.bottom())*LyricsFonts.EM;
        float u=uv.left()/atlas.width(),u2=uv.right()/atlas.width(),v=1-uv.top()/atlas.height(),v2=1-uv.bottom()/atlas.height();
        vertex(b,m,x,y,u,v,palette,alpha,o,pass,layout);vertex(b,m,x,y+h,u,v2,palette,alpha,o,pass,layout);
        vertex(b,m,x+w,y+h,u2,v2,palette,alpha,o,pass,layout);vertex(b,m,x+w,y,u2,v,palette,alpha,o,pass,layout);
    }
    private int color(float x,float y,LyricsPalette p,float alpha,LyricsOptions o,int pass,LyricsLayout l) {
        int argb=p.at(x,y,l.width(),l.height(),alpha);
        if(pass==1)return argb&0xff000000;
        if(pass==0&&!o.glowInherit.isEnabled())return (Math.round((argb>>>24)*o.glowColor.getColor().getAlpha()/255f)<<24)|(o.glowColor.getColor().getRGB()&0xffffff);
        return argb;
    }
    private void vertex(BufferBuilder b,Matrix4f m,float x,float y,float u,float v,LyricsPalette p,float alpha,LyricsOptions o,int pass,LyricsLayout l) { b.vertex(m,x,y,0).texture(u,v).color(color(x,y,p,alpha,o,pass,l)); }
    private void vanilla(List<LyricsLayout.Glyph> glyphs,LyricsVerse v,Matrix4f base,LyricsPalette palette,LyricsPalette emphasis,float alpha,LyricsOptions o,int pass,LyricsAnimation.Transform wordEffect,LyricsAnimation.Transform effect,double seconds,boolean through) {
        var mc=MinecraftClient.getInstance();var layer=through?TextRenderer.TextLayerType.SEE_THROUGH:TextRenderer.TextLayerType.NORMAL;
        for(var g:glyphs) {
            float shatter=glyphShatter(g,v,effect,seconds);
            Matrix4f matrix=glyphMatrix(base,g,v,wordEffect,shatter,seconds);float a=alpha*wordAlpha(g,v,wordEffect)*shatterAlpha(g,v,shatter)*pieceIn(g,v,seconds);
            if(a<.004f)continue;
            LyricsPalette p=emphasized(g,v)?emphasis:palette;
            String text=new String(Character.toChars(g.codepoint()));
            if(pass==0) {
                for(int i=0;i<8;i++){double r=i*Math.PI/4;vanillaGlyph(text,g.x()+(float)Math.cos(r)*o.glowRadius.getCurrentValue(),g.y()+(float)Math.sin(r)*o.glowRadius.getCurrentValue(),color(g.x(),g.y(),p,a*o.glowPower.getCurrentValue()/100*.16f,o,pass,v.layout),matrix,layer);}
            } else {
                if(pass==2&&o.outline.isEnabled())for(int i=0;i<8;i++){double r=i*Math.PI/4;int c=o.outlineColor.getColor().getRGB();vanillaGlyph(text,g.x()+(float)Math.cos(r)*o.outlineWidth.getCurrentValue(),g.y()+(float)Math.sin(r)*o.outlineWidth.getCurrentValue(),(Math.round((c>>>24)*a)<<24)|(c&0xffffff),matrix,layer);}
                vanillaGlyph(text,g.x()+(pass==1?1:0),g.y()+(pass==1?1:0),color(g.x(),g.y(),p,a*(pass==1?.5f:1),o,pass,v.layout),matrix,layer);
            }
        }
    }
    private void vanillaGlyph(String text,float x,float y,int color,Matrix4f matrix,TextRenderer.TextLayerType layer) {
        if((color>>>24)<5)return;
        MinecraftClient.getInstance().textRenderer.draw(Text.literal(text).asOrderedText(),x,y,color,false,matrix,consumers,layer,0,0xF000F0);
    }
}
