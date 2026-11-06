package dev.bkclient.utility.inventory;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.text.Normalizer;
import java.util.Locale;

public final class ItemStackExtractor {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("\u00a7x(\u00a7[0-9A-Fa-f]){6}");
    private static final java.util.regex.Pattern BK_RE_1 = java.util.regex.Pattern.compile("\u00a7[0-9A-FK-ORXa-fk-orx]");
    private static final java.util.regex.Pattern BK_RE_2 = java.util.regex.Pattern.compile("\u00C2\u00A7x(\u00C2\u00A7[0-9A-Fa-f]){6}");
    private static final java.util.regex.Pattern BK_RE_3 = java.util.regex.Pattern.compile("\u00C2\u00A7[0-9A-FK-ORXa-fk-orx]");
    private static final java.util.regex.Pattern BK_RE_4 = java.util.regex.Pattern.compile("&(?=[0-9A-FK-ORXa-fk-orx])");
    private static final java.util.regex.Pattern BK_RE_5 = java.util.regex.Pattern.compile("\u00C3\u201A\u00C2\u00A7x(\u00C3\u201A\u00C2\u00A7[0-9A-Fa-f]){6}");
    private static final java.util.regex.Pattern BK_RE_6 = java.util.regex.Pattern.compile("\\s+");
    private static final java.util.regex.Pattern BK_RE_7 = java.util.regex.Pattern.compile("\\p{M}");
    private static final java.util.regex.Pattern BK_RE_8 = java.util.regex.Pattern.compile("[\u23AF\u2039\u203A\u00AB\u00BB\u2014\u2015\u2500\u25AC\u039E\u26A0\u00D7\u2022\u795E\u1410 \\-]+");
    private static final java.util.regex.Pattern BK_RE_9 = java.util.regex.Pattern.compile(".*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*");

    private ItemStackExtractor() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final java.util.Map<Character, Character> SMALL_FONT_MAP = new java.util.HashMap<>() {
        {
            put('\u1D00', 'a'); put('\u1D01', 'a'); put('\u1D00', 'a'); put('\u1D03', 'b'); put('\u0299', 'b');
            put('\u1D04', 'c'); put('\u1D05', 'd'); put('\u1D07', 'e'); put('\uA730', 'f'); put('\u0493', 'f');
            put('\u0262', 'g'); put('\u029C', 'h'); put('\u026A', 'i'); put('\u1D0A', 'j'); put('\u1D0B', 'k');
            put('\u029F', 'l'); put('\u1D0C', '\u0142'); put('\u1D0D', 'm'); put('\u0274', 'n'); put('\u1D0F', 'o'); put('\u1D18', 'p');
            put('\u0280', 'r'); put('\uA731', 's'); put('\u1D1B', 't'); put('\u1D1C', 'u'); put('\u1D20', 'v');
            put('\u1D21', 'w'); put('\u028F', 'y'); put('\u1D22', 'z'); put('\u1D43', 'a'); put('\u1D47', 'b');
            put('\u1D9C', 'c'); put('\u1D48', 'd'); put('\u1D49', 'e'); put('\u1DA0', 'f'); put('\u1D4D', 'g');
            put('\u02B0', 'h'); put('\u2071', 'i'); put('\u02B2', 'j'); put('\u1D4F', 'k'); put('\u02E1', 'l');
            put('\u1D50', 'm'); put('\u207F', 'n'); put('\u1D52', 'o'); put('\u1D56', 'p'); put('\u02B3', 'r');
            put('\u02E2', 's'); put('\u1D57', 't'); put('\u1D58', 'u'); put('\u1D5B', 'v'); put('\u02B7', 'w');
            put('\u02E3', 'x'); put('\u02B8', 'y'); put('\u1DBB', 'z');
        }
    };

    private static String mapSmallFont(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            sb.append(SMALL_FONT_MAP.getOrDefault(c, c));
        }
        return sb.toString();
    }

    private static String stripLegacyCodes(String input) {
        if (input == null) return "";
        return BK_RE_3.matcher(BK_RE_2.matcher(BK_RE_1.matcher(BK_RE_0.matcher(input).replaceAll("")).replaceAll("")).replaceAll("")).replaceAll("")
                .replace("\u00a0", " ");
    }

    private static String normalizeLegacyCodeMarkers(String input) {
        if (input == null) return "";
        return BK_RE_4.matcher(input).replaceAll("\u00a7");
    }

    private static String stripLegacyHexCodes(String input) {
        if (input == null) return "";
        return BK_RE_5.matcher(BK_RE_0.matcher(input).replaceAll("")).replaceAll("");
    }

    private static String fixRapyGlyphs(String input) {
        if (input == null) return "";
        return input
                .replace('\u0493', 'f')
                .replace('\u0492', 'F')
                .replace('\u0E18', '\u0142')
                .replace('\u0E1B', '\u017C')
                .replace('\u0E1E', '\u015B')
                .replace('\u0E17', '\u0107')
                .replace('\u0E16', '\u0119');
    }

    private static String firstLegacyPrefix(String input) {
        if (input == null || input.isEmpty()) return "";
        for (int i = 0; i < input.length() - 1; i++) {
            char marker = input.charAt(i);
            if (marker != '\u00a7' && marker != '\u00C2') continue;
            int codeIndex = marker == '\u00a7' ? i + 1 : i + 2;
            if (codeIndex >= input.length()) continue;
            char code = Character.toLowerCase(input.charAt(codeIndex));
            if (code == 'x') {
                i = codeIndex + 12;
                continue;
            }
            if ("0123456789abcdef".indexOf(code) >= 0) {
                return "\u00a7" + code;
            }
        }
        return "";
    }

    private static String compactLegacyText(String raw) {
        String normalized = normalizeLegacyCodeMarkers(raw);
        String clean = BK_RE_6.matcher(fixRapyGlyphs(stripLegacyHexCodes(normalized))).replaceAll(" ")
                .trim();
        if (stripLegacyCodes(clean).trim().isEmpty()) return "";
        if (!clean.startsWith("\u00a7") && !clean.startsWith("\u00C3\u201A\u00C2\u00A7")) {
            clean = firstLegacyPrefix(normalized) + clean;
        }
        return clean;
    }

    private static String normalizeLoreForFilter(String input) {
        String clean = mapSmallFont(fixRapyGlyphs(stripLegacyCodes(input))).toLowerCase(Locale.ROOT).trim();
        clean = BK_RE_7.matcher(Normalizer.normalize(clean, Normalizer.Form.NFD)).replaceAll("");
        return clean;
    }

    private static boolean shouldSkipStoredLoreLine(String clean) {
        if (clean == null || clean.isEmpty()) return true;
        if (BK_RE_8.matcher(clean).replaceAll("").isEmpty()) return true;
        if (BK_RE_9.matcher(clean).matches()) return true;

        return clean.contains("sprzedaj") || clean.contains("seller")
                || clean.contains("wystawi") || clean.contains("wystawiono")
                || clean.contains("wygas") || clean.contains("expire")
                || clean.contains("kliknij") || clean.contains("click")
                || clean.contains("koszt") || clean.contains("cena") || clean.contains("price")
                || clean.contains("szyfr") || clean.contains("unikalny") || clean.contains("wygenerowany")
                || clean.contains("weryfik") || clean.contains("odblok")
                || clean.contains("komend") || clean.contains("darmo")
                || clean.contains("utrat") || clean.contains("utrac");
    }

    public static boolean shouldSkipLoreLine(Text line) {
        return shouldSkipStoredLoreLine(normalizeLoreForFilter(toLegacy(line)));
    }

    public static String toReadablePlain(Text text) {
        return toReadablePlain(toLegacy(text));
    }

    public static String toReadablePlain(String legacyText) {
        String clean = BK_RE_6.matcher(mapSmallFont(fixRapyGlyphs(stripLegacyCodes(legacyText)))).replaceAll(" ")
                .trim();
        return clean;
    }

    public static String toEditableLegacy(String legacyText) {
        return mapSmallFont(compactLegacyText(normalizeLegacyCodeMarkers(legacyText)))
                .replace('\u01eb', 'o')
                .replace('\u01ea', 'O')
                .replace('\u00a7', '&')
                .replace("\u00C3\u201A\u00C2\u00A7", "&");
    }

    public static String toEditableLore(String legacyLore) {
        if (legacyLore == null || legacyLore.isBlank()) return "";
        List<String> parts = new ArrayList<>();
        for (String part : legacyLore.split(";")) {
            String editable = toEditableLegacy(part);
            if (!editable.isBlank()) {
                parts.add(editable);
            }
        }
        return String.join(";", parts);
    }

    public static String fromEditableLegacy(String editableText) {
        return BK_RE_6.matcher(fixRapyGlyphs(normalizeLegacyCodeMarkers(editableText))).replaceAll(" ")
                .trim();
    }

    public static String fromEditableLore(String editableLore) {
        if (editableLore == null || editableLore.isBlank()) return "";
        List<String> parts = new ArrayList<>();
        for (String part : editableLore.split(";")) {
            String legacy = fromEditableLegacy(part);
            if (!legacy.isBlank()) {
                parts.add(legacy);
            }
        }
        return String.join(";", parts);
    }

    public static PriceEntry extract(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return null;

        // Material
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String material = itemId.getPath();

        // CustomModelData is a stable identifier for most server custom items.
        net.minecraft.component.type.CustomModelDataComponent cmd =
                stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA);
        boolean hasCustomModelData = cmd != null && !cmd.floats().isEmpty();

        // Display Name: keep the server font glyphs, but collapse per-letter gradients.
        String name = compactLegacyText(toLegacy(stack.getName()));

        PriceEntry entry = new PriceEntry(name, material);
        int count = Math.max(1, stack.getCount());
        entry.setMinBuyCount(count);
        entry.setMinSellCount(count);

        if (hasCustomModelData) {
            entry.setCustomModelData(cmd.floats().get(0).intValue());
        }

        // Potion Effects \u2014 store via PotionContentsComponent (not lore)
        try {
            net.minecraft.component.type.PotionContentsComponent potionContents =
                    stack.get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS);
            if (potionContents != null && stack.getItem() instanceof net.minecraft.item.PotionItem) {
                java.util.List<dev.bkclient.systems.setting.settings.rynek.PriceEntry.PotionEffectEntry> effects =
                        new java.util.ArrayList<>();
                for (net.minecraft.entity.effect.StatusEffectInstance fx : potionContents.getEffects()) {
                    String effectId = fx.getEffectType().getKey()
                            .map(k -> k.getValue().getPath()).orElse("unknown");
                    effects.add(new dev.bkclient.systems.setting.settings.rynek.PriceEntry.PotionEffectEntry(
                            effectId, fx.getAmplifier(), fx.getDuration()));
                }
                if (!effects.isEmpty()) entry.setPotionEffects(effects);
            }
        } catch (Exception ignored) {}

        // Lore Extraction \u2014 use LoreComponent (custom lore only, no vanilla tooltip lines)
        try {
            net.minecraft.component.type.LoreComponent lore = stack.get(net.minecraft.component.DataComponentTypes.LORE);
            if (lore != null) {
                StringBuilder sb = new StringBuilder();
                for (Text line : lore.lines()) {
                    String rawLine = toLegacy(line);
                    String clean = normalizeLoreForFilter(rawLine);
                    if (shouldSkipStoredLoreLine(clean)) continue;
                    String storedLine = compactLegacyText(rawLine);
                    if (storedLine.isEmpty()) continue;
                    if (sb.length() > 0) sb.append(";");
                    sb.append(storedLine);
                }
                if (sb.length() > 0) entry.setLore(sb.toString());
            }
        } catch (Exception ignored) {}


        // Enchantments. Block listings may carry hidden server-side components used only for rendering
        // or metadata; do not turn those into required enchant filters for quick-add.
        if (!(stack.getItem() instanceof net.minecraft.item.BlockItem)) {
            Object2IntArrayMap<RegistryEntry<Enchantment>> enchantments = new Object2IntArrayMap<>();
            EnchantmentUtility.getEnchantments(stack, enchantments);

            StringBuilder enchantsBuilder = new StringBuilder();
            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> e : Object2IntMaps.fastIterable(enchantments)) {
                e.getKey().getKey().ifPresent(key -> {
                    String enchantName = key.getValue().getPath();
                    int level = e.getIntValue();
                    entry.getEnchantments().put(enchantName, level);

                    if (enchantsBuilder.length() > 0)
                        enchantsBuilder.append(", ");
                    enchantsBuilder.append(enchantName).append(" ").append(level);
                });
            }
            entry.setEnchants(enchantsBuilder.toString());
        } else {
            entry.setEnchants("");
        }

        // DyeColor (leather armor, leather horse armor)
        net.minecraft.component.type.DyedColorComponent dyedColor =
                stack.get(net.minecraft.component.DataComponentTypes.DYED_COLOR);
        if (dyedColor != null) {
            entry.setDyeColor(dyedColor.rgb());
        }

        // Player head texture (for custom player heads / pets)
        try {
            net.minecraft.component.type.ProfileComponent profile =
                    stack.get(net.minecraft.component.DataComponentTypes.PROFILE);
            if (profile != null && profile.gameProfile() != null) {
                com.mojang.authlib.properties.PropertyMap props = profile.gameProfile().getProperties();
                java.util.Collection<com.mojang.authlib.properties.Property> textures = props.get("textures");
                if (!textures.isEmpty()) {
                    entry.setHeadTextureValue(textures.iterator().next().value());
                }
            }
        } catch (Exception ignored) {}

        // Icon: cache the ItemStack itself for rendering (transient, not persisted)
        entry.setCachedIconStack(stack.copy());

        // Defaults
        entry.setBuyEnabled(true);
        entry.setSellEnabled(false);

        return entry;
    }

    public static String toLegacy(Text text) {
        if (text == null)
            return "";
        StringBuilder sb = new StringBuilder();
        text.visit((style, string) -> {
            net.minecraft.util.Formatting color = null;
            if (style.getColor() != null) {
                // Try standard colors first
                for (net.minecraft.util.Formatting f : net.minecraft.util.Formatting.values()) {
                    if (f.isColor() && f.getColorValue() != null
                            && f.getColorValue().equals(style.getColor().getRgb())) {
                        color = f;
                        break;
                    }
                }
            }

            if (color != null) {
                sb.append("\u00A7").append(color.getCode());
            } else if (style.getColor() != null) {
                // Hex color support: \u00A7x\u00A7r\u00A7r\u00A7g\u00A7g\u00A7b\u00A7b
                String hex = String.format("%06x", style.getColor().getRgb());
                sb.append("\u00A7x");
                for (char c : hex.toCharArray()) {
                    sb.append("\u00A7").append(c);
                }
            }

            if (style.isBold())
                sb.append("\u00A7l");
            if (style.isItalic())
                sb.append("\u00A7o");
            if (style.isUnderlined())
                sb.append("\u00A7n");
            if (style.isStrikethrough())
                sb.append("\u00A7m");
            if (style.isObfuscated())
                sb.append("\u00A7k");

            sb.append(string);
            // Append reset if next parts might have different style
            // sb.append("\u00A7r"); // Usually unnecessary if every visit starts with colors
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return sb.toString();
    }
}


