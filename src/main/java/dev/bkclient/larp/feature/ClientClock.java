package dev.bkclient.larp.feature;

public final class ClientClock {
    private static long ticks;

    private ClientClock() {
    }

    public static void tick() {
        ++ticks;
    }

    static long ticks() {
        return ticks;
    }
}
