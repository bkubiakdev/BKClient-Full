package dev.bkclient.utility.text;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Reads the server pack's named digit glyphs; never guesses unknown characters. */
public final class FontDigitReader {
    private static final Pattern DIGIT = Pattern.compile("(?:[^:]+:)?font/technical/font_number_([0-9])\\.png");
    private static final Pattern COMMAND = Pattern.compile("/kopanie\\s+([^\\s]+)");
    private FontDigitReader() {}

    /** Apply packs from low to high priority; earlier providers win inside each font. */
    public static void apply(JsonObject font, Map<Integer, Character> digits) {
        if (!font.has("providers")) return;
        var providers = font.getAsJsonArray("providers");
        for (int i = providers.size() - 1; i >= 0; i--) {
            var provider = providers.get(i).getAsJsonObject();
            if (!provider.has("chars") || !provider.has("file")) continue;
            var rows = provider.getAsJsonArray("chars");
            var match = DIGIT.matcher(provider.get("file").getAsString());
            boolean single = rows.size() == 1 && rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length()) == 1;
            Character digit = single && match.matches() ? match.group(1).charAt(0) : null;
            for (var row : rows) row.getAsString().codePoints().filter(cp -> cp != 0).forEach(cp -> {
                if (digit == null) digits.remove(cp); else digits.put(cp, digit);
            });
        }
    }

    public static Optional<String> read(String text, Map<Integer, Character> digits) {
        var match = COMMAND.matcher(text.replaceAll("§[0-9a-fk-orA-FK-OR]", ""));
        if (!match.find()) return Optional.empty();
        int[] code = match.group(1).codePoints().toArray();
        if (code.length == 0 || code.length > 16) return Optional.empty();
        StringBuilder result = new StringBuilder();
        for (int cp : code) {
            Character digit = digits.get(cp);
            if (digit == null && cp >= '0' && cp <= '9') digit = (char)cp;
            if (digit == null) return Optional.empty();
            result.append(digit);
        }
        return Optional.of(result.toString());
    }
}
