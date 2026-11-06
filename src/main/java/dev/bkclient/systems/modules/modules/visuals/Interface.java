package dev.bkclient.systems.modules.modules.visuals;

import lombok.Generated;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.GameTickEvent;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.localization.Language;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.theme.Theme;
import dev.bkclient.utility.colors.ColorRGBA;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@ModuleInfo(name = "Interface", category = ModuleCategory.VISUALS, enabledByDefault = true)
public class Interface extends BaseModule {
   private static final float CS_MENU_LAYOUT_W = 690.0F;
   private static final float CS_MENU_LAYOUT_H = 335.0F;

   private static final class ThemeAccentSetting extends ColorSetting {
      private ThemeAccentSetting(Interface parent) {
         super(parent, "modules.settings.interface.accentColor");
      }

      @Override
      public ColorRGBA getColor() {
         try {
            return BKClientInit.getInstance().getThemeManager().getCurrentTheme().getAccentColor();
         } catch (Exception ignored) {
            return new ColorRGBA(20.0F, 100.0F, 255.0F);
         }
      }

      @Override
      public void setColor(ColorRGBA color) {
         if (color == null) return;
         Theme.DARK.setAccentColor(color.withAlpha(255.0F));
         Theme.LIGHT.setAccentColor(color.withAlpha(255.0F));
      }

      @Override
      public JsonElement save() {
         ColorRGBA color = this.getColor();
         JsonObject json = new JsonObject();
         json.addProperty("r", color.getRed());
         json.addProperty("g", color.getGreen());
         json.addProperty("b", color.getBlue());
         json.addProperty("a", color.getAlpha());
         return json;
      }

      @Override
      public void load(JsonElement element) {
         super.load(element);
         this.setColor(super.getColor());
      }
   }

   private static final class ClientLanguageSetting extends ModeSetting {
      private final Interface owner;

      private ClientLanguageSetting(Interface parent) {
         super(parent, "modules.settings.interface.language");
         this.owner = parent;
      }

      @Override
      public void setValue(ModeSetting.Value value) {
         super.setValue(value);
         this.owner.updateLanguage();
      }

      @Override
      public void load(JsonElement element) {
         String saved = element.getAsString();
         String migrated = saved.equalsIgnoreCase("Polish") || saved.equalsIgnoreCase("polski")
            ? "Polish"
            : "English";
         for (ModeSetting.Value value : this.getValues()) {
            if (value.getName().equalsIgnoreCase(migrated)) {
               this.setValue(value);
               return;
            }
         }
      }
   }

   private final ModeSetting themeMode = new ModeSetting(this, "modules.settings.interface.theme");
   public final ModeSetting.Value dark = new ModeSetting.Value(this.themeMode, "modules.settings.interface.dark");
    public final ModeSetting.Value light = new ModeSetting.Value(this.themeMode, "modules.settings.interface.light");
   private final ColorSetting accentColor = new ThemeAccentSetting(this);
   private final BooleanSetting hudAccentGlow = new BooleanSetting(this, "theme.hud_accent_glow").enable();

   public BooleanSetting getHudAccentGlow() { return this.hudAccentGlow; }

   public static boolean hudAccentGlow() { return module().hudAccentGlow.isEnabled(); }
   private final SliderSetting glassBlur = new SliderSetting(this, "theme.blur_strength")
       .min(0.0F)
        .max(3.0F)
       .step(0.1F)
       .currentValue(0.6F);
   private final SliderSetting menuScale = new SliderSetting(this, "quick.menu_scale", () -> true)
      .min(0.0F)
      .max(130.0F)
      .step(10.0F)
      .currentValue(100.0F);
   private final ModeSetting renderQuality = new ModeSetting(this, "quick.render_quality", () -> true);
   private final ModeSetting.Value qualityBest = new ModeSetting.Value(this.renderQuality, "Best");
   private final ModeSetting.Value qualityBalanced = new ModeSetting.Value(this.renderQuality, "Balanced");
   private final ModeSetting.Value qualityPerformance = new ModeSetting.Value(this.renderQuality, "Performance");
   private final BooleanSetting sync = new BooleanSetting(this, "quick.sync", () -> true);
   private final BooleanSetting discordRpc = new BooleanSetting(this, "quick.discord_rpc", () -> true);
   private final ModeSetting language = new ClientLanguageSetting(this);
   private ModeSetting.Value lastLangValue;
   private final EventListener<HudRenderEvent> onHudRenderEvent = event -> {
      this.updateLanguage();
      BKClientInit.getInstance().getThemeManager().setCurrentTheme(this.dark.isSelected() ? Theme.DARK : Theme.LIGHT);
   };

   private final EventListener<GameTickEvent> onTickEvent = event -> this.updateLanguage();

   private Language valueToLanguage(ModeSetting.Value v) {
      String n = v.getName();
      if ("English".equals(n)) return Language.EN_US;
      if ("Polish".equalsIgnoreCase(n) || "polski".equalsIgnoreCase(n)) return Language.PL_PL;
      return Language.EN_US;
   }

   private void updateLanguage() {
      ModeSetting.Value current = this.language.getValue();
      if (current == this.lastLangValue) return;
      this.lastLangValue = current;
      Localizator.setLanguage(this.valueToLanguage(current));
      dev.bkclient.ui.clicksys.port.concurrent.Translations.getInstance().update();
   }

   public void toggleLanguage() {
      String next = this.valueToLanguage(this.language.getValue()) == Language.PL_PL ? "English" : "Polish";
      for (ModeSetting.Value value : this.language.getValues()) {
         if (next.equals(value.getName())) {
            value.select();
            return;
         }
      }
   }

   public Interface() {
      this.setHidden(true);
      this.dark.select();
      new ModeSetting.Value(this.language, "English").select();
      new ModeSetting.Value(this.language, "Polish");
   }

   public static boolean glassSelected() {
      return true;
   }

   public static boolean customChatEnabled() {
      try {
         return BKClientInit.getInstance().getModuleManager().getModule(CustomChatModule.class).isEnabled();
      } catch (Exception ignored) {
         return true;
      }
   }

    public static float glass() {
       return 1.0F;
    }

   public static float minimalizm() {
      return 0.0F;
   }

   public static boolean showGlass() {
      return true;
   }

   public static boolean showMinimalizm() {
      return false;
   }

   private static Interface module() {
      return BKClientInit.getInstance().getModuleManager().getModule(Interface.class);
   }

    public static float glassDistortion() {
        return 0.3F * renderQualityStrength();
    }

   public static float glassBlur() {
      try {
         return module().glassBlur.getCurrentValue();
      } catch (Exception e) {
         return 0.5F;
      }
   }

   public static boolean backgroundBlurEnabled() { return true; }

   public static boolean popTiltMotion() { return false; }

   public static float navigationScale() { return 1.0F; }

    public static float glassGlare() {
       return 0.2F * renderQualityStrength();
    }

    public static float glassChromatic() {
        return 0.05F * renderQualityStrength();
    }

   public static float menuScale() {
      try {
         float value = module().menuScale.getCurrentValue();
         float fit = 1.0F;
         if (mc != null && mc.getWindow() != null) {
            float screenWidth = Math.max(1.0F, mc.getWindow().getScaledWidth() - 24.0F);
            float screenHeight = Math.max(1.0F, mc.getWindow().getScaledHeight() - 24.0F);
            fit = Math.min(screenWidth / 465.0F, screenHeight / 320.0F);
         }
         if (value <= 0.0F) {
            float percent = Math.round(fit * 10.0F) * 10.0F;
            return Math.clamp(percent, 60.0F, 130.0F) / 100.0F;
         }
         return Math.min(value / 100.0F, Math.max(0.45F, fit));
      } catch (Exception ignored) {
         return 1.0F;
      }
   }

   public static float menuScaleMultiplier() {
      try {
         float value = module().menuScale.getCurrentValue();
         float fit = 1.0F;
         if (mc != null && mc.getWindow() != null) {
            float screenWidth = Math.max(1.0F, mc.getWindow().getScaledWidth() - 24.0F);
            float screenHeight = Math.max(1.0F, mc.getWindow().getScaledHeight() - 24.0F);
            fit = Math.min(screenWidth / CS_MENU_LAYOUT_W, screenHeight / CS_MENU_LAYOUT_H);
         }
         if (value <= 0.0F) {
            float percent = Math.round(fit * 10.0F) * 10.0F;
            return Math.clamp(percent, 60.0F, 130.0F) / 100.0F;
         }
         return Math.min(value / 100.0F, Math.max(0.45F, fit));
      } catch (Exception ignored) {
         return 1.0F;
      }
   }

   public static boolean blurGui() {
      return backgroundBlurEnabled() && renderQualityStrength() > 0.25F;
   }

   public static boolean blurHud() {
      return backgroundBlurEnabled() && renderQualityStrength() > 0.65F;
   }

   public static boolean shadow() {
      return renderQualityStrength() > 0.25F;
   }

   public static long animDuration(long full) {
      float quality = renderQualityStrength();
      if (quality <= 0.25F) return Math.max(1L, full / 2L);
      return full;
   }

   public static float renderQualityStrength() {
      try {
         Interface module = module();
         if (module.qualityPerformance.isSelected()) return 0.25F;
         if (module.qualityBalanced.isSelected()) return 0.65F;
      } catch (Exception ignored) {
      }
      return 1.0F;
   }

   @Generated
   public ModeSetting getThemeMode() {
      return this.themeMode;
   }

   @Generated
   public ModeSetting.Value getDark() {
      return this.dark;
   }

   @Generated
   public ModeSetting.Value getLight() {
      return this.light;
   }

   @Generated
   public ModeSetting getLanguage() {
      return this.language;
   }

   @Generated
   public SliderSetting getMenuScale() {
      return this.menuScale;
   }

   @Generated
   public SliderSetting getGlassBlur() {
      return this.glassBlur;
   }

   public ColorSetting getAccentColor() {
      return this.accentColor;
   }

   @Generated
   public ModeSetting getRenderQuality() {
      return this.renderQuality;
   }

   @Generated
   public BooleanSetting getSync() {
      return this.sync;
   }

   @Generated
   public BooleanSetting getDiscordRpc() {
      return this.discordRpc;
   }



   @Generated
    public ModeSetting.Value getLastLangValue() {
       return this.lastLangValue;
    }

   @Generated
   public EventListener<HudRenderEvent> getOnHudRenderEvent() {
      return this.onHudRenderEvent;
   }
}
