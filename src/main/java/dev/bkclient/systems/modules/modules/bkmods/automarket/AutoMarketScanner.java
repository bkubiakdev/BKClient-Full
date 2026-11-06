package dev.bkclient.systems.modules.modules.bkmods.automarket;

import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import dev.bkclient.utility.game.XSK;
import dev.bkclient.utility.inventory.EnchantmentUtility;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.text.Normalizer;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Silnik skanowania i dopasowywania przedmiot\u00F3w.
 * Obs\u0142uguje dynamiczne regexy dla cen i sprzedawc\u00F3w (Parzysto\u015B\u0107 1:1 z
 * bk-rynek).
 */
public final class AutoMarketScanner {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("\u00A7[0-9a-fk-or]");
    private static final java.util.regex.Pattern BK_RE_1 = java.util.regex.Pattern.compile(".*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*");
    private static final java.util.regex.Pattern BK_RE_2 = java.util.regex.Pattern.compile("[^0-9.,]");
    private static final java.util.regex.Pattern BK_RE_3 = java.util.regex.Pattern.compile("^[+-]?\\d+.*");
    private static final java.util.regex.Pattern BK_RE_4 = java.util.regex.Pattern.compile(".*\\b(i|ii|iii|iv|v|vi|vii|viii|ix|x|1|2|3|4|5|6|7|8|9|10)\\b.*");
    private static final java.util.regex.Pattern BK_RE_5 = java.util.regex.Pattern.compile("[;\\n]+");
    private static final java.util.regex.Pattern BK_RE_6 = java.util.regex.Pattern.compile("\\s+");
    private static final java.util.regex.Pattern BK_RE_7 = java.util.regex.Pattern.compile("\u00a7x(\u00a7[0-9A-Fa-f]){6}");
    private static final java.util.regex.Pattern BK_RE_8 = java.util.regex.Pattern.compile("\u00a7[0-9A-FK-ORXa-fk-orx]");
    private static final java.util.regex.Pattern BK_RE_9 = java.util.regex.Pattern.compile("&(?=[0-9A-FK-ORXa-fk-orx])");
    private static final java.util.regex.Pattern BK_RE_10 = java.util.regex.Pattern.compile("(?s).*\u00a7[0-9A-FK-ORXa-fk-orx].*");
    private static final java.util.regex.Pattern BK_RE_11 = java.util.regex.Pattern.compile("(?i)\u00a7[ro]");
    private static final java.util.regex.Pattern BK_RE_12 = java.util.regex.Pattern.compile("[;\\r\\n]+");
    private static final java.util.regex.Pattern BK_RE_13 = java.util.regex.Pattern.compile("[\\p{Co}\u2022\u25E6\u25AA\u25AB\u25CF\u25CB]+");
    private static final java.util.regex.Pattern BK_RE_14 = java.util.regex.Pattern.compile("\\p{M}");
    private static final java.util.regex.Pattern BK_RE_15 = java.util.regex.Pattern.compile("[\u23AF\u2039\u203A\u00AB\u00BB\u2014\u2015\u2500\u25AC\u039E\u26A0\u00D7\u2022\u795E\u1410 \\-]+");
    private static final java.util.regex.Pattern BK_RE_16 = java.util.regex.Pattern.compile("\u00C2\u00A7x(\u00C2\u00A7[0-9A-Fa-f]){6}");
    private static final java.util.regex.Pattern BK_RE_17 = java.util.regex.Pattern.compile("\u00C2\u00A7[0-9A-FK-ORXa-fk-orx]");
    private static final java.util.regex.Pattern BK_RE_18 = java.util.regex.Pattern.compile("\u00A7x(\u00A7[0-9A-Fa-f]){6}");
    private static final java.util.regex.Pattern BK_RE_19 = java.util.regex.Pattern.compile("\u00A7[0-9A-FK-ORXa-fk-orx]");
    private static final java.util.regex.Pattern BK_RE_20 = java.util.regex.Pattern.compile("[^0-9.,A-Za-z]");
    private static final java.util.regex.Pattern BK_RE_21 = java.util.regex.Pattern.compile("\\$");
    private static final java.util.regex.Pattern BK_RE_22 = java.util.regex.Pattern.compile("[.,]");

    private static final String DEFAULT_PRICE_REGEX = "(?i).*(?:Koszt|Cena|Price).*?(?:\\$\\s*)?([\\d\\s.,]+(?:mld|[kmbt])?)\\s*\\$?.*";
    private String loreRegex = DEFAULT_PRICE_REGEX;
    private static final Pattern FALLBACK_PRICE_PATTERN = Pattern.compile(
            "(?i).*(?:koszt|cena|price).*?(?:\\$\\s*)?([\\d\\s.,]+(?:mld|[kmbt])?)\\s*\\$?.*");
    private String loreSellerRegex = "";
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    public volatile boolean debugMatch = false; // toggled via debug setting; no-op in clean/autocfg

    private static final Map<Character, Character> SMALL_FONT_MAP = new java.util.HashMap<>() {
        {
            put('\u1D00', 'a'); put('\u0299', 'b'); put('\u1D04', 'c'); put('\u1D05', 'd'); put('\u1D07', 'e');
            put('\uA730', 'f'); put('\u0262', 'g'); put('\u029C', 'h'); put('\u026A', 'i'); put('\u1D0A', 'j');
            put('\u1D0B', 'k'); put('\u029F', 'l'); put('\u1D0C', '\u0142'); put('\u1D0D', 'm'); put('\u0274', 'n'); put('\u1D0F', 'o');
            put('\u1D18', 'p'); put('\u0280', 'r'); put('\uA731', 's'); put('\u1D1B', 't'); put('\u1D1C', 'u');
            put('\u1D20', 'v'); put('\u1D21', 'w'); put('\u028F', 'y'); put('\u1D22', 'z');
            put('\u1D43', 'a'); put('\u1D47', 'b'); put('\u1D9C', 'c'); put('\u1D48', 'd'); put('\u1D49', 'e');
            put('\u1DA0', 'f'); put('\u1D4D', 'g'); put('\u02B0', 'h'); put('\u2071', 'i'); put('\u02B2', 'j');
            put('\u1D4F', 'k'); put('\u02E1', 'l'); put('\u1D50', 'm'); put('\u207F', 'n'); put('\u1D52', 'o');
            put('\u1D56', 'p'); put('\u02B3', 'r'); put('\u02E2', 's'); put('\u1D57', 't'); put('\u1D58', 'u');
            put('\u1D5B', 'v'); put('\u02B7', 'w'); put('\u02E3', 'x'); put('\u02B8', 'y'); put('\u1DBB', 'z');
            put('\u0493', 'f'); put('\u0492', 'F');
            put('\u0E18', '\u0142'); put('\u0E1B', '\u017C'); put('\u0E1E', '\u015B'); put('\u0E17', '\u0107'); put('\u0E16', '\u0119');
        }
    };

    private String mapSmallFont(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (SMALL_FONT_MAP.containsKey(c)) {
                sb.append(SMALL_FONT_MAP.get(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public AutoMarketScanner() {
    }


    public void setLoreRegex(String v) {
        String next = v == null ? "" : v;
        if (!java.util.Objects.equals(this.loreRegex, next)) {
            this.loreRegex = next;
            this.patternCache.clear();
        }
    }

    public void setLoreSellerRegex(String v) {
        String next = v == null ? "" : v;
        if (!java.util.Objects.equals(this.loreSellerRegex, next)) {
            this.loreSellerRegex = next;
            this.patternCache.clear();
        }
    }


    public static class ScanResult {
        public final boolean matched;
        public final boolean cheap;
        public final PriceEntry entry;
        public final double foundPrice;
        public final int slotId;
        public final String itemName;
        public final int stackSize;

        public ScanResult(boolean matched, boolean cheap, PriceEntry entry, double foundPrice, int slotId,
                String itemName, int stackSize) {
            this.matched = matched;
            this.cheap = cheap;
            this.entry = entry;
            this.foundPrice = foundPrice;
            this.slotId = slotId;
            this.itemName = itemName;
            this.stackSize = stackSize;
        }

        public static ScanResult noMatch() {
            return new ScanResult(false, false, null, -1, -1, "", 0);
        }
    }


    /**
     * Skanuje pojedynczy slot pod k\u0105tem dopasowania do listy cen.
     */
    public ScanResult evaluateSlot(Slot slot, Map<String, List<PriceEntry>> entryMap, boolean isSumMode) {
        if (slot == null || !slot.hasStack() || entryMap == null || entryMap.isEmpty())
            return ScanResult.noMatch();

        ItemStack stack = slot.getStack();

        String materialId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();
        List<PriceEntry> searchEntries = entryMap.get(materialId);
        if (searchEntries == null || searchEntries.isEmpty())
            return ScanResult.noMatch();

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null)
            return ScanResult.noMatch();

        List<String> loreLines = new ArrayList<>();
        List<Text> tooltip = lore.lines();
        boolean skipNext = false;
        for (Text line : tooltip) {
            String raw = dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(line);
            // mapSmallFont: Minestar pisze lore unicode small-capsami (\u1D21\u028F\u0262\u1D00\uA731\u1D00), kt\u00F3rych toLowerCase
            // NIE zamienia na 'wygasa' \u2014 bez mapowania filtr daty by nie zadzia\u0142a\u0142 na Minestar.
            String plain = mapSmallFont(BK_RE_0.matcher(raw).replaceAll("")).toLowerCase().trim();

            if (skipNext) { skipNext = false; continue; }
            if (AnarchiaPetExpiry.isExpiryLine(plain)) continue;
            // Skip variable pet lines: expiry date.
            // Anarchia: "wyga\u015Bnie: DD-MM-YYYY". Minestar: "ZWIERZ\u0104TKO/KOSTIUM WYGASA: DD.MM.YYYY HH:MM:SS".
            if (plain.contains("wyga\u015Bnie:") || plain.contains("wygasnie:") || plain.contains("wygasnie za")
                    || plain.contains("wygasa:") || plain.contains("wyga\u015Ba:")) continue;
            // Skip cipher label AND following UUID line
            if (plain.contains("szyfr")) { skipNext = true; continue; }
            // Also skip lines that contain "wygenerowany" (cipher header without "szyfr" keyword)
            if (plain.contains("wygenerowany") && plain.contains("unikalny")) { skipNext = true; continue; }
            // Skip any line containing a UUID (standalone or inline)
            if (BK_RE_1.matcher(plain).matches()) continue;

            loreLines.add(raw);
        }

        Integer daysLeft = remainingDays(tooltip);

        // Pre-compute name conversions once, reuse for all entries
        String itemName = dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(stack.getName());
        String itemNameClean = cleanForMatch(itemName);
        String itemNameStripped = stripColors(itemName);

        ScanResult bestMatch = null;

        for (PriceEntry entry : searchEntries) {
            if (!entry.isBuyEnabled())
                continue;

            // Pet/Kostium: skip if item expires too soon
            if (!AnarchiaPetExpiry.meetsMinimum(daysLeft, entry.getMinDaysLeft())) continue;

            if (matchesEntry(stack, tooltip, loreLines, entry, itemNameClean, itemNameStripped)) {
                // Price may live in the full tooltip (server-injected), not in LoreComponent \u2014 try both
                double foundPrice = extractPrice(loreLines);
                if (foundPrice < 0)
                    foundPrice = extractPrice(getFullTooltipLines(stack));
                if (foundPrice < 0)
                    continue;

                // Precision Fix: If we matched a line with an approximated price (like 1.1k),
                // search for a more precise version in brackets like ($1,199) in the SAME line.
                for (String line : loreLines) {
                    if (isCurrentLorePriceLine(line)) {
                        String clean = BK_RE_0.matcher(line).replaceAll("");
                        if (clean.contains("(") && clean.contains(")")) {
                            String brk = BK_RE_2.matcher(clean.substring(clean.indexOf("(") + 1, clean.indexOf(")"))).replaceAll("");
                            if (!brk.isEmpty()) {
                                double precise = parsePriceWithSuffix(brk);
                                if (precise > 0)
                                    foundPrice = precise;
                            }
                        }
                        break;
                    }
                }

                double max = entry.getMaxPrice();
                int stackSize = stack.getCount();

                dev.bkclient.systems.modules.modules.bkmods.AutoMarket autoMarket = dev.bkclient.BKClientInit
                        .getInstance().getModuleManager()
                        .getModule(dev.bkclient.systems.modules.modules.bkmods.AutoMarket.class);
                boolean isUnit = entry.isUnitMode()
                        || (autoMarket != null && autoMarket.unitModeGlobal.is(autoMarket.unitModeVal));

                // Count semantics:
                // Unit = at least this many items, because price is checked per item.
                // Sum = exactly this stack size, because price is checked for the whole stack.
                int requiredCount = Math.max(1, entry.getMinBuyCount());
                if (isUnit) {
                    if (stackSize < requiredCount) {
                        continue;
                    }
                } else if (stackSize != requiredCount) {
                    continue;
                }

                // 2. Unit Mode Price Check (Global or Per-Item)
                double effectivePrice = isUnit ? (foundPrice / Math.max(1, stackSize)) : foundPrice;

                if (max > 0 && effectivePrice > max) {
                    continue;
                }

                boolean isCheap = max <= 0 || effectivePrice < max;
                ScanResult candidate = new ScanResult(true, isCheap, entry, foundPrice, slot.id,
                        itemName, stackSize);

                // If we already have a match, keep the one with the tighter (lower) maxPrice
                if (bestMatch == null || (max > 0 && (bestMatch.entry.getMaxPrice() <= 0
                        || max < bestMatch.entry.getMaxPrice()))) {
                    bestMatch = candidate;
                }
            }
        }

        return bestMatch != null ? bestMatch : ScanResult.noMatch();
    }

    private static final java.util.Set<String> ENCHANTMENT_KEYWORDS = java.util.Set.of(
        "ostro\u015B\u0107", "sharpness", "pogromca nieumar\u0142ych", "smite", "zmora stawonog\u00F3w", "bane of arthropods",
        "odrzut", "knockback", "zakl\u0119ty ogie\u0144", "fire aspect", "grabie\u017C", "looting", "szerokie ostrze", "sweeping edge",
        "wydajno\u015B\u0107", "efficiency", "jedwabny dotyk", "silk touch", "niezniszczalno\u015B\u0107", "unbreaking", "szcz\u0119\u015Bcie", "fortune",
        "moc", "power", "uderzenie", "punch", "p\u0142omie\u0144", "flame", "niesko\u0144czono\u015B\u0107", "infinity", "morskie szcz\u0119\u015Bcie", "luck of the sea",
        "przyn\u0119ta", "lure", "naprawa", "mending", "oddychanie", "respiration", "wydajno\u015B\u0107 pod wod\u0105", "aqua affinity",
        "ciernie", "thorns", "g\u0142\u0119binowy w\u0119drowiec", "depth strider", "lodowy w\u0119drowiec", "frost walker", "pr\u0119dko\u015B\u0107 duszy", "soul speed",
        "szybkie skradanie", "swift sneak", "powolne opadanie", "feather falling", "ochrona", "protection",
        "ochrona przed ogniem", "fire protection", "ochrona przed wybuchem", "blast protection", "ochrona przed pociskami", "projectile protection",
        "przebicie", "impaling", "wir", "riptide", "pora\u017Cenie", "channeling", "lojalno\u015B\u0107", "loyalty", "wielostrza\u0142", "multishot",
        "przeszycie", "piercing", "szybkie prze\u0142adowanie", "quick charge", "g\u0119sto\u015B\u0107", "density", "wy\u0142om", "breach", "podmuch wiatru", "wind burst"
    );

    private boolean isVanillaOrSystemLine(String line, String cleanName) {
        String plain = normalizeLoreForFilter(line);
        if (plain.isEmpty()) return true;
        if (plain.equals(normalizeLoreForFilter(cleanName))) return true;
        if (plain.startsWith("minecraft:")) return true;

        // System lines
        if (plain.contains("wystawi") || plain.contains("wystawil") ||
            plain.contains("wygas") ||
            plain.contains("kliknij") || plain.contains("koszt") ||
            plain.contains("cena") || plain.contains("seller") ||
            plain.contains("price") || plain.contains("click") ||
            plain.contains("expired") || plain.contains("expire") || plain.contains("wystawiono") ||
            plain.contains("weryfik") || plain.contains("odblok") ||
            plain.contains("komend") || plain.contains("darmo") ||
            plain.contains("utrat") || plain.contains("utrac")) {
            return true;
        }

        // Vanilla Attribute Modifiers \u2014 section headers and value lines
        if (plain.equals("enchantments") || plain.equals("enchanty") ||
            plain.contains("when in main hand") || plain.contains("when in off hand") ||
            plain.contains("when on head") || plain.contains("when on body") ||
            plain.contains("when on legs") || plain.contains("when on feet") ||
            plain.equals("when applied:") || plain.equals("when applied") ||
            plain.contains("po zastosowaniu") ||
            plain.contains("kiedy w") || plain.contains("kiedy na") ||
            plain.contains("w g\u0142\u00F3wnej r\u0119ce") || plain.contains("w drugiej r\u0119ce") ||
            plain.contains("na g\u0142owie") || plain.contains("na ciele") ||
            plain.contains("na nogach") || plain.contains("na stopach") ||
            plain.contains("obra\u017Cenia od ataku") || plain.contains("szybko\u015B\u0107 ataku") ||
            plain.contains("attack damage") || plain.contains("attack speed") ||
            plain.contains("armor") || plain.contains("pancerz") ||
            plain.contains("speed") || plain.contains("szybko\u015B\u0107") ||
            plain.contains("health") || plain.contains("zdrowie") ||
            plain.contains("durability") || plain.contains("wytrzyma\u0142o\u015B\u0107") ||
            plain.contains("toughness") || plain.contains("twardo\u015B\u0107") ||
            plain.contains("knockback") || plain.contains("odrzut") ||
            plain.contains("oxygen") || plain.contains("tlen") ||
            plain.contains("mining speed") || plain.contains("szybko\u015B\u0107 wydobycia") ||
            plain.contains("color") || plain.contains("kolor") ||
            BK_RE_3.matcher(plain).matches()) {
            return true;
        }

        // Enchantments
        for (String keyword : ENCHANTMENT_KEYWORDS) {
            if (plain.contains(keyword)) {
                if (BK_RE_4.matcher(plain).matches() || plain.equals(keyword)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Sprawdza dopasowanie przedmiotu pod k\u0105tem sprzeda\u017Cy.
     */
    public boolean matchesForSell(ItemStack stack, PriceEntry entry) {
        if (stack == null || stack.isEmpty() || !entry.isSellEnabled())
            return false;

        // Compare item identifiers.
        String m1 = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();
        String m2 = entry.getMaterial().toLowerCase();
        if (m2.startsWith(XSK.MINECRAFT_PFX))
            m2 = m2.substring(XSK.MINECRAFT_PFX.length());

        if (!m1.equals(m2)) {
            return false;
        }

        // 2. Name Match (strict equals after stripping colors \u2014 "klata +7" must NOT match "klata" entry)
        if (entry.getName() != null && !entry.getName().isEmpty()) {
            String itemName = dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(stack.getName());
            if (!matchesConfiguredName(itemName, entry.getName())) {
                return false;
            }
        }

        // 3. Potion Effects Match (isolated from lore \u2014 uses PotionContentsComponent directly).
        // Mirrors the buy path in matchesEntry so SELL also distinguishes Strength I 3min vs Strength II.
        boolean potionMatchDone = false;
        if (entry.hasPotionEffects()) {
            net.minecraft.component.type.PotionContentsComponent potionContents =
                    stack.get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS);
            if (potionContents == null) return false;
            for (dev.bkclient.systems.setting.settings.rynek.PriceEntry.PotionEffectEntry required : entry.getPotionEffects()) {
                if (required.getEffectId() == null) continue;
                boolean found = false;
                for (net.minecraft.entity.effect.StatusEffectInstance fx : potionContents.getEffects()) {
                    String fxId = fx.getEffectType().getKey()
                            .map(k -> k.getValue().getPath()).orElse("").toLowerCase();
                    if (fxId.equals(required.getEffectId().toLowerCase())
                            && fx.getAmplifier() == required.getAmplifier()
                            && (required.getDuration() <= 0 || fx.getDuration() == required.getDuration())) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            potionMatchDone = true; // skip lore check below \u2014 potion lore is dynamic, not custom
        }

        // 3b. Lore Match (uses tooltip like bk-rynek, not LoreComponent) \u2014 skipped for potions
        if (!potionMatchDone) {
            List<String> tooltipLines = stack.getTooltip(
                    net.minecraft.item.Item.TooltipContext.create(MinecraftClient.getInstance().world),
                    MinecraftClient.getInstance().player,
                    net.minecraft.item.tooltip.TooltipType.BASIC)
                .stream().map(t -> stripColors(dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(t))).toList();

            String cleanName = stripColors(dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(stack.getName())).toLowerCase().trim();

            // The tooltip also contains native component data (for example
            // firework flight duration). Only LORE is a server/custom description.
            LoreComponent customLore = stack.get(DataComponentTypes.LORE);
            boolean itemHasCustomLore = customLore != null && customLore.lines().stream()
                    .map(dev.bkclient.utility.inventory.ItemStackExtractor::toLegacy)
                    .anyMatch(line -> !isVanillaOrSystemLine(line, cleanName));

            if (!entry.getLoreLines().isEmpty()) {
                for (String requiredLore : entry.getLoreLines()) {
                    if (requiredLore == null || requiredLore.isEmpty()) continue;
                    String cleanRequired = normalizeLoreForFilter(requiredLore);
                    if (shouldIgnoreRequiredLore(cleanRequired)) continue;
                    boolean found = tooltipLines.stream()
                            .anyMatch(l -> normalizeLoreForFilter(l).contains(cleanRequired));
                    if (!found) return false;
                }
            } else if (entry.getLore() == null || entry.getLore().isEmpty()) {
                // Entry has no lore requirement at all \u2014 only match items that also have NO custom lore
                if (itemHasCustomLore && entry.getCustomModelData() == null) return false;
            }
        }

        // 4. Enchantment Match \u2014 przy SPRZEDAZY poziom musi byc DOKLADNIE rowny (nie "co najmniej").
        // Inaczej kilof eff 6 pasuje do wpisu eff 5 (6 >= 5) i sella sie jako tanszy wariant.
        // (Kupowanie celowo zostaje na "co najmniej" \u2014 tam chcemy lapac itemy >= wymaganego poziomu.)
        if (!entry.getEnchantments().isEmpty()) {
            for (Map.Entry<String, Integer> e : entry.getEnchantments().entrySet()) {
                int currentLevel = EnchantmentUtility.getEnchantmentLevel(stack, getEnchantmentKey(e.getKey()));
                if (currentLevel != e.getValue())
                    return false;
            }
        }

        // 5. Banned Enchants
        if (!entry.getBannedEnchantsList().isEmpty()) {
            for (String banned : entry.getBannedEnchantsList()) {
                if (EnchantmentUtility.getEnchantmentLevel(stack, getEnchantmentKey(banned)) > 0)
                    return false;
            }
        }

        // 6. CustomModelData \u2014 pomijane gdy item ma w\u0142\u0105czone "Ignore CMD" (kostiumy/pety, gdzie
        // CMD bywa r\u00F3\u017Cny per egzemplarz i blokowa\u0142by match).
        if (entry.getCustomModelData() != null && !entry.isIgnoreCustomModelData()) {
            var cmdComponent = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            if (cmdComponent == null || cmdComponent.floats().isEmpty()) {
                return false;
            }
            int stackCMD = Math.round(cmdComponent.floats().get(0));
            if (stackCMD != entry.getCustomModelData()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Public overload for UI/HUD usage.
     */
    public boolean matchesEntry(ItemStack stack, PriceEntry entry) {
        if (stack == null || stack.isEmpty() || entry == null)
            return false;
        List<Text> tooltip = stack.getTooltip(net.minecraft.item.Item.TooltipContext.create(MinecraftClient.getInstance().world),
                MinecraftClient.getInstance().player, (MinecraftClient.getInstance().options.advancedItemTooltips ? net.minecraft.item.tooltip.TooltipType.ADVANCED : net.minecraft.item.tooltip.TooltipType.BASIC));
        List<String> loreStrings = tooltip.stream().map(Text::getString).toList();
        String itemName = dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(stack.getName());
        return matchesEntry(stack, tooltip, loreStrings, entry, cleanForMatch(itemName), stripColors(itemName));
    }

    /** Shared by purchase filtering and Ctrl+right-click diagnostics. */
    public Integer remainingDays(List<Text> tooltip) {
        Integer daysLeft = null;
        for (Text line : tooltip) {
            // mapSmallFont \u2014 Minestar lore w small-capsach (\u1D21\u028F\u0262\u1D00\uA731\u1D00), patrz wy\u017Cej.
            String plain = mapSmallFont(BK_RE_0.matcher(dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(line)).replaceAll("")).toLowerCase();

            if (AnarchiaPetExpiry.isExpiryLine(plain)) {
                daysLeft = AnarchiaPetExpiry.daysLeft(plain);
                break;
            }

            if (plain.contains("wyga\u015Bnie:") || plain.contains("wygasnie:")) {
                try {
                    String dateStr = plain.substring(plain.indexOf(":") + 1).trim();
                    if (dateStr.contains(","))
                        dateStr = dateStr.substring(0, dateStr.indexOf(",")).trim();
                    String[] parts = dateStr.split("-");
                    if (parts.length == 3) {
                        int day = Integer.parseInt(parts[0].trim());
                        int month = Integer.parseInt(parts[1].trim());
                        int year = Integer.parseInt(parts[2].trim());
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        long now = cal.getTimeInMillis();
                        cal.set(year, month - 1, day, 23, 59, 59);
                        daysLeft = (int) ((cal.getTimeInMillis() - now) / (1000L * 60 * 60 * 24));
                    }
                } catch (Exception ignored) {}
                break;
            }

            // Osobna \u015Bcie\u017Cka \u2014 Anarchia powy\u017Cej zostaje nietkni\u0119ta. Bierzemy realn\u0105 godzin\u0119 z lore
            // (nie zaokr\u0105glamy do ko\u0144ca dnia jak Anarchia), bo Minestar podaje dok\u0142adny czas.
            if (plain.contains("wygasa:") || plain.contains("wyga\u015Ba:")) {
                try {
                    String rest = plain.substring(plain.indexOf(":") + 1).trim(); // "dd.mm.yyyy hh:mm:ss"
                    String datePart = rest.contains(" ") ? rest.substring(0, rest.indexOf(" ")).trim() : rest;
                    String timePart = rest.contains(" ") ? rest.substring(rest.indexOf(" ") + 1).trim() : "";
                    String[] d = datePart.split("\\.");
                    if (d.length == 3) {
                        int day = Integer.parseInt(d[0].trim());
                        int month = Integer.parseInt(d[1].trim());
                        int year = Integer.parseInt(d[2].trim());
                        int hh = 23, mm = 59, ss = 59;
                        String[] t = timePart.split(":");
                        if (t.length >= 2) {
                            hh = Integer.parseInt(t[0].trim());
                            mm = Integer.parseInt(t[1].trim());
                            ss = t.length >= 3 ? Integer.parseInt(t[2].trim()) : 0;
                        }
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        long now = cal.getTimeInMillis();
                        cal.set(year, month - 1, day, hh, mm, ss);
                        daysLeft = (int) ((cal.getTimeInMillis() - now) / (1000L * 60 * 60 * 24));
                    }
                } catch (Exception ignored) {}
                break;
            }
        }
        return daysLeft;
    }

    public List<String> debugCompare(ItemStack stack, Map<String, List<PriceEntry>> entryMap, boolean globalUnitMode) {
        List<String> out = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            out.add("\u00a7cAutoMarket debug: pusty slot.");
            return out;
        }

        String materialId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        List<PriceEntry> entries = entryMap == null ? null : entryMap.get(materialId);
        String itemNameRaw = dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(stack.getName());

        out.add("\u00a78\u00a7m--------------------------------");
        out.add("\u00a7aAutoMarket full debug \u00a78- \u00a7f" + stripColors(itemNameRaw));
        out.add("\u00a7eITEM");
        out.add("\u00a77material=\u00a7f" + materialId + " \u00a78| \u00a77count=\u00a7f" + stack.getCount());
        out.add("\u00a77name.raw=\u00a7f" + debugRaw(itemNameRaw));
        out.add("\u00a77name.plain=\u00a7f" + stripColors(itemNameRaw));
        out.add("\u00a77name.clean=\u00a7f" + debugRaw(cleanForMatch(itemNameRaw)));
        out.add("\u00a77name.format=\u00a7fcolor " + debugColor(itemNameRaw)
                + " style " + debugStyleMask(leadingStyleMask(itemNameRaw)));
        appendEnchantDump(out, stack);
        appendLoreDump(out, "item.lore", getDebugLoreLines(stack));
        var expiryLore=stack.get(DataComponentTypes.LORE);
        if(expiryLore!=null) {
            for(Text line:expiryLore.lines()) {
                String raw=dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(line);
                if(AnarchiaPetExpiry.isExpiryLine(mapSmallFont(raw)))
                    out.add("Pet expiry: " + stripColors(raw));
            }
            Integer remaining=remainingDays(expiryLore.lines());
            out.add("Expiry days read: " + (remaining==null ? "UNKNOWN (brak / bledny format)" : remaining + " pelnych dni"));
        }

        appendLoreDump(out, "item.tooltip", getFullTooltipLines(stack));

        if (entries == null || entries.isEmpty()) {
            out.add("\u00a7cBrak wpisu w configu dla materialu: \u00a7f" + materialId);
            out.add("\u00a78\u00a7m--------------------------------");
            return out;
        }

        DebugReport best = null;
        List<DebugReport> reports = new ArrayList<>();
        for (PriceEntry entry : entries) {
            DebugReport report = debugAgainstEntry(stack, entry, globalUnitMode);
            reports.add(report);
            if (best == null || report.failures < best.failures || (report.failures == best.failures && report.score > best.score)) {
                best = report;
            }
        }

        if (best == null) {
            out.add("\u00a7cNie udalo sie zbudowac raportu debug.");
            out.add("\u00a78\u00a7m--------------------------------");
            return out;
        }

        out.add("\u00a7eCONFIG closest \u00a78(\u00a77fail=\u00a7f" + best.failures
                + "\u00a78, \u00a77score=\u00a7f" + best.score + "\u00a78, \u00a77candidates=\u00a7f"
                + reports.size() + "\u00a78)");
        appendEntryDump(out, best.entry, best);

        if (best.failures == 0) {
            out.add("\u00a7aMATCH OK: item powinien sie podswietlac.");
        } else {
            out.add("\u00a7cFAILURES");
            for (String issue : best.issues) {
                out.add("\u00a78- \u00a7f" + issue);
            }
        }

        if (!best.notes.isEmpty()) {
            out.add("\u00a7eNOTES");
            for (String note : best.notes) {
                out.add("\u00a77" + note);
            }
        }

        out.add("\u00a7eCANDIDATES");
        for (DebugReport report : reports) {
            out.add("\u00a78- \u00a77fail=\u00a7f" + report.failures + " \u00a77score=\u00a7f" + report.score
                    + " \u00a77name=\u00a7f" + shorten(stripColors(report.entry.getName()), 42));
        }

        out.add("\u00a78\u00a7m--------------------------------");
        return out;
    }

    private static final class DebugReport {
        final PriceEntry entry;
        final List<String> issues = new ArrayList<>();
        final List<String> notes = new ArrayList<>();
        int failures;
        int score;
        boolean unitMode;
        int requiredCount;

        DebugReport(PriceEntry entry) {
            this.entry = entry;
        }

        void fail(String text) {
            issues.add(text);
            failures++;
        }

        void pass() {
            score++;
        }
    }

    private DebugReport debugAgainstEntry(ItemStack stack, PriceEntry entry, boolean globalUnitMode) {
        DebugReport report = new DebugReport(entry);

        if (!entry.isBuyEnabled()) {
            report.fail("wpis ma wy\u0142\u0105czone kupowanie (buyEnabled=false)");
        } else {
            report.pass();
        }

        String itemName = dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(stack.getName());
        String itemNameClean = cleanForMatch(itemName);
        if (entry.getName() != null && !entry.getName().isEmpty()) {
            if (matchesConfiguredName(itemNameClean, entry.getName())) {
                report.pass();
            } else {
                addNameDebugIssue(report, itemNameClean, entry.getName());
            }
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        List<String> loreLines = new ArrayList<>();
        if (lore != null) {
            for (Text line : lore.lines()) {
                loreLines.add(dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(line));
            }
        }

        String bannedLoreMatch = findBannedLore(entry, lore == null ? List.of() : lore.lines());
        if (bannedLoreMatch != null) {
            report.fail("REJECT: banned lore - " + bannedLoreMatch);
        } else if (!entry.getBannedLore().isBlank()) {
            report.notes.add("Banned lore: PASS (brak zakazanej frazy)");
        }

        Integer remaining=remainingDays(lore==null ? java.util.List.of() : lore.lines());
        Integer minimum=entry.getMinDaysLeft();
        String expiryInfo="Min Days Left: odczyt=" + (remaining==null ? "UNKNOWN" : remaining)
            + ", minimum=" + (minimum==null || minimum<=0 ? "OFF" : minimum);
        if(AnarchiaPetExpiry.meetsMinimum(remaining,minimum)) {
            report.notes.add(expiryInfo + " -> PASS");
        } else report.fail(expiryInfo + " -> REJECT (za malo dni lub brak odczytu)");

        if (entry.getLore() != null && !entry.getLore().isEmpty()) {
            for (String part : BK_RE_5.split(entry.getLore())) {
                if (part.trim().isEmpty()) continue;
                String cleanPart = normalizeLoreForFilter(part);
                if (shouldIgnoreRequiredLore(cleanPart)) continue;
                if (debugLorePartFound(cleanPart, loreLines)) {
                    report.pass();
                } else {
                    report.fail("lore brak: '" + shorten(stripColors(part), 44) + "'");
                }
            }
        }

        if (!entry.getEnchantments().isEmpty()) {
            for (Map.Entry<String, Integer> e : entry.getEnchantments().entrySet()) {
                int currentLevel = EnchantmentUtility.getEnchantmentLevel(stack, getEnchantmentKey(e.getKey()));
                int displayedLevel = getDisplayedEnchantmentLevel(loreLines, e.getKey());
                if (currentLevel >= e.getValue()) {
                    report.pass();
                } else if (displayedLevel >= e.getValue()) {
                    report.pass();
                    report.notes.add("enchant " + e.getKey() + ": NBT " + currentLevel
                            + ", lore " + displayedLevel + " OK");
                } else {
                    report.fail("enchant " + e.getKey() + ": NBT " + currentLevel
                            + ", lore " + displayedLevel + " < config " + e.getValue());
                }
            }
        }

        if (!entry.getBannedEnchantsList().isEmpty()) {
            for (String banned : entry.getBannedEnchantsList()) {
                if (EnchantmentUtility.hasEnchantments(stack, getEnchantmentKey(banned))) {
                    report.fail("zbanowany enchant obecny: " + banned);
                } else {
                    report.pass();
                }
            }
        }

        if (entry.getCustomModelData() != null && !entry.isIgnoreCustomModelData()) {
            var cmdComponent = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            int stackCMD = -1;
            if (cmdComponent != null && !cmdComponent.floats().isEmpty()) {
                stackCMD = Math.round(cmdComponent.floats().get(0));
            }
            if (stackCMD == entry.getCustomModelData().intValue()) {
                report.pass();
            } else {
                report.fail("CustomModelData: item " + stackCMD + " != config " + entry.getCustomModelData());
            }
        }

        double foundPrice = extractPrice(loreLines);
        if (foundPrice < 0) foundPrice = extractPrice(getFullTooltipLines(stack));
        if (foundPrice < 0) {
            report.fail("nie znaleziono ceny w tooltipie/lore");
        } else {
            report.pass();
            report.notes.add("Cena znaleziona: \u00A7f" + fmtDebugPrice(foundPrice) + "$");
        }

        report.unitMode = entry.isUnitMode() || globalUnitMode;
        report.requiredCount = Math.max(1, entry.getMinBuyCount());
        int stackSize = Math.max(1, stack.getCount());
        if (report.unitMode) {
            if (stackSize < report.requiredCount) {
                report.fail("ilo\u015B\u0107: item " + stackSize + " < min " + report.requiredCount + " (Unit)");
            } else {
                report.pass();
            }
        } else if (stackSize != report.requiredCount) {
            report.fail("ilo\u015B\u0107: item " + stackSize + " != config " + report.requiredCount + " (Sum)");
        } else {
            report.pass();
        }

        if (foundPrice >= 0) {
            double effectivePrice = report.unitMode ? foundPrice / stackSize : foundPrice;
            report.notes.add("Cena sprawdzana: \u00A7f" + fmtDebugPrice(effectivePrice) + "$"
                    + (report.unitMode ? " za szt." : " suma"));
            if (entry.getMaxPrice() > 0 && effectivePrice > entry.getMaxPrice()) {
                report.fail("cena: " + fmtDebugPrice(effectivePrice) + "$ > max " + fmtDebugPrice(entry.getMaxPrice()) + "$");
            } else {
                report.pass();
            }
        }

        return report;
    }

    private void addNameDebugIssue(DebugReport report, String itemName, String targetName) {
        String itemPlain = stripColors(itemName).trim();
        String targetPlain = stripColors(targetName).trim();
        if (!itemPlain.equalsIgnoreCase(targetPlain)) {
            report.fail("name.plain mismatch item='" + itemPlain + "' config='" + targetPlain + "'");
            return;
        }

        String itemColor = leadingColorCode(itemName);
        String targetColor = leadingColorCode(targetName);
        int itemStyles = leadingStyleMask(itemName);
        int targetStyles = leadingStyleMask(targetName);
        int issueCountBefore = report.issues.size();

        if (!hasLegacyFormatting(targetName)) {
            report.fail("name unknown mismatch despite same plain text");
            return;
        }

        if ("\u00a7f".equals(targetColor)) {
            targetColor = "";
        }

        if (!targetColor.isEmpty() && !targetColor.equals(itemColor)) {
            report.fail("name.color mismatch item=" + debugColor(itemName) + " config=" + debugColor(targetName));
        }

        if ((itemStyles & targetStyles) != targetStyles) {
            report.fail("name.style mismatch item=" + debugStyleMask(itemStyles)
                    + " config requires=" + debugStyleMask(targetStyles));
        }

        if (report.issues.size() == issueCountBefore) {
            report.fail("name.format mismatch item.raw=" + debugRaw(itemName)
                    + " config.raw=" + debugRaw(targetName));
        }
    }

    private List<String> getDebugLoreLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                lines.add(dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(line));
            }
        }
        return lines;
    }

    private void appendEnchantDump(List<String> out, ItemStack stack) {
        it.unimi.dsi.fastutil.objects.Object2IntArrayMap<net.minecraft.registry.entry.RegistryEntry<Enchantment>> enchants =
                new it.unimi.dsi.fastutil.objects.Object2IntArrayMap<>();
        EnchantmentUtility.getEnchantments(stack, enchants);
        out.add("\u00a77item.enchants.count=\u00a7f" + enchants.size());
        if (enchants.isEmpty()) {
            return;
        }
        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<net.minecraft.registry.entry.RegistryEntry<Enchantment>> e
                : it.unimi.dsi.fastutil.objects.Object2IntMaps.fastIterable(enchants)) {
            String id = e.getKey().getKey()
                    .map(k -> k.getValue().toString())
                    .orElse(e.getKey().value().description().getString());
            out.add("\u00a78  - \u00a7f" + id + " \u00a77lvl=\u00a7f" + e.getIntValue());
        }
    }

    private void appendLoreDump(List<String> out, String label, List<String> lines) {
        out.add("\u00a77" + label + ".count=\u00a7f" + (lines == null ? 0 : lines.size()));
        if (lines == null || lines.isEmpty()) {
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            out.add("\u00a78  " + i + ".raw=\u00a7f" + debugRaw(raw));
            out.add("\u00a78  " + i + ".norm=\u00a77" + normalizeLoreForFilter(raw));
        }
    }

    private void appendEntryDump(List<String> out, PriceEntry entry, DebugReport report) {
        String entryName = entry.getName() == null ? "" : entry.getName();
        out.add("\u00a77config.material=\u00a7f" + entry.getMaterial());
        out.add("\u00a77config.name.raw=\u00a7f" + debugRaw(entryName));
        out.add("\u00a77config.name.plain=\u00a7f" + stripColors(entryName));
        out.add("\u00a77config.name.clean=\u00a7f" + debugRaw(cleanForMatch(entryName)));
        out.add("\u00a77config.name.format=\u00a7fcolor " + debugColor(entryName)
                + " style " + debugStyleMask(leadingStyleMask(entryName)));
        out.add("\u00a77config.buyEnabled=\u00a7f" + entry.isBuyEnabled()
                + " \u00a77sellEnabled=\u00a7f" + entry.isSellEnabled());
        out.add("\u00a77config.maxPrice=\u00a7f" + fmtDebugPrice(entry.getMaxPrice())
                + " \u00a77sellPrice=\u00a7f" + fmtDebugPrice(entry.getSellPrice()));
        out.add("\u00a77config.unitMode=\u00a7f" + entry.isUnitMode()
                + " \u00a77effectiveMode=\u00a7f" + (report.unitMode ? "Unit" : "Sum"));
        out.add("\u00a77config.minBuy=\u00a7f" + entry.getMinBuyCount()
                + " \u00a77minSell=\u00a7f" + entry.getMinSellCount());
        out.add("\u00a77config.customModelData=\u00a7f" + entry.getCustomModelData()
                + " \u00a77ignoreCMD=\u00a7f" + entry.isIgnoreCustomModelData());
        out.add("\u00a77config.minDaysLeft=\u00a7f" + entry.getMinDaysLeft());

        out.add("config.bannedLore=" + debugRaw(entry.getBannedLore()));

        List<String> loreParts = entry.getLoreLines();
        out.add("\u00a77config.lore.count=\u00a7f" + loreParts.size());
        for (int i = 0; i < loreParts.size(); i++) {
            String part = loreParts.get(i);
            out.add("\u00a78  " + i + ".raw=\u00a7f" + debugRaw(part));
            out.add("\u00a78  " + i + ".norm=\u00a77" + normalizeLoreForFilter(part));
        }

        out.add("\u00a77config.enchantments.count=\u00a7f" + entry.getEnchantments().size());
        for (Map.Entry<String, Integer> enchant : entry.getEnchantments().entrySet()) {
            out.add("\u00a78  - \u00a7f" + enchant.getKey() + " \u00a77lvl=\u00a7f" + enchant.getValue());
        }

        out.add("\u00a77config.banned.count=\u00a7f" + entry.getBannedEnchantsList().size());
        for (String banned : entry.getBannedEnchantsList()) {
            out.add("\u00a78  - \u00a7f" + banned);
        }
    }

    private String debugRaw(String s) {
        if (s == null) return "null";
        return normalizeLegacyMarkers(s)
                .replace("\u00a7", "&")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String debugColor(String s) {
        String color = leadingColorCode(s);
        return color.isEmpty() ? "<none>" : debugRaw(color);
    }

    private String debugStyleMask(int mask) {
        if (mask == 0) return "<none>";
        List<String> styles = new ArrayList<>();
        if ((mask & 1) != 0) styles.add("bold");
        if ((mask & 2) != 0) styles.add("strikethrough");
        if ((mask & 4) != 0) styles.add("underlined");
        if ((mask & 8) != 0) styles.add("obfuscated");
        return String.join(",", styles);
    }

    private boolean debugLorePartFound(String cleanPart, List<String> loreLines) {
        java.util.regex.Matcher rangeMatcher = RANGE_PATTERN.matcher(cleanPart);
        if (rangeMatcher.matches()) {
            String prefix = rangeMatcher.group(1).trim();
            double rangeMin = Double.parseDouble(rangeMatcher.group(2).replace(",", "."));
            double rangeMax = Double.parseDouble(rangeMatcher.group(3).replace(",", "."));
            String suffix = rangeMatcher.group(4).trim();

            for (String tooltipLine : loreLines) {
                String cleanLine = normalizeLoreForFilter(tooltipLine);
                if (!prefix.isEmpty() && !cleanLine.contains(prefix)) continue;
                java.util.regex.Matcher numMatcher = NUMBER_PATTERN
                        .matcher(cleanLine.substring(cleanLine.contains(prefix) && !prefix.isEmpty()
                                ? cleanLine.indexOf(prefix) + prefix.length() : 0));
                while (numMatcher.find()) {
                    try {
                        double val = Double.parseDouble(numMatcher.group(1).replace(",", "."));
                        if (val >= rangeMin && val <= rangeMax && (suffix.isEmpty() || cleanLine.contains(suffix))) {
                            return true;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            return false;
        }

        for (String tooltipLine : loreLines) {
            if (normalizeLoreForFilter(tooltipLine).contains(cleanPart)) {
                return true;
            }
        }
        return false;
    }

    private int getDisplayedEnchantmentLevel(List<String> loreLines, String enchantId) {
        if (loreLines == null || loreLines.isEmpty() || enchantId == null || enchantId.isBlank()) {
            return 0;
        }

        String id = enchantId.toLowerCase(Locale.ROOT);
        if (id.contains(":")) {
            id = id.substring(id.indexOf(':') + 1);
        }
        id = id.replace(" ", "_");

        for (String line : loreLines) {
            String cleanLine = normalizeLoreForFilter(line);
            if (!containsDisplayedEnchantKeyword(cleanLine, id)) {
                continue;
            }

            int last = 0;
            java.util.regex.Matcher matcher = NUMBER_PATTERN.matcher(cleanLine);
            while (matcher.find()) {
                try {
                    last = (int) Math.floor(Double.parseDouble(matcher.group(1).replace(",", ".")));
                } catch (NumberFormatException ignored) {}
            }
            if (last > 0) {
                return last;
            }
        }

        return 0;
    }

    private boolean containsDisplayedEnchantKeyword(String cleanLine, String enchantId) {
        if (cleanLine == null || cleanLine.isEmpty()) {
            return false;
        }

        return switch (enchantId) {
            case "efficiency" -> cleanLine.contains("wydajnosc") || cleanLine.contains("efficiency");
            case "fortune" -> cleanLine.contains("szczescie") || cleanLine.contains("fortune");
            case "unbreaking" -> cleanLine.contains("niezniszczalnosc") || cleanLine.contains("unbreaking");
            case "sharpness" -> cleanLine.contains("ostrosc") || cleanLine.contains("sharpness");
            case "looting" -> cleanLine.contains("grabiez") || cleanLine.contains("looting");
            case "mending" -> cleanLine.contains("naprawa") || cleanLine.contains("mending");
            case "silk_touch" -> cleanLine.contains("jedwabny dotyk") || cleanLine.contains("silk touch");
            case "protection" -> cleanLine.contains("ochrona") || cleanLine.contains("protection");
            case "power" -> cleanLine.contains("moc") || cleanLine.contains("power");
            default -> cleanLine.contains(enchantId.replace("_", " "));
        };
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        String clean = BK_RE_6.matcher(text).replaceAll(" ").trim();
        if (clean.length() <= max) return clean;
        return clean.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static String fmtDebugPrice(double price) {
        if (price < 0) return "?";
        if (price >= 1_000_000_000) return String.format(Locale.ROOT, "%.2fb", price / 1_000_000_000.0);
        if (price >= 1_000_000) return String.format(Locale.ROOT, "%.2fm", price / 1_000_000.0);
        if (price >= 1_000) return String.format(Locale.ROOT, "%.2fk", price / 1_000.0);
        if (Math.floor(price) == price) return String.valueOf((long) price);
        return String.format(Locale.ROOT, "%.2f", price);
    }

    private String stripColors(String s) {
        if (s == null) return "";
        s = normalizeLegacyMarkers(s);
        // Strip \u00a7x\u00a7r\u00a7r\u00a7g\u00a7g\u00a7b\u00a7b hex color codes (14 chars) first, then standard \u00a7X codes
        String clean = BK_RE_8.matcher(BK_RE_7.matcher(s).replaceAll("")).replaceAll("")
                        .replace("\u00a0", " ");
        return BK_RE_6.matcher(mapSmallFont(clean)).replaceAll(" ").trim();
    }

    private String normalizeLegacyMarkers(String s) {
        if (s == null) return "";
        return BK_RE_9.matcher(s).replaceAll("\u00a7")
                .replace("\u00c2\u00a7", "\u00a7");
    }

    private boolean hasLegacyFormatting(String s) {
        return BK_RE_10.matcher(normalizeLegacyMarkers(s)).matches();
    }

    /**
     * Kolor z PREFIKSU nazwy \u2014 czyta tylko wiod\u0105cy ci\u0105g kod\u00f3w formatowania i przerywa
     * na pierwszym widocznym znaku, dok\u0142adnie tak jak {@link #leadingStyleMask(String)}.
     *
     * <p>Poprzednia wersja skanowa\u0142a ca\u0142y string, wi\u0119c dla wpisu bez wiod\u0105cego koloru
     * (np. {@code \u00a7l\u0274\u00a7l\u1d00\u00a7l\u1d18... \u00a76\u2605\u00a7l13}) zwraca\u0142a \u00a76 od gwiazdki na ko\u0144cu nazwy i
     * por\u00f3wnywa\u0142a j\u0105 z kolorem PIERWSZEJ litery itemu. Efekt: ka\u017cdy wpis, kt\u00f3ry nie
     * zaczyna si\u0119 kolorem, nigdy nie \u0142apa\u0142 itemu z gradientem \u2014 poza bia\u0142ym \u00a7f, kt\u00f3ry
     * by\u0142 specjalnie zerowany w {@code matchesConfiguredName} i przez przypadek przechodzi\u0142.
     *
     * <p>Pusty wynik = brak koloru w prefiksie = brak wymagania.
     */
    private String leadingColorCode(String s) {
        String normalized = normalizeLegacyMarkers(s);
        for (int i = 0; i < normalized.length() - 1; i++) {
            char c = normalized.charAt(i);
            if (c != '\u00a7') {
                if (!Character.isWhitespace(c)) break;
                continue;
            }
            char code = Character.toLowerCase(normalized.charAt(++i));
            if (code == 'x' && i + 12 < normalized.length()) {
                return normalized.substring(i - 1, i + 13).toLowerCase(Locale.ROOT);
            }
            if ("0123456789abcdef".indexOf(code) >= 0) {
                return "\u00a7" + code;
            }
            // \u00a7l/\u00a7m/\u00a7n/\u00a7k/\u00a7r \u2014 styl, nie kolor: lecimy dalej po prefiksie
        }
        return "";
    }

    private int leadingStyleMask(String s) {
        String normalized = normalizeLegacyMarkers(s);
        int mask = 0;
        for (int i = 0; i < normalized.length() - 1; i++) {
            char c = normalized.charAt(i);
            if (c != '\u00a7') {
                if (!Character.isWhitespace(c)) break;
                continue;
            }
            char code = Character.toLowerCase(normalized.charAt(++i));
            switch (code) {
                case 'l' -> mask |= 1;
                case 'm' -> mask |= 2;
                case 'n' -> mask |= 4;
                case 'k' -> mask |= 8;
                case 'r' -> mask = 0;
                case 'x' -> i += 12;
                default -> {
                    if ("0123456789abcdef".indexOf(code) >= 0) mask = 0;
                }
            }
        }
        return mask;
    }

    private boolean matchesConfiguredName(String itemName, String targetName) {
        String itemPlain = stripColors(itemName).trim();
        String targetPlain = stripColors(targetName).trim();
        if (!itemPlain.equalsIgnoreCase(targetPlain)) {
            return false;
        }

        if (!hasLegacyFormatting(targetName)) {
            return true;
        }

        String itemColor = leadingColorCode(itemName);
        String targetColor = leadingColorCode(targetName);
        if ("\u00a7f".equals(targetColor)) {
            targetColor = "";
        }
        if (!targetColor.isEmpty() && !targetColor.equals(itemColor)) {
            return false;
        }

        int requiredStyles = leadingStyleMask(targetName);
        int itemStyles = leadingStyleMask(itemName);
        return (itemStyles & requiredStyles) == requiredStyles;
    }

    private String cleanForMatch(String s) {
        if (s == null) return "";
        s = normalizeLegacyMarkers(s);
        // Usuwamy kody resetu (\u00A7r) i kursywy (\u00A7o), kt\u00F3re Minecraft dodaje automatycznie.
        // Zachowujemy jednak kolory (np. \u00A7c) i pogrubienie (\u00A7l) dla precyzji "BK-Rynek Style".
        String clean = BK_RE_11.matcher(s).replaceAll("").replace("\u00a0", " ");
        return mapSmallFont(clean).trim();
    }

    /** Same literal, case-insensitive decision for buying and Ctrl+right-click diagnostics. */
    public String findBannedLore(PriceEntry entry, List<Text> rawLore) {
        if (entry.getBannedLore().isBlank() || rawLore.isEmpty()) return null;
        List<String> lines = rawLore.stream()
                .map(Text::getString).map(this::normalizeLoreForFilter).toList();
        for (String phrase : BK_RE_12.split(entry.getBannedLore())) {
            String normalized = normalizeLoreForFilter(phrase);
            if (normalized.isEmpty()) continue;
            for (String line : lines) {
                if (line.contains(normalized)) return stripColors(phrase).trim();
            }
        }
        return null;
    }

    private String normalizeLoreForFilter(String input) {
        String clean = BK_RE_6.matcher(BK_RE_13.matcher(stripColors(input).toLowerCase(Locale.ROOT)).replaceAll(" ")).replaceAll(" ")
                .trim();
        clean = BK_RE_14.matcher(Normalizer.normalize(clean, Normalizer.Form.NFD)).replaceAll("");
        return clean;
    }

    private boolean shouldIgnoreRequiredLore(String cleanPart) {
        if (cleanPart == null || cleanPart.isEmpty()) return true;
        if (BK_RE_15.matcher(cleanPart).replaceAll("").isEmpty()) return true;
        if (BK_RE_1.matcher(cleanPart).matches()) return true;

        return cleanPart.contains("wygas")
                || cleanPart.contains("kliknij") || cleanPart.contains("click")
                || cleanPart.contains("koszt") || cleanPart.contains("cena") || cleanPart.contains("price")
                || cleanPart.contains("seller") || cleanPart.contains("wystawi") || cleanPart.contains("wystawiono")
                || cleanPart.contains("szyfr") || cleanPart.contains("unikalny") || cleanPart.contains("wygenerowany")
                || cleanPart.contains("weryfik") || cleanPart.contains("odblok")
                || cleanPart.contains("komend") || cleanPart.contains("darmo")
                || cleanPart.contains("utrat") || cleanPart.contains("utrac")
                || cleanPart.contains("expire");
    }


    /**
     * G\u0142\u00F3wna logika dopasowywania (Styl BK-Rynek). 
     * Wykorzystuje surowe ci\u0105gi legacy (z kolorami \u00A7), aby odr\u00F3\u017Cni\u0107 orygina\u0142y od podr\u00F3bek.
     */
    private static final java.util.regex.Pattern RANGE_PATTERN =
            java.util.regex.Pattern.compile("(.*?)(\\d+(?:[.,]\\d+)?)-(\\d+(?:[.,]\\d+)?)(.*)", java.util.regex.Pattern.DOTALL);
    private static final java.util.regex.Pattern NUMBER_PATTERN =
            java.util.regex.Pattern.compile("(\\d+(?:[.,]\\d+)?)");

    private boolean matchesEntry(ItemStack stack, List<Text> tooltip,
            List<String> loreStrings, PriceEntry entry, String itemNameClean, String itemNameStripped) {

        LoreComponent rawLore = stack.get(DataComponentTypes.LORE);
        if (findBannedLore(entry, rawLore == null ? List.of() : rawLore.lines()) != null) return false;

        // Material already guaranteed by entryMap lookup \u2014 skip redundant check

        // Compare normalized display names for legacy entries.
        if (entry.getName() != null && !entry.getName().isEmpty()) {
            if (!matchesConfiguredName(itemNameClean, entry.getName())) {
                return false;
            }
        }


        // 3. Potion Effects Match (isolated from lore \u2014 uses PotionContentsComponent directly)
        boolean potionMatchDone = false;
        if (entry.hasPotionEffects()) {
            net.minecraft.component.type.PotionContentsComponent potionContents =
                    stack.get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS);
            if (potionContents == null) return false;
            for (dev.bkclient.systems.setting.settings.rynek.PriceEntry.PotionEffectEntry required : entry.getPotionEffects()) {
                if (required.getEffectId() == null) continue;
                boolean found = false;
                for (net.minecraft.entity.effect.StatusEffectInstance fx : potionContents.getEffects()) {
                    String fxId = fx.getEffectType().getKey()
                            .map(k -> k.getValue().getPath()).orElse("").toLowerCase();
                    if (fxId.equals(required.getEffectId().toLowerCase())
                            && fx.getAmplifier() == required.getAmplifier()
                            && (required.getDuration() <= 0 || fx.getDuration() == required.getDuration())) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            potionMatchDone = true; // skip lore check below
        }

        // 3b. Lore Match (Split by ; or \n, all parts must be found) \u2014 skipped for potions
        if (!potionMatchDone && entry.getLore() != null && !entry.getLore().isEmpty()) {
            String[] requiredParts = BK_RE_5.split(entry.getLore());
            for (String part : requiredParts) {
                if (part.trim().isEmpty()) continue;

                String cleanPart = normalizeLoreForFilter(part);
                if (shouldIgnoreRequiredLore(cleanPart)) continue;

                // Range check: detect pattern like "text 30-40%" or "text 30-40 suffix"
                // Regex: find "NUMBER-NUMBER" in the required part
                java.util.regex.Matcher rangeMatcher = RANGE_PATTERN.matcher(cleanPart);

                boolean found = false;
                if (rangeMatcher.matches()) {
                    String prefix = rangeMatcher.group(1).trim();
                    double rangeMin = Double.parseDouble(rangeMatcher.group(2).replace(",", "."));
                    double rangeMax = Double.parseDouble(rangeMatcher.group(3).replace(",", "."));
                    String suffix = rangeMatcher.group(4).trim();

                    for (String tooltipLine : loreStrings) {
                        String cleanLine = normalizeLoreForFilter(tooltipLine);
                        // Line must contain the prefix text (before the range)
                        if (!prefix.isEmpty() && !cleanLine.contains(prefix)) continue;
                        // Extract number from the line at position after prefix
                        java.util.regex.Matcher numMatcher = NUMBER_PATTERN
                                .matcher(cleanLine.substring(cleanLine.contains(prefix) && !prefix.isEmpty()
                                        ? cleanLine.indexOf(prefix) + prefix.length() : 0));
                        while (numMatcher.find()) {
                            try {
                                double val = Double.parseDouble(numMatcher.group(1).replace(",", "."));
                                if (val >= rangeMin && val <= rangeMax) {
                                    // Also check suffix if present
                                    if (suffix.isEmpty() || cleanLine.contains(suffix)) {
                                        found = true;
                                        break;
                                    }
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                        if (found) break;
                    }
                } else {
                    // Normal contains check
                    for (String tooltipLine : loreStrings) {
                        if (normalizeLoreForFilter(tooltipLine).contains(cleanPart)) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) return false;
            }
        }


        // 4. Enchantment Match
        if (!entry.getEnchantments().isEmpty()) {
            for (Map.Entry<String, Integer> e : entry.getEnchantments().entrySet()) {
                int currentLevel = EnchantmentUtility.getEnchantmentLevel(stack, getEnchantmentKey(e.getKey()));
                if (currentLevel < e.getValue()
                        && getDisplayedEnchantmentLevel(loreStrings, e.getKey()) < e.getValue())
                    return false;
            }
        }

        // 5. Banned Enchants
        if (!entry.getBannedEnchantsList().isEmpty()) {
            for (String banned : entry.getBannedEnchantsList()) {
                if (EnchantmentUtility.hasEnchantments(stack, getEnchantmentKey(banned)))
                    return false;
            }
        }

        // 6. CustomModelData \u2014 pomijane gdy item ma w\u0142\u0105czone "Ignore CMD" (kostiumy/pety, gdzie
        // CMD bywa r\u00F3\u017Cny per egzemplarz i blokowa\u0142by match).
        if (entry.getCustomModelData() != null && !entry.isIgnoreCustomModelData()) {
            var cmdComponent = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            if (cmdComponent == null || cmdComponent.floats().isEmpty()) {
                return false;
            }
            int stackCMD = Math.round(cmdComponent.floats().get(0));
            if (stackCMD != entry.getCustomModelData().intValue()) {
                return false;
            }
        }

        return true;
    }

    /** Full tooltip as raw legacy (\u00A7-coded) lines \u2014 used as price-source fallback when LoreComponent lacks the price. */
    private List<String> getFullTooltipLines(ItemStack stack) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            return stack.getTooltip(
                    net.minecraft.item.Item.TooltipContext.create(mc.world), mc.player,
                    net.minecraft.item.tooltip.TooltipType.BASIC)
                .stream().map(dev.bkclient.utility.inventory.ItemStackExtractor::toLegacy).toList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Wyci\u0105ga cen\u0119 z lore przedmiotu (1:1 z legacy ScanEvaluator).
     */
    private boolean isCurrentLorePriceLine(String line) {
        if (line == null || loreRegex == null || loreRegex.isEmpty()) {
            return false;
        }
        String cleanLine = BK_RE_17.matcher(BK_RE_16.matcher(line).replaceAll("")).replaceAll("").trim();
        if (cleanLine.isEmpty()) {
            return false;
        }
        if (DEFAULT_PRICE_REGEX.equals(loreRegex) && !hasDefaultPriceLabel(cleanLine)) return false;
        if (isItemUsageCostLine(cleanLine)) {
            return false;
        }
        return getPattern(loreRegex).matcher(cleanLine).find();
    }

    private static boolean hasDefaultPriceLabel(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("koszt") || lower.contains("cena") || lower.contains("price");
    }

    private boolean isItemUsageCostLine(String cleanLine) {
        if (cleanLine == null || cleanLine.isBlank()) {
            return false;
        }
        String normalized = BK_RE_14.matcher(Normalizer.normalize(cleanLine.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll("");
        return normalized.contains("kosztuje")
                || normalized.contains("powyzej")
                || (normalized.contains("sztuk")
                    && !normalized.contains("cena")
                    && !normalized.contains("price")
                    && !normalized.contains("$"));
    }

    public double extractPrice(List<String> loreLines) {
        if (loreLines == null || loreLines.isEmpty())
            return -1;
        if (loreRegex == null || loreRegex.isEmpty())
            return -1;

        Pattern pattern = getPattern(loreRegex);

        for (String line : loreLines) {
            String cleanLine = BK_RE_19.matcher(BK_RE_18.matcher(line).replaceAll("")).replaceAll("").trim();
            if (cleanLine.isEmpty())
                continue;
            // The default regex cannot match a line without one of these labels.
            // Avoid its unanchored greedy search over long descriptive lore.
            boolean hasPriceLabel = hasDefaultPriceLabel(cleanLine);
            if (DEFAULT_PRICE_REGEX.equals(loreRegex) && !hasPriceLabel)
                continue;
            if (isItemUsageCostLine(cleanLine))
                continue;

            // Integrity: Ignore seller lines
            if (loreSellerRegex != null && !loreSellerRegex.isEmpty()) {
                if (getPattern(loreSellerRegex).matcher(cleanLine).find())
                    continue;
            }

            // Hardcoded fallback for common Polish seller prefixes
            String cleanLower = cleanLine.toLowerCase();
            if (cleanLower.contains(XSK.SCANNER_WYSTAWIL1) || cleanLower.contains(XSK.SCANNER_WYSTAWIL2)
                    || cleanLower.contains(XSK.SCANNER_WYSTAWIONO)) {
                continue;
            }

            Matcher matcher = pattern.matcher(cleanLine);
            if (matcher.find()) {
                String raw = matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
                double parsed = parsePriceWithSuffix(raw);
                double precise = extractBracketPrice(cleanLine);
                return precise > 0 ? precise : parsed;
            }

            Matcher fallbackMatcher = FALLBACK_PRICE_PATTERN.matcher(cleanLine);
            if (hasPriceLabel && fallbackMatcher.find()) {
                String raw = fallbackMatcher.group(1);
                double parsed = parsePriceWithSuffix(raw);
                double precise = extractBracketPrice(cleanLine);
                return precise > 0 ? precise : parsed;
            }
        }
        return -1;
    }

    /**
     * Parser cen z obs\u0142ug\u0105 przyrostk\u00F3w i preferencj\u0105 precyzji w nawiasach.
     */
    private static double extractBracketPrice(String cleanLine) {
        int open = cleanLine.indexOf('(');
        int close = open >= 0 ? cleanLine.indexOf(')', open + 1) : -1;
        if (open < 0 || close <= open) {
            return -1;
        }

        String bracket = BK_RE_20.matcher(cleanLine.substring(open + 1, close)).replaceAll("");
        if (bracket.isEmpty()) {
            return -1;
        }
        return parsePriceWithSuffix(bracket);
    }

    public static double parsePriceWithSuffix(String raw) {
        if (raw == null || raw.trim().isEmpty())
            return -1;

        raw = raw.toLowerCase().trim();

        // 1. PRECISION PREFERENCE: If we have ($1,199) or (1199), prefer that!
        if (raw.contains("(") && raw.contains(")")) {
            String insideBrackets = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")")).trim();
            // Recursive check for the content inside brackets (cleaner)
            if (!insideBrackets.isEmpty() && !insideBrackets.equals(raw)) {
                // Remove $ if part of brackets
                insideBrackets = BK_RE_21.matcher(insideBrackets).replaceAll("");
                double preciseParsed = parsePriceWithSuffix(insideBrackets);
                if (preciseParsed > 0)
                    return preciseParsed;
            }
            // Fallback: use the part BEFORE brackets
            raw = raw.substring(0, raw.indexOf("(")).trim();
        }

        double multiplier = 1.0;
        if (raw.endsWith(XSK.SCANNER_MLD) || raw.endsWith("b")) {
            multiplier = 1000000000.0;
            raw = raw.substring(0, raw.length() - (raw.endsWith(XSK.SCANNER_MLD) ? 3 : 1));
        } else if (raw.endsWith("t")) {
            multiplier = 1000000000000.0;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("m")) {
            multiplier = 1000000.0;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("k")) {
            multiplier = 1000.0;
            raw = raw.substring(0, raw.length() - 1);
        }

        // 2. Extract digital string
        String digits = BK_RE_2.matcher(raw).replaceAll("");
        if (digits.isEmpty())
            return -1;

        // 3. Handle decimal formatting logic
        int lastDot = digits.lastIndexOf('.');
        int lastComma = digits.lastIndexOf(',');
        int lastSep = Math.max(lastDot, lastComma);

        if (lastSep != -1) {
            int digitsAfter = digits.length() - lastSep - 1;
            // If exactly 3 digits after separator -> thousands separator ($1,300)
            if (digitsAfter == 3) {
                digits = BK_RE_22.matcher(digits).replaceAll("");
            } else if (digitsAfter > 0 && digitsAfter <= 2) {
                // It's a decimal (1.30 or 1,3)
                String intPart = BK_RE_22.matcher(digits.substring(0, lastSep)).replaceAll("");
                String fracPart = digits.substring(lastSep + 1);
                digits = intPart + "." + fracPart;
            } else {
                digits = BK_RE_22.matcher(digits).replaceAll("");
            }
        }

        try {
            return Double.parseDouble(digits) * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private RegistryKey<Enchantment> getEnchantmentKey(String id) {
        String normalized = id.toLowerCase().replace(" ", "_");
        Identifier identifier = normalized.contains(":") ? Identifier.of(normalized)
                : Identifier.of(XSK.MINECRAFT_NS, normalized);
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, identifier);
    }

    private Pattern getPattern(String regex) {
        return patternCache.computeIfAbsent(regex,
                r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE));
    }

    private static boolean isRegex(String s) {
        return s.contains("*") || s.contains("+") || s.contains("?")
                || s.contains("[") || s.contains("(") || s.contains("\\");
    }
}


