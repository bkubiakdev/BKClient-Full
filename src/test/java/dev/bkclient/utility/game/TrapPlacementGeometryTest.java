package dev.bkclient.utility.game;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public final class TrapPlacementGeometryTest {
    public static void main(String[] args) {
        check(new Box(.2, 64, .2, .8, 65.8, .8), List.of(new BlockPos(0,64,0)));
        check(new Box(-.8,-60,-.8,-.2,-58.2,-.2), List.of(new BlockPos(-1,-60,-1)));
        check(new Box(0,64,0,1,65.8,1), List.of(new BlockPos(0,64,0)));
        check(new Box(-.3,64,-.3,.3,65.8,.3), List.of(
            new BlockPos(-1,64,-1),new BlockPos(-1,64,0),new BlockPos(0,64,-1),new BlockPos(0,64,0)));
        check(new Box(.2,64,.2,.8,64.6,.8), List.of(new BlockPos(0,64,0)));
        Box slab = new Box(-3, 64, -5, -2, 64.5, -4);
        var eye = new net.minecraft.util.math.Vec3d(.5,65.6,.5);
        var near = new BlockPos(2,65,0);
        var far = new BlockPos(4,65,0);
        var roof = new BlockPos(3,66,0);
        var unfinished = List.of(near,far,roof);
        if (TrapPlacementGeometry.obstructionCost(near,unfinished,eye) <= TrapPlacementGeometry.obstructionCost(far,unfinished,eye))
            throw new AssertionError("Near wall must wait until the far side remains accessible");
        for (var face : net.minecraft.util.math.Direction.values()) {
            var points = TrapPlacementGeometry.faceSamples(slab, face);
            if (points.size() != 9 || points.stream().distinct().count() != 9) throw new AssertionError("Missing face samples");
            for (var p : points) {
                if (!slab.contains(p)) throw new AssertionError("Sample outside support: " + p);
                double distance = switch(face) {
                    case UP -> slab.maxY - p.y; case DOWN -> p.y - slab.minY;
                    case EAST -> slab.maxX - p.x; case WEST -> p.x - slab.minX;
                    case SOUTH -> slab.maxZ - p.z; case NORTH -> p.z - slab.minZ;
                };
                if (Math.abs(distance - .001) > 1e-9) throw new AssertionError("Wrong face: " + face);
            }
        }
        System.out.println("Trap face samples: all six directions and half-height support PASS");
        System.out.println("Trap footprint: standing, negative coordinates, exact boundaries, straddling cells and low pose PASS");
    }
    private static void check(Box bounds, List<BlockPos> expected) {
        var actual = TrapPlacementGeometry.footprint(bounds);
        if (!actual.equals(expected)) throw new AssertionError(bounds + ": " + actual + " expected " + expected);
    }
}
