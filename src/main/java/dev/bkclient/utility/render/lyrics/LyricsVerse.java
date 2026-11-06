package dev.bkclient.utility.render.lyrics;

import dev.bkclient.utility.sounds.LyricsTimeline;
import dev.bkclient.utility.sounds.LyricsWordReveal;

public final class LyricsVerse {
    public final LyricsTimeline.Cue cue;
    public final LyricsPose pose;
    public final long born;
    public final int[] wordEnds;
    public LyricsLayout layout;
    public String layoutKey;
    /** Set only in the Scattered display; null keeps the normal paragraph layout. */
    public LyricsScatter scatter;
    /** Per piece of the Scattered display: when it appeared (NaN = not yet), for its pop-in. */
    public double[] pieceShown;
    /** Scattered display: this verse is one piece of a line, placed on its own; -1 = no emphasised word. */
    public boolean piece;
    public int emphasisWord=-1;
    /** Piece words: song time (ms) each word appears; the piece leaves after {@code lineEndMs}. */
    public long[] wordRevealMs;
    public long lineEndMs;
    public float bobPhase;
    /** Piece placement as seen from the eye at spawn, so a Distance change moves it along the same view ray. */
    public org.joml.Vector3d anchorEye,anchorOffset;
    public float anchorDistance,anchorScale;
    public float scale, baseScale, entranceMs;
    public long retired;
    public float retiredAlpha=1;
    public int visibleEnd=Integer.MAX_VALUE,settledEnd=Integer.MAX_VALUE;
    public float wordProgress=1;
    public LyricsVerse(LyricsTimeline.Cue cue,LyricsPose pose,long born) {
        this.cue=cue;this.pose=pose;this.born=born;wordEnds=LyricsWordReveal.wordEnds(cue.text());
    }
    /** Scattered display: records when each piece first appears, for its pop-in. The whole verse stays on screen. */
    public void trackPieces(double seconds) {
        if(scatter==null)return;
        int n=scatter.chunks.length;
        if(pieceShown==null||pieceShown.length!=n){pieceShown=new double[n];java.util.Arrays.fill(pieceShown,Double.NaN);}
        for(int c=0;c<n;c++){
            if(scatter.words[scatter.chunks[c].firstWord()].firstEnd()>visibleEnd)break; // pieces reveal in order
            if(Double.isNaN(pieceShown[c]))pieceShown[c]=seconds;
        }
    }
    /** Rebuilds the layout (and the scattered placement) only when the layout-affecting settings change. */
    public void ensureLayout(LyricsOptions o,LyricsLayout.Metrics metrics) {
        String key=o.layoutKey();
        if(key.equals(layoutKey))return;
        boolean scattered=o.scattered();
        int align=switch(LyricsOptions.id(o.align)){case "left"->0;case "right"->2;default->1;};
        // Scattered pieces are placed individually, so the source layout stays on one row.
        layout=LyricsLayout.build(cue.text(),metrics,scattered?1e6f:o.wrap.getCurrentValue(),o.tracking.getCurrentValue(),o.leading.getCurrentValue(),scattered?1:align);
        if(piece){scatter=LyricsScatter.single(layout,o.emphasis.isEnabled()?emphasisWord:-1,o.emphasisSize.getCurrentValue()/100);pieceShown=null;layoutKey=key;return;}
        scatter=scattered?LyricsScatter.build(layout,cue.startMs()*1_315_423_911L+cue.text().hashCode(),
            o.scatterSpread.getCurrentValue()/100,o.scatterDepth.getCurrentValue(),o.scatterTilt.getCurrentValue(),
            o.scatterArc.getCurrentValue(),Math.round(o.scatterWords.getCurrentValue()),o.emphasis.isEnabled(),o.emphasisSize.getCurrentValue()/100):null;
        pieceShown=null;
        layoutKey=key;
    }
}
