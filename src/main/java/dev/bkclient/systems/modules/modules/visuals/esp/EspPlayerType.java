package dev.bkclient.systems.modules.modules.visuals.esp;

public enum EspPlayerType {
   OTHERS("others", "esp.targets.players.others"),
   LOCAL("local", "esp.targets.players.local"),
   FRIENDS("friends", "esp.targets.players.friends"),
   BKCLIENT_USERS("bkclient_users", "esp.targets.players.bkclient_users");

   private final String id;
   private final String translationKey;

   EspPlayerType(String id, String translationKey) {
      this.id = id;
      this.translationKey = translationKey;
   }

   public static EspPlayerType byId(String id) {
      for (EspPlayerType type : values()) {
         if (type.id.equals(id)) {
            return type;
         }
      }

      return null;
   }

   public String id() {
      return this.id;
   }

   public String translationKey() {
      return this.translationKey;
   }
}
