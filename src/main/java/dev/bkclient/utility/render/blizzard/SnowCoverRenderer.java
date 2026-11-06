package dev.bkclient.utility.render.blizzard;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.utility.render.VisualRenderState;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;

/** Cosmetic, sky-exposed snow cover. The owner supplies accumulation and melting progress. */
public final class SnowCoverRenderer {
    private static final ShaderProgramKey KEY = new ShaderProgramKey(
        Identifier.of("bkclient", "core/blizzard_cover"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private static final long SCAN_INTERVAL = 700_000_000L;
    private static final int SCAN_BUDGET = 768, MAX_RADIUS = 64, SHAPE_CACHE_LIMIT = 1024;
    private static final float SURFACE_LIFT = .004f;
    private final BlockPos.Mutable scanPos = new BlockPos.Mutable();
    // Cache the actual shape, not block state: outline shapes can depend on position/neighbours.
    private final IdentityHashMap<VoxelShape, Float> shapeTops = new IdentityHashMap<>();
    private final Vector3f forward = new Vector3f();
    private ClientWorld lastWorld;
    private Cell[] cells = new Cell[0];
    private int[][] offsets = new int[0][];
    private int radius = -1, span;
    private long retryAt;
    private double anchorX = Double.NaN, anchorZ = Double.NaN;

    private static final class Cell {
        int x, z, blockY;
        long scannedAt;
        float surface = Float.NaN, light, drift;
        BlockState state;
    }

    public void reset() {
        lastWorld = null;
        cells = new Cell[0];
        offsets = new int[0][];
        shapeTops.clear();
        radius = -1;
        span = 0;
        retryAt = 0;
        anchorX = anchorZ = Double.NaN;
    }

    /** Matrices consume camera-relative world positions, as with BKClient's world effects. */
    public void render(Matrix4f view, Matrix4f projection, Camera camera, float cover,
                       int requestedRadius, float maxDepth, int tint, Fog fog) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world != lastWorld) { reset(); lastWorld = world; }
        if (world == null || camera == null || !camera.isReady() || !Float.isFinite(cover)
                || cover <= .001f || requestedRadius < 1) return;
        cover = MathHelper.clamp(cover, 0, 1);
        maxDepth = Float.isFinite(maxDepth) ? MathHelper.clamp(maxDepth, 0, 2) : 0;
        int wantedRadius = MathHelper.clamp(requestedRadius, 1, MAX_RADIUS);
        if (wantedRadius != radius) allocate(wantedRadius);
        Vec3d cameraPos = camera.getPos();
        long now = System.nanoTime();
        update(world, cameraPos, now);
        if (now < retryAt) return;

        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int oldVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try (var state = new VisualRenderState()) {
            ShaderProgram shader;
            try { shader = RenderSystem.setShader(KEY); }
            catch (Exception error) {
                retryAt = now + 5_000_000_000L;
                BKClientInit.LOGGER.warn("Unable to load Blizzard snow cover shader", error);
                return;
            }
            if (shader == null) return;
            anchorX = anchor(anchorX, cameraPos.x);
            anchorZ = anchor(anchorZ, cameraPos.z);
            Fog appliedFog = fog == null ? Fog.DUMMY : fog;
            shader.getUniformOrDefault("WorldView").set(view);
            shader.getUniformOrDefault("WorldProjection").set(projection);
            shader.getUniformOrDefault("CameraOrigin").set((float)(cameraPos.x - anchorX),
                (float)cameraPos.y, (float)(cameraPos.z - anchorZ));
            shader.getUniformOrDefault("TintColor").set((tint >> 16 & 255) / 255f,
                (tint >> 8 & 255) / 255f, (tint & 255) / 255f);
            shader.getUniformOrDefault("CoverProgress").set(cover);
            shader.getUniformOrDefault("CoverFogColor").set(appliedFog.red(), appliedFog.green(), appliedFog.blue(), appliedFog.alpha());
            shader.getUniformOrDefault("CoverFogRange").set(appliedFog.start(), Math.max(appliedFog.start() + .001f, appliedFog.end()));
            shader.getUniformOrDefault("CoverFogShape").set(appliedFog.shape() == FogShape.CYLINDER ? 1f : 0f);

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            // The fragment shader stipples the outer edge, so discarded pixels keep terrain depth.
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            float buildup = MathHelper.clamp((cover - .55f) / .45f, 0, 1);
            buildup = maxDepth * buildup * buildup * (3 - 2 * buildup);
            forward.set(0, 0, -1).rotate(camera.getRotation());
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            emit(buffer, world, cameraPos, buildup);
            BuiltBuffer built = buffer.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        } finally {
            RenderSystem.depthFunc(oldDepthFunc);
            BufferRenderer.resetCurrentVertexBuffer();
            GL30.glBindVertexArray(oldVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
        }
    }

    private void allocate(int wantedRadius) {
        radius = wantedRadius;
        span = radius * 2 + 1;
        cells = new Cell[span * span];
        for (int i = 0; i < cells.length; i++) cells[i] = new Cell();
        ArrayList<int[]> positions = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) positions.add(new int[]{x, z});
            }
        }
        // A new world/teleport fills the nearest ground first, with a fixed per-frame scan budget.
        positions.sort(Comparator.comparingInt(p -> p[0] * p[0] + p[1] * p[1]));
        offsets = positions.toArray(int[][]::new);
    }

    private Cell cell(int x, int z) {
        return cells[Math.floorMod(x, span) * span + Math.floorMod(z, span)];
    }

    private void update(ClientWorld world, Vec3d camera, long now) {
        int centerX = MathHelper.floor(camera.x), centerZ = MathHelper.floor(camera.z), scanned = 0;
        for (int[] offset : offsets) {
            int x = centerX + offset[0], z = centerZ + offset[1];
            Cell cell = cell(x, z);
            boolean changed = cell.scannedAt == 0 || cell.x != x || cell.z != z;
            if (!changed && now - cell.scannedAt < SCAN_INTERVAL) continue;
            if (scanned++ >= SCAN_BUDGET) break;
            cell.x = x;
            cell.z = z;
            cell.scannedAt = now;
            cell.surface = Float.NaN;
            cell.state = null;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
            if (top <= world.getBottomY()) continue;
            scanPos.set(x, top - 1, z);
            BlockState block = world.getBlockState(scanPos);
            float height = supportTop(world, block);
            if (Float.isNaN(height)) continue;
            // Heightmap chooses the roof, never a floor beneath it. Sky visibility also rejects
            // dimension ceilings and opaque obstructions that do not enter this heightmap.
            scanPos.set(x, top, z);
            if (!world.isSkyVisible(scanPos)) continue;
            cell.blockY = top - 1;
            cell.surface = cell.blockY + height;
            cell.state = block;
            float light = world.getLightLevel(scanPos, world.getAmbientDarkness()) / 15f;
            cell.light = .16f + .84f * (float)Math.pow(MathHelper.clamp(light, 0, 1), 1.4);
            cell.drift = .6f + .8f * smoothNoise((x + .5) * .17, (z + .5) * .17);
        }
    }

    private float supportTop(ClientWorld world, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getRenderType() == BlockRenderType.INVISIBLE) return Float.NaN;
        VoxelShape shape = state.getOutlineShape(world, scanPos);
        if (shape.isEmpty()) return Float.NaN;
        Float known = shapeTops.get(shape);
        if (known != null) return known;
        double top = shape.getMax(Direction.Axis.Y);
        float result = Float.NaN;
        if (top > 0 && top <= 1) {
            VoxelShape slice = VoxelShapes.cuboid(0, Math.max(0, top - .001), 0, 1, top, 1);
            if (!VoxelShapes.matchesAnywhere(slice, shape, BooleanBiFunction.ONLY_FIRST)) result = (float)top;
        }
        if (shapeTops.size() >= SHAPE_CACHE_LIMIT) shapeTops.clear();
        shapeTops.put(shape, result);
        return result;
    }

    private void emit(BufferBuilder buffer, ClientWorld world, Vec3d camera, float buildup) {
        int centerX = MathHelper.floor(camera.x), centerZ = MathHelper.floor(camera.z);
        float fadeStart = radius * .7f, fadeWidth = Math.max(1, radius - fadeStart);
        for (int[] offset : offsets) {
            int x = centerX + offset[0], z = centerZ + offset[1];
            Cell cell = cell(x, z);
            if (cell.x != x || cell.z != z || Float.isNaN(cell.surface)) continue;
            float left = (float)(x - camera.x), north = (float)(z - camera.z);
            float dx = left + .5f, dz = north + .5f;
            float alpha = 1 - MathHelper.clamp(((float)Math.sqrt(dx * dx + dz * dz) - fadeStart) / fadeWidth, 0, 1);
            if (alpha <= .001f) continue;
            float bottom = (float)(cell.surface + SURFACE_LIFT - camera.y);
            float top = bottom + layerDepth(buildup, cell.drift);
            float distance = (float)Math.sqrt(dx * dx + top * top + dz * dz);
            if (distance > 3 && (dx * forward.x + top * forward.y + dz * forward.z) / distance < -.35f) continue;
            // Cheap current-height/state checks avoid drawing cached snow after chunk unloading,
            // roof placement, water flooding or terrain edits while the bounded rescan catches up.
            if (!world.isChunkLoaded(x >> 4, z >> 4)
                    || world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) != cell.blockY + 1) continue;
            scanPos.set(x, cell.blockY, z);
            if (world.getBlockState(scanPos) != cell.state) continue;
            int color = channel(alpha) << 24 | channel(cell.light) << 16;
            float east = left + 1, south = north + 1;
            vertex(buffer, left, top, north, color);
            vertex(buffer, left, top, south, color);
            vertex(buffer, east, top, south, color);
            vertex(buffer, east, top, north, color);
            side(buffer, neighbourTop(x - 1, z, camera.y, buildup), bottom, top, left, north, left, south, color);
            side(buffer, neighbourTop(x + 1, z, camera.y, buildup), bottom, top, east, south, east, north, color);
            side(buffer, neighbourTop(x, z - 1, camera.y, buildup), bottom, top, east, north, left, north, color);
            side(buffer, neighbourTop(x, z + 1, camera.y, buildup), bottom, top, left, south, east, south, color);
        }
    }

    private float neighbourTop(int x, int z, double cameraY, float buildup) {
        Cell neighbour = cell(x, z);
        return neighbour.x == x && neighbour.z == z && !Float.isNaN(neighbour.surface)
            ? (float)(neighbour.surface + SURFACE_LIFT - cameraY) + layerDepth(buildup, neighbour.drift) : Float.NaN;
    }

    private static void side(BufferBuilder buffer, float neighbourTop, float bottom, float top,
                             float x1, float z1, float x2, float z2, int color) {
        float low = Float.isNaN(neighbourTop) ? bottom : Math.max(bottom, neighbourTop);
        if (top <= low + .0001f) return;
        vertex(buffer, x1, low, z1, color);
        vertex(buffer, x1, top, z1, color);
        vertex(buffer, x2, top, z2, color);
        vertex(buffer, x2, low, z2, color);
    }

    private static void vertex(BufferBuilder buffer, float x, float y, float z, int color) {
        buffer.vertex(x, y, z).color(color);
    }

    private static float layerDepth(float buildup, float drift) { return Math.round(buildup * drift * 16) / 16f; }
    private static int channel(float value) { return MathHelper.clamp(Math.round(value * 255), 0, 255); }
    private static double anchor(double previous, double position) {
        return Double.isNaN(previous) || Math.abs(position - previous) >= 2048 ? Math.floor(position / 1024) * 1024 : previous;
    }

    private static float smoothNoise(double x, double z) {
        int ix = MathHelper.floor(x), iz = MathHelper.floor(z);
        float fx = (float)(x - ix), fz = (float)(z - iz);
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        return MathHelper.lerp(fz, MathHelper.lerp(fx, lattice(ix, iz), lattice(ix + 1, iz)),
            MathHelper.lerp(fx, lattice(ix, iz + 1), lattice(ix + 1, iz + 1)));
    }

    private static float lattice(int x, int z) {
        int hash = x * 668265261 ^ z * 374761393;
        hash ^= hash >>> 15;
        hash *= -2048144789;
        hash ^= hash >>> 13;
        return (hash & 0xffffff) / 16777215f;
    }
}
