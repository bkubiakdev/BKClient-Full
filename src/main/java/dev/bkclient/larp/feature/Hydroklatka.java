package dev.bkclient.larp.feature;

import java.util.List;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeItemOption;
import dev.bkclient.larp.feature.LegacyText;

public final class Hydroklatka {
    private static final List<String> LORE = List.of("&8 \u00bb &7Jest to przedmiot zdobyty podczas", "&8 \u00bb &fwydarzenia wakacyjnego 2025&7!", "&7", "&8 \u00bb &7Po u\u017cyciu wystrzeliwuje pocisk, kt\u00f3ry", "&8 \u00bb &7tworzy &3wodn\u0105 klatk\u0119&7 uwi\u0119ziaj\u0105c\u0105", "&8 \u00bb &7przeciwnik\u00f3w w miejscu!");
    private static final Disguise.Spec SPEC = new Disguise.Spec(Items.BLAZE_ROD, LegacyText.parse("&3&lWyrzutnia Hydro Klatki"), Hydroklatka.lore(), Float.valueOf(2.0f), true, true, true, Disguise.Ability.HYDRO_CAGE, null, 0);
    public static final FakeItemOption OPTION = new FakeItemOption(() -> SPEC);

    private Hydroklatka() {
    }

    private static List<Text> lore() {
        return LORE.stream().map(LegacyText::parse).toList();
    }
}
