package dev.bkclient.systems.modules.modules.combat.backtrack;

/** Distance starts on selection; Hit stays armed until the selected target is reset. */
public final class BacktrackTrigger {
    private boolean hitMode;
    private boolean armed;
    public boolean setHitMode(boolean value) {
        if (hitMode == value) return false;
        hitMode = value;
        armed = false;
        return true;
    }
    public void attack() { if (hitMode) armed = true; }
    public boolean canHold(boolean validTarget) { return validTarget && (!hitMode || armed); }
    public void reset() { armed = false; }

    public static boolean inRange(double distance, double range) {
        return Double.isFinite(distance) && Double.isFinite(range)
                && distance >= 0 && range >= 0 && distance <= range;
    }
}
