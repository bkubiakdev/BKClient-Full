package dev.bkclient.systems.modules.modules.bkmods.automarket;

import dev.bkclient.systems.setting.settings.ItemListSetting;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public final class BannedLoreTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        AutoMarketScanner scanner = new AutoMarketScanner();
        PriceEntry entry = new PriceEntry("", "diamond_sword");
        List<Text> screenshot = List.of(Text.literal("Otrucie przeciwnika I"));
        check(scanner.findBannedLore(entry, screenshot) == null, "Old entries remain unrestricted");
        entry.setBannedLore("Otrucie");
        check("Otrucie".equals(scanner.findBannedLore(entry, screenshot)), "Screenshot must be rejected");
        check(scanner.findBannedLore(entry, List.of(Text.literal("Odporność I"))) == null, "Unrelated lore allowed");
        check(scanner.findBannedLore(entry, List.of()) == null, "Absent lore allowed");
        check(scanner.findBannedLore(entry, List.of(Text.literal("§E§LOTRUCIE przeciwnika I"))) != null, "Case and legacy colors ignored");
        check(scanner.findBannedLore(entry, List.of(Text.literal("Otrucie").styled(s -> s.withColor(0xFFFF00)).append(" przeciwnika I"))) != null, "Styled text supported");
        entry.setBannedLore(" ; Spowolnienie;\r\n Otrucie ;");
        check("Otrucie".equals(scanner.findBannedLore(entry, screenshot)), "Multiple phrases use OR, ignore blanks");
        entry.setBannedLore("Otrucie przeciwnika");
        check(scanner.findBannedLore(entry, List.of(Text.literal("Otrucie\u00a0przeciwnika I"))) != null, "NBSP normalized");
        check(scanner.findBannedLore(entry, List.of(Text.literal("Otrucie"), Text.literal("przeciwnika I"))) == null, "Do not join unrelated lines");
        entry.setBannedLore(".*");
        check(scanner.findBannedLore(entry, screenshot) == null, "Phrases are literal, never regex");
        entry.setBannedLore(" ; §e ;\n");
        check(scanner.findBannedLore(entry, screenshot) == null, "Empty/color-only fragments do not reject everything");

        var settings = new ArrayList<dev.bkclient.systems.setting.Setting>();
        var registry = new ItemListSetting(() -> settings, "items");
        entry.setBannedLore("Otrucie; Spowolnienie");
        entry.setLore("Required text");
        entry.setMaxPrice(123);
        entry.setSellEnabled(true);
        registry.getValue().add(entry);
        var saved = registry.save();
        registry.load(saved);
        var loaded = registry.getValue().getFirst();
        check(loaded.getBannedLore().equals(entry.getBannedLore()), "Config round trip retains phrases");
        check(loaded.getLore().equals("Required text") && loaded.getMaxPrice() == 123 && loaded.isSellEnabled(), "Other settings preserved");
        saved.getAsJsonArray().get(0).getAsJsonObject().remove("bannedLore");
        registry.load(saved);
        check(registry.getValue().getFirst().getBannedLore().isEmpty(), "Old config defaults empty");
        entry.setBannedLore(null);
        check(entry.getBannedLore().isEmpty(), "Null means disabled");

        System.out.println("Banned lore: literal matching, formatting, empty/default and config persistence passed");
    }

    /** Run through Fabric, where Minecraft registry access transformations are applied. */
    public static void runIntegration() throws Exception {
        AutoMarketScanner scanner = new AutoMarketScanner();
        PriceEntry entry = new PriceEntry("", "diamond_sword");
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        List<Text> raw = List.of(Text.literal("Otrucie przeciwnika I"), Text.literal("Cena: 10$"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(raw));
        entry.setLore(null);
        entry.setBannedLore("Otrucie");
        var match = AutoMarketScanner.class.getDeclaredMethod("matchesEntry", ItemStack.class, List.class, List.class, PriceEntry.class, String.class, String.class);
        match.setAccessible(true);
        // Pass an empty filtered list: the ban must still inspect raw lore, not a filtered tooltip.
        check(!(boolean)match.invoke(scanner, stack, raw, List.of(), entry, "", ""), "Real purchase matcher rejects banned raw lore");
        var inventory = new net.minecraft.inventory.SimpleInventory(stack);
        var slot = new net.minecraft.screen.slot.Slot(inventory, 0, 0, 0);
        check(!scanner.evaluateSlot(slot, java.util.Map.of("diamond_sword", List.of(entry)), false).matched, "Scanner rejects banned offer");
        entry.setBannedLore("");
        check(scanner.evaluateSlot(slot, java.util.Map.of("diamond_sword", List.of(entry)), false).matched, "Scanner accepts eligible offer after clearing ban");
        check((boolean)match.invoke(scanner, stack, raw, List.of(), entry, "", ""), "Clearing ban restores purchase matching");
        entry.setBannedLore("Pet wygaśnie");
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("Pet wygaśnie za: 5d 2h"))));
        check(!(boolean)match.invoke(scanner, stack, List.of(), List.of(), entry, "", ""), "Explicit ban also applies to otherwise ignored dynamic lines");
        entry.setBannedLore("Otrucie");
        stack.set(DataComponentTypes.LORE, new LoreComponent(raw));
        var debug = AutoMarketScanner.class.getDeclaredMethod("debugAgainstEntry", ItemStack.class, PriceEntry.class, boolean.class);
        debug.setAccessible(true);
        Object report = debug.invoke(scanner, stack, entry, false);
        var issues = report.getClass().getDeclaredField("issues");
        issues.setAccessible(true);
        check(issues.get(report).toString().contains("REJECT: banned lore - Otrucie"), "Ctrl+RMB diagnostics expose matching phrase");
        System.out.println("Banned lore: matching, formatting, empty/default, persistence, purchase and diagnostics passed");
    }
}
