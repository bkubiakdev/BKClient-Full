package dev.bkclient.utility.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Horizontal occupied cells, independent of the target's pose/height. */
public final class TrapPlacementGeometry {
    private TrapPlacementGeometry() {}

    /** Count unfinished cells whose direct approach would be blocked by this cube. */
    public static int obstructionCost(BlockPos candidate, List<BlockPos> unfinished, Vec3d eye) {
        Box obstacle = new Box(candidate).contract(.001);
        int count = 0;
        for (BlockPos other : unfinished) {
            if (!candidate.equals(other) && obstacle.raycast(eye, Vec3d.ofCenter(other)).isPresent()) count++;
        }
        return count;
    }

    public static List<Vec3d> faceSamples(Box shape, Direction face) {
        List<Vec3d> samples = new ArrayList<>(9);
        double[] fractions = {.15, .5, .85};
        double normal = switch (face) {
            case WEST -> shape.minX + .001;
            case EAST -> shape.maxX - .001;
            case DOWN -> shape.minY + .001;
            case UP -> shape.maxY - .001;
            case NORTH -> shape.minZ + .001;
            case SOUTH -> shape.maxZ - .001;
        };
        for (double a : fractions) for (double b : fractions) {
            samples.add(switch (face.getAxis()) {
                case X -> new Vec3d(normal, MathHelper.lerp(a, shape.minY, shape.maxY), MathHelper.lerp(b, shape.minZ, shape.maxZ));
                case Y -> new Vec3d(MathHelper.lerp(a, shape.minX, shape.maxX), normal, MathHelper.lerp(b, shape.minZ, shape.maxZ));
                case Z -> new Vec3d(MathHelper.lerp(a, shape.minX, shape.maxX), MathHelper.lerp(b, shape.minY, shape.maxY), normal);
            });
        }
        return samples;
    }

    public static List<BlockPos> footprint(Box bounds) {
        List<BlockPos> cells = new ArrayList<>();
        int y = MathHelper.floor(bounds.minY + 1.0e-7);
        for (int x = MathHelper.floor(bounds.minX + 1.0e-7); x <= MathHelper.floor(bounds.maxX - 1.0e-7); x++) {
            for (int z = MathHelper.floor(bounds.minZ + 1.0e-7); z <= MathHelper.floor(bounds.maxZ - 1.0e-7); z++) {
                cells.add(new BlockPos(x, y, z));
            }
        }
        return cells;
    }
}
