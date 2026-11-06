package dev.bkclient.systems.notifications;

import lombok.Generated;
import dev.bkclient.utility.colors.ColorRGBA;

public enum NotificationType {
   SUCCESS("success", ColorRGBA.GREEN),
   CHECK("check", ColorRGBA.GREEN),
   ERROR("error", ColorRGBA.RED),
   INFO("info", new ColorRGBA(234.0F, 179.0F, 8.0F));

   private final String name;
   private final ColorRGBA color;

   public static NotificationType get(String name) {
      for (NotificationType type : values()) {
         if (type.getName().equalsIgnoreCase(name)) {
            return type;
         }
      }

      return INFO;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public ColorRGBA getColor() {
      return this.color;
   }

   @Generated
   NotificationType(final String name, final ColorRGBA color) {
      this.name = name;
      this.color = color;
   }
}
