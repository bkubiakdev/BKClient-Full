package dev.bkclient.systems.emotes;

import java.util.List;
import net.minecraft.util.Identifier;

/** All emotes of the wheel. Several animations in one emote are variants played one after another on repeated use. */
public record EmoteRegistry(String id, String title, String file, List<String> animations, boolean loop) {
   public static final List<EmoteRegistry> ALL = List.of(
      emote("wave", "Machanie ręką", "wave", false),
      one("wiggling_torso", "Kręcenie tułowiem", "basic_dances", true, "wiggling_torso"),
      emote("twerk", "Twerk", "twerk", true),
      emote("take_the_l", "Taniec przegranego", "take_the_l", true),
      emote("gangnam_style", "Gangnam Style", "gangnam_style", true),
      emote("sit", "Siad", "sit", true),
      emote("backflip", "Salto w tył", "backflip", false),
      emote("cheer", "Radość", "cheer", true),
      emote("shrug", "Wzruszenie ramion", "shrug", false),
      emote("hug", "Przytulenie", "hug", false),
      emote("laugh", "Śmiech", "laugh", true),
      emote("yawn", "Ziewnięcie", "yawn", false),
      emote("hands_up", "Ręce w górę", "hands_up", true),
      emote("flex_muscle", "Pokaz mięśni", "flex_muscle", false),
      emote("arms_crossed", "Skrzyżowane ręce", "arms_crossed", true),
      emote("hands_on_hips", "Ręce na biodrach", "hands_on_hips", true),
      one("head_nod", "Kiwnięcie głową", "head_affirm", false, "haed_nod"),
      one("head_shake", "Kręcenie głową", "head_affirm", false, "haed_shake"),
      emote("arm_stretch", "Rozciąganie rąk", "arm_stretch", true),
      one("side_step_shuffle", "Szuflowanie w bok", "basic_dances", true, "side_step_shuffle"),
      one("arm_wave_dance", "Fala rękami", "basic_dances", true, "arm_wave_dance"),
      emote("air_drum", "Perkusja w powietrzu", "air_drum", true),
      emote("robot_dance", "Taniec robota", "robot_dance", true),
      emote("hide_barrel", "Schowanie się", "hide", true),
      variants("clap", "Brawa", "clap", true, "clap", "clap2", "clap3"),
      variants("point", "Wskazanie", "point", false, "point", "point2"),
      variants("cry", "Płacz", "cry", true, "cry", "cry2"),
      variants("salute", "Salut", "salute", true, "salute", "salute2"),
      variants("facepalm", "Facepalm", "facepalm", false, "facepalm", "facepalm2", "facepalm3"),
      variants("kneel", "Klęknięcie", "kneel", true, "kneel", "kneel2", "kneel3"),
      variants("lay", "Położenie się", "lay", true, "lay", "lay2", "lay3"),
      variants("bow", "Ukłon", "bow", true, "bow", "bow2", "bow3"),
      variants("lean_to_left", "Pochylenie", "lean", true, "lean_to_left", "lean_to_right", "lean_to_back"),
      emote("floss", "Floss", "floss", true),
      emote("macarena", "Macarena", "macarena", true),
      emote("orange_justice", "Orange Justice", "orange_justice", true),
      emote("disco_fever", "Gorączka disco", "disco_fever", true),
      emote("kazotsky_kick", "Kozacki taniec", "kazotsky_kick", true));

   /** The wheel icon; two emotes reuse the icon of another one. */
   public Identifier icon() {
      String name = switch (this.id) {
         case "twerk" -> "buttsit";
         case "take_the_l" -> "robot_dance";
         default -> this.id;
      };
      return Identifier.of("bkclient", "textures/emotes/" + name + ".png");
   }

   private static EmoteRegistry variants(String id, String title, String file, boolean loop, String... animations) {
      return new EmoteRegistry(id, title, file, List.of(animations), loop);
   }

   private static EmoteRegistry one(String id, String title, String file, boolean loop, String animation) {
      return new EmoteRegistry(id, title, file, List.of(animation), loop);
   }

   private static EmoteRegistry emote(String id, String title, String file, boolean loop) {
      return one(id, title, file, loop, id);
   }
}
