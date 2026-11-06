package dev.bkclient.utility.animation;

import dev.bkclient.utility.render.voronoi.VoronoiOfQuad;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Breaks a rectangle of screen into Voronoi shards that fly outwards and fade.
 *
 * <p>This class only produces geometry: call {@link #begin} with the rectangle to
 * shatter, then {@link #buildGeometry} once per frame with the animation progress.
 * It fills a vertex buffer of {@code (x, y, z, u, v, alpha)} floats in triangle-list
 * order, ready to be drawn with the snapshot of that rectangle as a texture.
 *
 * <p>Coordinates are normalised 0..1 over the whole screen, so
 * the same shards work at any resolution and the UVs fall straight out of them.
 */
public final class GuiShatterAnimation {
    /** Cap on shards, and therefore on the buffer the caller has to allocate. */
    public static final int MAX_SHARDS = 28;
    public static final int VERTEX_BYTES = 24;
    private static final int MAX_POLY_VERTICES = 24;

    /** Worst case: every shard a full 24-gon, fanned into 22 triangles. */
    public static final int MAX_VERTICES = MAX_SHARDS * (MAX_POLY_VERTICES - 2) * 3;

    private static final int MIN_COUNT = 22;
    private static final int MAX_COUNT = 28;
    private static final float RELAX_STRENGTH = 0.45f;
    private static final float SEED_JITTER = 0.32f;
    private static final float MIN_DISTANCE = 0.68f;

    /** How far a shard travels outwards at full progress, in screen fractions. */
    private static final float EXPAND = 0.34f;
    private static final float MARGIN = 0.004f;
    private static final float MAX_BLUR_RADIUS = 9.0f;
    private static final long GATHER_NS = 360_000_000L;

    private static final Random RANDOM = new Random();
    private static final State LOCAL = new State();

    private GuiShatterAnimation() {
    }

    public static State local() {
        return LOCAL;
    }

    public static State create() {
        return new State();
    }

    public static void begin(float x, float y, float width, float height, float pad,
                             float screenWidth, float screenHeight, long seed) {
        LOCAL.begin(x, y, width, height, pad, screenWidth, screenHeight, seed);
    }

    public static void begin(float x, float y, float width, float height, float pad,
                             float screenWidth, float screenHeight) {
        LOCAL.begin(x, y, width, height, pad, screenWidth, screenHeight, RANDOM.nextLong());
    }

    public static void cancel() {
        LOCAL.cancel();
    }

    public static int shardCount() {
        return LOCAL.shardCount();
    }

    public static boolean isActive() {
        return LOCAL.isActive();
    }

    public static boolean isGathering() {
        return LOCAL.isGathering();
    }

    public static long seed() {
        return LOCAL.seed();
    }

    /** Reverses the shatter, pulling the shards back together over {@code nanos}. */
    public static void gather(long nanos) {
        LOCAL.gather(nanos);
    }

    public static boolean resume() {
        return LOCAL.resume();
    }

    public static float blurRadius() {
        return LOCAL.blurRadius();
    }

    public static float[] beginRect() {
        return LOCAL.beginRect();
    }

    public static float progress(float raw) {
        return LOCAL.progress(raw);
    }

    public static int buildGeometry(ByteBuffer buffer, float raw, float screenWidth, float screenHeight) {
        return LOCAL.buildGeometry(buffer, raw, screenWidth, screenHeight, null);
    }

    public static int buildGeometry(ByteBuffer buffer, float raw, float screenWidth, float screenHeight, Remap remap) {
        return LOCAL.buildGeometry(buffer, raw, screenWidth, screenHeight, remap);
    }

    public static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public static float clampSigned(float value) {
        return Math.max(-1.0f, Math.min(1.0f, value));
    }

    /** Fraction of the run after which everything eases to fully transparent. */
    private static final float TAIL_START = 0.55f;

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0f - 2.0f * t);
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    /**
     * Rewrites emitted positions and UVs, for drawing the shatter somewhere other than
     * where it was captured, or out of an atlas rather than a full-screen snapshot.
     */
    public record Remap(float posOffsetX, float posOffsetY, float posScaleX, float posScaleY,
                        float uvOffsetX, float uvOffsetY, float uvScaleX, float uvScaleY) {
    }

    public static final class State {
        private final List<VoronoiOfQuad.Polygon> shards = new ArrayList<>();

        /** Per-shard flight direction, distance, fade rate and travel multiplier. */
        private float[] baseAngle = new float[0];
        private float[] baseRadius = new float[0];
        private float[] fade = new float[0];
        private float[] radial = new float[0];

        private boolean gathering;
        private long gatherStartNs;
        private long gatherNanos = GATHER_NS;
        private float gatherFrom;
        private float lastProgress;

        private float centerX;
        private float centerY;
        private float aspect = 1.0f;
        private boolean active;
        private long currentSeed;

        private float beginX;
        private float beginY;
        private float beginWidth;
        private float beginHeight;
        private float beginPad;
        private float beginScreenWidth;
        private float beginScreenHeight;

        public void begin(float x, float y, float width, float height, float pad,
                          float screenWidth, float screenHeight, long seed) {
            cancel();
            if (width <= 1.0f || height <= 1.0f || screenWidth <= 1.0f || screenHeight <= 1.0f) {
                return;
            }

            Random random = new Random(seed);
            this.currentSeed = seed;
            this.beginX = x;
            this.beginY = y;
            this.beginWidth = width;
            this.beginHeight = height;
            this.beginPad = pad;
            this.beginScreenWidth = screenWidth;
            this.beginScreenHeight = screenHeight;

            float minX = clamp01(x / screenWidth);
            float minY = clamp01(y / screenHeight);
            float maxX = clamp01((x + width) / screenWidth);
            float maxY = clamp01((y + height) / screenHeight);

            this.centerX = (minX + maxX) * 0.5f;
            this.centerY = (minY + maxY) * 0.5f;
            this.aspect = Math.max(0.001f, screenWidth / screenHeight);

            int count = MIN_COUNT + random.nextInt(MAX_COUNT - MIN_COUNT + 1);
            // Seeds are scattered slightly outside the rectangle so the shards along the
            // edge are whole cells rather than clipped slivers.
            VoronoiOfQuad diagram = VoronoiOfQuad.spread(minX, minY, maxX, maxY,
                minX - MARGIN, minY - MARGIN, maxX + MARGIN, maxY + MARGIN,
                count, this.aspect, RELAX_STRENGTH, SEED_JITTER, MIN_DISTANCE, random);

            this.shards.clear();
            this.shards.addAll(diagram.getPolygons());

            int size = this.shards.size();
            this.baseAngle = new float[size];
            this.baseRadius = new float[size];
            this.fade = new float[size];
            this.radial = new float[size];

            for (int i = 0; i < size; i++) {
                VoronoiOfQuad.Polygon shard = this.shards.get(i);
                float dx = (shard.center.x - this.centerX) * this.aspect;
                float dy = shard.center.y - this.centerY;
                this.baseAngle[i] = (float) Math.atan2(dy, dx) + (random.nextFloat() - 0.5f) * 0.35f;
                this.baseRadius[i] = (float) Math.sqrt(dx * dx + dy * dy);
                this.fade[i] = 0.55f + 0.45f * random.nextFloat();
                this.radial[i] = 0.65f + 0.7f * random.nextFloat();
            }

            this.active = true;
        }

        public void cancel() {
            this.active = false;
            this.gathering = false;
            this.lastProgress = 0.0f;
            this.shards.clear();
        }

        public int shardCount() {
            return this.shards.size();
        }

        public boolean isActive() {
            return this.active;
        }

        public boolean isGathering() {
            return this.active && this.gathering;
        }

        public long seed() {
            return this.currentSeed;
        }

        public void gather(long nanos) {
            if (!this.active) return;
            this.gathering = true;
            this.gatherStartNs = System.nanoTime();
            this.gatherNanos = Math.max(1_000_000L, nanos);
            this.gatherFrom = this.lastProgress;
        }

        public boolean resume() {
            if (!this.active) return false;
            this.gathering = false;
            return true;
        }

        public float blurRadius() {
            return this.active ? MAX_BLUR_RADIUS * (1.0f - this.lastProgress) : 0.0f;
        }

        public float[] beginRect() {
            return new float[]{this.beginX, this.beginY, this.beginWidth, this.beginHeight,
                this.beginPad, this.beginScreenWidth, this.beginScreenHeight};
        }

        /**
         * Resolves the progress to draw at. While gathering this ignores the caller and
         * eases back to zero on its own clock, cancelling itself when it gets there.
         */
        public float progress(float raw) {
            if (!this.active) return 0.0f;

            if (this.gathering) {
                long elapsed = System.nanoTime() - this.gatherStartNs;
                float t = clamp01((float) elapsed / (float) this.gatherNanos);
                this.lastProgress = this.gatherFrom * (1.0f - easeOutCubic(t));
                if (t >= 1.0f) {
                    cancel();
                    return 0.0f;
                }
                return this.lastProgress;
            }

            this.lastProgress = raw;
            return raw;
        }

        /**
         * Fills {@code buffer} with a triangle list and returns the vertex count.
         * Shards that have faded out are skipped entirely.
         */
        public int buildGeometry(ByteBuffer buffer, float raw, float screenWidth, float screenHeight, Remap remap) {
            if (!this.active || this.shards.isEmpty()) return 0;

            buffer.clear();
            int vertexCount = 0;
            float resolved = progress(raw);
            float eased = easeOutCubic(resolved);
            // Per-shard fade alone bottoms out at 0.55, so the brightest shards would still
            // be at 45 percent when the run ends and would blink out. This tail guarantees
            // everything reaches zero, and with zero slope at both ends so the landing is smooth.
            float tail = 1.0f - smoothstep(TAIL_START, 1.0f, resolved);

            for (int i = 0; i < this.shards.size(); i++) {
                VoronoiOfQuad.Polygon shard = this.shards.get(i);
                List<VoronoiOfQuad.Vec2f> vertices = shard.getAllVertices();
                if (vertices.size() < 3) continue;

                float alpha = (1.0f - eased * this.fade[i]) * tail;
                if (alpha <= 0.0f) continue;

                // Each shard slides straight out along its own angle. The offset is applied
                // at emit time so the polygon itself is never mutated and the animation can
                // run backwards during a gather.
                float angle = this.baseAngle[i];
                float radius = this.baseRadius[i] + eased * EXPAND * this.radial[i];
                float offsetX = this.centerX + (float) Math.cos(angle) * radius / this.aspect - shard.center.x;
                float offsetY = this.centerY + (float) Math.sin(angle) * radius - shard.center.y;

                VoronoiOfQuad.Vec2f first = vertices.get(0);
                for (int j = 1; j < vertices.size() - 1; j++) {
                    putVertex(buffer, first, offsetX, offsetY, alpha, screenWidth, screenHeight, remap);
                    putVertex(buffer, vertices.get(j), offsetX, offsetY, alpha, screenWidth, screenHeight, remap);
                    putVertex(buffer, vertices.get(j + 1), offsetX, offsetY, alpha, screenWidth, screenHeight, remap);
                    vertexCount += 3;
                }
            }

            buffer.flip();
            return vertexCount;
        }

        private void putVertex(ByteBuffer buffer, VoronoiOfQuad.Vec2f vertex, float offsetX, float offsetY,
                               float alpha, float screenWidth, float screenHeight, Remap remap) {
            float px = (vertex.x + offsetX) * screenWidth;
            float py = (vertex.y + offsetY) * screenHeight;
            float u = vertex.x;
            float v = vertex.y;

            if (remap != null) {
                px = remap.posOffsetX() + px * remap.posScaleX();
                py = remap.posOffsetY() + py * remap.posScaleY();
                u = remap.uvOffsetX() + u * remap.uvScaleX();
                v = remap.uvOffsetY() + v * remap.uvScaleY();
            }

            buffer.putFloat(px);
            buffer.putFloat(py);
            buffer.putFloat(0.0f);
            buffer.putFloat(u);
            buffer.putFloat(v);
            buffer.putFloat(alpha);
        }
    }
}
