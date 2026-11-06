package dev.bkclient.utility.render.lyrics;

import com.google.gson.Gson;
import dev.bkclient.framework.msdf.FontData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Lyrics-only font metrics; no name masking or accent transliteration. */
public final class LyricsFonts {
    public static final float EM=10;
    public record Face(Identifier texture,FontData data,Map<Integer,FontData.GlyphData> glyphs,Map<Long,Float> kerning) {
        @Override public int hashCode(){return texture.hashCode();}
    }
    public record Resolved(Face face,FontData.GlyphData glyph,int codepoint,float advance) { }
    private final Map<String,Face> faces=new HashMap<>();
    private final Map<String,Map<Integer,Resolved>> resolved=new HashMap<>();
    public void clear() { faces.clear();resolved.clear(); }
    private Face face(String name) {
        return faces.computeIfAbsent(name,n->{
            var mc=MinecraftClient.getInstance();
            try(var reader=new InputStreamReader(mc.getResourceManager().open(Identifier.of("bkclient","fonts/msdf/"+n+".json")),StandardCharsets.UTF_8)) {
                FontData data=new Gson().fromJson(reader,FontData.class);
                Map<Integer,FontData.GlyphData> glyphs=new HashMap<>();for(var g:data.glyphs())glyphs.put(g.unicode(),g);
                Map<Long,Float> kern=new HashMap<>();if(data.kernings()!=null)for(var k:data.kernings())kern.put(key(k.leftChar(),k.rightChar()),k.advance()*EM);
                return new Face(Identifier.of("bkclient","fonts/msdf/"+n+".png"),data,glyphs,kern);
            } catch(Exception ex) { throw new IllegalStateException("Cannot load lyrics font "+n,ex); }
        });
    }
    private static long key(int a,int b) { return ((long)a<<32)|(b&0xffffffffL); }
    public Resolved resolve(String name,int cp) {
        Map<Integer,Resolved> cache=resolved.computeIfAbsent(name,n->new HashMap<>());
        if(cache.size()>4096)cache.clear();
        return cache.computeIfAbsent(cp,c->{
            if(!name.equals("minecraft")) {
                Face f=face(name);var glyph=f.glyphs.get(c);
                if(glyph==null) { f=face("semibold");glyph=f.glyphs.get(c); }
                if(glyph!=null)return new Resolved(f,glyph,c,glyph.advance()*EM);
            }
            return new Resolved(null,null,c,MinecraftClient.getInstance().textRenderer.getWidth(new String(Character.toChars(c))));
        });
    }
    public LyricsLayout.Metrics metrics(String name) {
        return new LyricsLayout.Metrics() {
            public float advance(int cp) { return resolve(name,cp).advance; }
            public float kerning(int a,int b) {
                if(a<0)return 0;
                Resolved x=resolve(name,a),y=resolve(name,b);
                return x.face!=null&&x.face==y.face?x.face.kerning.getOrDefault(key(a,b),0f):0;
            }
        };
    }
}
