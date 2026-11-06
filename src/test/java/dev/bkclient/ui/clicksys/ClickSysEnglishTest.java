package dev.bkclient.ui.clicksys;

import dev.bkclient.ui.clicksys.port.concurrent.Translations;
import dev.bkclient.ui.clicksys.port.enums.Language;
import dev.bkclient.ui.clicksys.port.util.TextHash;

public final class ClickSysEnglishTest {
    public static void main(String[] args) {
        Translations translations = Translations.getInstance();
        for (int i = 0; i < 3; i++) {
            translations.update();
            if (translations.getLanguage() != Language.EN) throw new AssertionError("Russian mode returned");
            for (String text : new String[]{"Создать новый конфиг", "Сохранить конфиг", "Удалить друга", "Введите текст"}) {
                String translated = translations.getStringByString2(text);
                if (translated.matches(".*[\u0400-\u04ff].*")) throw new AssertionError(translated);
            }
        }
        if (!TextHash.isStringString("Матовость", "Matte") || !TextHash.isStringString("Подложки", "Surfaces"))
            throw new AssertionError("Legacy saved theme options lost compatibility");
        if (TextHash.isStringString("Матовость", "Waves")) throw new AssertionError("Different modes matched");
        System.out.println("ClickSys English labels, language fallback and legacy option aliases passed");
    }
}
