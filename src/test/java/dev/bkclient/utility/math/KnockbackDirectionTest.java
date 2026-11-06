package dev.bkclient.utility.math;

public final class KnockbackDirectionTest {
    public static void main(String[] args) {
        double[][] positions = {{1,0},{-1,0},{0,1},{0,-1},{3,4},{-3,-4}};
        for (double[] p : positions) {
            double yaw = Math.toRadians(KnockbackDirection.towardAttacker(p[0], p[1], 0));
            double dot = -Math.sin(yaw)*p[0] + Math.cos(yaw)*p[1];
            if (Math.abs(dot-Math.hypot(p[0],p[1])) > 1E-5) throw new AssertionError("Knockback must point toward attacker");
        }
        if (KnockbackDirection.towardAttacker(0,0,42)!=42) throw new AssertionError("Overlapping positions");
        if (KnockbackDirection.wrap(270)!=-90 || KnockbackDirection.wrap(-270)!=90) throw new AssertionError("Angle wrapping");
        for (float reference : new float[]{179,-179,719,-721,3600,-3600}) {
            for (float desired : new float[]{-179,179,0,90,-90,180}) {
                float result = KnockbackDirection.nearest(reference, desired);
                if (Math.abs(result-reference)>180.001F) throw new AssertionError("Full-turn jump");
                if (Math.abs(KnockbackDirection.wrap(result-desired))>.001F) throw new AssertionError("Changed facing");
            }
        }
        if (KnockbackDirection.nearest(179,-179)!=181) throw new AssertionError("Positive boundary");
        if (KnockbackDirection.nearest(-179,179)!=-181) throw new AssertionError("Negative boundary");
        System.out.println("Knockback direction: cardinal, diagonal, overlap and wrap checks passed");
    }
}
