package dev.bkclient.systems.emotes;

import dev.bkclient.systems.modules.modules.visuals.Emotes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;

/**
 * The emote that is playing for the local player (and, with "Nearby Dance", copied by players around). It only lives on
 * this client: other players cannot see it unless they run the same client.
 */
public final class EmoteManager {
   /** What a model needs to pose a player this frame. */
   public record Active(EmoteAnimation animation, float time, float weight) {
   }

   private static final float FADE_SECONDS = 0.18F;
   private static final float NANOS = 1.0E9F;
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private static EmoteRegistry emote;
   private static EmoteAnimation animation;
   private static long startNanos;
   private static long stopNanos;
   private static int startTick;
   private static final Map<String, Integer> variantCounter = new HashMap<>();
   private static Perspective previousPerspective;

   private EmoteManager() {
   }

   public static boolean isPlaying() {
      return emote != null;
   }

   public static EmoteRegistry current() {
      return emote;
   }

   /** Starts an emote (the next variant for one that has several); selecting the one that already plays stops it. */
   public static void toggle(EmoteRegistry selected) {
      if (emote == selected && stopNanos == 0L) {
         stop();
         return;
      }
      play(selected);
   }

   public static void play(EmoteRegistry selected) {
      Emotes module = Emotes.getInstance();
      if (module == null || !module.isEnabled() || mc.player == null) return;
      int index = variantCounter.getOrDefault(selected.id(), 0);
      String name = selected.animations().get(index % selected.animations().size());
      variantCounter.put(selected.id(), (index + 1) % selected.animations().size());
      emote = selected;
      animation = EmoteAnimation.load(selected.file(), name);
      startNanos = System.nanoTime();
      stopNanos = 0L;
      startTick = mc.player.age;
      if (module.autoThirdPerson() && mc.options.getPerspective().isFirstPerson()) {
         previousPerspective = mc.options.getPerspective();
         mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
      }
   }

   /** Fades the emote out; it is removed once the fade ends. */
   public static void stop() {
      if (emote != null && stopNanos == 0L) stopNanos = System.nanoTime();
   }

   /** Forgets everything at once (module off, world change). */
   public static void reset() {
      emote = null;
      animation = null;
      stopNanos = 0L;
      restorePerspective();
   }

   public static void tick() {
      if (emote == null) return;
      Emotes module = Emotes.getInstance();
      if (module == null || !module.isEnabled() || mc.player == null || mc.world == null) {
         reset();
         return;
      }
      long now = System.nanoTime();
      if (stopNanos != 0L) {
         if ((now - stopNanos) / NANOS >= FADE_SECONDS) reset();
         return;
      }
      // A few ticks of grace, so the key that opened the wheel does not cancel the emote at once.
      if (module.cancelOnMove() && mc.player.age - startTick > 3 && isMoving()) {
         stop();
         return;
      }
      if (!emote.loop() && (now - startNanos) / NANOS * module.speed() >= animation.length()) stop();
   }

   private static boolean isMoving() {
      var player = mc.player;
      return player.input.movementForward != 0.0F || player.input.movementSideways != 0.0F
         || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed() || player.isGliding() || player.isSwimming();
   }

   private static void restorePerspective() {
      if (previousPerspective != null && mc.options != null) mc.options.setPerspective(previousPerspective);
      previousPerspective = null;
   }

   /** The pose to apply to the player with this UUID right now, or null when no emote concerns them. */
   public static Active activeFor(UUID uuid) {
      if (emote == null || animation == null || uuid == null || mc.player == null || mc.world == null) return null;
      Emotes module = Emotes.getInstance();
      if (module == null || !module.isEnabled()) return null;
      if (!uuid.equals(mc.player.getUuid())) {
         if (!module.nearbyDance()) return null;
         PlayerEntity other = mc.world.getPlayerByUuid(uuid);
         double range = module.nearbyRange();
         if (other == null || mc.player.squaredDistanceTo(other) > range * range) return null;
      }
      long now = System.nanoTime();
      float seconds = (now - startNanos) / NANOS * module.speed();
      float time = animation.wrap(seconds, emote.loop());
      float fadeIn = smooth(Math.min(1.0F, (now - startNanos) / NANOS / FADE_SECONDS));
      float fadeOut = stopNanos == 0L ? 1.0F : smooth(Math.max(0.0F, 1.0F - (now - stopNanos) / NANOS / FADE_SECONDS));
      return new Active(animation, time, Math.min(fadeIn, fadeOut));
   }

   private static float smooth(float t) {
      return t * t * (3.0F - 2.0F * t);
   }
}
