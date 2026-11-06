package dev.bkclient.systems.modules.modules.visuals.particles;

import dev.bkclient.BKClientInit;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Random;

/** The shapes a spark can be drawn as, in the order the module's shape setting declares them. */
public final class ParticleShapes {

    public static final Identifier[] TEXTURES = {
            texture("point"),
            texture("star"),
            texture("lighting"),
            texture("cross"),
            texture("crown"),
            texture("heart"),
            texture("line"),
            texture("rhombus"),
            texture("dollar"),
            texture("snowflake"),
            texture("triangle")
    };

    private ParticleShapes() {
    }

    private static Identifier texture(String name) {
        return BKClientInit.id("textures/features/hitparticles/" + name + ".png");
    }

    /**
     * Picks one of the shapes the player left selected.
     *
     * <p>Falls back to the whole set rather than drawing nothing, because a burst that silently
     * does not appear reads as the module being broken.
     */
    public static Identifier pick(List<Integer> selectedIndices, Random random) {
        if (selectedIndices.isEmpty()) {
            return TEXTURES[random.nextInt(TEXTURES.length)];
        }
        int index = selectedIndices.get(random.nextInt(selectedIndices.size()));
        return TEXTURES[Math.floorMod(index, TEXTURES.length)];
    }
}
