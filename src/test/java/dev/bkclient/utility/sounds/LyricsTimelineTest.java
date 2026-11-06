package dev.bkclient.utility.sounds;

public final class LyricsTimelineTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    private static LyricsTimeline.Cue at(SyncedLyrics lyrics, long time) {
        return LyricsTimeline.at(lyrics, time, 0, 60000);
    }
    public static void main(String[] args) {
        var lyrics = SyncedLyrics.parseLrc("[00:01.00]Pierwszy wers\n[00:03.50]Żółć i świat\n[00:05.00]\n[00:12.00]Ostatni wers");
        check(lyrics.isSynced(), "LRC recognized");
        check(at(lyrics, 999) == null, "no early verse");
        check(at(lyrics, 1000).text().equals("Pierwszy wers"), "exact start");
        check(at(lyrics, 3499).index() == 0, "before boundary");
        check(at(lyrics, 3500).text().equals("Żółć i świat"), "Polish and next boundary");
        check(at(lyrics, 5000) == null && at(lyrics, 11999) == null, "instrumental marker clears text");
        check(at(lyrics, 12000).index() == 3, "after instrumental");
        check(at(lyrics, 20000) == null, "last verse expires");
        check(at(lyrics, 1500).index() == 0, "seek backwards");
        check(LyricsTimeline.at(lyrics, 500, 500, 60000).index() == 0, "positive offset advances");
        check(LyricsTimeline.at(lyrics, 1000, -500, 60000) == null, "negative offset delays");
        check(LyricsTimeline.at(lyrics, 1500, 0, 1500) == null, "track ends");
        check(at(SyncedLyrics.EMPTY, 2000) == null, "empty");
        check(at(SyncedLyrics.parsePlain("plain text\nnext line"), 2000) == null, "never invent sync");
        var repeated = SyncedLyrics.parseLrc("[00:02.5][00:08.25]Refren\n[00:05.125]Wers");
        check(repeated.getLines().size() == 3, "multiple timestamps");
        check(at(repeated, 2500).text().equals("Refren"), "one decimal digit");
        check(at(repeated, 5125).text().equals("Wers"), "three decimal digits");
        check(at(repeated, 8250).text().equals("Refren"), "two decimal digits and sorted timestamps");
        var shifted = SyncedLyrics.parseLrc("[offset:500]\n[00:01]Test");
        check(at(shifted, 1499) == null && at(shifted, 1500) != null, "LRC metadata offset");
        check(at(SyncedLyrics.parseLrc("[00:99.00]Invalid"), 0) == null, "invalid seconds are not timed lyrics");
        var formatted = SyncedLyrics.parseLrc("[00:00]§kExample\ttext");
        check(at(formatted, 1000).text().equals("Example text"), "external formatting stripped");
        check(at(SyncedLyrics.parseLrc("[00:00]" + "a".repeat(10000)), 0).text().length() <= 241, "bounded layout input");
        try { lyrics.getLines().clear(); throw new AssertionError("mutable lyrics"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("LyricsTimelineTest PASS: " + checks + " checks");
    }
}
