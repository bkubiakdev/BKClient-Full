package dev.bkclient.utility.render.lyrics;

import java.util.ArrayList;
import java.util.List;

/** Immutable full-verse layout. Reveal never changes wrapping or glyph positions. */
public record LyricsLayout(List<Glyph> glyphs, float width, float height, int rows) {
    public interface Metrics {
        float advance(int codepoint);
        default float kerning(int previous, int codepoint) { return 0; }
        default float height() { return 10; }
    }
    public record Glyph(int codepoint, int start, int end, int word, float x, float y, float advance) { }
    public static LyricsLayout build(String text, Metrics metrics, float wrap, float tracking, float leading, int alignment) {
        List<Glyph> glyphs = new ArrayList<>();
        List<Float> widths = new ArrayList<>();
        float x = 0, contentWidth = 0, lineHeight = Math.max(1, metrics.height() + leading);
        int row = 0, word = -1, previous = -1;
        boolean inWord = false;
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i), end = i + Character.charCount(cp);
            if (cp == '\r') { i = end; continue; }
            if (cp == '\n') { widths.add(contentWidth); row++; x=contentWidth=0; previous=-1; inWord=false; i=end; continue; }
            boolean space = Character.isWhitespace(cp);
            if (!space && !inWord) {
                word++;
                float token = 0; int prev = -1;
                for (int j=i;j<text.length();) {
                    int c=text.codePointAt(j); if(Character.isWhitespace(c))break;
                    token += metrics.kerning(prev,c) + Math.max(.01f,metrics.advance(c)+tracking);
                    prev=c; j+=Character.charCount(c);
                }
                if (x > 0 && x + token - tracking > wrap) { widths.add(contentWidth); row++; x=contentWidth=0; previous=-1; }
            }
            float advance = Math.max(.01f, metrics.advance(cp) + tracking);
            float kern = metrics.kerning(previous,cp);
            if (!space && x > 0 && x + kern + advance - tracking > wrap) {
                widths.add(contentWidth); row++; x=contentWidth=0; previous=-1; kern=0;
            }
            if (!space) glyphs.add(new Glyph(cp,i,end,word,x+kern,row*lineHeight,advance));
            if (!space || x > 0) x += advance + kern;
            if (!space) contentWidth=Math.max(0,x-tracking);
            previous=cp; inWord=!space; i=end;
        }
        widths.add(contentWidth);
        float width = Math.max(1,widths.stream().max(Float::compare).orElse(1f));
        float height = Math.max(1,row*lineHeight+metrics.height());
        List<Glyph> aligned=new ArrayList<>(glyphs.size());
        for(Glyph g:glyphs) {
            int r=Math.round(g.y()/lineHeight);
            float shift=switch(alignment) { case 0 -> -width/2; case 2 -> width/2-widths.get(r); default -> -widths.get(r)/2; };
            aligned.add(new Glyph(g.codepoint,g.start,g.end,g.word,g.x+shift,g.y-height/2,g.advance));
        }
        return new LyricsLayout(List.copyOf(aligned),width,height,row+1);
    }
}
