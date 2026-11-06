package dev.bkclient.systems.emotes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * One emote animation in the Bedrock-style JSON used by the bundled files ({@code assets/bkclient/player_animation}):
 * bones with {@code rotation} (degrees) and {@code position} (model pixels) keyframes, each with an easing towards it.
 * Bend channels are not used.
 */
public final class EmoteAnimation {
   /** A bone's value at one moment; rotation in degrees, position in model pixels. */
   public record Pose(boolean hasRotation, float rx, float ry, float rz, boolean hasPosition, float px, float py, float pz) {
      static final Pose NONE = new Pose(false, 0, 0, 0, false, 0, 0, 0);
   }

   private record Key(float time, float[] vector, String easing) {
   }

   private static final Map<String, EmoteAnimation> CACHE = new HashMap<>();

   private float length;
   private final float loopStart;
   private final float loopEnd;
   private final Map<String, Map<String, List<Key>>> bones = new HashMap<>();

   private EmoteAnimation(JsonObject animation) {
      this.length = animation.has("animation_length") ? animation.get("animation_length").getAsFloat() : 1.0F;
      this.loopStart = animation.has("loop_start") ? animation.get("loop_start").getAsFloat() : 0.0F;
      this.loopEnd = animation.has("loop_end") ? animation.get("loop_end").getAsFloat() : 0.0F;
      if (!animation.has("bones")) return;
      for (var bone : animation.getAsJsonObject("bones").entrySet()) {
         Map<String, List<Key>> channels = new HashMap<>();
         for (String channel : new String[]{"rotation", "position"}) {
            JsonElement element = bone.getValue().getAsJsonObject().get(channel);
            if (element == null) continue;
            List<Key> keys = new ArrayList<>();
            if (element.isJsonArray()) {
               // A bare vector is a pose held for the whole animation.
               keys.add(new Key(0.0F, vector(element), "linear"));
            } else if (element.isJsonObject() && element.getAsJsonObject().has("vector")) {
               keys.add(new Key(0.0F, vector(element.getAsJsonObject().get("vector")), "linear"));
            } else if (element.isJsonObject()) {
               for (var frame : element.getAsJsonObject().entrySet()) {
                  float time = Float.parseFloat(frame.getKey());
                  if (frame.getValue().isJsonArray()) {
                     keys.add(new Key(time, vector(frame.getValue()), "linear"));
                  } else if (frame.getValue().isJsonObject() && frame.getValue().getAsJsonObject().has("vector")) {
                     JsonObject data = frame.getValue().getAsJsonObject();
                     keys.add(new Key(time, vector(data.get("vector")), data.has("easing") ? data.get("easing").getAsString() : "linear"));
                  }
               }
            }
            keys.sort(Comparator.comparingDouble(Key::time));
            if (!keys.isEmpty()) channels.put(channel, keys);
         }
         this.bones.put(bone.getKey(), channels);
      }
      if (!animation.has("animation_length")) {
         float last = 0.0F;
         for (var channels : this.bones.values()) for (var keys : channels.values()) last = Math.max(last, keys.get(keys.size() - 1).time());
         this.length = Math.max(1.0F, last);
      }
   }

   private static float[] vector(JsonElement element) {
      var array = element.getAsJsonArray();
      return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
   }

   /** Loads the named animation from a file, or the first animation of the file when the name is not in it. */
   public static EmoteAnimation load(String file, String name) {
      String cacheKey = file + "#" + name;
      EmoteAnimation cached = CACHE.get(cacheKey);
      if (cached != null) return cached;
      EmoteAnimation loaded = empty();
      try {
         var resource = MinecraftClient.getInstance().getResourceManager().getResource(Identifier.of("bkclient", "player_animation/" + file + ".json"));
         if (resource.isPresent()) {
            try (var reader = new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8)) {
               JsonObject animations = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
               JsonObject chosen = animations.has(name) ? animations.getAsJsonObject(name) : animations.entrySet().iterator().next().getValue().getAsJsonObject();
               loaded = new EmoteAnimation(chosen);
            }
         }
      } catch (Exception ignored) {
         // A missing or broken file leaves an empty animation: the emote simply does nothing.
      }
      CACHE.put(cacheKey, loaded);
      return loaded;
   }

   private static EmoteAnimation empty() {
      JsonObject none = new JsonObject();
      none.addProperty("animation_length", 1.0F);
      return new EmoteAnimation(none);
   }

   public float length() {
      return this.length;
   }

   /** Maps elapsed seconds to a time inside the animation; a looping one repeats from its loop start. */
   public float wrap(float seconds, boolean loop) {
      if (!loop) return Math.min(seconds, this.length);
      float end = this.loopEnd > this.loopStart ? this.loopEnd : this.length;
      if (end <= 0.0F || seconds < end) return seconds;
      float span = end - this.loopStart;
      return span <= 1.0E-4F ? seconds : this.loopStart + (seconds - this.loopStart) % span;
   }

   public Pose sample(String bone, float time) {
      Map<String, List<Key>> channels = this.bones.get(bone);
      if (channels == null) return Pose.NONE;
      float[] rotation = value(channels.get("rotation"), time);
      float[] position = value(channels.get("position"), time);
      return new Pose(rotation != null, rotation == null ? 0 : rotation[0], rotation == null ? 0 : rotation[1], rotation == null ? 0 : rotation[2],
         position != null, position == null ? 0 : position[0], position == null ? 0 : position[1], position == null ? 0 : position[2]);
   }

   private static float[] value(List<Key> keys, float time) {
      if (keys == null) return null;
      Key first = keys.get(0);
      if (time <= first.time()) return first.vector();
      Key last = keys.get(keys.size() - 1);
      if (time >= last.time()) return last.vector();
      for (int i = 0; i < keys.size() - 1; i++) {
         Key a = keys.get(i);
         Key b = keys.get(i + 1);
         if (time < a.time() || time >= b.time()) continue;
         if ("step".equals(b.easing())) return a.vector();
         float f = ease(b.easing(), (time - a.time()) / Math.max(1.0E-5F, b.time() - a.time()));
         return new float[]{
            a.vector()[0] + (b.vector()[0] - a.vector()[0]) * f,
            a.vector()[1] + (b.vector()[1] - a.vector()[1]) * f,
            a.vector()[2] + (b.vector()[2] - a.vector()[2]) * f};
      }
      return last.vector();
   }

   private static float ease(String name, float t) {
      return switch (name) {
         case "easeInSine" -> 1.0F - (float)Math.cos(t * Math.PI / 2.0);
         case "easeOutSine" -> (float)Math.sin(t * Math.PI / 2.0);
         case "easeInOutSine" -> -((float)Math.cos(Math.PI * t) - 1.0F) / 2.0F;
         case "easeInQuad" -> t * t;
         case "easeOutQuad" -> 1.0F - (1.0F - t) * (1.0F - t);
         case "easeInOutQuad" -> t < 0.5F ? 2.0F * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 2.0) / 2.0F;
         case "easeInCubic" -> t * t * t;
         case "easeOutCubic" -> 1.0F - (float)Math.pow(1.0F - t, 3.0);
         case "easeInOutCubic" -> t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
         default -> t;
      };
   }
}
