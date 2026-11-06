package dev.bkclient.larp.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import dev.bkclient.larp.feature.DamageColor;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeItemOption;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.gui.NumberSetting;

public final class Excalibur {
    private static final double KILLS_PER_PERCENT = 10.0;
    private static final int SEGMENTS = 10;
    public static final FakeItemOption OPTION = new FakeItemOption(Excalibur::spec);
    private static NumberSetting damageSetting;
    private static NumberSetting killsSetting;

    private Excalibur() {
    }

    public static void bind(NumberSetting damage, NumberSetting kills) {
        damageSetting = damage;
        killsSetting = kills;
        damage.onChange(OPTION::refresh);
        kills.onChange(OPTION::refresh);
    }

    private static Disguise.Spec spec() {
        return new Disguise.Spec(Items.NETHERITE_SWORD, LegacyText.parse("&e&lExcalibur"), Excalibur.lore(), Float.valueOf(5.0f), true, true, true, Disguise.Ability.NONE, null, 0);
    }

    private static List<Text> lore() {
        int damage = damageSetting == null ? 51 : (int)Excalibur.damageSetting.value;
        int kills = killsSetting == null ? 9 : (int)Excalibur.killsSetting.value;
        ArrayList<String> lines = new ArrayList<String>(List.of(DamageColor.line(damage), "&r", "&8 \u00bb &7Jest to przedmiot zdobyty z", "&8 \u00bb &fwydarzenia wakacyjnego 2024&7!", "&r", "&8 \u00bb &7Aktualnie zab\u00f3jstw: &f" + kills, Excalibur.progressBar(kills), "&r", "&8 \u00bb &7Zape\u0142nienie paska zapewnia", "&8 \u00bb &7Ci &f12 punkt\u00f3w obra\u017ce\u0144&7, co", "&8 \u00bb &7jest r\u00f3wnoznaczne z &eostro\u015bci\u0105 7&7!", "&7", "&8 \u00bb &7Obra\u017cenia od ataku: &f11.5"));
        return lines.stream().map(LegacyText::parse).toList();
    }

    private static String progressBar(int kills) {
        double percent = Math.min(100.0, (double)Math.round((double)kills / 10.0 * 10.0) / 10.0);
        int lit = (int)Math.floor(percent / 10.0);
        StringBuilder bar = new StringBuilder("&8 \u00bb ");
        for (int i = 0; i < 10; ++i) {
            bar.append(i < lit ? "&a&m-" : "&f&m-");
        }
        return bar.append(" &r &e").append(Excalibur.formatPercent(percent)).append('%').toString();
    }

    private static String formatPercent(double percent) {
        if (percent >= 100.0) {
            return "100";
        }
        return String.format(Locale.ROOT, "%.1f", percent);
    }
}
