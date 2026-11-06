package dev.bkclient.systems.cosmetics.render;

import com.google.gson.*;
import dev.bkclient.systems.cosmetics.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.imageio.ImageIO;

public final class CharacterAnimationTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if(!value)throw new AssertionError(label); }
    private static JsonObject json(String path) throws Exception {
        try(var stream=CharacterAnimationTest.class.getResourceAsStream("/assets/bkclient/"+path)) {
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    public static void main(String[] args) throws Exception {
        var fixture=JsonParser.parseString("""
            {"animations":{"test":{"animation_length":2,"bones":{"head":{"rotation":{
              "0":{"vector":[0,0,0]},"1":{"vector":[20,40,60]}
            }}}},"wave":{"animation_length":2,"bones":{"head":{"rotation":[
              "math.sin(query.anim_time * 90) * 6","-15+math.sin(query.anim_time * -140) * 2","query.anim_time * 720"
            ]}}}}}
            """).getAsJsonObject();
        var clips=BedrockAnimation.parse(fixture);
        check(clips.get("test").sample("head",.5f).rotation()[1]==20,"GeckoLib vector frames interpolate");
        float[] wave=clips.get("wave").sample("head",1).rotation();
        check(Math.abs(wave[0]-6)<.0001,"Molang sine uses degrees and current animation time");
        check(Math.abs(wave[1]-(-15+Math.sin(Math.toRadians(-140))*2))<.0001,"Expression offset and negative frequency");
        check(wave[2]==720,"Continuous time rotation");
        var cosine=new AnimationVector(JsonParser.parseString("[\"math.cos(q.anim_time * 2000) * 3\",1,2]"),0);
        check(cosine.sample(0)[0]==3,"q alias and cosine");
        for(var slot:CosmeticSlot.values()) for(var entry:CosmeticCatalog.entries(slot)) {
            var model=BedrockModel.parse(json(entry.model()));
            var animations=entry.animation()==null?java.util.Map.<String,BedrockAnimation.Clip>of():BedrockAnimation.parse(json(entry.animation()));
            for(var clip:animations.values()) for(float fraction:new float[]{0,.25f,.75f,1}) {
                var pose=model.pose(clip,clip.duration()*fraction,null);
                for(var matrix:pose)check(matrix.isFinite(),"Finite animation: "+entry.id()+" / "+clip.name());
            }
            if(slot!=CosmeticSlot.CHARACTER)continue;
            check(animations.keySet().containsAll(Set.of("idle","walk","attack")),"Character animation set "+entry.id());
            var fit=CharacterPose.fit(model);
            var bounds=model.bounds(model.pose((BedrockAnimation.Clip)null,0,null),Set.of());
            check(fit.scale()>0&&Float.isFinite(fit.scale()),"Character scale "+entry.id());
            check((bounds[4]-bounds[1])*fit.scale()<=1.801f,"Player-sized height "+entry.id());
            check(Math.abs(bounds[1]-fit.floor())<.001f,"Feet alignment "+entry.id());
            for(float swing:new float[]{0,.3f,.8f})for(var matrix:CharacterPose.sample(model,animations,.43f,.2f,swing,30,-20))
                check(matrix.isFinite(),"Mixed character pose "+entry.id());
            try(var stream=CharacterAnimationTest.class.getResourceAsStream("/assets/bkclient/"+entry.texture())) {
                var image=ImageIO.read(stream);
                check(image!=null&&image.getWidth()==model.textureWidth&&image.getHeight()==model.textureHeight,"Texture matches geometry "+entry.id());
            }
        }
        System.out.println("PASS: characters, all cosmetic animation regressions, vector frames, time expressions, fitting and textures; checks="+checks);
    }
}
