package dev.bkclient.utility.render.blizzard;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.utility.render.VisualRenderState;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.concurrent.ThreadLocalRandom;

/** Wind-driven, depth-tested snow. The particles and their terrain cache are strictly bounded. */
public final class SnowflakeRenderer {
    private static final int MAX_FLAKES = 12_000;
    private static final int MAX_COLUMNS = 32_768;
    private final Flake[] flakes = new Flake[MAX_FLAKES];
    private final Long2FloatOpenHashMap heights = new Long2FloatOpenHashMap(4096);
    private ClientWorld world;
    private Vec3d lastCamera;
    private double heightCacheUntil;
    private int count;
    private float windX, windY, windZ;
    /**
     * Dense twin of {@link #heights} for the columns around the camera. Same lifetime as the map
     * (filled lazily, cleared together), so every column returns exactly the value the map would;
     * it only replaces a hash lookup per flake per frame with an array read.
     */
    private static final int GRID_MARGIN = 24;
    private float[] grid = new float[0];
    private int gridSize, gridX0, gridZ0, gridFilled;
    private static final Vector3f[] CORNERS = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

    public SnowflakeRenderer() {
        heights.defaultReturnValue(Float.NaN);
    }

    public void reset() {
        count = 0;
        world = null;
        lastCamera = null;
        heights.clear();
        clearGrid();
        heightCacheUntil = 0;
        windX = windY = windZ = 0;
    }

    private void clearGrid() {
        java.util.Arrays.fill(grid, Float.NaN);
        gridFilled = 0;
    }

    private void anchorGrid(Vec3d center, float radius) {
        int half = (int)Math.ceil(radius * 1.25f) + GRID_MARGIN;
        int size = half * 2 + 1;
        if (size != gridSize) {
            gridSize = size;
            grid = new float[size * size];
        }
        gridX0 = MathHelper.floor(center.x) - half;
        gridZ0 = MathHelper.floor(center.z) - half;
        clearGrid();
    }

    public void render(Matrix4f view, Matrix4f projection, Camera camera, float dt, double seconds,
                       float strength, float density, float wind, float turbulence, float size,
                       float radius, float opacity, int tintRGB, Fog fog) {
        ClientWorld currentWorld = MinecraftClient.getInstance().world;
        if (currentWorld == null) {
            reset();
            return;
        }
        radius = MathHelper.clamp(radius, 4, 128);
        Vec3d center = camera.getPos();
        if (world != currentWorld || (lastCamera != null
                && lastCamera.squaredDistanceTo(center) > radius * radius)) {
            reset();
            world = currentWorld;
        }
        lastCamera = center;
        if (seconds >= heightCacheUntil || seconds < heightCacheUntil - 0.5) {
            heights.clear();
            anchorGrid(center, radius);
            heightCacheUntil = seconds + 0.5;
        }
        strength = MathHelper.clamp(strength, 0, 1);
        dt = MathHelper.clamp(dt, 0, 0.1f);
        updateWind(seconds, wind, strength);
        int target = (int)Math.min(MAX_FLAKES, Math.PI * radius * radius * Math.max(0, density) * 9 * strength);
        simulate(center, dt, seconds, radius, turbulence, target);
        if (count == 0 || opacity <= 0 || strength <= 0) return;
        draw(view, projection, camera, radius, Math.max(0.01f, size), MathHelper.clamp(opacity, 0, 1), tintRGB, fog);
    }

    private void updateWind(double seconds, float wind, float strength) {
        double angle = seconds * 0.02 + Math.sin(seconds * 0.45) * 0.35;
        double gust = 1 + Math.sin(seconds * 0.9) * 0.35 + Math.sin(seconds * 2.3) * 0.15;
        double speed = MathHelper.clamp(wind, 0, 1) * 16 * gust * strength;
        windX = (float)(Math.cos(angle) * speed);
        windZ = (float)(Math.sin(angle) * speed);
        windY = (float)(Math.sin(seconds * 2.3) * speed * 0.12);
    }

    private void simulate(Vec3d center, float dt, double seconds, float radius, float turbulence, int target) {
        count = Math.min(count, target);
        double limitSquared = radius * radius * 1.5625;
        float sway = Math.max(0, turbulence) * 1.1f;
        for (int i = count - 1; i >= 0; i--) {
            Flake flake = flakes[i];
            flake.vx = windX + (float)Math.sin(seconds * flake.rate + flake.phase) * sway;
            flake.vy = windY - 1.6f * flake.fall;
            flake.vz = windZ + (float)Math.cos(seconds * flake.rate * 0.83 + flake.phase * 1.7) * sway;
            flake.x += flake.vx * dt;
            flake.y += flake.vy * dt;
            flake.z += flake.vz * dt;
            flake.age += dt;
            double dx = flake.x - center.x, dz = flake.z - center.z;
            if (flake.y <= ground(MathHelper.floor(flake.x), MathHelper.floor(flake.z))
                    || dx * dx + dz * dz > limitSquared || flake.y > center.y + 16 || flake.y < center.y - 10) {
                if (!respawn(flake, center, radius)) {
                    flakes[i] = flakes[--count];
                    flakes[count] = flake;
                }
            }
        }
        // A roofed camera can have no valid spawn positions. Bound failed work as well as live flakes.
        while (count < target) {
            Flake flake = flakes[count];
            if (flake == null) flakes[count] = flake = new Flake();
            if (!respawn(flake, center, radius)) break;
            count++;
        }
    }

    private float ground(int x, int z) {
        int gx = x - gridX0, gz = z - gridZ0;
        if (gx >= 0 && gz >= 0 && gx < gridSize && gz < gridSize) {
            int index = gx * gridSize + gz;
            float cached = grid[index];
            if (!Float.isNaN(cached)) return cached;
            float height = columnHeight(x, z);
            if (heights.size() + gridFilled >= MAX_COLUMNS) { heights.clear(); clearGrid(); }
            grid[index] = height;
            gridFilled++;
            return height;
        }
        long key = ((long)x << 32) ^ (z & 0xffffffffL);
        float cached = heights.get(key);
        if (!Float.isNaN(cached)) return cached;
        float height = columnHeight(x, z);
        if (heights.size() + gridFilled >= MAX_COLUMNS) { heights.clear(); clearGrid(); }
        heights.put(key, height);
        return height;
    }

    private float columnHeight(int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4)
                ? world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) : Float.POSITIVE_INFINITY;
    }

    private boolean respawn(Flake flake, Vec3d center, float radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 3; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = radius * Math.sqrt(random.nextDouble());
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            double low = Math.max(ground(MathHelper.floor(x), MathHelper.floor(z)), center.y - 6);
            double high = center.y + 8;
            if (high <= low) continue;
            flake.x = x;
            flake.y = low + random.nextDouble() * (high - low);
            flake.z = z;
            flake.fall = (float)random.nextDouble(0.6, 1.5);
            flake.size = (float)random.nextDouble(0.55, 1.45);
            flake.alpha = (float)random.nextDouble(0.45, 1);
            flake.phase = (float)(random.nextDouble() * Math.PI * 2);
            flake.rate = (float)random.nextDouble(1.2, 3.4);
            flake.age = 0;
            flake.vx = windX;
            flake.vy = windY - 1.6f * flake.fall;
            flake.vz = windZ;
            return true;
        }
        return false;
    }

    private void draw(Matrix4f view, Matrix4f projection, Camera camera, float radius, float size,
                      float opacity, int tintRGB, Fog fog) {
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        ProjectionType projectionType = RenderSystem.getProjectionType();
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int vbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try (VisualRenderState ignored = new VisualRenderState()) {
            modelView.identity();
            RenderSystem.setProjectionMatrix(projection, ProjectionType.PERSPECTIVE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            Vec3d center = camera.getPos();
            Vector3f forward = camera.getHorizontalPlane();
            Vector3f up = camera.getRotation().transform(new Vector3f(0, 1, 0));
            for (int i = 0; i < count; i++) {
                emit(buffer, view, flakes[i], center, forward, up, radius, size, opacity, tintRGB, fog);
            }
            BuiltBuffer built = buffer.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        } finally {
            modelView.popMatrix();
            RenderSystem.setProjectionMatrix(previousProjection, projectionType);
            RenderSystem.depthFunc(depthFunction);
            BufferRenderer.resetCurrentVertexBuffer();
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        }
    }

    private static void emit(BufferBuilder buffer, Matrix4f view, Flake flake, Vec3d center,
                             Vector3f forward, Vector3f up, float radius, float size,
                             float opacity, int tintRGB, Fog fog) {
        float x = (float)(flake.x - center.x), y = (float)(flake.y - center.y), z = (float)(flake.z - center.z);
        float distance = (float)Math.sqrt(x * x + y * y + z * z);
        if (distance <= 0.25f) return;
        float nx = x / distance, ny = y / distance, nz = z / distance;
        if (distance > 3 && nx * forward.x + ny * forward.y + nz * forward.z < -0.35f) return;
        float alpha = MathHelper.clamp((distance - 0.25f) / 0.65f, 0, 1)
                * Math.min(flake.age / 0.45f, 1) * flake.alpha * opacity;
        float horizontal = (float)Math.sqrt(x * x + z * z);
        alpha *= MathHelper.clamp((radius * 1.25f - horizontal) / (radius * 0.25f), 0, 1);
        int a = MathHelper.clamp(Math.round(alpha * 255), 0, 255);
        if (a == 0) return;

        float speed = (float)Math.sqrt(flake.vx * flake.vx + flake.vy * flake.vy + flake.vz * flake.vz);
        float dx = speed > 0.0001f ? flake.vx / speed : up.x;
        float dy = speed > 0.0001f ? flake.vy / speed : up.y;
        float dz = speed > 0.0001f ? flake.vz / speed : up.z;
        float sx = dy * nz - dz * ny, sy = dz * nx - dx * nz, sz = dx * ny - dy * nx;
        float sideLength = (float)Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sideLength <= 0.0001f) {
            sx = up.y * nz - up.z * ny;
            sy = up.z * nx - up.x * nz;
            sz = up.x * ny - up.y * nx;
            sideLength = (float)Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sideLength <= 0.0001f) return;
        }
        float halfWidth = Math.max(0.028f * size * flake.size, distance * 0.0024f);
        float tail = halfWidth + speed * 0.022f * flake.size;
        float head = tail * 0.3f + halfWidth * 0.7f;
        sx *= halfWidth / sideLength;
        sy *= halfWidth / sideLength;
        sz *= halfWidth / sideLength;
        int rgb = fogColor(tintRGB, fog, distance, horizontal, y);
        int opaque = rgb | a << 24;
        // Four triangles share a bright center; the transparent rim makes a soft velocity streak.
        // The five distinct points are transformed once each (same Matrix4f.transformPosition the
        // matrix overload of vertex() uses) instead of once per emitted vertex.
        Vector3f c = view.transformPosition(x, y, z, CORNERS[0]);
        Vector3f minus = view.transformPosition(x - sx, y - sy, z - sz, CORNERS[1]);
        Vector3f front = view.transformPosition(x + dx * head, y + dy * head, z + dz * head, CORNERS[2]);
        Vector3f plus = view.transformPosition(x + sx, y + sy, z + sz, CORNERS[3]);
        Vector3f back = view.transformPosition(x - dx * tail, y - dy * tail, z - dz * tail, CORNERS[4]);
        triangle(buffer, c, minus, front, opaque, rgb);
        triangle(buffer, c, front, plus, opaque, rgb);
        triangle(buffer, c, plus, back, opaque, rgb);
        triangle(buffer, c, back, minus, opaque, rgb);
    }

    private static int fogColor(int rgb, Fog fog, float distance, float horizontal, float vertical) {
        rgb &= 0xffffff;
        if (fog == null || fog.end() <= fog.start()) return rgb;
        float fogDistance = fog.shape() == FogShape.CYLINDER ? Math.max(horizontal, Math.abs(vertical)) : distance;
        float blend = MathHelper.clamp((fogDistance - fog.start()) / (fog.end() - fog.start()), 0, 1);
        blend = blend * blend * (3 - 2 * blend) * fog.alpha();
        int r = MathHelper.clamp(Math.round(MathHelper.lerp(blend, (rgb >> 16) & 255, fog.red() * 255)), 0, 255);
        int g = MathHelper.clamp(Math.round(MathHelper.lerp(blend, (rgb >> 8) & 255, fog.green() * 255)), 0, 255);
        int b = MathHelper.clamp(Math.round(MathHelper.lerp(blend, rgb & 255, fog.blue() * 255)), 0, 255);
        return r << 16 | g << 8 | b;
    }

    private static void triangle(BufferBuilder buffer, Vector3f c, Vector3f a, Vector3f b, int center, int edge) {
        buffer.vertex(c.x, c.y, c.z).color(center);
        buffer.vertex(a.x, a.y, a.z).color(edge);
        buffer.vertex(b.x, b.y, b.z).color(edge);
    }

    private static final class Flake {
        double x, y, z;
        float vx, vy, vz, fall, size, alpha, phase, rate, age;
    }
}
