package dev.bkclient.systems.modules.modules.other.tinytask;

public class TinyTaskEvent {
    public Type type;
    public long tickIndex;
    public String text;
    public String screenClass;
    public double mouseX;
    public double mouseY;
    public int button;

    public enum Type {
        COMMAND,
        CHAT,
        GUI_CLICK
    }
}
