package dev.bkclient.systems.notifications;

import lombok.Generated;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.time.Timer;

public class Notification {
   private final NotificationType type;
   private final String text;
   private final Timer timer = new Timer();
   private final long duration;
   private final Animation animation = new Animation(300L, Easing.EXPO_OUT);
   private final Animation showing = new Animation(300L, Easing.BAKEK_SIZE);

   public Notification(NotificationType type, String text) {
      this(type, text, 1000L);
   }

   public Notification(NotificationType type, String text, long duration) {
      this.type = type;
      this.text = text;
      this.duration = Math.max(250L, duration);
   }

   public void update() {
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
   public String getText() {
      return this.text;
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
}
