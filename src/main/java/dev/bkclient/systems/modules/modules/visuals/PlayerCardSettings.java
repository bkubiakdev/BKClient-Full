package dev.bkclient.systems.modules.modules.visuals;

/** Local placement state for the legacy Themes preview. */
public final class PlayerCardSettings {

    public static volatile boolean cornersMode = false;

    /** 0=top 1=bottom 2=left 3=right */
    public static volatile int gearSide = 1;
    public static volatile float gearAlong = 0.5f;

    /** 0=top 1=bottom 2=left 3=right */
    public static volatile int nameSide = 0;
    public static volatile float nameAlong = 0.5f;

    public static volatile boolean showName = true;
    public static volatile boolean showPing = true;
    public static volatile boolean showHealth = false;

    private PlayerCardSettings() {}
}
