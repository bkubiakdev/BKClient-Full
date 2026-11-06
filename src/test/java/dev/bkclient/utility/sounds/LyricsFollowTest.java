package dev.bkclient.utility.sounds;

import com.google.gson.JsonObject;
import dev.bkclient.utility.render.LyricsCameraFollower;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LyricsFollowTest {
    static int checks;
    static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); checks++; }
    static JsonObject track(String title, String artist, double duration) {
        JsonObject o = new JsonObject(); o.addProperty("trackName",title);o.addProperty("artistName",artist);o.addProperty("duration",duration);return o;
    }
    public static void main(String[] args) {
        check(LyricsMatch.matches(track("ITEPE ITEDE","sanah",201.301),"SANAH","ITEPE ITEDE",201),"matching recording");
        check(!LyricsMatch.matches(track("ITEPE ITEDE","sanah",175),"SANAH","ITEPE ITEDE",201),"reject wrong-length studio recording");
        check(!LyricsMatch.matches(track("ITEPE ITEDE","other",201),"SANAH","ITEPE ITEDE",201),"reject cover");
        check(!LyricsMatch.matches(track("ITEPE ITEDE (sped up)","sanah",201),"SANAH","ITEPE ITEDE",201),"reject different version");
        check(!LyricsMatch.matches(track("ITEPE ITEDE","sanah",201),"SANAH","ITEPE ITEDE",0),"unknown playback length not invented");
        check(!LyricsMatch.matches(new JsonObject(),"SANAH","ITEPE ITEDE",201),"malformed result");
        var parsed=LyricsFetcher.parseTrackInfo("ENKLAWA","ENKLAWA [S02 / E02] - SANAH: ITEPE ITEDE");
        check(parsed.artist().equals("SANAH")&&parsed.title().equals("ITEPE ITEDE"),"exact reported media title");
        for(int fps:new int[]{20,60,144}) {
            var follow = new LyricsCameraFollower();long time=1_000_000_000L;
            for(int i=0;i<fps*3;i++) {
                time+=1_000_000_000L/fps;
                Quaternionf target=new Quaternionf().rotationYXZ((float)(i*4*Math.PI/fps), (float)Math.sin(i*.1)*1.4f,0);
                Quaternionf q=follow.update(target,time,.14f);
                float angle=(float)(2*Math.acos(Math.clamp(Math.abs(q.dot(target)),0,1)));
                check(angle<.141f,"bounded lag at "+fps+" FPS");
                check(new Vector3f(0,0,-1).rotate(q).dot(new Vector3f(0,0,-1).rotate(target))>.98,"always in front");
            }
            Quaternionf reverse=new Quaternionf().rotationY((float)Math.PI);
            follow.reset();follow.update(new Quaternionf(),time,.14f);
            check(Math.abs(follow.update(reverse,time+1_000_000, .14f).dot(reverse))>.9999,"instant 180-degree turn stays visible");
        }
        var text=SyncedLyrics.parseLrc("[00:00]One\n[00:10]Two\n[00:20]Three");
        for(int fps:new int[]{20,60,144})for(int frame=0;frame<fps*8;frame++) {
            var cue=LyricsTimeline.at(text,frame*1000L/fps,0,30000);
            check(cue!=null&&cue.index()==0,"render FPS cannot accelerate lyrics");
        }
        for (double[] direction : new double[][]{{0,0,1},{0,0,-1},{1,0,0},{-1,0,0},{3,2,-4},{0,5,0},{0,-5,0}}) {
            var rotation=dev.bkclient.utility.render.LyricsBillboard.facing(direction[0],direction[1],direction[2]);
            Vector3f normal=new Vector3f(0,0,1).rotate(rotation);
            Vector3f target=new Vector3f((float)direction[0],(float)direction[1],(float)direction[2]).normalize();
            check(normal.dot(target)>.9999f,"billboard faces viewer from every side");
        }
        System.out.println("LyricsFollowTest PASS: "+checks+" checks");
    }
}
