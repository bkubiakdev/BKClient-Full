package dev.bkclient.systems.modules.modules.bkmods.enchant;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure rules shared by the live reader and its regression checks. */
public final class EnchantRules {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("(?i)\u00a7[0-9a-fk-orx]");
    private static final java.util.regex.Pattern BK_RE_1 = java.util.regex.Pattern.compile("\\p{M}+");
    private static final java.util.regex.Pattern BK_RE_2 = java.util.regex.Pattern.compile("[\\p{Z}\\s]+");
    private static final Pattern DAMAGE = Pattern.compile(
            "^[^a-z0-9]*dodatkowe obrazenia\\s*:?\\s*([+-]?\\d+(?:[.,]\\d+)?)\\s*%[^a-z0-9%]*$");

    public enum Mode { MINIMUM, RANGE, EXACT }
    public enum MenuAction { CURRENT, REATTACH, WAIT, STOP }
    public record Reading(double percent, String line) {}

    private EnchantRules() {}

    /** Only a pending roll may bridge a server close/reopen of the same menu. */
    public static MenuAction menuAction(boolean hasSession, boolean sameHandler, boolean matchingMenu,
                                       boolean noScreen, boolean pendingRoll, boolean manualClose,
                                       long elapsed, long timeout) {
        if (manualClose) return MenuAction.STOP;
        if (matchingMenu && (!hasSession || sameHandler)) return MenuAction.CURRENT;
        if (hasSession && pendingRoll && elapsed >= 0 && elapsed < timeout) {
            if (matchingMenu) return MenuAction.REATTACH;
            if (noScreen) return MenuAction.WAIT;
        }
        return MenuAction.STOP;
    }

    public static String normalize(String text) {
        if (text == null) return "";
        String clean = BK_RE_0.matcher(text).replaceAll("")
                .replace('\u2212', '-').replace('\u0142', 'l').replace('\u0141', 'L');
        return BK_RE_2.matcher(BK_RE_1.matcher(Normalizer.normalize(clean, Normalizer.Form.NFKD)).replaceAll("")).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }

    /** Missing, malformed or ambiguous lore is never interpreted as zero. */
    public static Reading read(List<String> lines) {
        Reading result = null;
        for (String line : lines) {
            String plain = normalize(line);
            if (!plain.contains("dodatkowe obrazenia")) continue;
            var matcher = DAMAGE.matcher(plain);
            if (!matcher.matches() || result != null) return null;
            try {
                double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
                if (!Double.isFinite(value)) return null;
                result = new Reading(value, line);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result;
    }

    public static boolean matches(double value, Mode mode, double from, double to) {
        if (!Double.isFinite(value) || !Double.isFinite(from) || !Double.isFinite(to)) return false;
        return switch (mode) {
            case MINIMUM -> value >= from;
            case RANGE -> from <= to && value >= from && value <= to;
            case EXACT -> Double.compare(value, from) == 0;
        };
    }

    /** A refreshed GUI alone is not evidence that a roll actually took place. */
    public static boolean completedRoll(double before, double after, int costBefore, int costNow) {
        return Double.isFinite(after) && (Double.compare(before, after) != 0 || costNow < costBefore);
    }
}
