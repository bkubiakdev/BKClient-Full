package dev.bkclient.utility.game;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.bkclient.systems.modules.modules.bkmods.automarket.AutoMarketScanner;
import dev.bkclient.utility.inventory.ItemStackExtractor;

/**
 * Utility to extract player balance from the scoreboard sidebar.
 * Ported from bk-rynek's ScoreboardUtils (1.21.1 -> 1.21.4).
 */
public final class ScoreboardUtility {

    private static final Pattern RAPY_FINANCE_LINE_PATTERN = Pattern.compile(
            "(?i)finanse\\s*[:\\-]*[^0-9.,]*([0-9.,]+)\\s?([kmbt]?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BALANCE_LINE_PATTERN = Pattern.compile(
            "(?i)(?:pieni\u0105dze|pieniadze|balance|stan konta|portfel|wallet|\\$|\u00BB)\\s*[:\\-\u00bb]*[^0-9.,]*([0-9.,]+)\\s?([kmbt]?)",
            Pattern.CASE_INSENSITIVE);

    private ScoreboardUtility() {
    }

    /**
     * Attempts to find the player's balance in the scoreboard.
     * Returns the raw double value, or -1 if not found.
     */
    public static double getBalance() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null)
                return -1;

            Scoreboard scoreboard = client.world.getScoreboard();

            // 1. Try Sidebar first
            ScoreboardObjective sidebarObj = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (sidebarObj != null) {
                double result = extractFromObjective(scoreboard, sidebarObj);
                if (result >= 0)
                    return result;
            }

            // 2. Fallback: Search ALL objectives if sidebar didn't work
            for (ScoreboardObjective obj : scoreboard.getObjectives()) {
                if (obj == sidebarObj)
                    continue;
                double result = extractFromObjective(scoreboard, obj);
                if (result >= 0)
                    return result;
            }

        } catch (Exception ignored) {
        }
        return -1;
    }

    private static double extractFromObjective(Scoreboard scoreboard, ScoreboardObjective objective) {
        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);

        for (ScoreboardEntry entry : entries) {
            String owner = entry.owner();
            StringBuilder fullLine = new StringBuilder();

            Team team = scoreboard.getScoreHolderTeam(owner);
            if (team != null) {
                fullLine.append(team.getPrefix().getString());
                fullLine.append(owner);
                fullLine.append(team.getSuffix().getString());
            } else {
                fullLine.append(owner);
            }

            if (entry.display() != null) {
                fullLine.append(" ").append(entry.display().getString());
            }

            // Clean colors for regex
            String clean = fullLine.toString().replaceAll("\u00A7[0-9a-fk-or]", "").trim();

            clean = ItemStackExtractor.toReadablePlain(fullLine.toString());
            Matcher m = BALANCE_LINE_PATTERN.matcher(clean);
            boolean found = m.find();
            if (!found) {
                m = RAPY_FINANCE_LINE_PATTERN.matcher(clean);
                found = m.find();
            }
            if (found) {
                String valStr = m.group(1) + (m.group(2) != null ? m.group(2) : "");
                return AutoMarketScanner.parsePriceWithSuffix(valStr);
            }
        }
        return -1;
    }

    /**
     * Returns a formatted version of the balance for UI display.
     */
    public static String getFormattedBalance() {
        double bal = getBalance();
        if (bal < 0)
            return "---";
        return TextUtility.formatMoney(bal) + "$";
    }
}


