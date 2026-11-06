package dev.bkclient.larp.gui;

import java.util.ArrayList;
import java.util.List;
import dev.bkclient.larp.feature.Excalibur;
import dev.bkclient.larp.feature.FakeItemOption;
import dev.bkclient.larp.feature.Hydroklatka;
import dev.bkclient.larp.feature.KrolicyMiecz;
import dev.bkclient.larp.feature.Presets;
import dev.bkclient.larp.feature.TotemUlaskawienia;
import dev.bkclient.larp.gui.Category;
import dev.bkclient.larp.gui.ModOption;
import dev.bkclient.larp.gui.NumberSetting;

public final class Panels {
    public static final List<Category> CATEGORIES = new ArrayList<Category>();

    private Panels() {
    }

    public static void init() {
        CATEGORIES.clear();
        Category fake = new Category("fakeeventowka", "FakeEvent\u00f3wka", 20, 20);
        fake.with(Panels.simple("totem_ulaskawienia", "Totem U\u0142askawienia", TotemUlaskawienia.OPTION));
        fake.with(Panels.excalibur());
        fake.with(Panels.krolicyMiecz());
        fake.with(Panels.simple("hydroklatka", "Hydroklatka", Hydroklatka.OPTION));
        fake.with(Panels.simple("siekiera_grincha", "Siekiera Grincha", Presets.GRINCH_AXE));
        fake.with(Panels.simple("sakiewka_dropu", "Sakiewka dropu", Presets.DROP_POUCH));
        fake.with(Panels.simple("wampirze_jablko", "Wampirze Jab\u0142ko", Presets.VAMPIRE_APPLE_ITEM));
        fake.with(Panels.simple("wedka_guardiana", "W\u0119dka Guardiana", Presets.GUARDIAN_ROD));
        fake.with(Panels.simple("rozdzka_iluzjonisty", "R\u00f3\u017cd\u017cka Iluzjonisty", Presets.ILLUSIONIST_WAND));
        fake.with(Panels.simple("blok_widmo", "Blok Widmo", Presets.PHANTOM_BLOCK));
        fake.with(Panels.simple("cudowna_latarnia", "Cudowna Latarnia", Presets.MAGIC_LANTERN));
        fake.with(Panels.simple("lizak", "Lizak", Presets.LOLLIPOP));
        fake.with(Panels.simple("wedka_nielota", "W\u0119dka nielota", Presets.FLIGHTLESS_ROD));
        fake.with(Panels.simple("zatruty_olowek", "Zatruty o\u0142\u00f3wek", Presets.POISON_PENCIL));
        fake.with(Panels.simple("roza_kupidyna", "R\u00f3\u017ca kupidyna", Presets.CUPID_ROSE));
        fake.with(Panels.simple("plecak_drakuli", "Plecak Drakuli", Presets.DRACULA_BACKPACK));
        fake.with(Panels.simple("kosa", "Kosa", Presets.SCYTHE));
        fake.with(Panels.simple("lopata_grincha", "\u0141opata Grincha", Presets.GRINCH_SHOVEL));
        fake.with(Panels.simple("boski_topor", "Boski top\u00f3r", Presets.DIVINE_AXE));
        fake.with(Panels.simple("arcus_magnus", "Arcus Magnus", Presets.ARCUS_MAGNUS));
        fake.with(Panels.simple("kostka_rubika", "Kostka rubika", Presets.RUBIKS_CUBE));
        fake.with(Panels.simple("rozga", "R\u00f3zga", Presets.ROD));
        CATEGORIES.add(fake);
    }

    private static ModOption simple(String id, String name, FakeItemOption option) {
        return new ModOption(id, name).item(option).onToggle(enabled -> enabled ? option.enable() : option.disable());
    }

    private static ModOption excalibur() {
        NumberSetting damage = new NumberSetting("obrazenia", "Dodatkowe obra\u017cenia", 51.0, -10.0, 100.0, 1.0, "%");
        NumberSetting kills = new NumberSetting("zabojstwa", "Zab\u00f3jstwa", 9.0, 0.0, 1000.0, 1.0, "");
        Excalibur.bind(damage, kills);
        return new ModOption("excalibur", "Excalibur").with(damage).with(kills).item(Excalibur.OPTION).onToggle(enabled -> enabled ? Excalibur.OPTION.enable() : Excalibur.OPTION.disable());
    }

    private static ModOption krolicyMiecz() {
        NumberSetting damage = new NumberSetting("obrazenia", "Dodatkowe obra\u017cenia", 54.0, -10.0, 100.0, 1.0, "%");
        KrolicyMiecz.bind(damage);
        return new ModOption("kroliczy_miecz", "Kr\u00f3liczy miecz").with(damage).item(KrolicyMiecz.OPTION).onToggle(enabled -> enabled ? KrolicyMiecz.OPTION.enable() : KrolicyMiecz.OPTION.disable());
    }

    public static Category byId(String id) {
        for (Category c : CATEGORIES) {
            if (!c.id.equals(id)) continue;
            return c;
        }
        return null;
    }

    public static ModOption optionById(String categoryId, String optionId) {
        Category category = Panels.byId(categoryId);
        if (category == null) {
            return null;
        }
        for (ModOption option : category.options) {
            if (!option.id.equals(optionId)) continue;
            return option;
        }
        return null;
    }
}
