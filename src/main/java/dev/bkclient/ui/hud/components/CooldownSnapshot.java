package dev.bkclient.ui.hud.components;

import dev.bkclient.mixin.accessors.ItemCooldownEntryAccessor;
import dev.bkclient.mixin.accessors.ItemCooldownManagerAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Reads one row per cooldown group, including the last consumed vanilla item. */
public final class CooldownSnapshot {
   public record Entry(Identifier group, Item item, String label, int remainingTicks, int totalTicks) {
      public float fraction() { return Math.clamp(remainingTicks / (float)Math.max(1, totalTicks), 0, 1); }
      public String time() { return formatTime(remainingTicks); }
   }
   private record Label(Item item, String name) { }
   private final Map<Identifier, Label> labels = new HashMap<>();

   public List<Entry> read(ItemCooldownManager cooldowns, Iterable<ItemStack> inventory) {
      var manager = (ItemCooldownManagerAccessor)cooldowns;
      var entries = manager.getEntries();
      if (entries.isEmpty()) { labels.clear(); return new ArrayList<>(); }
      for (ItemStack stack : inventory) {
         if (stack == null || stack.isEmpty()) continue;
         Identifier group = manager.invokeGetGroup(stack);
         if (entries.containsKey(group)) labels.putIfAbsent(group, new Label(stack.getItem(), stack.getName().getString()));
      }
      labels.keySet().retainAll(entries.keySet());
      List<Entry> rows = new ArrayList<>();
      for (var entry : entries.entrySet()) {
         if (!(entry.getValue() instanceof ItemCooldownEntryAccessor timer)) continue;
         int remaining = timer.getEndTick() - manager.getTick();
         if (remaining <= 0) continue;
         Label label = labels.get(entry.getKey());
         if (label == null && Registries.ITEM.containsId(entry.getKey())) {
            Item item = Registries.ITEM.get(entry.getKey());
            label = new Label(item, item.getName().getString());
         }
         if (label != null) rows.add(new Entry(entry.getKey(), label.item(), label.name(), remaining,
            Math.max(1, timer.getEndTick() - timer.getStartTick())));
      }
      rows.sort(java.util.Comparator.comparing(row -> row.group().toString()));
      return rows;
   }
   public void clear() { labels.clear(); }
   /** Whether any cooldown group is tracked at all; lets callers skip collecting the inventory. */
   public static boolean hasEntries(ItemCooldownManager cooldowns) {
      return !((ItemCooldownManagerAccessor)cooldowns).getEntries().isEmpty();
   }

   /** Formatted labels per displayed value; strings are immutable, so a racy fill is harmless. */
   private static final String[] TENTHS = new String[601];
   private static final Map<Integer, String> MINUTES = new HashMap<>();

   public static String formatTime(int ticks) {
      int positive = Math.max(0, ticks);
      if (positive >= 1200) {
         int seconds = (positive + 19) / 20;
         synchronized (MINUTES) {
            String cached = MINUTES.get(seconds);
            if (cached == null) {
               if (MINUTES.size() >= 256) MINUTES.clear();
               cached = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
               MINUTES.put(seconds, cached);
            }
            return cached;
         }
      }
      int tenths = (positive + 1) / 2;
      String cached = TENTHS[tenths];
      if (cached == null) {
         cached = tenths % 10 == 0 ? tenths / 10 + "s" : String.format(Locale.ROOT, "%.1fs", tenths / 10f);
         TENTHS[tenths] = cached;
      }
      return cached;
   }
}
