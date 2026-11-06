package dev.bkclient.systems.modules.modules.bkmods.enchant;

import java.util.List;
import static dev.bkclient.systems.modules.modules.bkmods.enchant.EnchantRules.*;

public final class EnchantRulesTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void reads(String line, double expected) {
        var reading = read(List.of("Cena: 500%", line, "Inny bonus: 100%"));
        check(reading != null && reading.percent() == expected && reading.line().equals(line), line);
    }

    public static void main(String[] args) {
        reads("Dodatkowe obrażenia 0%", 0);
        reads("Dodatkowe obrażenia 13%", 13);
        reads("§7Dodatkowe obrażenia §f50%", 50);
        reads("§x§F§F§0§0§A§ADODATKOWE OBRAŻENIA: §l+50 %", 50);
        reads("» Dodatkowe\u00a0obrażenia: 49,5 %", 49.5);
        reads("Dodatkowe obrazenia −10%", -10);
        reads("Dodatkowe obrażenia 50.25%", 50.25);
        reads("  Dodatkowe   obrażenia  100%  ", 100);
        check(read(List.of()) == null, "Missing lore is not zero");
        check(read(List.of("Szansa: 50%", "Cena: 13%")) == null, "Ignore unrelated percentages");
        check(read(List.of("Dodatkowe obrażenia ?%")) == null, "Malformed value");
        check(read(List.of("Dodatkowe obrażenia 50")) == null, "Require percent suffix");
        check(read(List.of("Dodatkowe obrażenia 13%", "Dodatkowe obrażenia 50%")) == null, "Ambiguous lines");
        check(read(List.of("Dodatkowe obrażenia 13%", "Dodatkowe obrażenia ?%")) == null, "Malformed duplicate");
        check(read(List.of("Dodatkowe obrażenia 13% / 50%")) == null, "Do not mistake a range for the actual bonus");
        check(read(List.of("UWAGA! Przemieniasz bonus Dodatkowe obrażenia 50%")) == null, "Do not read button instructions");
        check(matches(50, Mode.MINIMUM, 50, 0), "Minimum includes threshold");
        check(matches(51, Mode.MINIMUM, 50, 0), "Minimum permits greater values");
        check(!matches(49.9, Mode.MINIMUM, 50, 0), "No rounding up");
        check(matches(40, Mode.RANGE, 40, 50), "Range includes lower bound");
        check(matches(50, Mode.RANGE, 40, 50), "Range includes upper bound");
        check(!matches(51, Mode.RANGE, 40, 50), "Range rejects greater values");
        check(!matches(39, Mode.RANGE, 40, 50), "Range rejects smaller values");
        check(!matches(45, Mode.RANGE, 50, 40), "Invalid bounds fail closed");
        check(matches(50, Mode.EXACT, 50, 0), "Exact value accepted");
        check(!matches(51, Mode.EXACT, 50, 0), "Exact is not minimum");
        check(!matches(49.99, Mode.EXACT, 50, 0), "Exact does not round");
        check(!matches(Double.NaN, Mode.MINIMUM, 50, 0), "Invalid reading rejected");
        check(!completedRoll(13, 13, 10, 10), "An unchanged refresh cannot trigger another click");
        check(completedRoll(13, 13, 10, 9), "Repeated percentage with spent currency is a roll");
        check(completedRoll(13, 14, 10, 10), "Changed bonus confirms a roll");
        check(!completedRoll(13, Double.NaN, 10, 9), "Currency alone cannot replace valid lore");
        check(!completedRoll(13, 13, 10, 11), "Incoming currency is not a roll");
        check(menuAction(false, false, true, false, false, false, 0, 5000) == MenuAction.CURRENT,
                "Snipe key can attach to the initial menu");
        check(menuAction(true, true, true, false, true, false, 100, 5000) == MenuAction.CURRENT,
                "In-place server updates keep the existing handler");
        check(menuAction(true, false, true, false, true, false, 100, 5000) == MenuAction.REATTACH,
                "Screenshot regression: server replaces menu after 19% -> 3%, retain pending roll");
        check(menuAction(true, false, false, true, true, false, 100, 5000) == MenuAction.WAIT,
                "Server close before reopen must not end the roll or allow another click");
        check(menuAction(true, false, true, false, true, false, 250, 5000) == MenuAction.REATTACH,
                "After the temporary close, accept the same menu with a new handler");
        check(completedRoll(19, read(List.of("Dodatkowe obrażenia 3%")).percent(), 64, 63),
                "Snapshot from reopened GUI confirms the screenshot roll, even if the packet arrived before reattachment");
        check(!matches(3, Mode.EXACT, 50, 80), "Continue after reopened menu shows 3% instead of target 50%");
        check(matches(50, Mode.EXACT, 50, 80), "Stop if the reopened menu already contains the target");
        check(menuAction(true, false, true, false, true, true, 100, 5000) == MenuAction.STOP,
                "Manual close wins even if the server immediately reopens the menu");
        check(menuAction(true, false, false, true, true, true, 100, 5000) == MenuAction.STOP,
                "Escape/manual close stops immediately during a pending roll");
        check(menuAction(true, false, false, false, true, false, 100, 5000) == MenuAction.STOP,
                "Do not continue in a different menu or layout");
        check(menuAction(true, false, true, false, false, false, 100, 5000) == MenuAction.STOP,
                "A new handler without our pending click is not an automatic refresh");
        check(menuAction(true, false, false, true, true, false, 4999, 5000) == MenuAction.WAIT,
                "Permit a delayed reopen inside the original response deadline");
        check(menuAction(true, false, false, true, true, false, 5000, 5000) == MenuAction.STOP,
                "A menu that never reopens cannot wait indefinitely");
        check(menuAction(true, false, true, false, true, false, 5000, 5000) == MenuAction.STOP,
                "Repeated refreshes cannot extend the original response deadline");
        check(menuAction(true, false, false, true, false, false, 100, 5000) == MenuAction.STOP,
                "Ordinary screen close without a pending roll stops");
        System.out.println("Auto Zmianka: " + checks + " lore, threshold, roll and menu-refresh checks passed");
    }
}
