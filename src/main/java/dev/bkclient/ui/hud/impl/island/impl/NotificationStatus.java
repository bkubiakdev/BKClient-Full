package dev.bkclient.ui.hud.impl.island.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.gradient.impl.HorizontalGradient;
import dev.bkclient.systems.notifications.Notification;
import dev.bkclient.systems.notifications.NotificationType;

import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.impl.island.DynamicDock;
import dev.bkclient.ui.hud.impl.island.IslandTokenStatus;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.RingRenderer;
import java.util.List;

public class NotificationStatus extends IslandTokenStatus {

   private static final float FONT_SIZE = 7.0F;
   private static final float DIVIDER_WIDTH = 1.0F;
   private static final float DIVIDER_HEIGHT = 5.0F;
   private static final ColorRGBA ENABLED_COLOR = new ColorRGBA(74.0F, 222.0F, 128.0F);
   private static final ColorRGBA DISABLED_COLOR = new ColorRGBA(239.0F, 68.0F, 68.0F);

   public NotificationStatus(SelectSetting setting) {
      super(setting, "alerts");
   }

   @Override
   public void draw(CustomDrawContext context) {
      DynamicDock island = BKClientInit.getInstance().getHud().getIsland();
      List<Notification> notifications = BKClientInit.getInstance().getNotificationManager().getNotifications();
      if (!hasNotification()) {
         super.draw(context);
         return;
      }

      Notification active = notifications.getLast();
      Font font = Fonts.MEDIUM.getFont(FONT_SIZE);
      float padding = 8.0F;
      float gap = 5.0F;
      float ringSize = 6.0F;
      float contentWidth = ringSize + gap + DIVIDER_WIDTH + gap + font.width(active.getText());
      this.size.width = padding * 2.0F + contentWidth;
      this.size.height = 15.0F;
      float left = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
      float centerY = this.size.height / 2.0F;

      for (Notification notification : notifications) {
         notification.getShowing().setDuration(500L);
         notification.getShowing().update(active == notification);
         float alpha = Math.clamp(this.animation.getValue() * notification.getShowing().getValue(), 0.0F, 1.0F);
         float remainingMillis = notification.getDuration() - notification.getTimer().getElapsedTime();
         alpha *= Math.clamp(remainingMillis / 250.0F, 0.0F, 1.0F);
         if (alpha <= 0.001F) continue;
         ColorRGBA tint = notificationColor(notification.getType());
         // Tint stays inside the capsule and fades out across its left 69%.
         context.drawRoundedRect(left, 0.0F, island.getSize().width * 0.69F, this.size.height,
            BorderRadius.left(7.0F, 7.0F),
            new HorizontalGradient(tint.withAlpha(71.4F * alpha), tint.withAlpha(0.0F)));
      }

      // Keep every notification's text and indicator above the crossfading backgrounds.
      for (Notification notification : notifications) {
         float alpha = this.animation.getValue() * notification.getShowing().getValue();
         if (alpha <= 0.001F) {
            continue;
         }

         ColorRGBA accent = notificationColor(notification.getType());
         float cursor = left + padding;
         float remaining = 1.0F - Math.min(1.0F, (float)notification.getTimer().getElapsedTime() / notification.getDuration());
         RingRenderer.progress(
            context.getMatrices(),
            cursor + ringSize / 2.0F,
            centerY,
            ringSize / 2.0F,
            1.0F,
            remaining,
            0.0F,
            accent.withAlpha(38.0F * alpha),
            accent.withAlpha(255.0F * alpha)
         );
         cursor += ringSize + gap;
         drawDivider(context, cursor, centerY, alpha);
         cursor += DIVIDER_WIDTH + gap;
         context.drawText(font, notification.getText(), cursor, centerY - font.height() / 2.0F, ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      }
   }

   private static ColorRGBA notificationColor(NotificationType type) {
      return switch (type) {
         case SUCCESS, CHECK -> ENABLED_COLOR;
         case ERROR -> DISABLED_COLOR;
         default -> type.getColor();
      };
   }

   private static void drawDivider(CustomDrawContext context, float x, float centerY, float alpha) {
      context.drawRoundedRect(
         x,
         centerY - DIVIDER_HEIGHT / 2.0F,
         DIVIDER_WIDTH,
         DIVIDER_HEIGHT,
         BorderRadius.all(0.5F),
         ColorRGBA.WHITE.withAlpha(64.0F * alpha)
      );
   }

   @Override
   public boolean canShow() {
      return hasNotification();
   }

   public boolean hasNotification() {
      List<Notification> notifications = BKClientInit.getInstance().getNotificationManager().getNotifications();
      return !notifications.isEmpty() && !notifications.getLast().getTimer().finished(notifications.getLast().getDuration());
   }

   @Override
   protected List<Token> buildTokens() { return List.of(); }
}
