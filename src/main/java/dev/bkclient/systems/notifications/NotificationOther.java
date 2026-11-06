package dev.bkclient.systems.notifications;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.time.Timer;

public class NotificationOther {
   private final NotificationType type;
   private final String title;
   private final String desc;
   private final Timer timer = new Timer();
   private final long duration;
   private final boolean themed;
   private final Animation animation = new Animation(400L, Easing.BAKEK);
   private final Animation showing = new Animation(300L, Easing.BAKEK_SIZE);
   private final Animation animY = new Animation(300L, Easing.BAKEK_SMALLER);

   public NotificationOther(NotificationType type, String title, String desc) {
      this(type, title, desc, 2000L);
   }

   public NotificationOther(NotificationType type, String title, String desc, long duration) {
      this(type, title, desc, duration, false);
   }

   public NotificationOther(NotificationType type, String title, String desc, long duration, boolean themed) {
      this.type = type;
      this.title = title;
      this.desc = desc;
      this.duration = Math.max(500L, duration);
      this.themed = themed;
   }

   public void draw(CustomDrawContext context, float off) {
      float textWidth = Math.max(Fonts.BOLD.getFont(7.0F).width(this.title), Fonts.MEDIUM.getFont(6.0F).width(this.desc));
      float width = textWidth + 32.0F;
      this.animY.setEasing(Easing.BAKEK_SIZE);
      this.animY.setDuration(300L);
      float x = context.getScaledWindowWidth() / 2.0F - width / 2.0F;
      float y = context.getScaledWindowHeight() - 90.0F - this.animY.update(off);
      float height = 26.0F;
      int alpha = (int)(255.0F * this.animation.getValue());
      RenderUtility.scale(context.getMatrices(), x + width / 2.0F, y + 12.0F + height / 2.0F, 0.5F + 0.5F * this.animation.getValue());
      if (Interface.showGlass()) {
         context.drawLiquidGlass(
            x, y, width, height, 7.0F, 0.08F, BorderRadius.all(7.0F), ColorRGBA.WHITE.withAlpha(255.0F * this.animation.getValue() * Interface.glass())
         );
         context.drawSquircle(
            x,
            y,
            width,
            height,
            7.0F,
            BorderRadius.all(7.0F),
            Colors.getBackgroundColor().withAlpha(255.0F * (0.8F - 0.6F * Interface.glass()) * this.animation.getValue())
         );
      } else {
         context.drawBlurredRect(
            x, y, width, height, 45.0F, 7.0F, BorderRadius.all(7.0F), ColorRGBA.WHITE.withAlpha(255.0F * this.animation.getValue() * Interface.minimalizm())
         );
         context.drawSquircle(
            x, y, width, height, 7.0F, BorderRadius.all(7.0F), new ColorRGBA(0.0F, 0.0F, 0.0F).withAlpha((int)(140.25F * this.animation.getValue()))
         );
         context.drawRoundedRect(
            x + height / 2.0F - 9.0F,
            y + height / 2.0F - 9.0F,
            18.0F,
            18.0F,
            BorderRadius.all(4.0F),
            new ColorRGBA(0.0F, 0.0F, 0.0F).withAlpha((int)(51.0F * this.animation.getValue()))
         );
      }

      ColorRGBA accent = this.themed ? Colors.getAccent() : this.type.getColor();
      if (this.themed) {
         context.drawRoundedRect(x + 4.0F, y + 4.0F, 18.0F, 18.0F,
            BorderRadius.all(4.0F), accent.withAlpha(alpha * 0.18F));
      }

      context.drawTexture(
         BKClientInit.id("icons/" + this.type.getName() + ".png"),
         x + height / 2.0F - 4.5F,
         y + height / 2.0F - 4.5F,
         10.0F,
         10.0F,
         accent.withAlpha(alpha * 0.8F)
      );
      ColorRGBA foreground = this.themed && Interface.showGlass() ? Colors.getTextColor() : ColorRGBA.WHITE;
      context.drawText(Fonts.BOLD.getFont(7.0F), this.title, x + 27.0F, y + 7.0F,
         (this.themed ? accent : foreground).withAlpha(alpha));
      context.drawText(Fonts.MEDIUM.getFont(6.0F), this.desc, x + 27.0F, y + 15.0F, foreground.withAlpha(alpha));
      RenderUtility.end(context.getMatrices());
   }

   public void update() {
      this.animation.setDuration(400L);
      this.animation.setEasing(this.timer.finished(this.duration) ? Easing.BAKEK_BACK : Easing.BAKEK);
      this.animation.update(this.timer.finished(this.duration) ? 0.0F : 1.0F);
   }

   public boolean isFinished() {
      return this.animation.getValue() == 0.0F && this.timer.finished(this.duration);
   }

   @Generated
   public NotificationType getType() {
      return this.type;
   }

   @Generated
   public String getTitle() {
      return this.title;
   }

   @Generated
   public String getDesc() {
      return this.desc;
   }

   @Generated
   public Timer getTimer() {
      return this.timer;
   }

   @Generated
   public long getDuration() {
      return this.duration;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public Animation getShowing() {
      return this.showing;
   }

   @Generated
   public Animation getAnimY() {
      return this.animY;
   }
}
