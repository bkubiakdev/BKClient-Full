package dev.bkclient.ui.clicksys.port.gui.widget;
public final class UiContext {
 private static long frame; public static void update(){frame++;} public static long getTime(){return frame;}
}