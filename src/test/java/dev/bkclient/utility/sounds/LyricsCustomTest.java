package dev.bkclient.utility.sounds;

import dev.bkclient.utility.render.lyrics.*;
import org.joml.*;
import java.util.*;

public final class LyricsCustomTest {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    public static void main(String[] args) {
        LyricsLayout.Metrics metrics=cp->cp==' '?3:6;
        String text="Żółć płynie przez świat Привет ЇЄІҐ 😀";
        for(int align=0;align<3;align++)for(float width:new float[]{30,80,280}) {
            var layout=LyricsLayout.build(text,metrics,width,.2f,2,align);
            check(layout.width()<=width+.01,"wrap limit");
            StringBuilder actual=new StringBuilder();for(var g:layout.glyphs())actual.appendCodePoint(g.codepoint());
            check(actual.toString().equals(text.replace(" ","")),"all Unicode code points preserved");
            check(layout.glyphs().stream().allMatch(g->g.end()-g.start()==Character.charCount(g.codepoint())),"UTF16 boundaries");
            for(var g:layout.glyphs())check(Float.isFinite(g.x())&&Float.isFinite(g.y()),"finite coordinates");
            var words=LyricsWordReveal.wordEnds(text);
            var original=List.copyOf(layout.glyphs());
            for(int time=0;time<5000;time+=23){LyricsWordReveal.frame(words,0,5000,time);check(original.equals(layout.glyphs()),"reveal never moves glyphs");}
        }
        check(LyricsLayout.build("one\n\ntwo",metrics,100,0,1,1).rows()==3,"explicit empty row");
        check(LyricsLayout.build("",metrics,100,0,1,1).glyphs().isEmpty(),"empty safe");
        check(LyricsLayout.build("one     two",metrics,25,0,1,1).width()==18,"trailing spaces do not exceed wrap or shift alignment");
        check(LyricsLayout.build("one   \n",metrics,80,0,1,1).width()==18,"explicit break trims only layout whitespace");
        for(boolean follow:new boolean[]{false,true})for(boolean face:new boolean[]{false,true}) {
            var pose=new LyricsPose(new Vector3d(1,2,3),new Quaternionf());
            pose.follow(new Vector3d(5,6,7),follow,.1,0);pose.face(new Vector3d(9,2,1),face);
            check(pose.position.equals(follow?new Vector3d(5,6,7):new Vector3d(1,2,3)),"independent position toggle");
            check(face||pose.rotation.equals(new Quaternionf()),"independent rotation toggle");
            var frozen=new Vector3d(pose.position);var rotation=new Quaternionf(pose.rotation);
            pose.follow(new Vector3d(90,80,70),false,1,0);pose.face(new Vector3d(-10,0,-10),false);
            check(frozen.equals(pose.position)&&rotation.equals(pose.rotation),"off freezes without jump");
        }
        for(int fps:new int[]{20,60,144}) {
            var pose=new LyricsPose(new Vector3d(),new Quaternionf());
            for(int i=0;i<fps;i++)pose.follow(new Vector3d(10,0,0),true,1.0/fps,100);
            check(java.lang.Math.abs(pose.position.x-10*(1-java.lang.Math.exp(-10)))<1e-6,"FPS independent following");
            for(String name:new String[]{"none","fade","slide","pop","zoom","flip","wave"})for(int i=0;i<=fps;i++) {
                var t=LyricsAnimation.enter(name,i/(float)fps,2);
                check(t.alpha()>=0&&t.alpha()<=1&&t.scale()>.05&&Float.isFinite(t.y())&&Float.isFinite(t.flip()),"bounded entrance");
                if(i==fps)check(java.lang.Math.abs(t.scale()-1)<.0001&&java.lang.Math.abs(t.y())<.0001&&t.alpha()==1,"settled transform");
            }
        }
        for(String name:new String[]{"none","fade","float","shrink"})check(LyricsAnimation.exit(name,1,1).alpha()==0,"exit completes");
        check(java.lang.Math.abs(LyricsAnimation.word("pop",0,1).scale()-1.08f)<.0001,"legacy word pop scale");
        check(LyricsAnimation.word("pop",1,1).equals(LyricsAnimation.Transform.IDENTITY),"word pop settles");
        var mixed=List.of(new LyricsAnimation.Effect("fade",200),new LyricsAnimation.Effect("pop",1000),new LyricsAnimation.Effect("slide",800));
        var independent=LyricsAnimation.stack("entry",mixed,200,2000,1);
        check(independent.alpha()==1&&independent.y()>0,"fade finishes while longer movement continues");
        check(LyricsAnimation.stack("entry",mixed,1000,2000,1).equals(LyricsAnimation.Transform.IDENTITY),"mixed entrance finishes");
        check(LyricsAnimation.stack("entry",mixed,200,200,1).equals(LyricsAnimation.Transform.IDENTITY),"short verse compresses every effect proportionally");
        check(LyricsAnimation.stack("entry",List.of(),0,100,1).equals(LyricsAnimation.Transform.IDENTITY),"empty selection means no entrance");
        var wordsMixed=List.of(new LyricsAnimation.Effect("pop",160),new LyricsAnimation.Effect("wave",400));
        var wordMixed=LyricsAnimation.stack("word",wordsMixed,160,400,1);
        check(wordMixed.scale()==1&&java.lang.Math.abs(wordMixed.y())>.01,"word effects have independent clocks");
        for(int fps:new int[]{20,60,144})for(int frame=0;frame<=fps;frame++){
            var t=LyricsAnimation.stack("entry",mixed,frame*1000f/fps,1000,2);
            check(t.alpha()>=0&&t.alpha()<=1&&t.scale()>0&&Float.isFinite(t.y()),"mixed effects finite at supported frame rates");
        }
        var palette=new LyricsPalette(0xffff0000,0xff00ff00,0xff0000ff,3,0,0,false);
        check(palette.at(-50,0,100,20,1)==0xffff0000,"gradient start");
        check(palette.at(0,0,100,20,1)==0xff00ff00,"gradient middle");
        check(palette.at(50,0,100,20,1)==0xff0000ff,"gradient end");
        check((palette.at(0,0,100,20,.5f)>>>24)==128,"alpha independent");
        int[] words=LyricsWordReveal.wordEnds("Pierwsze drugie trzecie");
        check(LyricsWordReveal.frame(words,0,5000,1000,.2,1200,160).visibleEnd()==words[2],"custom reveal fraction");
        check(LyricsWordReveal.frame(words,0,200,199,1,650,160).visibleEnd()==words[2],"short cue reveals final word before next cue");
        check(LyricsWordReveal.frame(words,0,200,200,1,650,160).settledEnd()==words[2],"short cue entrance completes on time");
        check(LyricsWordReveal.frame(words,0,10000,1300,1,1200,160).visibleEnd()==words[2],"word gap never exceeds 650ms");
        check(LyricsWordReveal.frame(words,0,5000,0).visibleEnd()==words[0],"seek resets progress");
        for(int fps:new int[]{20,60,144}){
            var clock=new LyricsPreviewClock();clock.tick(0);
            for(int i=1;i<=fps;i++)clock.tick(java.lang.Math.round(i*1e9/fps));
            check(clock.positionMs()==1000,"preview time independent of FPS");
            clock.toggle(1_000_000_000L);clock.tick(4_000_000_000L);
            check(clock.positionMs()==1000&&!clock.playing(),"preview pause holds exact frame");
            clock.toggle(4_000_000_000L);clock.tick(4_500_000_000L);
            check(clock.positionMs()==1500,"preview resume excludes pause");
            clock.replay(5_000_000_000L);check(clock.positionMs()==0&&clock.playing(),"preview replay starts independently");
        }
        var notices=new LyricsNoticeGate();
        check(!notices.accept("A","loading",1000),"loading does not notify");
        check(notices.accept("A","unavailable",1000),"first outage notifies");
        check(!notices.accept("B","rate_limited",2000),"track changes do not spam provider failures");
        check(!notices.accept("C","unavailable",60000),"failure cooldown spans provider statuses");
        check(notices.accept("D","ready",3000),"successful recovery notifies immediately");
        check(notices.accept("B","rate_limited",61000),"new failure can notify after a minute");
        check(!notices.accept("B","rate_limited",130000),"unchanged status does not repeat indefinitely");
        notices.reset();check(notices.accept("B","unavailable",140000),"module re-enable resets notices");
        System.out.println("Lyrics customization: PASS "+checks+" checks");
    }
}
