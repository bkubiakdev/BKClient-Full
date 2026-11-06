package dev.bkclient.systems.friends;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts a player name from the server's friend-request messages. */
public final class FriendRequestDetector {
   private static final String NAME = "([A-Za-z0-9_]{1,16})";
   private static final String REQUEST = "(?:friend\\s+request|(?:zaproszenie|pro[\\u015bs]b[\\u0119ea])\\s+do\\s+znajomych)";
   private static final Pattern RECEIVED = Pattern.compile(
      "(?:received|got|otrzymano|otrzyma(?:łeś|les)|dosta(?:łeś|les))\\s+(?:a\\s+)?" + REQUEST
         + "\\s+(?:from|od)\\s+" + NAME, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
   private static final Pattern SENT = Pattern.compile(
      "\\b" + NAME + "\\s+(?:sent|has\\s+sent|wysłał|wyslal|przesłał|przeslal)\\s+(?:(?:you|ci)\\s+)?(?:a\\s+)?" + REQUEST,
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
   private static final Pattern FROM = Pattern.compile(
      REQUEST + "\\s+(?:from|od)\\s+" + NAME, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

   private FriendRequestDetector() {
   }

   public static String findSender(String raw, String self) {
      if (raw == null || raw.isBlank()) {
         return null;
      }
      String message = raw.replaceAll("(?i)§[0-9a-fk-orx]", "")
         .replace('\u00a0', ' ')
         .strip();
      for (Pattern pattern : new Pattern[]{RECEIVED, SENT, FROM}) {
         Matcher matcher = pattern.matcher(message);
         if (!matcher.find()) {
            continue;
         }
         String sender = matcher.group(matcher.groupCount());
         if (isValidSender(sender, self)) {
            return sender;
         }
      }
      return null;
   }

   private static boolean isValidSender(String sender, String self) {
      if (sender == null || !sender.matches("[A-Za-z0-9_]{1,16}")) {
         return false;
      }
      if (self != null && sender.equalsIgnoreCase(self)) {
         return false;
      }
      return switch (sender.toLowerCase(Locale.ROOT)) {
         case "friend", "request", "received", "otrzymano", "from", "od" -> false;
         default -> true;
      };
   }
}
