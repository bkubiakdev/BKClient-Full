package dev.bkclient.systems.update;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.notifications.NotificationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One-shot, non-blocking update check ported from BK-Client. */
public final class UpdateChecker {
   private static final String VERSION_URL = "https://changeme.net/api/mods/8/version";
   private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
   private static boolean checked;

   private UpdateChecker() {
   }

   public static synchronized void checkOnce() {
      if (checked) return;
      checked = true;
      Thread worker = new Thread(UpdateChecker::runCheck, "BKClient-UpdateChecker");
      worker.setDaemon(true);
      worker.start();
   }

   private static void runCheck() {
      try {
         String current = FabricLoader.getInstance().getModContainer(BKClientInit.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("0");
         String latest = fetchLatestVersion();
         if (latest == null || !isOlder(current, latest)) return;
         MinecraftClient.getInstance().execute(() ->
            BKClientInit.getInstance().getNotificationManager().addNotificationOther(
               NotificationType.INFO, "Update available",
               "BK-Client " + latest + " is available (installed: " + current + ").", 10000L));
      } catch (Exception error) {
         BKClientInit.LOGGER.debug("Update check skipped", error);
      }
   }

   private static String fetchLatestVersion() {
      try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(VERSION_URL))
            .timeout(Duration.ofSeconds(5)).header("User-Agent", "BK-Client").GET().build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         if (response.statusCode() != 200) return null;
         Matcher matcher = VERSION_PATTERN.matcher(response.body());
         return matcher.find() ? matcher.group(1).trim() : null;
      } catch (Exception ignored) {
         return null;
      }
   }

   static boolean isOlder(String current, String latest) {
      String[] left = current.split("\\.");
      String[] right = latest.split("\\.");
      int length = Math.max(left.length, right.length);
      for (int i = 0; i < length; i++) {
         int a = i < left.length ? parse(left[i]) : 0;
         int b = i < right.length ? parse(right[i]) : 0;
         if (a != b) return a < b;
      }
      return false;
   }

   private static int parse(String value) {
      int result = 0;
      for (int i = 0; i < value.length(); i++) {
         char character = value.charAt(i);
         if (!Character.isDigit(character)) break;
         result = result * 10 + character - '0';
      }
      return result;
   }
}
