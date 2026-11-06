package dev.bkclient.utility.sounds;

public final class LyricsWordRevealTest {
    private static int checks;
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    public static void main(String[] args) {
        String text = "Żółć,  słowo 👋🏽 koniec!";
        int[] ends = LyricsWordReveal.wordEnds(text);
        check(ends.length == 4, "unicode and punctuation remain whole words");
        check(LyricsWordReveal.visibleEnd(ends, 1000, 5000, 999) == 0, "before verse");
        check(text.substring(0, LyricsWordReveal.visibleEnd(ends,1000,5000,1000)).equals("Żółć,"), "first whole word at verse start");
        check(text.substring(0, LyricsWordReveal.visibleEnd(ends,1000,5000,1600)).equals("Żółć,  słowo"), "second word appends without losing punctuation");
        check(text.substring(0, LyricsWordReveal.visibleEnd(ends,1000,5000,2200)).endsWith("👋🏽"), "no split emoji");
        check(LyricsWordReveal.visibleEnd(ends,1000,5000,2800)==text.length(), "full verse completes in 45 percent of verse");
        check(LyricsWordReveal.visibleEnd(ends,1000,5000,1000)==ends[0], "seeking backwards reveals correct prefix");
        check(LyricsWordReveal.visibleEnd(new int[0],0,1000,500)==0,"blank line");
        check(LyricsWordReveal.visibleEnd(LyricsWordReveal.wordEnds("One"),0,1000,0)==3,"single word");
        var entry=LyricsWordReveal.frame(ends,1000,5000,1600);
        check(entry.progress()==0 && entry.settledEnd()==ends[0],"new word animates without moving earlier words");
        check(LyricsWordReveal.frame(ends,1000,5000,1760).progress()==1,"word settles in 160 ms");
        check(LyricsWordReveal.visibleEnd(LyricsWordReveal.wordEnds("One two three"),0,8000,1300)==13,"short verses do not wait seconds between words");
        for(int fps:new int[]{20,60,144}) {
            int previous=0;
            for(int i=0;i<fps*4;i++) {
                long time=1000+i*1000L/fps;
                int end=LyricsWordReveal.visibleEnd(ends,1000,5000,time);
                check(end>=previous,"monotonic at "+fps+" FPS");previous=end;
                check(end==ends[0]||end==ends[1]||end==ends[2]||end==ends[3],"never partial word");
            }
        }
        System.out.println("LyricsWordRevealTest PASS: "+checks+" checks");
    }
}
