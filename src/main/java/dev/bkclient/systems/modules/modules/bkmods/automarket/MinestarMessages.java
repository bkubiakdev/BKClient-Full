package dev.bkclient.systems.modules.modules.bkmods.automarket;

import dev.bkclient.utility.inventory.ItemStackExtractor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Complete server messages, not keywords shared with player chat or client feedback. */
public final class MinestarMessages {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("&(?=[0-9A-FK-ORXa-fk-orx])");
    private static final java.util.regex.Pattern BK_RE_1 = java.util.regex.Pattern.compile("\\p{M}");
    private static final java.util.regex.Pattern BK_RE_2 = java.util.regex.Pattern.compile("\\s+");
    public enum Kind { PURCHASE, LISTING, SALE, LIMIT }

    public record Event(Kind kind, double amount, int quantity, double taxPercent,
                        int marketLimit, String displayPrice) {
        /** TAX from the user's settings controls accounting; parsed tax is message metadata. */
        public double proceeds(double configuredTaxPercent) {
            return BigDecimal.valueOf(amount)
                    .multiply(BigDecimal.valueOf(100).subtract(
                            BigDecimal.valueOf(Math.max(0, Math.min(100, configuredTaxPercent)))))
                    .movePointLeft(2).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
    }

    private static final String PREFIX = "^[\\s\\u00d7\\u2022-]*";
    private static final String NUMBER = "(?:[0-9]{1,3}(?:[ _.,][0-9]{3})+|[0-9]+)(?:[.,][0-9]{1,2})?";
    private static final String PRICE = "\\$?\\s*(?<price>" + NUMBER + "\\s*(?:mld|[kmbt])?)\\s*\\$?";
    private static final Pattern PURCHASE = Pattern.compile(PREFIX
            + "zakupiono przedmiot za\\s*" + PRICE + "\\s*[!.]?$");
    private static final Pattern LISTING = Pattern.compile(PREFIX
            + "wystawiono x(?<quantity>[1-9][0-9]*) za\\s*" + PRICE + "\\s*[!.]?$");
    private static final Pattern SALE = Pattern.compile(PREFIX
            + "twoj przedmiot na rynku zostal kupiony(?: przez [a-z0-9_]{1,16})? za\\s*" + PRICE
            + "(?: przez [a-z0-9_]{1,16})?\\s*[!.]?"
            + "(?:\\s+pobrano podatek (?<tax>[0-9]+(?:[.,][0-9]{1,2})?)\\s*%[!.]?)?$");
    private static final Pattern LIMIT = Pattern.compile(PREFIX
            + "w jednym momencie mozesz miec wystawione maksymalnie (?<limit>[1-9][0-9]*) przedmiot(?:ow|y)?[!.]?"
            + "(?: limit mozesz zwiekszyc (?:po przez|poprzez|przez) zakup rangi premium, wiecej pod /rangi[!.]?)?$");

    private MinestarMessages() {}

    public static String plain(String message) {
        String readable = ItemStackExtractor.toReadablePlain(
                message == null ? "" : BK_RE_0.matcher(message).replaceAll("\u00a7"));
        return BK_RE_2.matcher(BK_RE_1.matcher(Normalizer.normalize(readable, Normalizer.Form.NFD)).replaceAll("")
                .replace('\u0142', 'l').replace('\u0141', 'L')
                .replace('\u202f', ' ')).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }

    public static Event parse(String message) {
        String text = plain(message);
        try {
            Matcher limit = LIMIT.matcher(text);
            if (limit.matches()) {
                return new Event(Kind.LIMIT, 0, 0, 0, Integer.parseInt(limit.group("limit")), "");
            }
            Matcher purchase = PURCHASE.matcher(text);
            if (purchase.matches()) return priced(Kind.PURCHASE, purchase, 0, 0);
            Matcher listing = LISTING.matcher(text);
            if (listing.matches()) return priced(Kind.LISTING, listing, Integer.parseInt(listing.group("quantity")), 0);
            Matcher sale = SALE.matcher(text);
            if (sale.matches()) {
                double tax = sale.group("tax") == null ? 0 : Double.parseDouble(sale.group("tax").replace(',', '.'));
                if (tax > 100) return null;
                return priced(Kind.SALE, sale, 0, tax);
            }
        } catch (NumberFormatException ignored) {
            // Overflowing quantities, limits and prices are not confirmations.
        }
        return null;
    }

    private static Event priced(Kind kind, Matcher match, int quantity, double tax) {
        String price = match.group("price").trim();
        double amount = AutoMarketScanner.parsePriceWithSuffix(price);
        return Double.isFinite(amount) && amount > 0
                ? new Event(kind, amount, quantity, tax, 0, price) : null;
    }
}
