package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.client.world.ClientWorld;

/**
 * Smooths the camera position (not the view direction), so walking, sprinting and stepping do not jerk the view.
 * The smoothing is time based (milliseconds), so it feels the same at any frame rate, and the camera can never stay
 * more than {@code Max Offset} blocks away from the real position. A teleport, respawn, dimension change or a long
 * pause snaps it back at once, so the world is always where the player is.
 */
@ModuleInfo(name = "Smooth Camera", category = ModuleCategory.VISUALS, desc = "Makes your camera movement smoother")
public final class SmoothCamera extends BaseModule {
   /** A real position change between two camera updates above this many blocks is a teleport, not walking. */
   private static final double TELEPORT_BLOCKS = 3.0;
   private static final double PAUSE_SECONDS = 0.5;

   private static SmoothCamera instance;

   private final SliderSetting smoothness = new SliderSetting(this, "Smoothness").min(0.0F).max(300.0F).step(5.0F).currentValue(80.0F).suffix("ms");
   private final SliderSetting verticalSmoothness = new SliderSetting(this, "Vertical Smoothness").min(0.0F).max(300.0F).step(5.0F).currentValue(120.0F).suffix("ms");
   private final SliderSetting maxOffset = new SliderSetting(this, "Max Offset").min(0.2F).max(3.0F).step(0.1F).currentValue(1.0F).suffix("blocks");
   private final BooleanSetting firstPerson = new BooleanSetting(this, "Enable First POV");
   private final BooleanSetting resetOnPerspective = new BooleanSetting(this, "Reset On Perspective").enabled(true);

   private double x, y, z;
   private double realX, realY, realZ;
   private boolean ready;
   private boolean lastThirdPerson;
   private boolean seenPerspective;
   private long lastNanos;
   private ClientWorld lastWorld;

   public SmoothCamera() {
      instance = this;
   }

   public static SmoothCamera getInstance() {
      return instance;
   }

   @Override
   public void onDisable() {
      this.ready = false;
      this.seenPerspective = false;
      this.lastNanos = 0L;
      this.lastWorld = null;
   }

   /** Returns the smoothed camera position for the real one; called for every camera update while enabled. */
   public double[] apply(double realX, double realY, double realZ, boolean thirdPerson, ClientWorld world) {
      long now = System.nanoTime();
      double dt = this.lastNanos == 0L ? 0.0 : (now - this.lastNanos) / 1.0E9;
      this.lastNanos = now;

      boolean perspectiveChanged = this.seenPerspective && this.lastThirdPerson != thirdPerson;
      this.lastThirdPerson = thirdPerson;
      this.seenPerspective = true;

      double jump = Math.sqrt(square(realX - this.realX) + square(realY - this.realY) + square(realZ - this.realZ));
      this.realX = realX;
      this.realY = realY;
      this.realZ = realZ;
      boolean newWorld = world != this.lastWorld;
      this.lastWorld = world;

      if (!this.ready || newWorld || jump > TELEPORT_BLOCKS || dt > PAUSE_SECONDS
         || (perspectiveChanged && this.resetOnPerspective.isEnabled())
         || (!thirdPerson && !this.firstPerson.isEnabled())) {
         this.snap(realX, realY, realZ);
         return new double[]{this.x, this.y, this.z};
      }

      this.x += (realX - this.x) * this.blend(dt, this.smoothness.getCurrentValue());
      this.z += (realZ - this.z) * this.blend(dt, this.smoothness.getCurrentValue());
      this.y += (realY - this.y) * this.blend(dt, this.verticalSmoothness.getCurrentValue());
      this.limitOffset(realX, realY, realZ);
      return new double[]{this.x, this.y, this.z};
   }

   /** Share of the remaining distance covered in this frame for a smoothing time constant in milliseconds. */
   private double blend(double dt, double millis) {
      if (millis <= 0.0) return 1.0;
      return 1.0 - Math.exp(-Math.max(0.0, dt) / (millis / 1000.0));
   }

   /** The smoothed position may lag behind, but never further than the chosen distance from the real one. */
   private void limitOffset(double realX, double realY, double realZ) {
      double dx = this.x - realX;
      double dy = this.y - realY;
      double dz = this.z - realZ;
      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
      double limit = this.maxOffset.getCurrentValue();
      if (distance > limit && distance > 1.0E-6) {
         double scale = limit / distance;
         this.x = realX + dx * scale;
         this.y = realY + dy * scale;
         this.z = realZ + dz * scale;
      }
   }

   private void snap(double realX, double realY, double realZ) {
      this.x = realX;
      this.y = realY;
      this.z = realZ;
      this.ready = true;
   }

   private static double square(double value) {
      return value * value;
   }
}
