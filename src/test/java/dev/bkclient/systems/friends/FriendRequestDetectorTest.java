package dev.bkclient.systems.friends;

public final class FriendRequestDetectorTest {
   public static void main(String[] args) {
      check("Received friend request from xvy", "xvy");
      check("[Friends] xvy sent you a friend request!", "xvy");
      check("Otrzymano zaproszenie do znajomych od xvy", "xvy");
      check("§a[VIP] Alice wysłał ci zaproszenie do znajomych", "Alice");
      check("Received friend request from Local", null);
      check("Gracz test4535351 wyslal prosbe o teleportacje! Wpisz /tpaccept", null);
      check("Gracz test4535351 wys\u0142a\u0142 pro\u015bb\u0119 o teleportacj\u0119! Wpisz /tpaccept", null);
      check("Alice sent you a teleport request", null);
      check("Alice wyslal ci zaproszenie do gildii", null);
      check("Alice wyslal ci prosbe do znajomych", "Alice");
      if (FriendRequestDetector.findSender("Player joined the game", "Local") != null) {
         throw new AssertionError("ordinary chat must not trigger a request");
      }
      if (FriendRequestDetector.findSender("Received friend request from xvy", "xvy") != null) {
         throw new AssertionError("self request must be ignored");
      }
      System.out.println("Friend request detection checks passed");
   }

   private static void check(String message, String expected) {
      String actual = FriendRequestDetector.findSender(message, "Local");
      if (expected == null ? actual != null : !expected.equals(actual)) {
         throw new AssertionError(message + " -> " + actual);
      }
   }
}
