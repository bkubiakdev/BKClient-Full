package dev.bkclient.systems.modules.modules.visuals;

import lombok.Generated;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.shader.impl.SkyShaderProgram;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.LinkedModeSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.DrawUtility;
import net.minecraft.util.Identifier;

@ModuleInfo(name = "Sky", category = ModuleCategory.VISUALS, desc = "Sky effects, stars, aurora and storms")
public class Sky extends BaseModule {
    public Sky() {
        LinkedModeSetting.toggle(this, "color.source", this.themeSync, "Theme", "Custom");
    }
   private final SelectSetting changeColor = new SelectSetting(this, "modules.settings.ambience.change_color");
   private final SelectSetting.Value skyColorTarget = new SelectSetting.Value(this.changeColor, "modules.settings.ambience.change_color.sky").select();
   private final SelectSetting.Value cloudColorTarget = new SelectSetting.Value(this.changeColor, "modules.settings.ambience.change_color.clouds").select();
   private final SelectSetting.Value starsColorTarget = new SelectSetting.Value(this.changeColor, "modules.settings.ambience.change_color.stars").select();
   private final BooleanSetting themeSync = new BooleanSetting(this, "theme.sync").enable();
   private final ColorSetting skyColor = new ColorSetting(
         this, "modules.settings.ambience.sky_color", () -> this.themeSync.isEnabled() || !this.skyColorTarget.isSelected()
      )
      .color(Colors.ACCENT)
      .alpha(false);
   private final ColorSetting cloudColor = new ColorSetting(
         this, "modules.settings.ambience.cloud_color", () -> this.themeSync.isEnabled() || !this.cloudColorTarget.isSelected()
      )
      .color(Colors.ACCENT)
      .alpha(false);
   private final ColorSetting starsColor = new ColorSetting(
         this, "modules.settings.ambience.stars_color", () -> this.themeSync.isEnabled() || !this.starsColorTarget.isSelected()
      )
      .color(Colors.ACCENT);
   private final ModeSetting skybox = new ModeSetting(this, "modules.settings.ambience.skybox");
   private final ModeSetting.Value defaultSkybox = new ModeSetting.Value(this.skybox, "modules.settings.ambience.skybox.default");
   private final ModeSetting.Value brightCloudsSkybox = new ModeSetting.Value(this.skybox, "modules.settings.ambience.skybox.bright_clouds").select();
   private final ModeSetting.Value lakeSkybox = new ModeSetting.Value(this.skybox, "modules.settings.ambience.skybox.lake");
   private final ModeSetting.Value cloudSpaceSkybox = new ModeSetting.Value(this.skybox, "modules.settings.ambience.skybox.cloud_space");
   private final ModeSetting.Value clearEveningSkybox = new ModeSetting.Value(this.skybox, "modules.settings.ambience.skybox.clear_evening");
   private final ModeSetting.Value underwaterSkybox = new ModeSetting.Value(this.skybox, "modules.settings.ambience.skybox.underwater");
   private final ModeSetting.Value effectNebula = new ModeSetting.Value(this.skybox, "Nebula");
   private final ModeSetting.Value effectAurora = new ModeSetting.Value(this.skybox, "Aurora");
   private final ModeSetting.Value effectStars = new ModeSetting.Value(this.skybox, "Classic Stars");
   private final ModeSetting.Value effectPlasma = new ModeSetting.Value(this.skybox, "Plasma");
   private final ModeSetting.Value effectNeon = new ModeSetting.Value(this.skybox, "Neon");
   private final ModeSetting.Value effectOvercast = new ModeSetting.Value(this.skybox, "Overcast");
   private final ModeSetting.Value effectStarfall = new ModeSetting.Value(this.skybox, "Starfall");
   private final ModeSetting.Value effectCurtains = new ModeSetting.Value(this.skybox, "Aurora Curtains");
   private final ModeSetting.Value stars = new ModeSetting.Value(this.skybox, "Stars");
   private final ModeSetting.Value effectCyberspace = new ModeSetting.Value(this.skybox, "Cyberspace");
   private final BooleanSetting shader = new BooleanSetting(this, "modules.settings.ambience.shader", () -> getSkyEffectMode() != null);
   private final ModeSetting shaderType = new ModeSetting(this, "modules.settings.ambience.shader_type", () -> !this.shader.isEnabled() || getSkyEffectMode() != null);
   private final ModeSetting.Value nebulaShader = new ModeSetting.Value(this.shaderType, "modules.settings.ambience.shader_type.nebula").select();
   private final ModeSetting.Value causticShader = new ModeSetting.Value(this.shaderType, "modules.settings.ambience.shader_type.caustic");
   private final ModeSetting.Value galaxyShader = new ModeSetting.Value(this.shaderType, "modules.settings.ambience.shader_type.galaxy");
   private final ModeSetting.Value spaceShader = new ModeSetting.Value(this.shaderType, "modules.settings.ambience.shader_type.space");
   private final ModeSetting.Value radiantShader = new ModeSetting.Value(this.shaderType, "Cyber Radiant");
   private final ModeSetting.Value sunsetShader = new ModeSetting.Value(this.shaderType, "Sunset Godrays");
   private final SliderSetting shaderSpeed = new SliderSetting(this, "Shader Speed", () -> !this.shader.isEnabled() && getSkyEffectMode() == null)
      .min(0.1F)
      .max(5.0F)
      .step(0.1F)
      .currentValue(1.0F);
   private final SliderSetting shaderOpacity = new SliderSetting(this, "modules.settings.ambience.shader_opacity", () -> !this.shader.isEnabled() && getSkyEffectMode() == null)
      .min(0.0F)
      .max(100.0F)
      .step(5.0F)
      .currentValue(70.0F)
      .suffix("%");
   private final dev.bkclient.utility.render.ambience.SkyEffectsRenderer skyEffects = new dev.bkclient.utility.render.ambience.SkyEffectsRenderer(this);
   public String getSkyEffectMode() {
      if (effectCyberspace.isSelected()) return "Cyberspace";
      if (stars.isSelected()) return "Stars";
      if (effectOvercast.isSelected()) return "Overcast";
      if (effectStarfall.isSelected()) return "Starfall";
      if (effectCurtains.isSelected()) return "Aurora Curtains";
      if (effectNebula.isSelected()) return "Nebula";
      if (effectAurora.isSelected()) return "Aurora";
      if (effectStars.isSelected()) return "Classic Stars";
      if (effectPlasma.isSelected()) return "Plasma";
      if (effectNeon.isSelected()) return "Neon";
      return null;
   }

   public boolean renderSkyEffect() {
      return isEnabled() && getSkyEffectMode() != null && skyEffects.render(getSkyEffectMode(), getShaderSpeed(), getShaderOpacity());
   }

   public boolean hasCustomSkybox() {
      return getSkyEffectMode() == null && !this.skybox.is(this.defaultSkybox);
   }

   public boolean hasShaderSkybox() {
      SkyShaderProgram program = this.getSkyShaderProgram();
      return getSkyEffectMode() == null && this.shader.isEnabled() && program != null && program.isReady();
   }

   public boolean shouldRenderCustomSkybox() {
      return this.isEnabled() && (this.hasCustomSkybox() || this.hasShaderSkybox());
   }

   public boolean shouldTintSky() {
      return this.isEnabled() && this.skyColorTarget.isSelected();
   }

   public boolean shouldTintClouds() {
      return this.isEnabled() && this.cloudColorTarget.isSelected();
   }

   public boolean shouldTintStars() {
      return this.isEnabled() && this.starsColorTarget.isSelected();
   }

   public ColorRGBA getResolvedSkyColor() {
      return this.themeSync.isEnabled() ? Colors.getFlatColor() : this.skyColor.getColor();
   }

   public ColorRGBA getResolvedCloudColor() {
      return this.themeSync.isEnabled() ? Colors.getFlatColor() : this.cloudColor.getColor();
   }

   public ColorRGBA getResolvedStarsColor() {
      return this.themeSync.isEnabled() ? Colors.getFlatColor() : this.starsColor.getColor();
   }

   public Identifier getSkyboxTexture() {
      int index = Math.max(this.skybox.getValues().indexOf(this.skybox.getValue()), 1);
      return BKClientInit.id("sky/" + index + ".png");
   }

   public SkyShaderProgram getSkyShaderProgram() {
      if (this.galaxyShader.isSelected()) return DrawUtility.skyGalaxyProgram;
      if (this.spaceShader.isSelected()) return DrawUtility.skySpaceProgram;
      if (this.causticShader.isSelected()) return DrawUtility.skyCausticProgram;
      if (this.radiantShader.isSelected()) return DrawUtility.skyRadiantProgram;
      if (this.sunsetShader.isSelected()) return DrawUtility.skySunsetProgram;
      return DrawUtility.skyNebulaProgram;
   }

   public float getShaderSpeed() {
      return this.shaderSpeed.getCurrentValue();
   }

   public float getShaderOpacity() {
      return this.shaderOpacity.getCurrentValue() / 100.0F;
   }

   @Generated
   public SelectSetting getChangeColor() {
      return this.changeColor;
   }

   @Generated
   public SelectSetting.Value getSkyColorTarget() {
      return this.skyColorTarget;
   }

   @Generated
   public SelectSetting.Value getCloudColorTarget() {
      return this.cloudColorTarget;
   }

   @Generated
   public SelectSetting.Value getStarsColorTarget() {
      return this.starsColorTarget;
   }

   @Generated
   public BooleanSetting getThemeSync() {
      return this.themeSync;
   }

   @Generated
   public ColorSetting getSkyColor() {
      return this.skyColor;
   }

   @Generated
   public ColorSetting getCloudColor() {
      return this.cloudColor;
   }

   @Generated
   public ColorSetting getStarsColor() {
      return this.starsColor;
   }

   @Generated
   public ModeSetting getSkybox() {
      return this.skybox;
   }

   @Generated
   public BooleanSetting getShader() {
      return this.shader;
   }

   @Generated
   public ModeSetting getShaderType() {
      return this.shaderType;
   }

   @Generated
   public SliderSetting getShaderOpacitySetting() {
      return this.shaderOpacity;
   }


    private final EventListener<dev.bkclient.systems.event.impl.render.Render3DEvent> onWorldRender = event -> {
        World world = BKClientInit.getInstance().getModuleManager().getModule(World.class);
        skyEffects.renderWorldStreaks(event, isEnabled() && "Cyberspace".equals(getSkyEffectMode())
            && (world == null || !world.rendersShootingStars()), getShaderSpeed(), getShaderOpacity());
    };

    public void onWorldChanged() { skyEffects.reset(); }
    private final EventListener<dev.bkclient.systems.event.impl.game.WorldChangeEvent> onWorldChange = event -> onWorldChanged();
    public void renderWorldFlash() {
        if (isEnabled() && "Overcast".equals(getSkyEffectMode())) skyEffects.renderWorldFlash();
    }
    @Override public void onEnable() { skyEffects.reset(); super.onEnable(); }
    @Override public void onDisable() { skyEffects.reset(); super.onDisable(); }
}
