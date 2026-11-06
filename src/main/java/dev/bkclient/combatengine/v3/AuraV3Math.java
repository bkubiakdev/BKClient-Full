package dev.bkclient.combatengine.v3;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Java port of the pure parts of aura-working/src/aura_engine.h and
 * bk_adaptive_aura.h. It deliberately contains no Minecraft or JNI calls. The
 * game adapter supplies observations and consumes the returned rotations.
 */
public final class AuraV3Math {
   private AuraV3Math() { }

   public static float wrap(float angle) {
      if (!Float.isFinite(angle)) return 0.0F;
      float wrapped = (angle + 180.0F) % 360.0F;
      if (wrapped < 0.0F) wrapped += 360.0F;
      return wrapped - 180.0F;
   }

   public static float clampPitch(float pitch) {
      return Math.clamp(Float.isFinite(pitch) ? pitch : 0.0F, -90.0F, 90.0F);
   }

   private static float roundAway(float value) {
      return (float)(value < 0.0F ? Math.ceil(value - 0.5F) : Math.floor(value + 0.5F));
   }

   public record Rotation(float yaw, float pitch) { }
   public record HitBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      public HitBox canonical() {
         return new HitBox(Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ),
            Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ));
      }
   }
   public record AimPoint(double x, double y, double z, int index) { }

   public static final class MotionTrack {
      private boolean valid;
      private double x, y, z, vx, vy, vz;
      private long stamp;
      private double sampleSeconds = 0.05;

      public void reset() { valid = false; x = y = z = vx = vy = vz = 0.0; stamp = 0L; sampleSeconds = 0.05; }
      public void observe(double px, double py, double pz, long now, double tickSeconds) {
         if (!Double.isFinite(px + py + pz)) return;
         if (!valid) { x = px; y = py; z = pz; stamp = now; valid = true; sampleSeconds = tickSeconds > 0 ? tickSeconds : sampleSeconds; return; }
         double dt = Math.max(0L, now - stamp) / 1000.0;
         if (dt > 1.0) { x = px; y = py; z = pz; vx = vy = vz = 0.0; stamp = now; return; }
         if (tickSeconds > 0) sampleSeconds = tickSeconds;
         if (dt > 1.0e-4) {
            double k = 0.55;
            vx = vx * (1 - k) + ((px - x) / dt) * k;
            vy = vy * (1 - k) + ((py - y) / dt) * k;
            vz = vz * (1 - k) + ((pz - z) / dt) * k;
         }
         x = px; y = py; z = pz; stamp = now;
      }
      public boolean valid() { return valid; }
      public double speed() { return Math.sqrt(vx * vx + vy * vy + vz * vz); }
      public double[] predict(double ticks, double verticalScale) {
         if (!valid || !Double.isFinite(ticks) || ticks < 0) return new double[]{x, y, z};
         double t = Math.min(ticks, 12.0) * sampleSeconds;
         return new double[]{x + vx * t, y + vy * t * (Double.isFinite(verticalScale) ? verticalScale : 1.0), z + vz * t};
      }
   }

   public static AimPoint resolveAimPoint(HitBox raw, double fromX, double fromY, double fromZ,
                                          double distance, float fovDegrees, int samples) {
      HitBox box = Objects.requireNonNull(raw).canonical();
      double cx = (box.minX() + box.maxX()) * .5, cy = (box.minY() + box.maxY()) * .5, cz = (box.minZ() + box.maxZ()) * .5;
      double height = Math.max(.05, box.maxY() - box.minY());
      int perSide = samples > 1 ? (int)Math.ceil(Math.sqrt(Math.clamp(samples, 1, 26))) : 1;
      double inset = height * (height < 1.5 ? .38 : .18), loY = box.minY() + inset, hiY = Math.max(loY, box.maxY() - inset);
      double tanFov = Math.tan(Math.clamp(fovDegrees, 5.0F, 180.0F) * .5 * Math.PI / 180.0);
      double dx = cx - fromX, dz = cz - fromZ, forward = Math.hypot(dx, dz);
      if (forward < 1.0e-6 && Math.abs(box.maxX() - box.minX()) < 1.0e-3)
         return new AimPoint(cx, Math.clamp(fromY, loY, hiY), cz, 0);
      AimPoint best = new AimPoint(cx, Math.clamp(cy, loY, hiY), cz, 0);
      double bestScore = Double.POSITIVE_INFINITY;
      int index = 0;
      for (int iy = 0; iy < perSide; iy++) for (int ix = 0; ix < perSide; ix++) for (int iz = 0; iz < perSide; iz++) {
         double ty = perSide == 1 ? (loY + hiY) * .5 : loY + (hiY - loY) * iy / (perSide - 1.0);
         double tx = perSide == 1 ? cx : box.minX() + (box.maxX() - box.minX()) * ix / (perSide - 1.0);
         double tz = perSide == 1 ? cz : box.minZ() + (box.maxZ() - box.minZ()) * iz / (perSide - 1.0);
         double ox = tx - fromX, oy = ty - fromY, oz = tz - fromZ, flat = Math.hypot(ox, oz);
         if (forward > 1e-6 && flat > 1e-6 && (ox * dx + oz * dz) / (flat * forward) < 1.0 / Math.sqrt(1 + tanFov * tanFov)) continue;
         double edge = Math.abs(tx - cx) + Math.abs(ty - cy) + Math.abs(tz - cz);
         double score = Math.sqrt(ox * ox + oy * oy + oz * oz) + edge * .35 + Math.abs(ty - (box.minY() + height * .62)) * .5;
         if (score < bestScore) { bestScore = score; best = new AimPoint(tx, ty, tz, index); }
         index++;
      }
      return Double.isFinite(best.x() + best.y() + best.z()) && distance >= 0 ? best : new AimPoint(cx, Math.clamp(cy, loY, hiY), cz, 0);
   }

   public static final class Backoff {
      private int remaining, level;
      public void reset() { remaining = level = 0; }
      public boolean blocked() { return remaining > 0; }
      public void tick() { if (remaining > 0) remaining--; }
      public void reject() { level = Math.min(level + 1, 5); remaining = Math.min(2 + level, 8); }
      public void accept() { level = remaining = 0; }
   }

   public static final class SwingVerifier {
      private boolean pending;
      private long sent;
      private double x, y, z;
      private float health, absorption;
      private int hurtTime;
      public void arm(long now, double px, double py, double pz, float hp, float absorb, int hurt) { pending = true; sent = now; x = px; y = py; z = pz; health = hp; absorption = absorb; hurtTime = hurt; }
      public boolean pending() { return pending; }
      public void clear() { pending = false; }
      public Result check(long now, double px, double py, double pz, float hp, float absorb, int hurt, boolean alive) {
         if (!pending) return Result.NONE;
         if (!alive || hp < health - .001F || absorb < absorption - .001F || hurt > hurtTime) return Result.CONFIRMED;
         if (now - sent < 500) return Result.NONE;
         return Math.abs(px - x) + Math.abs(py - y) + Math.abs(pz - z) > .08 ? Result.NONE : Result.REJECTED;
      }
      public enum Result { NONE, CONFIRMED, REJECTED }
   }

   public static final class Cadence {
      private long lastAttack, intervalMs, jitterMs;
      private long rngState = 0x2545F4914F6CDD1DL;
      public void reset() { lastAttack = intervalMs = 0L; }
      public void configure(long interval, long jitter) { intervalMs = interval; jitterMs = Math.max(0L, jitter); }
      public long next() {
         rngState ^= rngState << 13; rngState ^= rngState >>> 7; rngState ^= rngState << 17;
         long span = jitterMs != 0L ? Long.remainderUnsigned(rngState, jitterMs * 2L + 1L) : 0L;
         return intervalMs + span - jitterMs;
      }
      public boolean ready(long now, float strength, float threshold) {
         return strength > threshold && (lastAttack == 0L || now - lastAttack >= intervalMs);
      }
      public void attacked(long now) { lastAttack = now; }
   }

   public record Profile(float response, float jitter, float overshoot, boolean quantize) { }
   public static Profile profileFor(int mode) {
      return switch (mode) {
         case 0 -> new Profile(26.0F, .55F, .6F, true);
         case 1 -> new Profile(20.0F, 1.10F, 1.2F, true);
         case 2 -> new Profile(34.0F, .25F, .2F, false);
         case 3 -> new Profile(30.0F, .75F, .4F, true);
         case 4 -> new Profile(22.0F, .40F, 0.0F, true);
         case 5 -> new Profile(18.0F, .20F, 2.4F, false);
         default -> new Profile(24.0F, .60F, .5F, true);
      };
   }

   public static float trackingResponse(double halfSize, double distance) {
      double halfAngle = Math.atan2(Math.max(halfSize, .001), Math.max(distance, .01)) * 57.295779513;
      return (float)Math.clamp(24.0 * 8.0 / halfAngle, 24.0, 48.0);
   }

   public static Rotation smooth(Rotation current, Rotation target, float seconds, float response) {
      float blend = (float)-Math.expm1(-response * Math.clamp(seconds, 0.0F, .1F));
      return new Rotation(current.yaw() + wrap(target.yaw() - current.yaw()) * blend,
         clampPitch(current.pitch() + (target.pitch() - current.pitch()) * blend));
   }

   public static final class MouseSmoother {
      private float yawRemainder, pitchRemainder;
      public void reset() { yawRemainder = pitchRemainder = 0.0F; }
      public Rotation step(Rotation current, Rotation target, float seconds, float gcd, float response) {
         if (!Float.isFinite(gcd) || gcd <= 0.0F || seconds <= 0.0F) return current;
         Rotation next = smooth(current, target, seconds, response);
         float totalYaw = wrap(next.yaw() - current.yaw()) + yawRemainder;
         float deltaYaw = roundAway(totalYaw / gcd) * gcd;
         yawRemainder = totalYaw - deltaYaw;
         float totalPitch = next.pitch() - current.pitch() + pitchRemainder;
         float deltaPitch = roundAway(totalPitch / gcd) * gcd;
         pitchRemainder = totalPitch - deltaPitch;
         float pitch = clampPitch(current.pitch() + deltaPitch);
         if (pitch != current.pitch() + deltaPitch) pitchRemainder = 0.0F;
         return new Rotation(current.yaw() + deltaYaw, pitch);
      }
   }

   public record CriticalState(boolean valid, boolean grounded, boolean jump, boolean blocked,
                               double fallDistance, double verticalSpeed) {
      public boolean falling() { return valid && !blocked && !grounded && fallDistance > 0.0 && verticalSpeed < 0.0; }
      public boolean ready() { return valid && (blocked || grounded || falling()); }
   }

   /** Exact critically-damped BKAdaptiveAura tracker from bk_adaptive_aura.h. */
   public static final class SmoothTracker {
      private Rotation position = new Rotation(0, 0), velocity = new Rotation(0, 0), lastOutput = new Rotation(0, 0);
      private boolean initialized;
      public void reset() { initialized = false; velocity = new Rotation(0, 0); }
      public Rotation step(Rotation current, Rotation target, float dt, float gcd) {
         if (!Float.isFinite(dt) || dt <= 0 || !Float.isFinite(gcd) || gcd <= 0) return current;
         if (!initialized || Math.abs(wrap(current.yaw() - lastOutput.yaw())) > 5 || Math.abs(current.pitch() - lastOutput.pitch()) > 5) { position = current; velocity = new Rotation(0, 0); initialized = true; }
         dt = Math.clamp(dt, 0, .1F);
         float[] y = advance(position.yaw(), velocity.yaw(), position.yaw() + wrap(target.yaw() - position.yaw()), dt);
         float[] p = advance(position.pitch(), velocity.pitch(), clampPitch(target.pitch()), dt);
         position = new Rotation(y[0], clampPitch(p[0])); velocity = new Rotation(y[1], p[1]);
         lastOutput = new Rotation(current.yaw() + roundAway(wrap(position.yaw() - current.yaw()) / gcd) * gcd,
            clampPitch(current.pitch() + roundAway((position.pitch() - current.pitch()) / gcd) * gcd));
         return lastOutput;
      }
      private static float[] advance(float p, float v, float goal, float dt) { float e = p - goal, omega = 32, j = v + omega * e, decay = (float)Math.exp(-omega * dt); return new float[]{goal + (e + j * dt) * decay, (v - omega * j * dt) * decay}; }
   }

   public static final class Tracker {
      private final SmoothTracker tracker = new SmoothTracker();
      private float phase;
      public void reset() { tracker.reset(); phase = 0.0F; }
      public Rotation step(Rotation current, Rotation target, float dt, float gcd, float leadDegrees, float jitter) {
         phase += Math.clamp(dt, 0.0F, .05F);
         float lead = Math.clamp(leadDegrees, -20.0F, 20.0F);
         Rotation aimed = new Rotation(target.yaw() + lead,
            clampPitch(target.pitch() + jitter * (float)Math.sin(phase * 4.7F) * .35F));
         return tracker.step(current, aimed, dt, gcd);
      }
   }

   public static int delay(int mode, long hits) {
      int[] funtime = {10, 10, 10, 10, 12}, smooth = {10, 10, 10, 13};
      return mode == 0 || mode == 3 ? funtime[(int)(hits % 5)] : mode == 1 ? smooth[(int)(hits % 4)] : 10;
   }

   /** Direct port of BKAdaptiveAura::State. Mode 2 is the active source path. */
   public static final class BKAdaptiveState {
      private long hits, lastHit;
      private int cooldown;
      private Rotation start = new Rotation(0, 0), goal = new Rotation(0, 0);
      private float progress = 1.0F, offsetYaw, offsetPitch;

      public void reset() { hits = lastHit = 0L; cooldown = 0; start = goal = new Rotation(0, 0); progress = 1.0F; }
      public void tick() { if (cooldown > 0) cooldown--; }
      public boolean ready(float strength) { return strength > .9F && cooldown == 0; }
      public int cooldown() { return cooldown; }
      public long hits() { return hits; }
      public void attack(int mode, long now) { cooldown = delay(mode, hits); hits++; lastHit = now; }
      private static float random(float lo, float hi) { return ThreadLocalRandom.current().nextFloat(lo, hi); }

      public Rotation step(int mode, Rotation current, Rotation target, boolean critReady,
                           boolean attackNextTick, boolean holyCritReady, int age,
                           long now, long wallMs, float dt) {
         float yaw = wrap(target.yaw() - current.yaw()), pitch = wrap(target.pitch() - current.pitch());
         float length = (float)Math.hypot(yaw, pitch);
         if (length < 1.0e-6F) return current;
         float cy = Math.clamp(yaw, -Math.abs(yaw / length) * 180.0F, Math.abs(yaw / length) * 180.0F);
         float cp = Math.clamp(pitch, -Math.abs(pitch / length) * 180.0F, Math.abs(pitch / length) * 180.0F);
         long elapsed = lastHit != 0L ? now - lastHit : now;
         float a = wallMs % 2000L / 75.0F, b = wallMs % 1000L / 75.0F;
         float wy = hits % 4 == 0 ? (float)Math.cos(a) : hits % 4 == 3 ? (float)-Math.cos(a) : (float)Math.sin(a);
         float wp = hits % 4 == 1 ? (float)Math.cos(b) : hits % 4 == 2 ? (float)-Math.cos(b) : (float)Math.sin(b);
         Rotation out = current;
         switch (mode) {
            case 0 -> {
               if (critReady) out = new Rotation(current.yaw() + cy, current.pitch() + cp);
               else {
                  float f = elapsed < 500L ? -.2F : random(0, 1) < .5F ? .4F : .2F;
                  out = new Rotation(current.yaw() + Math.clamp(random(f, f + .2F), 0, 1) * cy + (elapsed < 1000L ? random(6, 12) * wy : 0),
                     current.pitch() + Math.clamp(random(f, f + .2F), 0, 1) * cp + (elapsed < 1000L ? random(2, 4) * wp : 0));
               }
            }
            case 1 -> {
               cy = Math.clamp(yaw, -Math.abs(yaw / length) * 60, Math.abs(yaw / length) * 60);
               cp = Math.clamp(pitch, -Math.abs(pitch / length) * 40, Math.abs(pitch / length) * 40);
               if (!critReady) cy += (float)Math.ceil(random(2, 5) * Math.sin(wallMs / 40.0));
               out = new Rotation(current.yaw() + random(.6F, 1.2F) * cy, current.pitch() + random(.5F, 1) * cp);
            }
            case 2 -> {
               float blend = attackNextTick ? 1.0F : elapsed >= 50L ? .7F : .45F;
               out = new Rotation(current.yaw() + blend * cy, current.pitch() + blend * cp);
            }
            case 3 -> {
               float f = random(.75F, .95F);
               out = new Rotation(current.yaw() + f * cy + (!critReady ? 8 * wy : 0),
                  current.pitch() + f * cp + (!critReady ? 8 * wp : 0));
            }
            case 4 -> {
               if ((age & 1) != 0) return current;
               out = new Rotation(wrap(current.yaw() + Math.clamp(yaw, -80, 80) + (float)Math.sin(wallMs / 40.0) * random(3, 4)),
                  clampPitch(current.pitch() + (holyCritReady ? pitch : Math.clamp(pitch, -4, 4)) + (float)Math.cos(wallMs / 40.0) * random(1, 2)));
            }
            case 5 -> {
               if ((Math.abs(target.yaw() - goal.yaw()) > 3 || Math.abs(target.pitch() - goal.pitch()) > 3) && progress >= 1) {
                  start = current; goal = target; progress = 0; offsetYaw = random(-1, 1); offsetPitch = random(-.5F, .5F);
               }
               progress = Math.clamp(progress + dt / .1F, 0, 1);
               float t = progress, u = 1 - t, dy = wrap(goal.yaw() - start.yaw());
               out = new Rotation(start.yaw() + 2 * u * t * (dy * .5F + offsetYaw) + t * t * dy,
                  u * u * start.pitch() + 2 * u * t * ((start.pitch() + goal.pitch()) * .5F + offsetPitch) + t * t * goal.pitch());
            }
            default -> { }
         }
         return new Rotation(out.yaw(), clampPitch(out.pitch()));
      }
   }
}
