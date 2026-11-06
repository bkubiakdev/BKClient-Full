package dev.bkclient.utility.render.ambience;

/**
 * Real world time remembered while Ambience overrides the time of day.
 *
 * <p>Built from the last server time packet (or from the world when the override starts), so
 * disabling the override restores the server's clock instead of a stale or unrelated value.
 */
public record AmbienceTime(long age, long timeOfDay, boolean ticking) {
    /** Time of day the server's clock has reached by {@code currentAge}. */
    public long restoredTimeOfDay(long currentAge) {
        return ticking ? timeOfDay + Math.max(0L, currentAge - age) : timeOfDay;
    }
}
