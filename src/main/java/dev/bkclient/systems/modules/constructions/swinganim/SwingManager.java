package dev.bkclient.systems.modules.constructions.swinganim;

import java.util.ArrayList;
import java.util.List;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.systems.modules.constructions.swinganim.presets.SwingPreset;
import dev.bkclient.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import dev.bkclient.systems.setting.settings.BezierSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.math.MathUtility;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public class SwingManager {
   private final List<SwingPreset> presets = new ArrayList<>();
   private String current = "autosave";
   private final SwingSettings sharedSettings = new SwingSettings();
   private final SwingPhase startPhase = new SwingPhase();
   private final SwingPhase endPhase = new SwingPhase();
   private final BezierSetting bezier = new BezierSetting(this.sharedSettings, "animation").start(0.5F, 1.0F).end(0.5F, 0.0F);
   private final BooleanSetting back = new BooleanSetting(this.sharedSettings, "swing.back").enable();
   private final SliderSetting speed = new SliderSetting(this.sharedSettings, "swing.wing_speed").step(0.5F).min(1.0F).max(5.0F).currentValue(2.0F);

   private void initialize() {
      this.presets
         .add(
            new SwingPreset(
               "swings.block_hit",
               new Vec2f(0.5F, 1.0F),
               new Vec2f(0.5F, 0.0F),
               true,
               2.0F,
               new SwingTransformations(0.0F, -0.05F, -0.7F, 1.0500001F, -0.7F, -1.1F, -120.0F, -135.0F, -60.0F),
               new SwingTransformations(0.0F, -0.05F, -0.7F, 1.0500001F, -0.7F, -1.1F, -120.0F, -180.0F, -60.0F)
            )
         );
      this.presets
         .add(
            new SwingPreset(
               "swings.bonk",
               new Vec2f(0.40131578F, 0.53543305F),
               new Vec2f(0.0F, -0.24409449F),
               true,
               2.0F,
               new SwingTransformations(0.0F, -0.4F, -0.65000004F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
               new SwingTransformations(0.0F, -0.4F, -0.65000004F, 0.0F, 0.0F, 0.0F, -45.0F, 0.0F, 0.0F)
            )
         );
      this.presets
         .add(
            new SwingPreset(
               "swings.rotate_360",
               new Vec2f(0.43421054F, 0.61417323F),
               new Vec2f(0.04605263F, -0.26771653F),
               false,
               2.0F,
               new SwingTransformations(0.0F, -0.4F, -0.65000004F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
               new SwingTransformations(0.0F, -0.4F, -0.65000004F, 0.0F, 0.0F, 0.0F, -360.0F, 0.0F, 0.0F)
            )
         );
      this.presets
         .add(
            new SwingPreset(
               "swings.from_me",
               new Vec2f(0.42105263F, 0.87401575F),
               new Vec2f(0.3881579F, -0.4566929F),
               true,
               2.0F,
               new SwingTransformations(0.0F, 0.0F, -1.1F, 0.2F, 0.0F, -0.1F, -135.0F, 45.0F, 60.0F),
               new SwingTransformations(0.0F, 0.0F, -1.1F, 0.2F, 0.0F, -0.3F, -180.0F, 45.0F, 60.0F)
            )
         );
   }

   public SwingManager() {
      this.initialize();
      this.applyPreset(this.presets.getFirst());
   }

   public void applyPreset(SwingPreset preset) {
      bezier.start(preset.getBezierStart()).end(preset.getBezierEnd());
      back.enabled(preset.isSwingBack());
      speed.setCurrentValue(preset.getSpeed());
      startPhase.restore(preset.getFrom().values());
      endPhase.restore(preset.getTo().values());
      current = preset.getName();
      SwingPresetManager files = BKClientInit.getInstance().getSwingPresetManager();
      if (files != null) files.setCurrent(null);
   }

   public float[] snapshot() {
      float[] state = new float[24];
      System.arraycopy(startPhase.values(), 0, state, 0, 9);
      System.arraycopy(endPhase.values(), 0, state, 9, 9);
      state[18] = bezier.start().x; state[19] = bezier.start().y;
      state[20] = bezier.end().x; state[21] = bezier.end().y;
      state[22] = back.isEnabled() ? 1 : 0; state[23] = speed.getCurrentValue();
      return state;
   }

   public void restore(float[] state) {
      if (state.length != 24) throw new IllegalArgumentException("A swing snapshot needs 24 values");
      startPhase.restore(java.util.Arrays.copyOfRange(state, 0, 9));
      endPhase.restore(java.util.Arrays.copyOfRange(state, 9, 18));
      bezier.start(state[18], state[19]).end(state[20], state[21]);
      back.enabled(state[22] != 0);
      speed.setCurrentValue(state[23]);
   }

   public SwingTransformations transformations(float progress) {
      progress = this.bezier.easing().ease(progress, 0.0F, 1.0F, 1.0F);
      if (this.back.isEnabled()) {
         progress = MathHelper.sin(MathHelper.sqrt(Math.max(0.0F, progress)) * (float) Math.PI);
      }

      return new SwingTransformations(
         this.get(this.startPhase.getAnchorX(), this.endPhase.getAnchorX(), progress),
         this.get(this.startPhase.getAnchorY(), this.endPhase.getAnchorY(), progress),
         this.get(this.startPhase.getAnchorZ(), this.endPhase.getAnchorZ(), progress),
         this.get(this.startPhase.getMoveX(), this.endPhase.getMoveX(), progress),
         this.get(this.startPhase.getMoveY(), this.endPhase.getMoveY(), progress),
         this.get(this.startPhase.getMoveZ(), this.endPhase.getMoveZ(), progress),
         this.get(this.startPhase.getRotateX(), this.endPhase.getRotateX(), progress),
         this.get(this.startPhase.getRotateY(), this.endPhase.getRotateY(), progress),
         this.get(this.startPhase.getRotateZ(), this.endPhase.getRotateZ(), progress)
      );
   }

   private float get(SliderSetting start, SliderSetting end, float progress) {
      return MathUtility.interpolate(start.getCurrentValue(), end.getCurrentValue(), progress);
   }

   public String getCurrent() {
      SwingPresetManager manager = BKClientInit.getInstance().getSwingPresetManager();
      return manager.getCurrent() != null ? manager.getCurrent().getFileName() : this.current;
   }

   @Generated
   public List<SwingPreset> getPresets() {
      return this.presets;
   }

   @Generated
   public SwingSettings getSharedSettings() {
      return this.sharedSettings;
   }

   @Generated
   public SwingPhase getStartPhase() {
      return this.startPhase;
   }

   @Generated
   public SwingPhase getEndPhase() {
      return this.endPhase;
   }

   @Generated
   public BezierSetting getBezier() {
      return this.bezier;
   }

   @Generated
   public BooleanSetting getBack() {
      return this.back;
   }

   @Generated
   public SliderSetting getSpeed() {
      return this.speed;
   }

   @Generated
   public void setCurrent(String current) {
      this.current = current;
   }
}
