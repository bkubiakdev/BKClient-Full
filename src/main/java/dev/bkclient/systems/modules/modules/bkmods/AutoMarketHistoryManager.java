package dev.bkclient.systems.modules.modules.bkmods;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.bkmods.automarket.MinestarMessages;
import dev.bkclient.systems.modules.modules.bkmods.automarket.MinestarPacketRouter;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import java.util.List;

public class AutoMarketHistoryManager implements IMinecraft {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("0+$");
    private static final java.util.regex.Pattern BK_RE_1 = java.util.regex.Pattern.compile("\\.$");
    private static final java.util.regex.Pattern BK_RE_2 = java.util.regex.Pattern.compile("[^0-9.kmld]");
    private static final java.util.regex.Pattern BK_RE_3 = java.util.regex.Pattern.compile("[^0-9.]");
    private static final java.util.regex.Pattern BK_RE_4 = java.util.regex.Pattern.compile("\\s+");

    private static AutoMarketHistoryManager instance;

    private int boughtCount = 0;
    private int soldCount = 0;
    private int listedCount = 0;
    private double totalProfit = 0;
    private double walletBalance = -1;
    private long startTime;
    private final List<HistoryEntry> history = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final MinestarPacketRouter minestarPackets = new MinestarPacketRouter();

    public record HistoryEntry(String name, double price, int count, net.minecraft.item.ItemStack stack,
            String wallet) {
    }

    public static String formatPrice(double value) {
        double absVal = Math.abs(value);
        String suffix = "";
        double displayVal = absVal;
        if (absVal >= 1_000_000_000) {
            displayVal /= 1_000_000_000;
            suffix = "MLD";
        } else if (absVal >= 1_000_000) {
            displayVal /= 1_000_000;
            suffix = "M";
        } else if (absVal >= 1_000) {
            displayVal /= 1_000;
            suffix = "K";
        }
        String formatted = String.format(java.util.Locale.US, "%.2f", displayVal);
        if (formatted.contains(".")) {
            formatted = BK_RE_1.matcher(BK_RE_0.matcher(formatted).replaceAll("")).replaceAll("");
        }
        return (value < 0 ? "-" : "") + formatted + suffix;
    }

    public static double parsePrice(String raw) {
        if (raw == null || raw.isEmpty())
            return -1;
        String clean = raw.trim().toLowerCase().replace(",", ".");
        // Remove common currency symbols and non-essential characters at the end
        clean = BK_RE_2.matcher(clean).replaceAll("");

        double multiplier = 1.0;
        if (clean.endsWith("mld")) {
            multiplier = 1_000_000_000.0;
            clean = clean.substring(0, clean.length() - 3);
        } else if (clean.endsWith("m")) {
            multiplier = 1_000_000.0;
            clean = clean.substring(0, clean.length() - 1);
        } else if (clean.endsWith("k")) {
            multiplier = 1000.0;
            clean = clean.substring(0, clean.length() - 1);
        }

        // Final cleanup for safety
        clean = BK_RE_3.matcher(clean).replaceAll("");
        try {
            return Double.parseDouble(clean) * multiplier;
        } catch (Exception e) {
            return -1;
        }
    }

    public void addSnipeEntry(String name, double price, int count, net.minecraft.item.ItemStack stack,
            String wallet) {
        history.add(0, new HistoryEntry(name, price, count, stack.copy(), wallet));
        if (history.size() > 5) {
            history.remove(history.size() - 1);
        }
    }

    private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
        Packet<?> p = event.getPacket();
        if (!(p instanceof GameMessageS2CPacket
                || p instanceof net.minecraft.network.packet.s2c.play.TitleS2CPacket
                || p instanceof net.minecraft.network.packet.s2c.play.SubtitleS2CPacket)) return;
        AutoMarket market = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
        if (market != null && market.minestarMode.isEnabled()) {
            var connection = mc.getNetworkHandler();
            mc.execute(() -> {
                if (connection == null || connection != mc.getNetworkHandler() || !market.minestarMode.isEnabled()) return;
                handleMinestarPacket(p, connection, market.sellTax.getCurrentValue(), message -> {
                    if (market.isEnabled()) market.onMinestarMessage(message);
                });
            });
            return;
        }
        if (p instanceof GameMessageS2CPacket packet) {
            processChatMessage(packet.content().getString().toLowerCase(java.util.Locale.ROOT));
        }
    };

    /** The caller selects Minestar and marshals to the client thread before entering here. */
    public void handleMinestarPacket(Packet<?> packet, Object connection, double sellTaxPercent,
            java.util.function.Consumer<MinestarPacketRouter.Message> consumer) {
        minestarPackets.accept(packet, connection, message -> {
            MinestarMessages.Event event = message.event();
            if (event != null) {
                switch (event.kind()) {
                    case PURCHASE -> {
                        boughtCount++;
                        totalProfit -= event.amount();
                    }
                    case LISTING -> listedCount++;
                    case SALE -> {
                        soldCount++;
                        decrementListed();
                        totalProfit += event.proceeds(sellTaxPercent);
                    }
                    case LIMIT -> { }
                }
            }
            consumer.accept(message);
        });
    }

    private void processChatMessage(String msg) {
        // Ignore AH error messages to avoid false statistics (Parity with legacy)
        if (msg.contains("prawdopodobnie") || msg.contains("usuni\u0119t")
                || msg.contains("nie jest ju\u017c dost\u0119pny")
                || msg.contains("ju\u017c sprzedany") || msg.contains("juz sprzedany")) {
            return;
        }

        // SPRZEDA\u017b musi byc sprawdzana PRZED zakupem \u2014 minestar pisze
        // "Twoj przedmiot na rynku zostal kupiony przez X za Y" (zawiera "kupiony",
        // co inaczej zostaloby blednie policzone jako zakup).
        if (msg.contains("you sold") || msg.contains("sprzedali\u015b") || msg.contains("sprzedano")
                || msg.contains("sprzedany")
                || (msg.contains("tw\u00f3j przedmiot") && msg.contains("zosta\u0142 kupiony"))
                || (msg.contains("twoj przedmiot") && msg.contains("zostal kupiony"))) {
            soldCount++;
            decrementListed();
            extractPriceAndAddProfit(msg, true);
        } else if (msg.contains("you bought") || msg.contains("kupili\u015b") || msg.contains("zakupiono")
                || msg.contains("zakupi\u0142e\u015b") || msg.contains("zakupile\u015b")) {
            boughtCount++;
            extractPriceAndAddProfit(msg, false);
        } else if (msg.contains("you listed") || msg.contains("wystawili\u015b") || msg.contains("wystawiono")) {
            // anarchia.gg sends two chat packets per listing: a notice ("... widoczny dla wszystkich graczy
            // dopiero za 10 sekund ...") and the confirmation ("Pomyslnie wystawiono produkt na rynek.").
            // Count only the confirmation.
            if (msg.contains("widoczny dla wszystkich")) return;
            listedCount++;
        } else if (msg.contains("balance") || msg.contains("stan konta")) {
            extractBalance(msg);
        }
    }

    public AutoMarketHistoryManager() {
        instance = this;
        this.startTime = System.currentTimeMillis();
        BKClientInit.getInstance().getEventManager().subscribe(this);
    }

    public List<HistoryEntry> getHistory() {
        return history;
    }

    public static AutoMarketHistoryManager getInstance() {
        return instance;
    }

    private void extractPriceAndAddProfit(String msg, boolean positive) {
        try {
            // Regex to find price like 940$ or 1.1k$
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([\\d\\s.,]+[kKmMldLD]?)\\$");
            java.util.regex.Matcher matcher = pattern.matcher(msg);
            double price = 0;
            if (matcher.find()) {
                price = parsePrice(matcher.group(1));
            } else {
                // Fallback (np. minestar "...zostal kupiony przez X za 590") \u2014 liczba po "za"
                java.util.regex.Matcher za = java.util.regex.Pattern
                        .compile("za\\s+([\\d.,]+[kKmMldLD]?)").matcher(msg);
                if (za.find()) {
                    price = parsePrice(za.group(1));
                }
            }
            if (price > 0) {
                if (positive) {
                    totalProfit += price;
                } else {
                    totalProfit -= price;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void extractBalance(String msg) {
        try {
            String numeric = BK_RE_3.matcher(msg).replaceAll(" ");
            String[] parts = BK_RE_4.split(numeric.trim());
            if (parts.length > 0) {
                this.walletBalance = Double.parseDouble(parts[parts.length - 1]);
            }
        } catch (Exception ignored) {
        }
    }

    public int getBoughtCount() {
        return boughtCount;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public int getListedCount() {
        return listedCount;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public double getWalletBalance() {
        double current = dev.bkclient.utility.game.ScoreboardUtility.getBalance();
        if (current >= 0) {
            this.walletBalance = current;
        }
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }

    public void addProfit(double amount) {
        this.totalProfit += amount;
    }

    public void incrementBought() {
        this.boughtCount++;
    }

    public void incrementSold() {
        this.soldCount++;
    }

    public void incrementListed() {
        this.listedCount++;
    }

    public void decrementListed() {
        if (this.listedCount > 0)
            this.listedCount--;
    }

    public void resetStats() {
        this.boughtCount = 0;
        this.soldCount = 0;
        this.listedCount = 0;
        this.totalProfit = 0;
        this.history.clear();
        this.startTime = System.currentTimeMillis();
        BKClientInit.getInstance().getNotificationManager().addNotificationOther(
            dev.bkclient.systems.notifications.NotificationType.SUCCESS,
            "Auto-Market", dev.bkclient.systems.localization.Localizator.translate(
                "modules.notifications.auto_market.stats_reset"));
    }

    public long getStartTime() {
        return startTime;
    }
}


