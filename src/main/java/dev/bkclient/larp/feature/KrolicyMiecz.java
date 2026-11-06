package dev.bkclient.larp.feature;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import dev.bkclient.larp.feature.DamageColor;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeItemOption;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.gui.NumberSetting;

public final class KrolicyMiecz {
    public static final FakeItemOption OPTION = new FakeItemOption(KrolicyMiecz::spec);
    private static NumberSetting damageSetting;

    private KrolicyMiecz() {
    }

    public static void bind(NumberSetting damage) {
        damageSetting = damage;
        damage.onChange(OPTION::refresh);
    }

    private static Disguise.Spec spec() {
        return new Disguise.Spec(Items.NETHERITE_SWORD, LegacyText.parse("&3&lKr\u00f3liczy miecz"), KrolicyMiecz.lore(), Float.valueOf(2.0f), true, false, true, Disguise.Ability.RABBIT_SWORD, null, 0);
    }

    private static List<Text> lore() {
        int damage = damageSetting == null ? 54 : (int)KrolicyMiecz.damageSetting.value;
        ArrayList<String> lines = new ArrayList<String>(List.of("&7Enchantments", "   &6Sharpness VI", "   &6Fire Aspect II", DamageColor.line(damage), "&7", "&8 \u00bb &7Jest to przedmiot z &fwydarzenia", "&8 \u00bb &fwielkanocnego &7z &f2024 roku&7.", "&r", "&8 \u00bb &7Po uderzeniu przeciwnika blokuje", "&8 \u00bb &7mo\u017cliwo\u015b\u0107 skakania na &f4 sekundy&7!"));
        return lines.stream().map(LegacyText::parse).toList();
    }
}
