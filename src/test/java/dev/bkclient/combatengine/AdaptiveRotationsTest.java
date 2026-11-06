package dev.bkclient.combatengine;

import dev.bkclient.combatengine.client.modules.impl.combat.adaptive.AdaptiveRotations;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;

public final class AdaptiveRotationsTest {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) {
        AdaptiveRotations r = new AdaptiveRotations();
        Rotation equal = r.step("Default", new Rotation(10, 20), new Rotation(10, 20), false, false, false, 2, 0, 900, 3);
        check(equal.getYaw() == 10 && equal.getPitch() == 20, "Default zero error");

        for (int i = 0; i < 500; i++) {
            Rotation out = r.step("Default", new Rotation(i * 7 - 150, i % 170 - 85), new Rotation(-i * 11, 85 - i % 170),
                i % 2 == 0, i % 3 == 0, i % 4 == 0, i, i, i % 1200, 3);
            check(Float.isFinite(out.getYaw()) && Float.isFinite(out.getPitch()), "Default finite angles");
            check(Math.abs(out.getPitch()) <= 89, "Default pitch limits");
        }
        System.out.println("AdaptiveRotations test passed");
    }
}
