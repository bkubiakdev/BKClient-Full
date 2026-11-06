package dev.bkclient.utility.rotations;

public final class ContinuousYawTest {
    public static void main(String[] args) {
        check(ContinuousYaw.next(179, -179), 181);
        check(ContinuousYaw.next(-179, 179), -181);
        check(ContinuousYaw.next(719, 1), 721);
        check(ContinuousYaw.next(181, 179), 179);
        check(ContinuousYaw.next(-721, 1), -719);
        for (int direction : new int[]{-1,1}) {
            float previous = 0;
            for (int i = 1; i <= 4000; i++) {
                float desired = direction * i * 1.5F;
                float normalized = (desired + 180) % 360;
                if (normalized < 0) normalized += 360;
                normalized -= 180;
                float next = ContinuousYaw.next(previous, normalized);
                check(next, desired);
                if (Math.abs(next - previous) > 1.501F) throw new AssertionError("Seam jump");
                previous = next;
            }
        }
        System.out.println("Continuous yaw: both seams, multiple turns and return to camera PASS");
    }
    private static void check(float actual, float expected) {
        if (Math.abs(actual - expected) > .001F) throw new AssertionError(actual + " != " + expected);
    }
}
