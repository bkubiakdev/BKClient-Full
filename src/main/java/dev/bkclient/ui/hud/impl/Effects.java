package dev.bkclient.ui.hud.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.ui.components.animated.AnimatedNumber;
import dev.bkclient.ui.hud.HudList;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.server.ServerUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.mixins.StatusEffectInstanceAddition;
import dev.bkclient.utility.render.batching.Batching;
import dev.bkclient.utility.render.batching.impl.FontBatching;
import dev.bkclient.utility.render.batching.impl.RectBatching;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;

public class Effects extends HudList {
   private static final float EFFECT_ICON_SIZE = 10.0F;
   private static final float EFFECT_ICON_GAP = 3.0F;
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");
   private final ModeSetting sorting = new ModeSetting(this, "hud.list.sort");
   private final ModeSetting.Value sortName = new ModeSetting.Value(sorting, "hud.list.sort_name").select();
   private final ModeSetting.Value sortTime = new ModeSetting.Value(sorting, "hud.editor.order_time");
   private final BooleanSetting showLevel = new BooleanSetting(this, "hud.effects.level").enable();
   private final BooleanSetting warnExpiring = new BooleanSetting(this, "hud.effects.warn_expiring");
   private final dev.bkclient.systems.setting.settings.SliderSetting warningSeconds = new dev.bkclient.systems.setting.settings.SliderSetting(this, "hud.effects.warning_seconds", () -> !warnExpiring.isEnabled()).min(1).max(30).step(1).currentValue(10);
   int lastSize = -1;
   private final Map<String, StatusEffectInstance> effects = new TreeMap<>();
   private final Map<StatusEffect, Boolean> ended = new HashMap<>();
   private final BooleanSetting alert = new BooleanSetting(this, "hud.effects.alert");

   public Effects() {
      super("hud.effects", "icons/hud/potion.png");
   }

   @Override
   public void update(UIContext context) {
      this.frameEffects = null;
      this.width = 92.0F;
      this.height = 18.0F;
      if (mc.player == null) {
         super.update(context);
         return;
      }
      Collection<StatusEffectInstance> original = mc.player.getStatusEffects();

      for (StatusEffectInstance eff : original) {
         StatusEffect potion = (StatusEffect)eff.getEffectType().value();
         String realName = potion.getName().getString();
         if (realName != null && !ServerUtility.isCM()) {
            if (this.effects.containsKey(realName)) {
               this.effects.replace(realName, eff);
               Animation anim = ((StatusEffectInstanceAddition)eff).bkclient$getAnimPotion();
               if (anim.getValue() == 0.0F) {
                  anim.setValue(1.0F);
               }
            } else {
               this.effects.put(realName, eff);
            }
         }
      }

      // The map only changes in the loop above and in the removal at the end of renderContent.
      this.frameEffects = null;
      if (!this.effects.isEmpty()) {
         this.height += 5.0F;
      }

      for (StatusEffectInstance effx : frameEffects()) {
         Animation anim = ((StatusEffectInstanceAddition)effx).bkclient$getAnimPotion();
         StatusEffect potion = (StatusEffect)effx.getEffectType().value();
         if (this.alert.isEnabled()) {
            String effectName = potion.getName().getString() + " " + (effx.getAmplifier() > 0 ? effx.getAmplifier() + 1 : "");
            if (!mc.player.hasStatusEffect(effx.getEffectType())) {
               if (!this.ended.getOrDefault(potion, false) && !potion.getCategory().equals(StatusEffectCategory.HARMFUL)) {
                  BKClientInit.getInstance()
                     .getNotificationManager()
                     .addNotificationOther(NotificationType.INFO, "Effect " + effectName + " ended", "The effect is no longer active");
                  this.ended.put(potion, true);
               }
            } else {
               this.ended.put(potion, false);
            }
         }

         anim.update(original.contains(effx));
         anim.setEasing(Easing.BAKEK);
         String effectName = effectName(effx);
         this.width = Math.max(
            Fonts.REGULAR.getFont(7.0F).width(effectName) + 60.0F + EFFECT_ICON_SIZE + EFFECT_ICON_GAP,
            this.width
         );
         this.height = this.height + 18.0F * anim.getValue();
      }

      if (HudTheme.isOld()) {
         Font nameFont = Fonts.MEDIUM.getFont(5.5F);
         Font timeFont = Fonts.SEMIBOLD.getFont(5.5F);
         this.width = 70.0F;
         float rowPresence = 0.0F;
         for (StatusEffectInstance effect : frameEffects()) {
            Animation anim = ((StatusEffectInstanceAddition)effect).bkclient$getAnimPotion();
            String name = effectName(effect);
            this.width = Math.max(this.width, 8.0F + 7.0F + 2.0F + nameFont.width(name) + 4.0F + timeFont.width(formatTime(effect)) + 5.0F + 8.0F);
            rowPresence += anim.getValue();
         }
         this.height = rowPresence <= 0.0F ? 16.0F : 18.0F + 9.0F * rowPresence + 6.0F;
      }

      float presence = 0;
      for (StatusEffectInstance effect : effects.values()) presence += ((StatusEffectInstanceAddition)effect).bkclient$getAnimPotion().getValue();
      this.height = listTop(HudTheme.isOld() ? 18 : 22) + listStride(HudTheme.isOld() ? 9 : 18) * presence + 3;
      super.update(context);
   }

   @Override
   protected void renderContent(UIContext context) {
      if (mc.player != null && mc.world != null) {
         if (HudTheme.isOld()) {
            this.renderOldContent(context);
            return;
         }

         Font font = Fonts.REGULAR.getFont(7.0F);
         float offset = listTop(22.0F);
         Font headerFont = Fonts.MEDIUM.getFont(7.0F);
         super.renderContent(context);
          StatusEffectInstance toRemove = null;
         Batching split = new RectBatching(VertexFormats.POSITION_COLOR, context.getMatrices());

         for (StatusEffectInstance eff : frameEffects()) {
            Animation anim = ((StatusEffectInstanceAddition)eff).bkclient$getAnimPotion();
            if (anim.getValue() == 0.0F) {
               toRemove = eff;
            } else {
               float off = -4.5F + 4.5F * anim.getValue();
               if (offset != listTop(22.0F)) {
                  context.drawRect(this.x, this.y + offset + off, this.width, 0.5F, HudTheme.textColor().withAlpha(5.1F * anim.getValue()));
               }

               offset += listStride(18.0F) * anim.getValue();
            }
         }

          split.draw();

          // Effect sprites use Minecraft's texture atlas and therefore must be
          // rendered before opening the MSDF font batch. Mixing both textures in
          // one active batch corrupts the atlas coordinates and hides the labels.
          offset = listTop(22.0F);
          for (StatusEffectInstance effect : frameEffects()) {
             Animation anim = ((StatusEffectInstanceAddition)effect).bkclient$getAnimPotion();
             if (anim.getValue() != 0.0F) {
                float off = -4.5F + 4.5F * anim.getValue();
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
                float iconX = this.x + 8.0F * anim.getValue();
                float iconY = this.y + offset + off + (18.0F - EFFECT_ICON_SIZE) / 2.0F;
                context.drawTexture(
                   sprite.getAtlasId(),
                   iconX,
                   iconY,
                   EFFECT_ICON_SIZE,
                   EFFECT_ICON_SIZE,
                   sprite.getMinU(),
                   sprite.getMaxU(),
                   sprite.getMinV(),
                   sprite.getMaxV(),
                   ColorRGBA.WHITE.withAlpha(255.0F * anim.getValue())
                );
                offset += listStride(18.0F) * anim.getValue();
             }
          }

           Batching fontBatching = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, font.getFont());
          offset = listTop(22.0F);

          for (StatusEffectInstance effxx : frameEffects()) {
             Animation anim = ((StatusEffectInstanceAddition)effxx).bkclient$getAnimPotion();
             AnimatedNumber timeAnimation = ((StatusEffectInstanceAddition)effxx).bkclient$getTimeAnimation();
             StatusEffect potion = (StatusEffect)effxx.getEffectType().value();
             if (anim.getValue() != 0.0F) {
                float off = -4.5F + 4.5F * anim.getValue();
                String effectName = effectName(effxx);
                if (!effxx.isInfinite() && effxx.getDuration() < 999999999) {
                   int totalSeconds = effxx.getDuration() / 20;
                   int minutes = totalSeconds / 60;
                   int seconds = totalSeconds % 60;
                   String timeStr = clockText(totalSeconds);
                   String minutesAndSeparator = minuteText(minutes);
                   float timeX = this.x + this.width - 7.0F * anim.getValue();
                   float timeY = this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F);
                   float minutesWidth = font.width(minutesAndSeparator);
                   float totalWidth = font.width(timeStr);
                   context.drawText(font, minutesAndSeparator, timeX - totalWidth, timeY, effectColor(effxx).withAlpha(255.0F * anim.getValue()));
                   timeAnimation.settings(true, effectColor(effxx).withAlpha(255.0F * anim.getValue()));
                   timeAnimation.update(seconds);
                   timeAnimation.pos(timeX - totalWidth + minutesWidth, timeY);
                   timeAnimation.render(context);
                } else {
                   String duration = "**:**";
                   float timeX = this.x + this.width - 7.0F * anim.getValue();
                   float timeY = this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F);
                   context.drawRightText(font, duration, timeX, timeY, effectColor(effxx).withAlpha((int)(255.0F * anim.getValue())));
                }

                context.drawText(
                   font,
                   effectName,
                   this.x + (8.0F + EFFECT_ICON_SIZE + EFFECT_ICON_GAP) * anim.getValue(),
                   this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
                   effectColor(effxx).withAlpha(255.0F * anim.getValue())
                );
                offset += listStride(18.0F) * anim.getValue();
             }
          }

         fontBatching.draw();
         if (toRemove != null) {
            StatusEffect potion = (StatusEffect)toRemove.getEffectType().value();
            this.effects.remove(potion.getName().getString(), toRemove);
            this.frameEffects = null;
         }
      }
   }

   private void renderOldContent(UIContext context) {
      super.renderContent(context);
      Font headerFont = Fonts.SEMIBOLD.getFont(7.0F);
      Font nameFont = Fonts.MEDIUM.getFont(5.5F);
      Font timeFont = Fonts.SEMIBOLD.getFont(5.5F);


      float rowY = this.y + listTop(18.0F);
      StatusEffectInstance toRemove = null;
      for (StatusEffectInstance effect : frameEffects()) {
         Animation anim = ((StatusEffectInstanceAddition)effect).bkclient$getAnimPotion();
         if (anim.getValue() <= 0.0F) {
            toRemove = effect;
            continue;
         }
         Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
         context.drawTexture(sprite.getAtlasId(), this.x + 8.0F, rowY + 1.0F, 7.0F, 7.0F,
            sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), ColorRGBA.WHITE.withAlpha(255.0F * anim.getValue()));
         rowY += listStride(9.0F) * anim.getValue();
      }

      rowY = this.y + listTop(18.0F);
      for (StatusEffectInstance effect : frameEffects()) {
         Animation anim = ((StatusEffectInstanceAddition)effect).bkclient$getAnimPotion();
         if (anim.getValue() <= 0.0F) continue;
         String time = formatTime(effect);
         float badgeWidth = timeFont.width(time) + 5.0F;
         float badgeX = this.x + this.width - 8.0F - badgeWidth;
         float badgeY = rowY + 1.0F;
         context.drawShadow(badgeX, badgeY, badgeWidth, 7.0F, 12.5F, dev.bkclient.framework.objects.BorderRadius.all(2.0F), effectAccent(effect).withAlpha(64.0F * anim.getValue()));
         context.drawRoundedRect(badgeX, badgeY, badgeWidth, 7.0F, dev.bkclient.framework.objects.BorderRadius.all(2.0F), effectAccent(effect).withAlpha(255.0F * anim.getValue()));
         context.drawCenteredText(timeFont, time, badgeX + badgeWidth / 2.0F,
            badgeY + GuiUtility.getMiddleOfBox(timeFont.height(), 7.0F) - 0.5F, ColorRGBA.BLACK.withAlpha(191.0F * anim.getValue()));
         context.drawText(nameFont, effectName(effect), this.x + 17.0F,
            rowY + GuiUtility.getMiddleOfBox(nameFont.height(), 9.0F), HudTheme.textColor().withAlpha(255.0F * anim.getValue()));
         rowY += listStride(9.0F) * anim.getValue();
      }

      if (toRemove != null) {
         StatusEffect potion = (StatusEffect)toRemove.getEffectType().value();
         this.effects.remove(potion.getName().getString(), toRemove);
         this.frameEffects = null;
      }
   }

   /** Align the title and optional icon as one group, so Center is visually centered. */
   private java.util.List<StatusEffectInstance> frameEffects;
   /** One ordered snapshot per frame: update and every render pass read the same unchanged map. */
   private java.util.List<StatusEffectInstance> frameEffects() {
      if (frameEffects == null) frameEffects = orderedEffects();
      return frameEffects;
   }
   private java.util.List<StatusEffectInstance> orderedEffects() {
      var entries = new java.util.ArrayList<>(effects.values());
      if (sortTime.isSelected()) entries.sort(java.util.Comparator.comparingInt(effect -> effect.isInfinite() ? Integer.MAX_VALUE : effect.getDuration()));
      return entries;
   }
   private boolean expiring(StatusEffectInstance effect) {
      return warnExpiring.isEnabled() && !effect.isInfinite() && effect.getDuration() <= warningSeconds.getCurrentValue() * 20;
   }
   /** ColorRGBA is immutable (withAlpha returns a new instance), so one shared instance is identical. */
   private static final ColorRGBA EXPIRING = new ColorRGBA(245, 105, 80);
   private ColorRGBA effectColor(StatusEffectInstance effect) { return expiring(effect) ? EXPIRING : HudTheme.textColor(); }
   private ColorRGBA effectAccent(StatusEffectInstance effect) { return expiring(effect) ? EXPIRING : Colors.getAccent(); }

   private String effectName(StatusEffectInstance effect) {
      StatusEffect potion = (StatusEffect)effect.getEffectType().value();
      return potion.getName().getString() + (showLevel.isEnabled() && effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "");
   }

   private static String formatTime(StatusEffectInstance effect) {
      if (effect.isInfinite() || effect.getDuration() >= 999999999) return "**:**";
      return clockText(effect.getDuration() / 20);
   }

   /** String.format results per displayed value; dropped if the default format locale changes. */
   private static final Map<Integer, String> CLOCK_TEXT = new HashMap<>();
   private static final Map<Integer, String> MINUTE_TEXT = new HashMap<>();
   private static java.util.Locale textLocale;
   private static void checkTextLocale() {
      java.util.Locale locale = java.util.Locale.getDefault(java.util.Locale.Category.FORMAT);
      if (!locale.equals(textLocale) || CLOCK_TEXT.size() > 512 || MINUTE_TEXT.size() > 512) {
         textLocale = locale;
         CLOCK_TEXT.clear();
         MINUTE_TEXT.clear();
      }
   }
   private static String clockText(int totalSeconds) {
      checkTextLocale();
      String text = CLOCK_TEXT.get(totalSeconds);
      if (text == null) {
         text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
         CLOCK_TEXT.put(totalSeconds, text);
      }
      return text;
   }
   private static String minuteText(int minutes) {
      checkTextLocale();
      String text = MINUTE_TEXT.get(minutes);
      if (text == null) {
         text = String.format("%02d:", minutes);
         MINUTE_TEXT.put(minutes, text);
      }
      return text;
   }

   @Override
   public boolean show() {
      return mc.player != null && mc.world != null
         ? (!mc.player.getStatusEffects().isEmpty() || mc.currentScreen instanceof ChatScreen || this.alwaysDisplay.isEnabled()) && !ServerUtility.isCM()
         : false;
   }
}
