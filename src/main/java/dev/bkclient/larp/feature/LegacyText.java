package dev.bkclient.larp.feature;

import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.MutableText;

public final class LegacyText {
    private static final Style BASE = Style.EMPTY.withItalic(Boolean.valueOf(false));

    private LegacyText() {
    }

    public static Text parse(String legacy) {
        MutableText result = Text.empty().setStyle(BASE);
        StringBuilder buffer = new StringBuilder();
        Style style = BASE;
        for (int i = 0; i < legacy.length(); ++i) {
            Formatting formatting;
            char c = legacy.charAt(i);
            Formatting Schema1483 = formatting = c == '&' && i + 1 < legacy.length() ? Formatting.byCode((char)legacy.charAt(i + 1)) : null;
            if (formatting == null) {
                buffer.append(c);
                continue;
            }
            if (!buffer.isEmpty()) {
                result.append((Text)Text.literal((String)buffer.toString()).setStyle(style));
                buffer.setLength(0);
            }
            style = LegacyText.apply(style, formatting);
            ++i;
        }
        if (!buffer.isEmpty()) {
            result.append((Text)Text.literal((String)buffer.toString()).setStyle(style));
        }
        return result;
    }

    private static Style apply(Style style, Formatting formatting) {
        if (formatting == Formatting.RESET) {
            return BASE;
        }
        if (formatting.isColor()) {
            return BASE.withColor(formatting);
        }
        return style.withFormatting(formatting);
    }
}
