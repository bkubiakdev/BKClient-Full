package dev.bkclient.utility.animation;

import org.joml.Matrix4f;

public final class InspectAnimationTest {
    private static int checks;
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }
    private static Matrix4f matrix(InspectAnimation.Pose p) {
        float r = (float)Math.PI / 180;
        return new Matrix4f().translate(p.x(), p.y(), p.z()).rotateY(p.yaw()*r).rotateX(p.pitch()*r).rotateZ(p.roll()*r);
    }
    private static void close(InspectAnimation.Pose a, InspectAnimation.Pose b, float tolerance, String label) {
        check(matrix(a).equals(matrix(b), tolerance), label);
    }
    public static void main(String[] args) {
        var styles = InspectAnimation.Style.values();
        check(styles.length == 12, "twelve available animations");
        for (int a=0; a<styles.length; a++) for (int b=a+1; b<styles.length; b++) {
            boolean different = false;
            for (double progress : new double[]{.31, .43, .57, .69})
                different |= !matrix(InspectAnimation.sample(styles[a], progress, 1))
                    .equals(matrix(InspectAnimation.sample(styles[b], progress, 1)), .01f);
            check(different, "distinct choreography: " + styles[a] + " / " + styles[b]);
        }
        for (var style : InspectAnimation.Style.values()) {
            for (double seconds : new double[]{.6, 2.8, 8}) {
                for (int fps : new int[]{20, 60, 144}) {
                    var timeline = new InspectAnimation();
                    timeline.start(1_000_000_000, style, seconds, 1);
                    for (int frame=0; frame<fps*seconds; frame++) {
                        long time = 1_000_000_000 + Math.round(frame * 1e9 / fps);
                        timeline.advance(time, false);
                        close(timeline.pose(false), InspectAnimation.sample(style, (time-1e9)/1e9/seconds, 1), 1e-4f,
                            "frame-rate independent " + style + " " + fps);
                    }
                    timeline.advance(1_000_000_000 + Math.round(seconds*1e9), false);
                    check(!timeline.isActive(), "ends on time");
                    close(timeline.pose(false), InspectAnimation.Pose.IDENTITY, 1e-5f, "identity after finish");
                }
            }
            var previous = InspectAnimation.sample(style, 0, 1);
            for (int i=1; i<=10000; i++) {
                var pose = InspectAnimation.sample(style, i/10000.0, 1);
                check(matrix(pose).isFinite(), "finite pose");
                close(previous, pose, .006f, "no jump, including wrap to neutral");
                close(pose, pose.mirrored().mirrored(), 1e-6f, "mirror symmetry");
                previous = pose;
            }
            var timeline = new InspectAnimation();
            timeline.start(0, style, 4, 1);
            timeline.advance(2_000_000_000, false);
            var before = timeline.pose(false);
            timeline.advance(9_000_000_000L, true);
            close(before, timeline.pose(false), 1e-6f, "pause freezes progress");
            timeline.finish(9_000_000_000L);
            close(before, timeline.pose(false), 1e-5f, "second press continuous");
            timeline.advance(9_220_000_001L, false);
            check(!timeline.isActive(), "bounded return");
            timeline.start(0, style, Double.NaN, Float.NaN);
            timeline.advance(1_000_000_000, false);
            check(matrix(timeline.pose(false)).isFinite(), "invalid config sanitized");
            timeline.reset();
            check(!timeline.isActive(), "world/disable reset");
        }
        System.out.println("InspectAnimationTest PASS " + checks);
    }
}
