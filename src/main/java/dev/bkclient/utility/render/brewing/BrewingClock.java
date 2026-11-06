package dev.bkclient.utility.render.brewing;

/** Server-sampled brewing countdown. Estimates never restart a brewing cycle. */
public final class BrewingClock {
    private int ticks;
    private long observed;
    public void observe(int remainingTicks,long clientTick) {
        ticks=Math.clamp(remainingTicks,0,400);
        observed=clientTick;
    }
    public double remaining(double clientTick,boolean live) {
        return Math.clamp(ticks-(live?0:Math.max(0,clientTick-observed)),0,400);
    }
    public int sampleTicks() { return ticks; }
    public long observedAt() { return observed; }
}
