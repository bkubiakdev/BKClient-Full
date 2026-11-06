package dev.bkclient.systems.notifications;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Generated;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;

public class NotificationManager {
   private final List<Notification> notifications = new CopyOnWriteArrayList<>();
   private final List<NotificationOther> notificationsOther = new CopyOnWriteArrayList<>();
   private final EventListener<HudRenderEvent> onHudRenderEvent = event -> {
      for (Notification notification : this.notifications) {
         notification.update();
      }

      float off = 0.0F;

      for (NotificationOther notification : this.notificationsOther) {
         notification.update();
         notification.draw(event.getContext(), off);
         if (notification.getAnimation().getValue() >= 0.5F || !notification.getTimer().finished(notification.getDuration())) {
            off += 30.0F;
         }
      }

      this.notifications.removeIf(Notification::isFinished);
      this.notificationsOther.removeIf(NotificationOther::isFinished);
   };

   public NotificationManager() {
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   public void addNotification(NotificationType type, String text) {
      this.notifications.add(new Notification(type, text));
   }

   public void addNotification(NotificationType type, String text, long durationMs) {
      this.notifications.add(new Notification(type, text, durationMs));
   }

   public void addNotificationOther(NotificationType type, String title, String desc) {
      this.notificationsOther.add(new NotificationOther(type, title, desc));
   }

   public void addNotificationOther(NotificationType type, String title, String desc, long durationMs) {
      this.notificationsOther.add(new NotificationOther(type, title, desc, durationMs));
   }

   public void addThemedNotification(NotificationType type, String title, String desc, long durationMs) {
      this.notificationsOther.add(new NotificationOther(type, title, desc, durationMs, true));
   }

   @Generated
   public List<Notification> getNotifications() {
      return this.notifications;
   }

   @Generated
   public List<NotificationOther> getNotificationsOther() {
      return this.notificationsOther;
   }
}
