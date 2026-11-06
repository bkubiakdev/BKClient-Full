package dev.bkclient.larp.feature;

import java.util.List;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeItemOption;
import dev.bkclient.larp.feature.LegacyText;

public final class TotemUlaskawienia {
    private static final List<String> LORE = List.of("&8 \u00bb &7Jest to przedmiot z:", "&8 \u00bb &feventu halloween (2023)", "&r", "&8 \u00bb &7Trzymaj\u0105c go po \u015bmierci &dnie", "&8 \u00bb &dstracisz &7swoich przedmiot\u00f3w!");
    private static final Disguise.Spec SPEC = new Disguise.Spec(Items.TOTEM_OF_UNDYING, LegacyText.parse("&5&lTotem u\u0142askawienia"), TotemUlaskawienia.lore(), Float.valueOf(1.0f), false, true, false, Disguise.Ability.NONE, null, 0);
    public static final FakeItemOption OPTION = new FakeItemOption(() -> SPEC);

    private TotemUlaskawienia() {
    }

    private static List<Text> lore() {
        return LORE.stream().map(LegacyText::parse).toList();
    }
}
