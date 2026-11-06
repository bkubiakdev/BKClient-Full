package dev.bkclient.compat;

import dev.bkclient.utility.render.ambience.SodiumTerrainShaders;
import dev.bkclient.utility.render.ambience.SodiumWindTags;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import org.lwjgl.system.MemoryUtil;
import java.util.Random;

public final class SodiumTerrainTest {
    private static void check(boolean b, String message) { if(!b) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        check(SodiumWindTags.tag(1,0,true,false)==0,"grass root remains anchored");
        check(SodiumWindTags.tag(1,1,true,false)==1,"grass top waves");
        check(SodiumWindTags.tag(3,1,false,true)==0,"hanging root remains anchored");
        check(SodiumWindTags.tag(3,0,false,true)==1,"hanging tip waves");
        check(SodiumWindTags.tag(2,0,false,false)==2,"leaves wave");
        check(SodiumWindTags.tag(4,1,false,false)==3,"vines wave");
        check(SodiumWindTags.tag(0,1,false,false)==0,"stone is not tagged");
        SodiumWindTags.restore(23);
        var worker=new java.util.concurrent.FutureTask<Void>(()->{check(SodiumWindTags.current()==0,"worker tags isolated");SodiumWindTags.restore(5);return null;});
        Thread thread=new Thread(worker);
        thread.start();worker.get();check(SodiumWindTags.current()==23,"worker does not leak to parent");SodiumWindTags.restore(0);

        var encoder=new CompactChunkVertex().getEncoder();
        var vertices=ChunkVertexEncoder.Vertex.uninitializedQuad();
        long ptr=MemoryUtil.nmemAlloc(80);
        try {
            Random random=new Random(34);
            for(int quad=0;quad<1000;quad++) {
                for(var vertex:vertices){vertex.x=random.nextFloat()*16;vertex.y=random.nextFloat()*16;vertex.z=random.nextFloat()*16;vertex.color=random.nextInt();vertex.ao=random.nextFloat();vertex.light=random.nextInt();vertex.u=random.nextFloat();vertex.v=random.nextFloat();}
                check(encoder.write(ptr,7,vertices,255)==ptr+80,"compact stride");
                for(int i=0;i<4;i++) {
                    int original=MemoryUtil.memGetInt(ptr+i*20L);
                    check((original & 0xc0000000)==0,"Sodium reserved position bits are available");
                    for(int tag=0;tag<4;tag++) {
                        int encoded=original|(tag<<30);
                        check((encoded&0x3fffffff)==original,"tag preserves all position bits");
                        check(encoded>>>30==tag,"tag roundtrip");
                    }
                }
            }
        } finally {MemoryUtil.nmemFree(ptr);}
        check(SodiumTerrainShaders.patch("other","blocks/block_layer_opaque.vsh","plain").equals("plain"),"unrelated shaders preserved");
        for(String extension:new String[]{"vsh","fsh"}) {
            String path="blocks/block_layer_opaque."+extension;
            try(var input=SodiumTerrainTest.class.getResourceAsStream("/assets/sodium/shaders/"+path)) {
                String original=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                String patched=SodiumTerrainShaders.patch("sodium",path,original);
                check(patched.contains("#import <sodium:include/fog.glsl>"),"Sodium fog retained");
                check(patched.contains(extension.equals("vsh")?"texture(u_LightTex, _vert_tex_light_coord)":"diffuseColor.a < v_MaterialAlphaCutoff"),"lighting and cutout retained");
                check(patched.contains(extension.equals("vsh")?"position += bkWind":"diffuseColor.rgb += bkStarsTint"),"effect inserted");
            }
        }
        System.out.println("PASS: anchors, worker isolation, 1000 encoded quads, spare-bit preservation and real Sodium shader patching");
    }
}
