package dev.bkclient.utility.render.lyrics;

/** Keep provider outages from filling chat when tracks or media sessions change. */
public final class LyricsNoticeGate {
    private String lastNotice = "";
    private long nextFailureNotice;

    public boolean accept(String track, String status, long nowMs) {
        if (status.equals("idle") || status.equals("loading")) return false;
        String notice = track + ":" + status;
        boolean failure = status.equals("unavailable") || status.equals("rate_limited");
        if (notice.equals(lastNotice) || failure && nowMs < nextFailureNotice) return false;
        lastNotice = notice;
        if (failure) nextFailureNotice = nowMs + 60_000;
        return true;
    }

    public void reset() { lastNotice = ""; nextFailureNotice = 0; }
}
