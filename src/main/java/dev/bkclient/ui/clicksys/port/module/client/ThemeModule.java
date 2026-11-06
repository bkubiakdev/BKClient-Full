package dev.bkclient.ui.clicksys.port.module.client;

import dev.bkclient.ui.clicksys.port.data.ChoiceOption;
import dev.bkclient.ui.clicksys.port.data.Palette;
import dev.bkclient.ui.clicksys.port.data.ThemeConfig;
import dev.bkclient.ui.clicksys.port.enums.ThemePalette;
import dev.bkclient.ui.clicksys.port.module.Category;
import dev.bkclient.ui.clicksys.port.module.Module;
import dev.bkclient.ui.clicksys.port.setting.BooleanSetting;
import dev.bkclient.ui.clicksys.port.setting.ChoiceSetting;
import dev.bkclient.ui.clicksys.port.setting.ColorSetting;
import dev.bkclient.ui.clicksys.port.setting.CompactGroupSetting;
import dev.bkclient.ui.clicksys.port.setting.ListSetting;
import dev.bkclient.ui.clicksys.port.setting.Setting;
import dev.bkclient.ui.clicksys.port.setting.SliderSetting;
import dev.bkclient.ui.clicksys.port.util.MathUtil;
import dev.bkclient.ui.clicksys.port.util.UnsafeAccess;
import java.util.List;
import java.util.function.Supplier;

public class ThemeModule extends Module {
   public final BooleanSetting liquidPanels = new BooleanSetting("Liquid panels", "BKClient liquid glass for clicksys windows only.", false);
   public final SliderSetting liquidOpacity = new SliderSetting("Panel opacity", "Dark tint behind text. Higher values improve contrast.", .62, .25, .95, .01);
   public final SliderSetting liquidRefraction = new SliderSetting("Refraction", "Distortion of the scene behind clicksys windows.", .045, 0, .15, .005);
   private static final List<String> list = List.of("Matte", "Poster", "Pixels", "Waves", "Glow", "Sheen", "Grain", "Cursor light", "Cursor inertia", "Reveal", "Tinted rim", "Scene rim", "Prism", "Thickness", "Under glow", "Aurora");
   private static final List<String> list2 = List.of("Background", "Surfaces", "Text", "Borders");
   public final SliderSetting tintAmount = new SliderSetting("Glass tint amount", "Source tint range.",.02,0,.05,.001);
   public final BooleanSetting glass = new BooleanSetting("Glass enabled", "Sample the scene behind GUI surfaces.", true);
   public final SliderSetting guiBlurRadius = new SliderSetting("GUI blur radius", "Background blur while the GUI is open.",18,0,64,1);
   public final SliderSetting guiDim = new SliderSetting("GUI dim", "Darken the background behind the GUI.",35,0,100,5);
   public final SliderSetting cursorSize = new SliderSetting("Cursor size", "Radius of cursor illumination.",1,.3,3,.05);
   public final SliderSetting cursorStrength = new SliderSetting("Cursor strength", "Brightness of cursor illumination.",1,.2,3,.05);
   public final SliderSetting revealRadius = new SliderSetting("Reveal radius", "Radius of the cursor reveal.",1,.3,3,.05);
   public final SliderSetting revealStrength = new SliderSetting("Reveal strength", "How much the cursor reveals the scene.",1,.2,2,.05);
   public final ListSetting colorMode = new ListSetting("Color mode", "GUI accent palette.",
      List.of("Default","Custom","Bluepink","Polarize","Water","Lilac","Spring","Winter","Midnight","Earth","Forest","Galaxy"),List.of("Default"),false);
   public final BooleanSetting gradient = new BooleanSetting("Accent gradient", "Blend the two accent colors across GUI surfaces.",false);
   public final ColorSetting accentTo = new ColorSetting("Accent end", "Second gradient color.",-9814308);
   public final SliderSetting gradientAngle = new SliderSetting("Gradient angle", "Direction of the accent gradient.",45,0,360,5);
   private Runnable persist = () -> {};
   private boolean loadingSettings;
   private static boolean flag;
   private BooleanSetting kromeGui;
   private ChoiceSetting modeOtobrazheniya;
   private SliderSetting intensivnost;
   private SliderSetting iskazhenie;
   private SliderSetting hromatika;
   private SliderSetting blyur;
   private ColorSetting colorTinta;
   private SliderSetting strengthTinta;
   private ListSetting effects;
   private SliderSetting strengthMatovosti;
   private SliderSetting strengthPosterizacii;
   private SliderSetting sizePikseley;
   private SliderSetting strengthVoln;
   private SliderSetting speedVoln;
   private SliderSetting chastotaVoln;
   private SliderSetting strengthSvecheniya;
   private SliderSetting sizeSvecheniya;
   private ColorSetting colorSvecheniya;
   private SliderSetting sizeGui;
   private BooleanSetting guiBlyur;
   private BooleanSetting svoyAkcent;
   private ColorSetting akcent;
   private ListSetting gdePrimenyatAkcent;
   private static final UnsafeAccess<ThemeModule> unsafeAccess = new UnsafeAccess<>(ThemeModule.class);

   public ThemeModule() {
      super("Appearance", Category.CLIENT);
      BooleanSetting booleansetting = new BooleanSetting("", "", true);
      booleansetting.setName("Exclude GUI");
      booleansetting.setDescription("Legacy setting retained for configuration compatibility.");
      booleansetting.setVisibleWhen(() -> false);
      this.kromeGui = booleansetting;
      ChoiceSetting choicesetting = new ChoiceSetting("", "", new ChoiceOption("Regular"), new ChoiceOption("Transparent"), true);
      choicesetting.setName("Display Mode");
      choicesetting.setDescription("Set the rendering style for GUI elements only.");
      this.modeOtobrazheniya = choicesetting;
      SliderSetting slidersetting = new SliderSetting("", "", 1.0, 0.0, 1.0, 0.05);
      slidersetting.setName("Intensity");
      slidersetting.setDescription("How clearly does the glass effect appear on interface elements?");
      this.intensivnost = slidersetting;
      SliderSetting slidersetting1 = new SliderSetting("", "", 1.0, 0.0, 3.0, 0.05);
      slidersetting1.setName("Distortion");
      slidersetting1.setDescription("The force with which the picture behind the glass bends and floats.");
      this.iskazhenie = slidersetting1;
      SliderSetting slidersetting2 = new SliderSetting("", "", 1.0, 0.0, 3.0, 0.05);
      slidersetting2.setName("Chromatics");
      slidersetting2.setDescription("Light iridescent edging on the edges, like real optics.");
      this.hromatika = slidersetting2;
      SliderSetting slidersetting3 = new SliderSetting("", "", 0.4, 0.0, 0.8, 0.1);
      slidersetting3.setName("Blur");
      slidersetting3.setDescription("How blurry the background behind the interface elements becomes.");
      this.blyur = slidersetting3;
      ColorSetting colorsetting = new ColorSetting("", "", -3151361);
      colorsetting.setName("Tint color");
      colorsetting.setDescription("The color to which glass is slightly tinted.");
      this.colorTinta = colorsetting;
      SliderSetting slidersetting4 = new SliderSetting("", "", 0.15, 0.0, 1.0, 0.05);
      slidersetting4.setName("Tint strength");
      slidersetting4.setDescription("How strongly the chosen color stains the glass.");
      this.strengthTinta = slidersetting4;
      ListSetting listsetting = new ListSetting("", "", list, List.of(), true);
      listsetting.setName("Effects");
      listsetting.setDescription("Additional visual effects for glass. Turn on the ones you need.");
      this.effects = listsetting;
      SliderSetting slidersetting5 = new SliderSetting("", "", 0.5, 0.05, 1.0, 0.05);
      slidersetting5.setName("Matte strength");
      slidersetting5.setDescription("Makes glass appear cloudier, like in a bathroom.");
      this.strengthMatovosti = slidersetting5;
      SliderSetting slidersetting6 = new SliderSetting("", "", 0.5, 0.05, 1.0, 0.05);
      slidersetting6.setName("Posterization strength");
      slidersetting6.setDescription("Reduces the number of shades, the picture becomes a poster.");
      this.strengthPosterizacii = slidersetting6;
      SliderSetting slidersetting7 = new SliderSetting("", "", 0.3, 0.05, 1.0, 0.05);
      slidersetting7.setName("Pixel size");
      slidersetting7.setDescription("The higher you go, the larger the squares are visible.");
      this.sizePikseley = slidersetting7;
      SliderSetting slidersetting8 = new SliderSetting("", "", 0.5, 0.05, 1.0, 0.05);
      slidersetting8.setName("Wave strength");
      slidersetting8.setDescription("How much the picture sways, as if under water.");
      this.strengthVoln = slidersetting8;
      SliderSetting slidersetting9 = new SliderSetting("", "", 1.0, 0.0, 3.0, 0.1);
      slidersetting9.setName("Wave speed");
      slidersetting9.setDescription("How quickly the wave-like distortion travels.");
      this.speedVoln = slidersetting9;
      SliderSetting slidersetting10 = new SliderSetting("", "", 0.5, 0.05, 1.0, 0.05);
      slidersetting10.setName("Wave frequency");
      slidersetting10.setDescription("How many waves fit on glass at the same time?");
      this.chastotaVoln = slidersetting10;
      SliderSetting slidersetting11 = new SliderSetting("", "", 0.3, 0.05, 1.0, 0.05);
      slidersetting11.setName("Glow power");
      slidersetting11.setDescription("How brightly the glass glows from the inside at the edges.");
      this.strengthSvecheniya = slidersetting11;
      SliderSetting slidersetting12 = new SliderSetting("", "", 0.5, 0.05, 1.0, 0.05);
      slidersetting12.setName("Glow size");
      slidersetting12.setDescription("How far does the glow spread from the edge inward?");
      this.sizeSvecheniya = slidersetting12;
      ColorSetting colorsetting1 = new ColorSetting("", "", -8401921);
      colorsetting1.setName("Glow color");
      colorsetting1.setDescription("The color of the internal glow at the edges of the glass.");
      this.colorSvecheniya = colorsetting1;
      SliderSetting slidersetting13 = new SliderSetting("", "", 1.0, 0.5, 1.5, 0.05, "x", 2);
      slidersetting13.setName("GUI size");
      slidersetting13.setDescription("General scale of the client window.");
      this.sizeGui = slidersetting13;
      booleansetting = new BooleanSetting("", "", true);
      booleansetting.setName("GUI blur");
      booleansetting.setDescription("Blur the background behind the open client interface.");
      this.guiBlyur = booleansetting;
      booleansetting = new BooleanSetting("", "", false);
      booleansetting.setName("Custom accent");
      booleansetting.setDescription("Use a custom accent shade. Other colors are adjusted automatically.");
      this.svoyAkcent = booleansetting;
      ColorSetting colorsetting2 = new ColorSetting("", "", -2343358);
      colorsetting2.setName("Accent");
      colorsetting2.setDescription("One color - all GUI shades are derived from it automatically.");
      this.akcent = colorsetting2;
      ListSetting listsetting1 = new ListSetting("", "", list2, list2, true);
      listsetting1.setName("Accent targets");
      listsetting1.setDescription("What does its shade apply to? Unchecking the box leaves the group in the colors of the base theme.");
      this.gdePrimenyatAkcent = listsetting1;
      Supplier<Boolean> supplier = () -> false;
      this.intensivnost.setVisibleWhen(supplier);
      this.iskazhenie.setVisibleWhen(supplier);
      this.hromatika.setVisibleWhen(supplier);
      this.blyur.setVisibleWhen(supplier);
      this.colorTinta.setVisibleWhen(supplier);
      this.strengthTinta.setVisibleWhen(() -> false);
      tintAmount.setVisibleWhen(supplier);
      this.effects.setVisibleWhen(supplier);
      Supplier<Boolean> supplier1 = () -> this.getBooleanBySupplier7(supplier);
      Supplier<Boolean> supplier2 = () -> this.getBooleanBySupplier4(supplier);
      Supplier<Boolean> supplier3 = () -> this.getBooleanBySupplier(supplier);
      Supplier<Boolean> supplier4 = () -> this.getBooleanBySupplier5(supplier);
      Supplier<Boolean> supplier5 = () -> this.getBooleanBySupplier3(supplier);
      this.strengthMatovosti.setVisibleWhen(supplier1);
      this.strengthPosterizacii.setVisibleWhen(supplier2);
      this.sizePikseley.setVisibleWhen(supplier3);
      this.strengthVoln.setVisibleWhen(supplier4);
      this.speedVoln.setVisibleWhen(supplier4);
      this.chastotaVoln.setVisibleWhen(supplier4);
      this.strengthSvecheniya.setVisibleWhen(supplier5);
      this.sizeSvecheniya.setVisibleWhen(supplier5);
      this.colorSvecheniya.setVisibleWhen(supplier5);
      CompactGroupSetting compactgroupsetting1 = new CompactGroupSetting(
         "",
         "",
         this.intensivnost,
         this.iskazhenie,
         this.hromatika,
         this.blyur,
         this.colorTinta,
         this.strengthTinta,
         this.strengthMatovosti,
         this.strengthPosterizacii,
         this.sizePikseley,
         this.strengthVoln,
         this.speedVoln,
         this.chastotaVoln,
         this.strengthSvecheniya,
         this.sizeSvecheniya,
         this.colorSvecheniya
      );
      compactgroupsetting1.setName("Glass");
      CompactGroupSetting compactgroupsetting = compactgroupsetting1;
      compactgroupsetting.setVisibleWhen(supplier);
      Supplier<Boolean> supplier6 = () -> colorMode.isString("Custom");
      this.svoyAkcent.setVisibleWhen(() -> false);
      Supplier<Boolean> supplier7 = () -> colorMode.isString("Custom");
      this.akcent.setVisibleWhen(() -> true);
      this.gdePrimenyatAkcent.setVisibleWhen(() -> false);
      this.modeOtobrazheniya.setVisibleWhen(() -> false);
      glass.setVisibleWhen(supplier);
      liquidOpacity.setVisibleWhen(supplier);
      guiBlurRadius.setVisibleWhen(guiBlyur::isFlag3);
      cursorSize.setVisibleWhen(() -> supplier.get() && effects.isString("Cursor light"));
      cursorStrength.setVisibleWhen(cursorSize::isVisible);
      revealRadius.setVisibleWhen(() -> supplier.get() && effects.isString("Reveal"));
      revealStrength.setVisibleWhen(revealRadius::isVisible);
      gradient.setVisibleWhen(supplier7);
      accentTo.setVisibleWhen(() -> supplier7.get() && gradient.isFlag3());
      gradientAngle.setVisibleWhen(() -> supplier.get() && accentTo.isVisible());
      this.addSettings(
         new Setting[]{
            this.liquidOpacity,
            this.kromeGui,
            this.modeOtobrazheniya,
            this.sizeGui,
            this.guiBlyur,
            this.guiBlurRadius,
            this.guiDim,
            this.glass, this.tintAmount,
            this.effects,
            compactgroupsetting,
            cursorSize, cursorStrength, revealRadius, revealStrength, colorMode, gradient, accentTo, gradientAngle,
            this.svoyAkcent,
            this.akcent,
            this.gdePrimenyatAkcent
         }
      );
      // Retain legacy keys for old configs, but retire the removed GUI controls and effects.
      for (Setting retired : List.of(glass,tintAmount,effects,compactgroupsetting,cursorSize,cursorStrength,revealRadius,revealStrength))
         retired.setVisibleWhen(() -> false);
      colorMode.setOnChange(this::selectColor);
      akcent.setOnChange(this::selectCustomColor);
      super.setEnabled(true);
   }

   private Boolean getBooleanBySupplier(Supplier<Boolean> supplier) {
      return (Boolean)supplier.get() && this.effects.isString("Pixels");
   }

   public float getFloat() {
      return check5() ? Math.min(.8F,guiBlurRadius.getValueAsFloat()/80) : 0;
   }

   public float getFloat2() {
      return 0.0F;
   }

   public boolean check3() {
      return this.isEnabled() && dev.bkclient.ui.clicksys.ClickSysMaterial.context() != null && !colorMode.isString("Default");
   }

   private static Boolean getBooleanBySupplier2(Supplier<Boolean> supplier) {
      return !(Boolean)supplier.get();
   }

   public int getInt() {
      return this.colorSvecheniya.getInt3();
   }

   public float getFloat3() {
      return 1.0F;
   }

   public static void setFlag() {
      flag = false;
   }

   public boolean check4() {
      return false;
   }

   public static void update11() {
   }

   public float getFloat4() {
      return 0.0F;
   }

   public void onBoolean(boolean flag) {
      this.guiBlyur.setBoolean(flag);
   }

   private Boolean getBooleanBySupplier3(Supplier<Boolean> supplier) {
      return (Boolean)supplier.get() && this.effects.isString("Glow");
   }

   public float getFloat5() {
      return this.sizeSvecheniya.getValueAsFloat();
   }

   public float getFloat6() {
      return 0.0F;
   }

   @Override
   public void onDisable() {
   }

   public float getFloat7() {
      return 0.0F;
   }

   private Boolean getBooleanBySupplier4(Supplier<Boolean> supplier) {
      return (Boolean)supplier.get() && this.effects.isString("Poster");
   }

   public float getFloat8() {
      return this.chastotaVoln.getValueAsFloat();
   }

   private Boolean getBooleanBySupplier5(Supplier<Boolean> supplier) {
      return (Boolean)supplier.get() && this.effects.isString("Waves");
   }

   public float getFloat9() {
      return 0.0F;
   }

   public static void update12() {
   }

   public Palette getPaletteByPalette(Palette palette) {
      return resolvePalette(palette, dev.bkclient.ui.clicksys.ClickSysMaterial.context() != null);
   }
   // Theme.background()/surface()/border()/... resolve the palette for every quad, label and row
   // while the menu renders (hundreds of calls per frame). With a non-default "Color mode" the
   // previous implementation derived the whole accent palette on every call (HSL conversions, float arrays
   // and a new Palette), which cut the menu frame rate several times over. The result depends only
   // on the base palette instance, the two accent colors, the gradient flag and the light/dark
   // theme, so it is reused until one of them changes. Colors are unchanged.
   private Palette memoBase;
   private Palette memoResult;
   private int memoFrom;
   private int memoTo;
   private boolean memoGradient;
   private boolean memoDark;

   public Palette resolvePalette(Palette palette, boolean inGui) {
      if (!inGui || !isEnabled() || colorMode.isString("Default")) {
         return palette;
      } else {
         int from = accentColor(false);
         int to = accentColor(true);
         boolean grad = gradient.isFlag3();
         boolean dark = ThemeConfig.getThemePalette() == ThemePalette.INSTANCE2;
         Palette memo = this.memoResult;
         if (memo != null && palette == this.memoBase && from == this.memoFrom && to == this.memoTo && grad == this.memoGradient && dark == this.memoDark) {
            return memo;
         }
         Palette result = derivePalette(palette, from, to, grad, dark);
         this.memoBase = palette;
         this.memoFrom = from;
         this.memoTo = to;
         this.memoGradient = grad;
         this.memoDark = dark;
         this.memoResult = result;
         return result;
      }
   }

   /** The original derivation, unchanged except that its inputs arrive as parameters. */
   private Palette derivePalette(Palette palette, int from, int to, boolean grad, boolean flagx) {
      {
         float[] afloat = MathUtil.getFloatArrayByInt(grad ? dev.bkclient.ui.clicksys.port.data.AnimatedInt.getIntByIntFloatInt(to,.5F,from) : from);
         float f = afloat[0];
         float f1 = afloat[1];
         boolean flag1 = true;
         boolean flag2 = true;
         boolean flag3 = true;
         boolean flag4 = true;
         int l1;
         if (flag1) {
            float f26;
            if (flagx) {
               f26 = 0.9F;
            } else {
               f26 = afloat[2];
               float f4 = 0.35F;
               float f3 = 0.15F;
               float f2 = f26;
               f26 = getFloatByFloatFloatFloat(f4, f2, f3);
            }

            float f5 = f26;
            l1 = MathUtil.getIntByFloatFloatFloat(f5, f, f1);
         } else {
            l1 = palette.getValue7();
         }

         int i = l1;
         if (flag1) {
            float f24;
            if (flagx) {
               f24 = 0.8F;
            } else {
               f24 = afloat[2];
               float f8 = 0.4F;
               float f7 = 0.2F;
               float f6 = f24;
               f24 = getFloatByFloatFloatFloat(f8, f6, f7);
            }

            float f9 = f24;
            l1 = MathUtil.getIntByFloatFloatFloat(f9, f, f1);
         } else {
            l1 = palette.getValue6();
         }

         int j = l1;
         if (flag1) {
            float f25 = f1 * 0.55F;
            float f11 = flagx ? 0.62F : 0.5F;
            float f10 = f25;
            l1 = MathUtil.getIntByFloatFloatFloat(f11, f, f10);
         } else {
            l1 = palette.getValue5();
         }

         int k = l1;
         if (flag3) {
            float f20 = f1 * 0.4F;
            float f13 = flagx ? 0.28F : 0.88F;
            float f12 = f20;
            l1 = MathUtil.getIntByFloatFloatFloat(f13, f, f12);
         } else {
            l1 = palette.getValue4();
         }

         int l = l1;
         if (flag2) {
            float f21 = f1 * 0.35F;
            float f15 = flagx ? 0.22F : 0.94F;
            float f14 = f21;
            l1 = MathUtil.getIntByFloatFloatFloat(f15, f, f14);
         } else {
            l1 = palette.getValue3();
         }

         int i1 = l1;
         if (flag2) {
            float f22 = f1 * 0.3F;
            float f17 = flagx ? 0.16F : 0.97F;
            float f16 = f22;
            l1 = MathUtil.getIntByFloatFloatFloat(f17, f, f16);
         } else {
            l1 = palette.getValue2();
         }

         int j1 = l1;
         if (flag4) {
            float f23 = f1 * 0.25F;
            float f19 = flagx ? 0.1F : 0.99F;
            float f18 = f23;
            l1 = MathUtil.getIntByFloatFloatFloat(f19, f, f18);
         } else {
            l1 = palette.getValue();
         }

         int k1 = l1;
         return new Palette(k1, j1, i1, l, k, j, i, palette.getValue8(), palette.getValue9(), palette.getValue10(), palette.getValue11());
      }
   }

   public static void setFlag2() {
      flag = true;
   }

   private Boolean getBooleanBySupplier6(Supplier<Boolean> supplier) {
      return (Boolean)supplier.get() && this.svoyAkcent.isFlag3();
   }

   public boolean check5() {
      return this.guiBlyur.isFlag3();
   }

   public static float getFloat10() {
      ThemeModule thememodule = getThemeModule();
      return thememodule != null ? thememodule.sizeGui.getValueAsFloat() : 1.0F;
   }
   public void loadSettings(com.google.gson.JsonObject appearance) {
      loadingSettings = true;
      try {
         for(Setting setting:getSettings()) if(appearance.has(setting.getNameHash())) setting.fromJson(appearance.getAsJsonObject(setting.getNameHash()));
      } finally { loadingSettings = false; }
      if(!appearance.has(colorMode.getNameHash()) && svoyAkcent.isFlag3()) colorMode.addList(List.of("Custom"));
      if(!appearance.has(tintAmount.getNameHash())) tintAmount.setDouble(Math.min(.05,strengthTinta.getValue()/7.5));
   }
   public void bindPersistence(Runnable save) {
      persist = save;
      for (Setting setting : getSettings()) setting.setOnChange(setting == colorMode ? this::selectColor : setting == akcent ? this::selectCustomColor : save);
   }
   @Override public void setEnabled(boolean value) { super.setEnabled(value); persist.run(); }
   public int effectMask() { return 0; }
   public boolean effect(String name) { return false; }
   private void selectCustomColor() {
      if (loadingSettings) return;
      // A color picker edit takes precedence over a preset without a separate mode switch.
      colorMode.addList(List.of("Custom"));
      super.setEnabled(true);
      persist.run();
   }
   private void selectColor() {
      if (loadingSettings) return;
      // Selecting a palette is an explicit request to apply it, even after an old disabled config.
      super.setEnabled(true);
      persist.run();
   }
   public void restoreEnabled(boolean enabled) { super.setEnabled(enabled || !colorMode.isString("Default")); }
   public boolean transparent() { return false; }
   public boolean accentTarget(String name) { return check3() && list2.contains(name); }
   public boolean hasGradient() { return check3() && gradient.isFlag3() && colorMode.isString("Custom"); }
   public int accentColor(boolean end) {
      if(colorMode.isString("Custom")) return end && gradient.isFlag3() ? accentTo.getInt3() : akcent.getInt3();
      int[][] colors = {{0xffdc3e42,0xff6a3fdc},{0xffdc3e42,0xff6a3fdc},{0xff6aa6ff,0xffff7fb8},
         {0xff76ffcf,0xffff8ee9},{0xff40a4e8,0xff76e7da},{0xffc4a2ef,0xffedb7eb},{0xff9bd878,0xfff4d986},
         {0xffc4e8ff,0xff91a8ee},{0xff384985,0xff9473c7},{0xffb59a6d,0xffa3ad76},{0xff398663,0xff98bd70},{0xff9563db,0xffce63ba}};
      return colors[Math.clamp(colorMode.getInt2(),0,colors.length-1)][end ? 1 : 0];
   }
   public SliderSetting getScaleSetting() { return this.sizeGui; }

   public static ThemeModule getThemeModule() {
      return (ThemeModule)unsafeAccess.getModule();
   }

   public int getInt2() {
      return this.colorTinta.getInt3();
   }

   private static float getFloatByFloatFloatFloat(float value, float value2, float value3) {
      return Math.max(value3, Math.min(value, value2));
   }

   public float getFloat11() {
      return 0.0F;
   }

   public float getFloat12() {
      return this.speedVoln.getValueAsFloat();
   }

   private Boolean getBooleanBySupplier7(Supplier<Boolean> supplier) {
      return (Boolean)supplier.get() && this.effects.isString("Matte");
   }

   public float getFloat13() {
      return 0.0F;
   }

   @Override
   public void onEnable() {
   }

   public float getFloat14() {
      return 0.0F;
   }
}
