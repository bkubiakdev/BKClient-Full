package dev.bkclient.combatengine.bkengine;

public final class CriticalWindow {
    private CriticalWindow() {}
    public static boolean charged(float cooldown) { return cooldown > .9F; }
    /** Cobweb slowMovement resets local fallDistance each tick. */
    public static boolean webFalling(boolean grounded, double velocityY, boolean riding) {
        return !grounded && velocityY < 0 && !riding;
    }
    public static boolean falling(boolean grounded, float fallDistance, double velocityY, boolean riding) {
        return !grounded && fallDistance > 0 && velocityY < 0 && !riding;
    }
}
