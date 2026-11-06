package dev.bkclient.utility.sounds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;

public final class TimedLyricsSearchTest {
    private static int checks;
    private static void check(boolean value, String name) { if (!value) throw new AssertionError(name); checks++; }
    private static final String LRC = "[00:01.00]Original test line\n[00:10.00]Another test line";
    private static String songs(long millis, String artist) {
        JsonObject song = new JsonObject();song.addProperty("name", "itepe itede");song.addProperty("duration",millis);song.addProperty("id",42);
        JsonObject a = new JsonObject(); a.addProperty("name",artist); JsonArray artists = new JsonArray();artists.add(a);song.add("artists",artists);
        JsonArray songs = new JsonArray();songs.add(song);JsonObject result = new JsonObject();result.add("songs",songs);
        JsonObject root = new JsonObject();root.add("result",result);return root.toString();
    }
    private static String lyric(String text) {JsonObject lrc=new JsonObject();lrc.addProperty("lyric",text);JsonObject root=new JsonObject();root.add("lrc",lrc);return root.toString();}
    private static TimedLyricsSearch.Transport fake(int lrCode,long duration,String artist,String lrc,AtomicInteger queries) {
        return (url,form)-> {
            queries.incrementAndGet();
            if(url.contains("lrclib"))return new TimedLyricsSearch.Reply(lrCode,"[]",60);
            if(url.contains("search"))return new TimedLyricsSearch.Reply(200,songs(duration,artist),0);
            return new TimedLyricsSearch.Reply(200,lyric(lrc),0);
        };
    }
    public static void main(String[] args) {
        String raw = "ENKLAWA [S02 / E02] - SANAH: ITEPE ITEDE";
        check(TimedLyricsSearch.titleMatches("itepe itede (ENKLAWA LIVE)","ITEPE ITEDE",raw),"live label matches media context");
        check(!TimedLyricsSearch.titleMatches("itepe itede (sped up)","ITEPE ITEDE",raw),"no wrong tempo variant");
        check(!TimedLyricsSearch.titleMatches("itepe itede (Other Live)","ITEPE ITEDE",raw),"no wrong concert");
        check(TimedLyricsSearch.titleMatches("BLUEBERRY","B L U E B E R R Y","GUZIOR - B L U E B E R R Y"),"YouTube decorative letter spacing matches catalog title");
        check(!TimedLyricsSearch.titleMatches("Blueberry","Blue Berry","Blue Berry"),"ordinary separate words are not merged");
        check(TimedLyricsSearch.compactSpacedLetters("B L U E B E R R Y (Live)").equals("BLUEBERRY (Live)"),"recording qualifiers retained");
        AtomicInteger requests = new AtomicInteger();
        var r = TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true,fake(429,193000,"sanah",LRC,requests));
        check(r.lyrics().isSynced() && r.approximate() && r.source().equals("NetEase"),"second provider succeeds despite 429");
        check(requests.get()==3,"429 does not trigger more queries to that provider");
        check(r.recordingSeconds()==193,"original recording duration retained");
        check(r.lyrics().getLines().get(1).timeMs()==10000,"no automatic clock stretching");
        r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,false,fake(404,193000,"sanah",LRC,new AtomicInteger()));
        check(r.lyrics().isEmpty(),"strict mode rejects approximate result");
        r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true,fake(404,201000,"sanah",LRC,new AtomicInteger()));
        check(!r.approximate() && r.status().equals("ready"),"matching duration");
        for (long seconds : new long[]{0,160,260}) {
            r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,seconds,true,fake(404,193000,"sanah",LRC,new AtomicInteger()));
            check(r.lyrics().isEmpty(),"unknown or excessive duration mismatch");
        }
        r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true,fake(404,201000,"cover artist",LRC,new AtomicInteger()));
        check(r.lyrics().isEmpty(),"reject cover artist");
        r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true,fake(404,201000,"sanah","untimed words",new AtomicInteger()));
        check(r.lyrics().isEmpty(),"plain text cannot impersonate timed lyrics");
        r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true,(url,form)->new TimedLyricsSearch.Reply(429,"",60));
        check(r.status().equals("rate_limited"),"rate limit diagnostic");
        AtomicInteger unavailableQueries = new AtomicInteger();
        r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true,(url,form)->{
            unavailableQueries.incrementAndGet();
            return new TimedLyricsSearch.Reply(url.contains("lrclib")?503:404,"",60);
        });
        check(r.status().equals("unavailable"),"503 is an outage, not a rate limit");
        check(unavailableQueries.get()==2,"do not retry another endpoint on the unavailable provider");
        r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true,fake(503,201000,"sanah",LRC,new AtomicInteger()));
        check(r.status().equals("ready")&&r.source().equals("NetEase"),"second provider still succeeds during LRCLIB outage");
        var backoff=new TimedLyricsSearch.ProviderBackoff();
        backoff.record("lrclib.net",503,60,1000);
        check(backoff.remaining("lrclib.net",2000).code()==503,"backoff preserves service unavailable");
        check(backoff.remaining("lrclib.net",2000).retrySeconds()==59,"remaining wait is accurate");
        check(backoff.remaining("music.163.com",2000)==null,"one provider cannot block the other");
        check(backoff.remaining("lrclib.net",61000)==null,"expired backoff allows retry");
        backoff.record("lrclib.net",429,120,1000);
        check(backoff.remaining("lrclib.net",2000).code()==429,"real rate limit stays rate limited");
        check(backoff.remaining("lrclib.net",121000)==null,"real rate limit expires");
        r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true,(url,form)->{throw new java.io.IOException("offline");});
        check(r.status().equals("unavailable"),"network diagnostic");
        r=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true,(url,form)->new TimedLyricsSearch.Reply(200,"invalid json",0));
        check(r.status().equals("unavailable"),"malformed response isolated");
        var timeline=SyncedLyrics.parseLrc(LRC);
        check(LyricsTimeline.at(timeline,18000,-8000,30000).index()==1,"offset beyond old five-second limit");
        System.out.println("TimedLyricsSearchTest PASS: "+checks+" checks");
        if (args.length > 0 && args[0].equals("live-blueberry")) {
            var live=LyricsFetcher.fetchLyricsResult("GUZIOR","GUZIOR - B L U E B E R R Y",182,true);
            System.out.println("BLUEBERRY live lookup: status="+live.status()+", source="+live.source()+", recording="+live.recordingSeconds()+"s, lines="+live.lyrics().getLines().size());
            check(live.lyrics().isSynced(),"reported YouTube title returns timed lyrics");
        }
        if (args.length > 0 && args[0].equals("live")) {
            var live=TimedLyricsSearch.search("SANAH","ITEPE ITEDE",raw,201,true);
            System.out.println("LIVE lookup: status="+live.status()+", source="+live.source()+", recording="+live.recordingSeconds()+"s, lines="+live.lyrics().getLines().size());
            check(live.lyrics().isSynced(),"real provider returns timed lyrics for reported song");
        }
    }
}
