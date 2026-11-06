package dev.bkclient.utility.render;

public final class HotbarSelectionAnimationTest {
    public static void main(String[] args) {
        float expected = -1;
        for (int frames : new int[]{6, 12, 29, 200}) {
            HotbarSelectionAnimation animation = new HotbarSelectionAnimation();
            animation.update(0, 0);
            float previous = 0;
            for (int frame = 1; frame <= frames; frame++) {
                animation.update(8, 200_000_000L * frame / frames);
                if (animation.weight(8) <= previous) throw new AssertionError("Motion stalled between render frames");
                previous = animation.weight(8);
                checkWeights(animation);
            }
            if (expected >= 0 && Math.abs(animation.weight(8) - expected) > 0.00001F)
                throw new AssertionError("Motion depends on frame rate");
            if (animation.weight(8) < .96F) throw new AssertionError("Selection remains sluggish after 200 ms");
            expected = animation.weight(8);
        }

        HotbarSelectionAnimation animation = new HotbarSelectionAnimation();
        animation.update(0, 0);
        for (int frame = 1; frame <= 200; frame++) {
            animation.update(frame % 9, frame * 1_000_000L);
            checkWeights(animation);
        }
        float before = animation.weight(2);
        animation.update(2, 200_000_000L);
        if (animation.weight(2) != before) throw new AssertionError("Same-frame retarget caused a jump");
        animation.reset();
        animation.update(5, 500_000_000L);
        if (animation.weight(5) != 1 || animation.weight(2) != 0)
            throw new AssertionError("Reset kept stale selection");
        System.out.println("Hotbar animation: frame-rate independence, smooth retargeting, bounds and reset passed");
    }

    private static void checkWeights(HotbarSelectionAnimation animation) {
        float total = 0;
        for (int slot = 0; slot < 9; slot++) {
            float weight = animation.weight(slot);
            if (!Float.isFinite(weight) || weight < 0 || weight > 1)
                throw new AssertionError("Overshoot in slot " + slot);
            if (animation.neighborWeight(slot) > 1.00001F)
                throw new AssertionError("Neighbor expansion overflow");
            total += weight;
        }
        if (Math.abs(total - 1) > 0.00001F) throw new AssertionError("Selection weights drifted");
    }
}
