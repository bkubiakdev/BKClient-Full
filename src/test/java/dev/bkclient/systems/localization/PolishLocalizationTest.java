package dev.bkclient.systems.localization;

import dev.bkclient.ui.clicksys.port.concurrent.Translations;
import dev.bkclient.ui.clicksys.port.setting.ActionSetting;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public final class PolishLocalizationTest {
    private static void equal(String actual, String expected) {
        if (!Objects.equals(actual, expected)) throw new AssertionError(actual + " != " + expected);
    }
    private static Map<String,String> read(String name) throws Exception {
        Map<String,String> result = new HashMap<>();
        for (String line : Files.readAllLines(Path.of("src/main/resources/assets/bkclient/lang/" + name + ".lang"))) {
            line = line.split("#", 2)[0].trim();
            int at = line.indexOf('=');
            if (at > 0) result.put(line.substring(0, at).trim(), line.substring(at + 1).trim());
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        var en = read("en_us"); var pl = read("pl_pl");
        var missing = new TreeSet<>(en.keySet()); missing.removeAll(pl.keySet());
        if (!missing.isEmpty()) throw new AssertionError("Missing Polish resource keys: " + missing);
        var format = Pattern.compile("%(?:[0-9]+\\$)?[-#+ 0,(]*[0-9]*(?:\\.[0-9]+)?[a-zA-Z%]");
        for (String key : en.keySet()) {
            var a = format.matcher(en.get(key)).results().map(m -> m.group()).toList();
            var b = format.matcher(pl.get(key)).results().map(m -> m.group()).toList();
            if (!a.equals(b)) throw new AssertionError("Format placeholders changed: " + key + " " + a + " / " + b);
        }
        var setting = new ActionSetting("Panel opacity", "Dark tint behind text. Higher values improve contrast.");
        Localizator.setLanguage(Language.EN_US);
        equal(setting.getDisplayName(), "Panel opacity");
        Localizator.setLanguage(Language.PL_PL);
        equal(setting.getDisplayName(), "Widoczność paneli");
        equal(setting.getDisplayDescription(), "Ciemne tło tekstu. Wyższa wartość poprawia kontrast.");
        equal(Translations.getInstance().getStringByString2("Search configs..."), "Szukaj konfiguracji...");
        equal(Translations.getInstance().getStringByString2("Create"), "Utwórz");
        equal(Localizator.translate("quick.menu_scale"), "Skala menu");
        equal(Localizator.translate("esp.equipment.simple"), "Prosty");
        equal(Localizator.translate("commands.target.added", "Test123"), "Dodano cel Test123");
        equal(setting.getName(), "Panel opacity");
        equal(Localizator.translate("my-personal-config-123"), "my-personal-config-123");
        Localizator.setLanguage(Language.EN_US);
        equal(setting.getDisplayName(), "Panel opacity");
        equal(Translations.getInstance().getStringByString2("Search configs..."), "Search configs...");
        System.out.println("Polish localization: resource coverage, placeholders, live EN/PL switch and stable identifiers passed");
    }
}
