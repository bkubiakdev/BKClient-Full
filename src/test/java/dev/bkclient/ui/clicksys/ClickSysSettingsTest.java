package dev.bkclient.ui.clicksys;

import dev.bkclient.ui.clicksys.port.setting.BooleanSetting;
import dev.bkclient.ui.clicksys.port.setting.ListSetting;
import java.util.List;
import dev.bkclient.ui.clicksys.port.util.TextHash;

/** Regression checks for labels changed after they have already been displayed. */
public final class ClickSysSettingsTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        BooleanSetting toggle = new BooleanSetting("Original", "Description", false);
        check(toggle.getName().equals("Original"), "initial name");
        check(toggle.getDescription().equals("Description"), "initial description");
        String oldHash = toggle.getNameHash();
        toggle.setName("Renamed");
        toggle.setDescription("Updated");
        check(toggle.getDisplayName().equals("Renamed"), "rename after first draw");
        check(toggle.getDisplayDescription().equals("Updated"), "description after first draw");
        check(!oldHash.equals(toggle.getNameHash()), "rename updates translation key");
        toggle.setName(null);
        toggle.setDescription(null);
        check(toggle.getName().isEmpty() && toggle.getDescription().isEmpty(), "null labels");

        ListSetting mode = new ListSetting("Mode", "", List.of("A", "B"), List.of("B"), false, 1, 1);
        check(mode.getString().equals("B"), "initial selection");
        check(mode.getList2().equals(List.of("A", "B")), "initial option labels");
        mode.addList2(List.of("B", "C"));
        check(mode.getList2().equals(List.of("B", "C")), "replaced option labels");
        check(mode.getString().equals("B"), "selection retained when options reorder");
        mode.addList(List.of("C"));
        check(mode.getString().equals("C"), "selection uses replacement labels");
        mode.onStringArrayArrayIntArray(new String[][]{{"D"}, {"E", "F"}}, new int[]{1});
        check(mode.getList2().equals(List.of("D", "EF")), "array-based replacement labels");
        check(mode.getString().equals("EF"), "array-based replacement selection");

        ListSetting legacy = new ListSetting("Style", "", List.of("Waves", "Matte"), List.of("Waves"), false, 1, 1);
        legacy.getList2();
        legacy.addList(List.of("Матовость"));
        check(legacy.getString().equals("Matte"), "legacy option matching");
        legacy.addList(List.of("Waves"));
        var saved = legacy.toJson();
        legacy.addList(List.of("Matte"));
        legacy.fromJson(saved);
        check(legacy.getString().equals("Waves"), "saved selection round trip");
        // Warm exact-name lookups, then replace/reorder options: cached indices must expire.
        ListSetting reordered = new ListSetting("Cached", "", List.of("First", "Second"), List.of("Second"), false, 1, 1);
        for (int i = 0; i < 100; i++) reordered.addList(List.of("Second"));
        reordered.addList3(List.of("Second", "First"));
        reordered.addList(List.of("Second"));
        check(reordered.getInt2() == 0, "cached option index invalidated after replacement");
        reordered.onStringArrayArrayIntArray(new String[][]{{"First"}, {"Second"}}, new int[]{0});
        reordered.addList(List.of("Second"));
        check(reordered.getInt2() == 1, "cached option index invalidated after array replacement");
        // Aliases keep first-match semantics and normalization's migration signal.
        ListSetting aliases = new ListSetting("Aliases", "", List.of("а", "a"), List.of(), false, 1, 0);
        TextHash.check();
        aliases.addList(List.of("a"));
        check(aliases.getInt2() == 0 && TextHash.check(), "normalized alias takes precedence");
        aliases.addList(List.of("a"));
        check(TextHash.check(), "repeated aliases retain migration signal");
        System.out.println("ClickSys label replacement, selection preservation and configuration round trip passed");
    }
}
