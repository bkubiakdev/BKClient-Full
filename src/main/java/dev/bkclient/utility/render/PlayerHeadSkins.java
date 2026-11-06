package dev.bkclient.utility.render;

import com.google.gson.JsonParser;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Shared, bounded skin lookup for senders absent from the server's player list. */
public final class PlayerHeadSkins implements IMinecraft {
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
   private static final class Entry {
      volatile Identifier skin;
      volatile long retryAt = Long.MAX_VALUE;
   }
   private PlayerHeadSkins() { }

   public static Identifier get(String name) {
      String key = name.toLowerCase(Locale.ROOT);
      UUID fallbackId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
      Identifier fallback = DefaultSkinHelper.getSkinTextures(fallbackId).texture();
      if (mc.getNetworkHandler() != null) {
         var player = mc.getNetworkHandler().getPlayerList().stream()
            .filter(p -> p.getProfile().getName().equalsIgnoreCase(name)).findFirst().orElse(null);
         if (player != null) {
            fallback = player.getSkinTextures().texture();
            if (player.getProfile().getProperties().containsKey("textures")) return fallback;
         }
      }
      if (!name.matches("[A-Za-z0-9_]{1,16}")) return fallback;
      Entry cached = CACHE.get(key);
      long now = System.nanoTime();
      if (cached != null && now >= cached.retryAt) { CACHE.remove(key, cached); cached = null; }
      if (cached == null && CACHE.size() >= 256)
         CACHE.entrySet().removeIf(item -> now >= item.getValue().retryAt);
      if (cached == null && CACHE.size() < 256) {
         Entry created = new Entry();
         cached = CACHE.putIfAbsent(key, created);
         if (cached == null) { cached = created; fetch(name, created); }
      }
      return cached != null && cached.skin != null ? cached.skin : fallback;
   }

   private static void fetch(String name, Entry entry) {
      var sessionService = mc.getSessionService();
      var request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
         .timeout(Duration.ofSeconds(8)).GET().build();
      HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
         .thenApplyAsync(response -> {
            if (response.statusCode() != 200) throw new CompletionException(new IllegalStateException("Profile unavailable"));
            String raw = JsonParser.parseString(response.body()).getAsJsonObject().get("id").getAsString();
            if (!raw.matches("[0-9a-fA-F]{32}")) throw new IllegalArgumentException("Invalid profile id");
            UUID id = UUID.fromString(raw.substring(0,8)+"-"+raw.substring(8,12)+"-"+raw.substring(12,16)+"-"+raw.substring(16,20)+"-"+raw.substring(20));
            var result = sessionService.fetchProfile(id, true);
            if (result == null) throw new IllegalStateException("No skin profile");
            return result.profile();
         }).thenCompose(profile -> {
            // Texture registration belongs to the client thread; the provider downloads asynchronously.
            var texture = new CompletableFuture<Optional<net.minecraft.client.util.SkinTextures>>();
            mc.execute(() -> {
               try { mc.getSkinProvider().fetchSkinTextures(profile).whenComplete((value, error) -> {
                  if (error != null) texture.completeExceptionally(error); else texture.complete(value);
               }); } catch (Exception failure) { texture.completeExceptionally(failure); }
            });
            return texture;
         }).orTimeout(20, TimeUnit.SECONDS).whenComplete((skin, failure) -> {
            if (failure == null && skin != null && skin.isPresent()) {
               entry.skin = skin.get().texture();
               entry.retryAt = System.nanoTime() + TimeUnit.MINUTES.toNanos(30);
            } else entry.retryAt = System.nanoTime() + TimeUnit.MINUTES.toNanos(2);
         });
   }
}
