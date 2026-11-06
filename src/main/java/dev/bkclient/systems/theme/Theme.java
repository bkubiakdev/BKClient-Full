package dev.bkclient.systems.theme;

import lombok.Generated;
import dev.bkclient.utility.colors.ColorRGBA;

public enum Theme implements CustomTheme {
    DARK(
       new ColorRGBA(255.0F, 255.0F, 255.0F),
       new ColorRGBA(12.0F, 12.0F, 12.0F),
       new ColorRGBA(24.0F, 24.0F, 27.0F),
       new ColorRGBA(32.0F, 32.0F, 32.0F),
       ColorRGBA.BLACK,
       ColorRGBA.WHITE,
       ColorRGBA.WHITE
    ),
    LIGHT(
       new ColorRGBA(10.0F, 10.0F, 10.0F),
       new ColorRGBA(229.0F, 229.0F, 229.0F),
       new ColorRGBA(255.0F, 255.0F, 255.0F),
       new ColorRGBA(32.0F, 32.0F, 32.0F),
       ColorRGBA.WHITE,
       new ColorRGBA(10.0F, 10.0F, 10.0F),
       new ColorRGBA(10.0F, 10.0F, 10.0F)
    );

   private ColorRGBA textColor;
   private ColorRGBA backgroundColor;
   private ColorRGBA additionalColor;
   private ColorRGBA outlineColor;
   private ColorRGBA flatColor;
    private ColorRGBA accentColor = new ColorRGBA(20.0F, 100.0F, 255.0F);
    private ColorRGBA iconsColor;
    private ColorRGBA enabledModulesColor;
   private final boolean separators = true;
   private final float blurStrength = 4.0F;
   private final float glassOpacity = 20.0F;
   private final float enabledOffset = 2.0F;
   private final float blurOffset = 0.5F;
   private final float hudAlpha = 0.8F;
   private final float disableAlphaGlass = 0.2F;
   private final float glassPower = 25.0F;
   private final float glassStrength = 0.08F;
   private final float padding = 2.0F;
   private final float splitters = 1.0F;
   private final float albumColor = 1.0F;

   @Generated
   @Override
   public ColorRGBA getTextColor() {
      return this.textColor;
   }

   public void setTextColor(ColorRGBA textColor) {
      this.textColor = textColor;
   }

   @Generated
   @Override
   public ColorRGBA getBackgroundColor() {
      return this.backgroundColor;
   }

   public void setBackgroundColor(ColorRGBA backgroundColor) {
      this.backgroundColor = backgroundColor;
   }

   @Generated
   @Override
   public ColorRGBA getAdditionalColor() {
      return this.additionalColor;
   }

   public void setAdditionalColor(ColorRGBA additionalColor) {
      this.additionalColor = additionalColor;
   }

   @Generated
   @Override
   public ColorRGBA getOutlineColor() {
      return this.outlineColor;
   }

   public void setOutlineColor(ColorRGBA outlineColor) {
      this.outlineColor = outlineColor;
   }

   @Generated
   @Override
   public ColorRGBA getFlatColor() {
      return this.flatColor;
   }

   public void setFlatColor(ColorRGBA flatColor) {
      this.flatColor = flatColor;
   }

   @Override
   public ColorRGBA getAccentColor() {
      return this.accentColor;
   }

   public void setAccentColor(ColorRGBA accentColor) {
      this.accentColor = accentColor;
   }

   @Override
   public ColorRGBA getIconsColor() {
      return this.iconsColor;
   }

   public void setIconsColor(ColorRGBA iconsColor) {
      this.iconsColor = iconsColor;
   }

   @Override
   public ColorRGBA getEnabledModulesColor() {
      return this.enabledModulesColor;
   }

   public void setEnabledModulesColor(ColorRGBA enabledModulesColor) {
      this.enabledModulesColor = enabledModulesColor;
   }

   @Override
   public boolean isDark() {
      return this == DARK;
   }

   @Override
   public boolean isSeparators() {
      return true;
   }

   @Override
   public float getBlurStrength() {
      return 4.0F;
   }

   @Override
   public float getGlassOpacity() {
      return 20.0F;
   }

   @Override
   public float getEnabledOffset() {
      return 2.0F;
   }

   @Override
   public float getBlurOffset() {
      return 0.5F;
   }

   @Override
   public float getHudAlpha() {
      return 0.8F;
   }

   @Override
   public float getDisableAlphaGlass() {
      return 0.2F;
   }

   @Override
   public float getGlassPower() {
      return 25.0F;
   }

   @Override
   public float getGlassStrength() {
      return 0.08F;
   }

   @Override
   public float getPadding() {
      return 2.0F;
   }

   @Override
   public float getSplitters() {
      return 1.0F;
   }

   @Override
   public float getAlbumColor() {
      return 1.0F;
   }

    @Generated
    Theme(final ColorRGBA textColor, final ColorRGBA backgroundColor, final ColorRGBA additionalColor, final ColorRGBA outlineColor, final ColorRGBA flatColor, final ColorRGBA iconsColor, final ColorRGBA enabledModulesColor) {
       this.textColor = textColor;
       this.backgroundColor = backgroundColor;
       this.additionalColor = additionalColor;
       this.outlineColor = outlineColor;
       this.flatColor = flatColor;
       this.iconsColor = iconsColor;
       this.enabledModulesColor = enabledModulesColor;
    }
}
