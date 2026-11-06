package dev.bkclient.utility.render.lyrics;

/** Editor-only clock. Never reads or seeks the user's media session. */
public final class LyricsPreviewClock {
    private long lastNanos=-1,elapsedNanos;
    private boolean playing=true;
    public void tick(long now){if(lastNanos>=0&&playing)elapsedNanos+=Math.max(0,now-lastNanos);lastNanos=now;}
    public long positionMs(){return elapsedNanos/1_000_000;}
    public boolean playing(){return playing;}
    public void toggle(long now){tick(now);playing=!playing;}
    public void replay(long now){elapsedNanos=0;lastNanos=now;playing=true;}
}
