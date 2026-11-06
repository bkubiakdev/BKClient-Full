package dev.bkclient.utility.render.voronoi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Splits a rectangle into Voronoi cells, used to shatter a flat surface into shards.
 *
 * <p>The cells are built by half-plane clipping rather than by a proper Fortune sweep:
 * for every seed, the bounding quad is clipped against the perpendicular bisector
 * between that seed and each other seed. That is O(n squared) per cell, which is
 * irrelevant at the couple of dozen shards this is used with, and it keeps the whole
 * thing to one file with no dependencies.
 */
public class VoronoiOfQuad {
    private static final Random RANDOM = new Random();

    /** Rejection-sampling attempts per seed when spreading points apart. */
    private static final int CANDIDATES_PER_POINT = 24;

    public final float x;
    public final float y;
    public final float x2;
    public final float y2;
    public final float cx;
    public final float cy;

    private final List<Polygon> polygons;

    public VoronoiOfQuad(float x, float y, float x2, float y2, List<Vec2f> seeds) {
        this.x = x;
        this.y = y;
        this.x2 = x2;
        this.y2 = y2;
        this.cx = x + (x2 - x) / 2.0f;
        this.cy = y + (y2 - y) / 2.0f;
        this.polygons = buildPolygons(x, y, x2, y2, seeds);
    }

    public VoronoiOfQuad(float x, float y, float x2, float y2, int count) {
        this(x, y, x2, y2, randomSeeds(x, y, x2, y2, count));
    }

    public List<Polygon> getPolygons() {
        return this.polygons;
    }

    /**
     * Builds a diagram whose cells are roughly even in size.
     *
     * @param aspect       width-to-height ratio, so distances stay circular on screen
     * @param relax        how far each seed is pulled towards its own cell centre, 0 to 1
     * @param jitter       random nudge applied afterwards, as a fraction of cell size
     * @param minDistance  rejection threshold when scattering, as a fraction of cell size
     */
    public static VoronoiOfQuad spread(float x, float y, float x2, float y2,
                                       float seedMinX, float seedMinY, float seedMaxX, float seedMaxY,
                                       int count, float aspect, float relax, float jitter,
                                       float minDistance, Random random) {
        float cellSize = (float) Math.sqrt((seedMaxX - seedMinX) * aspect * (seedMaxY - seedMinY)
            / (float) Math.max(1, count));
        float spacing = cellSize * minDistance;

        List<Vec2f> seeds = scatterSeeds(seedMinX, seedMinY, seedMaxX, seedMaxY, count, aspect, spacing, random);

        if (relax > 0.001f) {
            List<Polygon> relaxed = new VoronoiOfQuad(x, y, x2, y2, seeds).getPolygons();
            if (relaxed.size() == seeds.size()) {
                for (int i = 0; i < seeds.size(); i++) {
                    Vec2f seed = seeds.get(i);
                    Vec2f centre = relaxed.get(i).center;
                    seed.x = clamp(seed.x + (centre.x - seed.x) * relax, seedMinX, seedMaxX);
                    seed.y = clamp(seed.y + (centre.y - seed.y) * relax, seedMinY, seedMaxY);
                }
            }
        }

        if (jitter > 0.001f) {
            float jitterX = cellSize * jitter / aspect;
            float jitterY = cellSize * jitter;
            for (int i = 0; i < seeds.size(); i++) {
                Vec2f seed = seeds.get(i);
                float nx = clamp(seed.x + (random.nextFloat() * 2.0f - 1.0f) * jitterX, seedMinX, seedMaxX);
                float ny = clamp(seed.y + (random.nextFloat() * 2.0f - 1.0f) * jitterY, seedMinY, seedMaxY);
                // Only accept the nudge while it keeps the seed clear of its neighbours,
                // otherwise the jitter collapses cells into slivers.
                if (nearestDistanceSq(seeds, i, nx, ny, aspect) >= spacing * spacing) {
                    seed.x = nx;
                    seed.y = ny;
                }
            }
        }

        return new VoronoiOfQuad(x, y, x2, y2, seeds);
    }

    public static VoronoiOfQuad spread(float x, float y, float x2, float y2,
                                       float seedMinX, float seedMinY, float seedMaxX, float seedMaxY,
                                       int count, float aspect, float relax, float jitter, float minDistance) {
        return spread(x, y, x2, y2, seedMinX, seedMinY, seedMaxX, seedMaxY,
            count, aspect, relax, jitter, minDistance, RANDOM);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    private static List<Vec2f> randomSeeds(float x, float y, float x2, float y2, int count) {
        List<Vec2f> seeds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            seeds.add(new Vec2f(x + RANDOM.nextFloat() * (x2 - x), y + RANDOM.nextFloat() * (y2 - y)));
        }
        return seeds;
    }

    /** Mitchell best-candidate scattering: keeps seeds apart without a full relaxation pass. */
    private static List<Vec2f> scatterSeeds(float minX, float minY, float maxX, float maxY,
                                            int count, float aspect, float spacing, Random random) {
        List<Vec2f> seeds = new ArrayList<>(count);
        float spacingSq = spacing * spacing;

        for (int i = 0; i < count; i++) {
            Vec2f best = null;
            float bestDistance = -1.0f;

            for (int attempt = 0; attempt < CANDIDATES_PER_POINT; attempt++) {
                Vec2f candidate = new Vec2f(
                    minX + random.nextFloat() * (maxX - minX),
                    minY + random.nextFloat() * (maxY - minY));

                if (seeds.isEmpty()) {
                    best = candidate;
                    bestDistance = Float.MAX_VALUE;
                    break;
                }

                float distance = nearestDistanceSq(seeds, -1, candidate.x, candidate.y, aspect);
                if (distance > bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
                if (bestDistance >= spacingSq) break;
            }

            if (best != null && bestDistance >= spacingSq) {
                seeds.add(best);
            }
        }
        return seeds;
    }

    private static float nearestDistanceSq(List<Vec2f> seeds, int skipIndex, float px, float py, float aspect) {
        float nearest = Float.MAX_VALUE;
        for (int i = 0; i < seeds.size(); i++) {
            if (i == skipIndex) continue;
            Vec2f other = seeds.get(i);
            float dx = (px - other.x) * aspect;
            float dy = py - other.y;
            nearest = Math.min(nearest, dx * dx + dy * dy);
        }
        return nearest;
    }

    private List<Polygon> buildPolygons(float x, float y, float x2, float y2, List<Vec2f> seeds) {
        List<Polygon> result = new ArrayList<>();
        List<Vec2f> bounds = Arrays.asList(
            new Vec2f(x, y), new Vec2f(x2, y), new Vec2f(x2, y2), new Vec2f(x, y2));

        int count = seeds.size();
        for (int i = 0; i < count; i++) {
            Vec2f seed = seeds.get(i);
            List<Vec2f> cell = new ArrayList<>(bounds);

            for (int j = 0; j < count && !cell.isEmpty(); j++) {
                if (j == i) continue;
                Vec2f other = seeds.get(j);
                // Perpendicular bisector between the two seeds, as ax + by + c <= 0.
                float a = 2.0f * (other.x - seed.x);
                float b = 2.0f * (other.y - seed.y);
                float c = seed.x * seed.x + seed.y * seed.y - (other.x * other.x + other.y * other.y);
                cell = clipPolygon(cell, a, b, c);
            }

            if (cell.size() < 3) continue;
            result.add(new Polygon(new ArrayList<>(cell)));
        }
        return result;
    }

    /** Sutherland-Hodgman clip of a convex polygon against one half-plane. */
    private List<Vec2f> clipPolygon(List<Vec2f> polygon, float a, float b, float c) {
        List<Vec2f> clipped = new ArrayList<>();
        int count = polygon.size();

        for (int i = 0; i < count; i++) {
            Vec2f current = polygon.get(i);
            Vec2f next = polygon.get((i + 1) % count);
            boolean currentInside = isInside(current, a, b, c);
            boolean nextInside = isInside(next, a, b, c);

            if (currentInside) {
                if (nextInside) {
                    clipped.add(next);
                } else {
                    Vec2f crossing = findIntersection(current, next, a, b, c);
                    if (crossing != null) clipped.add(crossing);
                }
            } else if (nextInside) {
                Vec2f crossing = findIntersection(current, next, a, b, c);
                if (crossing != null) clipped.add(crossing);
                clipped.add(next);
            }
        }
        return clipped;
    }

    private boolean isInside(Vec2f point, float a, float b, float c) {
        return a * point.x + b * point.y + c <= 0.0f;
    }

    private Vec2f findIntersection(Vec2f from, Vec2f to, float a, float b, float c) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float denominator = a * dx + b * dy;
        if (Math.abs(denominator) < 1.0E-4f) return null;

        float t = -(a * from.x + b * from.y + c) / denominator;
        return t >= 0.0f && t <= 1.0f ? new Vec2f(from.x + t * dx, from.y + t * dy) : null;
    }

    /** One shard. Every transform mutates in place and returns {@code this} so calls chain. */
    public static class Polygon {
        public final Vec2f center;
        public final ArrayList<Vec2f> vertices;

        public Polygon(ArrayList<Vec2f> vertices) {
            this.vertices = vertices;
            this.center = computeCenter();
        }

        public Polygon copy() {
            ArrayList<Vec2f> copied = new ArrayList<>(this.vertices.size());
            for (Vec2f vertex : this.vertices) {
                copied.add(new Vec2f(vertex.x, vertex.y));
            }
            return new Polygon(copied);
        }

        public List<Vec2f> getAllVertices() {
            return this.vertices;
        }

        private Vec2f computeCenter() {
            Vec2f sum = new Vec2f(0.0f, 0.0f);
            for (Vec2f vertex : this.vertices) {
                sum.x += vertex.x;
                sum.y += vertex.y;
            }
            int count = Math.max(1, this.vertices.size());
            sum.x /= count;
            sum.y /= count;
            return sum;
        }

        public float distanceToAtCenter(float px, float py) {
            float dx = this.center.x - px;
            float dy = this.center.y - py;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        public Polygon moveXY(float dx, float dy) {
            this.center.x += dx;
            this.center.y += dy;
            for (Vec2f vertex : this.vertices) {
                vertex.x += dx;
                vertex.y += dy;
            }
            return this;
        }

        public Polygon setPolygonMidPos(float px, float py) {
            float dx = px - this.center.x;
            float dy = py - this.center.y;
            if (dx == 0.0f && dy == 0.0f) return this;
            return moveXY(dx, dy);
        }

        /** Slides the shard along the line through {@code (px, py)}, positive distance towards it. */
        public Polygon translateToPosLoc(float px, float py, float distance) {
            float dx = this.center.x - px;
            float dy = this.center.y - py;
            float angle = (float) (Math.atan2(dy, dx) + Math.toRadians(90.0));
            return moveXY(-(float) Math.sin(angle) * distance, (float) Math.cos(angle) * distance);
        }

        public Polygon translateAwayPosLoc(float px, float py, float distance) {
            return translateToPosLoc(px, py, -distance);
        }

        public Polygon rotateAngleOfCenter(float degrees) {
            if (degrees == 0.0f) return this;
            for (Vec2f vertex : this.vertices) {
                float dx = vertex.x - this.center.x;
                float dy = vertex.y - this.center.y;
                float angle = (float) Math.toRadians(Math.toDegrees(Math.atan2(dy, dx)) + degrees - 90.0);
                float radius = (float) Math.sqrt(dx * dx + dy * dy);
                vertex.x = this.center.x - (float) Math.sin(angle) * radius;
                vertex.y = this.center.y + (float) Math.cos(angle) * radius;
            }
            return this;
        }

        /** Rotation with the aspect ratio undone, so the shard does not skew on a wide screen. */
        public Polygon rotateAroundCenter(float degrees, float aspect) {
            if (degrees == 0.0f) return this;
            float radians = (float) Math.toRadians(degrees);
            float sin = (float) Math.sin(radians);
            float cos = (float) Math.cos(radians);
            for (Vec2f vertex : this.vertices) {
                float dx = (vertex.x - this.center.x) * aspect;
                float dy = vertex.y - this.center.y;
                vertex.x = this.center.x + (dx * cos - dy * sin) / aspect;
                vertex.y = this.center.y + (dx * sin + dy * cos);
            }
            return this;
        }

        /** Rotates each vertex more the further out it sits, curling the shard. */
        public Polygon twirlAroundCenter(float degrees, float aspect) {
            if (degrees == 0.0f) return this;

            float maxRadius = 0.0f;
            for (Vec2f vertex : this.vertices) {
                float dx = (vertex.x - this.center.x) * aspect;
                float dy = vertex.y - this.center.y;
                maxRadius = Math.max(maxRadius, (float) Math.sqrt(dx * dx + dy * dy));
            }
            if (maxRadius <= 1.0E-6f) return this;

            for (Vec2f vertex : this.vertices) {
                float dx = (vertex.x - this.center.x) * aspect;
                float dy = vertex.y - this.center.y;
                float radius = (float) Math.sqrt(dx * dx + dy * dy);
                float radians = (float) Math.toRadians(degrees * (radius / maxRadius));
                float sin = (float) Math.sin(radians);
                float cos = (float) Math.cos(radians);
                vertex.x = this.center.x + (dx * cos - dy * sin) / aspect;
                vertex.y = this.center.y + (dx * sin + dy * cos);
            }
            return this;
        }

        public Polygon rotateAtYOfAngleAwayPos(float px, float py, float scale) {
            float dx = this.center.x - px;
            float dy = this.center.y - py;
            return rotateAngleOfCenter(wrapDegrees(dx * scale * (dy < 0.0f ? 1 : -1)));
        }

        public Polygon rotateAtXOfAngleAwayPos(float px, float py, float scale) {
            float dx = this.center.x - px;
            float dy = this.center.y - py;
            return rotateAngleOfCenter(wrapDegrees(dy * scale * (dx > 0.0f ? 1 : -1)));
        }
    }

    public static class Vec2f {
        public float x;
        public float y;

        public Vec2f(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getUVTexX(float min, float max) {
            return (this.x - min) / (max - min);
        }

        public float getUVTexY(float min, float max) {
            return 1.0f - (max - this.y) / (max - min);
        }
    }
}
