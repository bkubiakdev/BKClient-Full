package dev.bkclient.systems.modules.modules.visuals;

import lombok.Generated;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.LinkedModeSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.EntityUtility;
import dev.bkclient.utility.render.DynamicLightUtility;
import dev.bkclient.utility.render.ambience.DynamicBrightnessArea;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Vector3f;
import org.joml.Matrix4f;

@ModuleInfo(name = "Ambience", category = ModuleCategory.VISUALS, desc = "modules.descriptions.ambience")
public class Ambience extends BaseModule {

   // Sections only filter the menu; all effects and saved setting keys stay independent.
   public final ModeSetting section = new ModeSetting(this, "modules.settings.ambience.section");
   private final ModeSetting.Value colorsSection = new ModeSetting.Value(section, "modules.settings.ambience.section.colors").select();
   private final ModeSetting.Value lightingSection = new ModeSetting.Value(section, "modules.settings.ambience.section.lighting");
   private final ModeSetting.Value weatherSection = new ModeSetting.Value(section, "modules.settings.ambience.section.weather");
   private final ModeSetting.Value terrainSection = new ModeSetting.Value(section, "modules.settings.ambience.section.terrain");

   public boolean hideColors() { return !colorsSection.isSelected(); }
   private boolean hideLighting() { return !lightingSection.isSelected(); }
   public boolean hideWeather() { return !weatherSection.isSelected(); }
   public boolean hideTerrain() { return !terrainSection.isSelected(); }

    public Ambience() {
        LinkedModeSetting.toggle(this, "color.source.night", this.nightModeThemeSync, "Theme", "Custom");
    }

   @Override
   public java.util.List<dev.bkclient.systems.setting.Setting> getDisplaySettings() {
      var display = new java.util.ArrayList<>(getSettings());
      display.sort(java.util.Comparator.comparingInt(setting -> {
         String name = setting.getName();
         if (setting == section) return 0;
         if (name.contains(".saturation")) return 1;
         if (name.contains("color_isolation")) return 2;
         if (name.endsWith(".custom_time") || name.endsWith(".time")) return 3;
         if (name.contains("night_mode") || name.equals("color.source.night")) return 4;
         if (name.contains(".fog")) return 6;
         if (name.contains(".rain")) return 7;
         if (name.contains(".wet_world")) return 8;
         if (name.contains("Wind")) return 9;
         if (name.startsWith("Stars")) return 10;
         return 5;
      }));
      return display;
   }

   private static final int CLIENT_NIGHT_VISION_DURATION = 400;
   private static final int CLIENT_NIGHT_VISION_REFRESH_TICKS = 220;
   private static final double DYNAMIC_UPDATE_DISTANCE_SQUARED = 0.0225;
   private final BooleanSetting customTime = new BooleanSetting(this, "modules.settings.ambience.custom_time", this::hideLighting).enable();
   private final SliderSetting time = new SliderSetting(this, "modules.settings.ambience.time", () -> hideLighting() || !this.customTime.isEnabled())
      .step(500.0F)
      .min(0.0F)
      .max(24000.0F)
      .currentValue(17000.0F);
   private final BooleanSetting nightMode = new BooleanSetting(this, "modules.settings.ambience.night_mode", this::hideLighting);
   public final BooleanSetting bright = new BooleanSetting(this, "modules.settings.ambience.bright", () -> hideLighting() || this.nightMode.isEnabled()).enable();
   private final ModeSetting brightnessMode = new ModeSetting(this, "modules.settings.ambience.mode", () -> hideLighting() || !this.isBrightnessEnabled());
   private final ModeSetting.Value gammaBrightness = new ModeSetting.Value(this.brightnessMode, "modules.settings.ambience.mode.gamma");
   private final ModeSetting.Value effectBrightness = new ModeSetting.Value(this.brightnessMode, "modules.settings.ambience.mode.effect");
   private final ModeSetting.Value dynamicBrightness = new ModeSetting.Value(this.brightnessMode, "modules.settings.ambience.mode.dynamic").select();
   private final SliderSetting dynamicRadius = new SliderSetting(
         this, "modules.settings.ambience.dynamic.radius", () -> hideLighting() || !this.isBrightnessEnabled() || !this.dynamicBrightness.isSelected()
      )
      .min(4.0F)
      .max(12.0F)
      .step(0.5F)
      .currentValue(12.0F);
   private final SliderSetting dynamicLight = new SliderSetting(
         this, "modules.settings.ambience.dynamic.light", () -> hideLighting() || !this.isBrightnessEnabled() || !this.dynamicBrightness.isSelected()
      )
      .min(8.0F)
      .max(15.0F)
      .step(1.0F)
      .currentValue(15.0F);
   private final BooleanSetting dynamicOnlyInCave = new BooleanSetting(
      this, "modules.settings.ambience.dynamic.only_in_cave", () -> hideLighting() || !this.isBrightnessEnabled() || !this.dynamicBrightness.isSelected()
   ).enable();
   private final BooleanSetting nightModeThemeSync = new BooleanSetting(this, "modules.settings.ambience.night_mode.sync", () -> hideLighting() || !this.nightMode.isEnabled())
      .enable();
   private final ColorSetting nightModeColor = new ColorSetting(
         this, "modules.settings.ambience.night_mode.color", () -> hideLighting() || !this.nightMode.isEnabled() || this.nightModeThemeSync.isEnabled()
      )
      .color(new ColorRGBA(80.0F, 120.0F, 220.0F, 255.0F))
      .alpha(false);
   private final SliderSetting nightModeStrength = new SliderSetting(this, "modules.settings.ambience.night_mode.strength", () -> hideLighting() || !this.nightMode.isEnabled())
      .min(0.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(70.0F)
      .suffix("%");
   private final dev.bkclient.utility.render.ambience.AmbienceEffects atmosphere = new dev.bkclient.utility.render.ambience.AmbienceEffects(this, this::hideColors, this::hideWeather);

   private final EventListener<dev.bkclient.systems.event.impl.game.WorldChangeEvent> onAtmosphereWorldChange = event -> {
      dev.bkclient.utility.render.ambience.AmbienceRenderer.clearWorld();
      dev.bkclient.utility.render.ambience.WetRipples.clear();
   };
   private final EventListener<dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent> onWetRippleTick = event ->
      dev.bkclient.utility.render.ambience.WetRipples.tick(atmosphere);

   public boolean hasCustomRain() {
      return isEnabled() && atmosphere.rain.isEnabled();
   }

   public void renderAtmosphere(Matrix4f view, Matrix4f projection, net.minecraft.client.render.Camera camera, float tickDelta) {
      if (isEnabled()) {
         dev.bkclient.utility.render.ambience.AmbienceRenderer.render(atmosphere, view, projection, camera, tickDelta);
      }
   }

   public final dev.bkclient.utility.render.ambience.TerrainStars stars = new dev.bkclient.utility.render.ambience.TerrainStars(this);

   public final dev.bkclient.utility.render.ambience.TerrainWind wind = new dev.bkclient.utility.render.ambience.TerrainWind(this);

   // Server clock to restore once the custom time stops. Written from the network thread.
   private volatile dev.bkclient.utility.render.ambience.AmbienceTime serverTime;
   private net.minecraft.client.world.ClientWorld timeWorld;
   private int[] dynamicLightBlock;
   private int[] dynamicLightSections;
   private int dynamicLightRadius = -1;
   private int dynamicLightLevel = -1;
   private boolean dynamicLightActive;
   private StatusEffectInstance clientNightVision;
   private net.minecraft.client.network.ClientPlayerEntity nightVisionPlayer;
   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket packet && this.customTime.isEnabled()) {
         this.serverTime = new dev.bkclient.utility.render.ambience.AmbienceTime(packet.time(), packet.timeOfDay(), packet.tickDayTime());
         event.cancel();
      }
   };
   private final EventListener<Render3DEvent> onRender3D = event -> this.updateDynamicBrightness();

   public boolean isBrightnessEnabled() {
      return this.bright.isEnabled() && !this.nightMode.isEnabled();
   }

   public boolean shouldUseGammaBrightness() {
      return this.isEnabled() && this.isBrightnessEnabled() && this.gammaBrightness.isSelected();
   }

   public boolean isNightModeActive() {
      return this.isEnabled() && this.nightMode.isEnabled();
   }

   public Vector3f getNightModeTint() {
      ColorRGBA color = this.nightModeThemeSync.isEnabled() ? Colors.getFlatColor() : this.nightModeColor.getColor();
      return new Vector3f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
   }

   public float getNightModeStrengthValue() {
      return this.nightModeStrength.getCurrentValue() / 100.0F;
   }

   @Override
   public void tick() {
      if (mc.world == null) {
         dev.bkclient.utility.render.ambience.AmbienceRenderer.clearWorld();
         this.removeClientNightVision();
         this.clearDynamicBrightness();
         this.forgetTimeOverride();
      } else {
         if (this.timeWorld != null && this.timeWorld != mc.world) {
            // New world (respawn, dimension change): restart the override but keep the last
            // server clock, which dimensions of one server share and the next packet refreshes.
            this.timeWorld = null;
         }

         if (this.customTime.isEnabled()) {
            if (this.timeWorld == null) {
               this.beginTimeOverride();
            }

            mc.world.getLevelProperties().setTimeOfDay((long)this.time.getCurrentValue());
         } else if (this.timeWorld != null) {
            this.restoreServerTime();
         }

         this.updateBrightnessEffect();
         if (!this.shouldUseDynamicBrightness()) {
            this.clearDynamicBrightness();
         }

         super.tick();
      }
   }

   @Override
   public void onDisable() {
      dev.bkclient.utility.render.ambience.AmbienceRenderer.clearWorld();
      this.removeClientNightVision();
      this.clearDynamicBrightness();
      if (EntityUtility.isInGame() && mc.world != null && this.timeWorld != null) {
         this.restoreServerTime();
      } else {
         this.forgetTimeOverride();
      }

      super.onDisable();
   }

   private void beginTimeOverride() {
      this.timeWorld = mc.world;
      if (this.serverTime == null) {
         // No packet yet: the world still holds the server's clock.
         this.serverTime = new dev.bkclient.utility.render.ambience.AmbienceTime(mc.world.getTime(), mc.world.getTimeOfDay(), true);
      }
   }

   private void restoreServerTime() {
      dev.bkclient.utility.render.ambience.AmbienceTime clock = this.serverTime;
      if (clock != null && mc.world != null) {
         long age = mc.world.getTime();
         mc.world.setTime(age, clock.restoredTimeOfDay(age), clock.ticking());
      }

      this.forgetTimeOverride();
   }

   private void forgetTimeOverride() {
      this.timeWorld = null;
      this.serverTime = null;
   }

   private void updateBrightnessEffect() {
      if (this.nightVisionPlayer != mc.player) {
         this.removeClientNightVision();
      }
      if (mc.player != null) {
         if (!this.shouldUseEffectBrightness()) {
            this.removeClientNightVision();
         } else {
            StatusEffectInstance current = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (current == null) {
               StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.NIGHT_VISION, CLIENT_NIGHT_VISION_DURATION, 0, false, false, false);
               mc.player.addStatusEffect(effect);
               this.clientNightVision = effect;
               this.nightVisionPlayer = mc.player;
            } else if (this.isClientNightVision(current) && current.getDuration() <= CLIENT_NIGHT_VISION_REFRESH_TICKS) {
               // Replace only our own effect, without touching potion/beacon effects.
               this.removeClientNightVision();
               this.updateBrightnessEffect();
            }
         }
      }
   }

   private boolean shouldUseEffectBrightness() {
      return this.isEnabled() && this.isBrightnessEnabled() && this.effectBrightness.isSelected();
   }

   private boolean shouldUseDynamicBrightness() {
      return this.isEnabled()
         && this.isBrightnessEnabled()
         && this.dynamicBrightness.isSelected()
         && (!this.dynamicOnlyInCave.isEnabled() || this.isPlayerInCave());
   }

   private boolean isPlayerInCave() {
      if (mc.player != null && mc.world != null) {
         BlockPos pos = BlockPos.ofFloored(mc.player.getPos());
         return !mc.world.isSkyVisible(pos) && mc.world.getLightLevel(LightType.SKY, pos) < 4;
      } else {
         return false;
      }
   }

   private void removeClientNightVision() {
      if (this.nightVisionPlayer != null) {
         StatusEffectInstance current = this.nightVisionPlayer.getStatusEffect(StatusEffects.NIGHT_VISION);
         if (this.isClientNightVision(current)) {
            this.nightVisionPlayer.removeStatusEffect(StatusEffects.NIGHT_VISION);
         }
      }
      this.clientNightVision = null;
      this.nightVisionPlayer = null;
   }

   private boolean isClientNightVision(StatusEffectInstance effect) {
      return effect != null && effect == this.clientNightVision
         && effect.getAmplifier() == 0 && effect.getDuration() <= CLIENT_NIGHT_VISION_DURATION
         && !effect.isAmbient() && !effect.shouldShowParticles() && !effect.shouldShowIcon();
   }

   private void updateDynamicBrightness() {
      if (mc.player != null && mc.world != null && this.shouldUseDynamicBrightness()) {
         Vec3d pos = mc.player.getPos().add(0.0, mc.player.getHeight() * 0.5, 0.0);
         float radius = this.dynamicRadius.getCurrentValue();
         float light = this.dynamicLight.getCurrentValue();
         int radiusCeil = MathHelper.ceil(radius);
         int lightFloor = MathHelper.floor(light);
         // Entities read the exact position every frame; terrain is rebuilt only when the light's
         // block, radius or level changes, since light levels are whole numbers.
         DynamicLightUtility.set(pos.x, pos.y, pos.z, radius, light);
         int x = MathHelper.floor(pos.x), y = MathHelper.floor(pos.y), z = MathHelper.floor(pos.z);
         if (DynamicBrightnessArea.needsRefresh(this.dynamicLightActive, this.dynamicLightBlock, this.dynamicLightRadius,
               this.dynamicLightLevel, x, y, z, radiusCeil, lightFloor)) {
            int padding = Math.max(1, MathHelper.ceil(radiusCeil / Math.max(light, 1.0F)));
            int[] sections = DynamicBrightnessArea.sectionBounds(x, y, z, radiusCeil, padding);
            this.scheduleDynamicBrightnessSections(DynamicBrightnessArea.union(this.dynamicLightSections, sections));
            this.dynamicLightBlock = new int[]{x, y, z};
            this.dynamicLightSections = sections;
            this.dynamicLightRadius = radiusCeil;
            this.dynamicLightLevel = lightFloor;
            this.dynamicLightActive = true;
         }
      } else {
         this.clearDynamicBrightness();
      }
   }

   private void clearDynamicBrightness() {
      DynamicLightUtility.clear();
      if (this.dynamicLightActive) {
         this.scheduleDynamicBrightnessSections(this.dynamicLightSections);
         this.dynamicLightActive = false;
         this.dynamicLightBlock = null;
         this.dynamicLightSections = null;
         this.dynamicLightRadius = -1;
         this.dynamicLightLevel = -1;
      }
   }

   private void scheduleDynamicBrightnessSections(int[] sections) {
      if (sections != null && mc.worldRenderer != null) {
         mc.worldRenderer.scheduleChunkRenders(sections[0], sections[1], sections[2], sections[3], sections[4], sections[5]);
      }
   }

   @Generated
   public BooleanSetting getCustomTime() {
      return this.customTime;
   }

   @Generated
   public SliderSetting getTime() {
      return this.time;
   }

   @Generated
   public BooleanSetting getNightMode() {
      return this.nightMode;
   }

   @Generated
   public BooleanSetting getBright() {
      return this.bright;
   }

   @Generated
   public ModeSetting getBrightnessMode() {
      return this.brightnessMode;
   }

   @Generated
   public SliderSetting getDynamicRadius() {
      return this.dynamicRadius;
   }

   @Generated
   public SliderSetting getDynamicLight() {
      return this.dynamicLight;
   }

   @Generated
   public BooleanSetting getDynamicOnlyInCave() {
      return this.dynamicOnlyInCave;
   }

   @Generated
   public BooleanSetting getNightModeThemeSync() {
      return this.nightModeThemeSync;
   }

   @Generated
   public ColorSetting getNightModeColor() {
      return this.nightModeColor;
   }

   @Generated
   public SliderSetting getNightModeStrength() {
      return this.nightModeStrength;
   }

   @Generated
   public EventListener<ReceivePacketEvent> getOnReceivePacket() {
      return this.onReceivePacket;
   }

   @Generated
   public EventListener<Render3DEvent> getOnRender3D() {
      return this.onRender3D;
   }
}
