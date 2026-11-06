package dev.bkclient.systems.modules.modules.combat.wtap;

/**
 * Pure W-Tap / S-Tap / Sprint Reset planning, kept free of Minecraft types for tests.
 * A sprint hit sets the server's sprint flag off; the client must send STOP then START
 * sprinting (at least one tick apart) so the next hit deals sprint knockback again.
 */
public final class TapRules {
    private TapRules() {
    }

    public enum Mode { SMART, W_TAP, S_TAP, SPRINT_RESET }

    public enum Action { NONE, RELEASE_W, PRESS_S, STOP_SPRINT }

    /**
     * SMART: no W held → only sprint can be reset; target already close → S-Tap keeps spacing;
     * otherwise W-Tap keeps chasing. The explicit modes fall back to a sprint reset when their key
     * makes no sense (W-Tap without W held).
     */
    public static Action choose(Mode mode, boolean forwardHeld, double distance, double closeDistance) {
        return switch (mode) {
            case SMART -> !forwardHeld ? Action.STOP_SPRINT
                    : distance <= closeDistance ? Action.PRESS_S : Action.RELEASE_W;
            case W_TAP -> forwardHeld ? Action.RELEASE_W : Action.STOP_SPRINT;
            case S_TAP -> Action.PRESS_S;
            case SPRINT_RESET -> Action.STOP_SPRINT;
        };
    }

    /** One tap at a time: hits during an active or queued tap do not extend or restart it. */
    public static final class Schedule {
        private Action action = Action.NONE;
        private long start;
        private long end;

        public boolean schedule(long nowTick, int delayTicks, int durationTicks, Action next) {
            if (next == Action.NONE || durationTicks <= 0 || nowTick < end) return false;
            action = next;
            start = nowTick + Math.max(0, delayTicks);
            end = start + durationTicks;
            return true;
        }

        public Action actionAt(long tick) {
            return tick >= start && tick < end ? action : Action.NONE;
        }

        /** True on the first tick after the tap, when sprint may be restored. */
        public boolean justEnded(long tick) {
            return action != Action.NONE && tick == end;
        }

        public void reset() {
            action = Action.NONE;
            start = 0;
            end = 0;
        }
    }
}
