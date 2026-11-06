package dev.bkclient.larp.gui;

public final class Status {
    private static String message = "";
    private static boolean error;
    private static long expiresAt;

    private Status() {
    }

    public static void info(String text) {
        Status.set(text, false);
    }

    public static void error(String text) {
        Status.set(text, true);
    }

    private static void set(String text, boolean isError) {
        message = text;
        error = isError;
        expiresAt = System.currentTimeMillis() + 4000L;
    }

    public static String current() {
        return System.currentTimeMillis() < expiresAt ? message : null;
    }

    public static int color() {
        return error ? -38037 : -11760129;
    }
}
