package dev.bkclient.utility.render.lyrics;

import dev.bkclient.utility.sounds.LyricsTimeline;
import dev.bkclient.utility.sounds.LyricsWordReveal;
import java.util.*;
import org.joml.Quaternionf;
import org.joml.Vector3d;

/** Bounded sample verses owned by the editor; layout and effects match the world renderer. */
public final class LyricsPreviewScene implements AutoCloseable {
    private static final String[] TEXT={"Muzyka zostawia ślad", "Słowa płyną przez świat", "Każdy wers ma swoje miejsce", "Zażółć gęślą jaźń — Привет ЇЄІҐ"};
    public static final int VERSE_MS=4000;
    public final LyricsRenderer renderer=new LyricsRenderer();
    private final Map<Long,LyricsVerse> verses=new HashMap<>();
    public List<LyricsRenderer.Draw> frame(long positionMs,LyricsOptions o){
        if(renderer.prepare())verses.clear();
        positionMs=Math.max(0,positionMs);
        long index=positionMs/VERSE_MS;
        int count=(int)Math.min(index,o.historyCount.getCurrentValue());
        verses.keySet().removeIf(key->key<index-count||key>index);
        List<LyricsRenderer.Draw> result=new ArrayList<>(4);
        for(int rank=count;rank>=0;rank--){
            long number=index-rank,start=number*VERSE_MS;
            float age=positionMs-start-VERSE_MS,life=o.historyTime.getCurrentValue()*1000;
            if(rank>0&&age>=life)continue;
            LyricsVerse v=verses.computeIfAbsent(number,n->new LyricsVerse(
                new LyricsTimeline.Cue((int)(n%TEXT.length),TEXT[(int)(n%TEXT.length)],start,start+VERSE_MS),
                new LyricsPose(new Vector3d(),new Quaternionf()),0));
            v.ensureLayout(o,renderer.fonts.metrics(LyricsOptions.id(o.font)));
            if(rank==0){
                var word=LyricsWordReveal.frame(v.wordEnds,start,start+VERSE_MS,positionMs,o.revealPercent.getCurrentValue()/100.,o.wordGap.getCurrentValue(),o.wordDurationMs());
                boolean full=LyricsOptions.is(o.display,"full_verse");
                v.visibleEnd=full?Integer.MAX_VALUE:word.visibleEnd();v.settledEnd=full?Integer.MAX_VALUE:word.settledEnd();
                v.wordProgress=full?1:1-(float)Math.cbrt(1-word.progress());
                result.add(new LyricsRenderer.Draw(v,o.entryAt(positionMs-start,VERSE_MS*.45f),1,0));
            }else{
                v.visibleEnd=v.settledEnd=Integer.MAX_VALUE;v.wordProgress=1;
                float exitMs=Math.min(o.exitDurationMs(),life);
                result.add(new LyricsRenderer.Draw(v,o.exitAt(Math.max(0,age-(life-exitMs)),exitMs),o.historyAlpha.getCurrentValue()/100,rank*o.historyOffset.getCurrentValue()*40*LyricsAnimation.ease(age/400)));
            }
        }
        return result;
    }
    public void reset(){verses.clear();}
    @Override public void close(){verses.clear();renderer.close();}
}
