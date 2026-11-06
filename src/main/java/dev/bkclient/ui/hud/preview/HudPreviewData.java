package dev.bkclient.ui.hud.preview;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

/** Local-only data used to preview HUD widgets while the chat editor is open. */
public final class HudPreviewData {
   public record Row(Item item, String label, String value) {}

   public static final List<Row> COOLDOWNS = List.of(
      new Row(Items.ENDER_PEARL, "Ender Pearl", "0:05"),
      new Row(Items.CHORUS_FRUIT, "Chorus Fruit", "1:23"),
      new Row(Items.GOLDEN_APPLE, "Golden Apple", "0:01"),
      new Row(Items.ENCHANTED_GOLDEN_APPLE, "Notch Apple", "0:42")
   );

   public static final List<Row> MARKET = List.of(
      new Row(Items.TOTEM_OF_UNDYING, "Totem of Undying", "$1.25M"),
      new Row(Items.NETHERITE_SWORD, "Netherite Sword", "$820K"),
      new Row(Items.ELYTRA, "Elytra", "$3.40M")
   );

   public static final List<Row> GEAR = List.of(
      new Row(Items.NETHERITE_HELMET, "Enemy Helmet", "84%"),
      new Row(Items.NETHERITE_CHESTPLATE, "Enemy Chestplate", "61%"),
      new Row(Items.TOTEM_OF_UNDYING, "Offhand Totem", "x2")
   );

   private HudPreviewData() {}
}
