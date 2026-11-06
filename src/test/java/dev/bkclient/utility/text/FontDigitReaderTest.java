package dev.bkclient.utility.text;
import com.google.gson.JsonParser;
import java.util.HashMap;
public class FontDigitReaderTest {
    public static void main(String[] args) {
        var map = new HashMap<Integer, Character>();
        String font = "{\"providers\":[{\"type\":\"bitmap\",\"file\":\"font/technical/font_number_0.png\",\"chars\":[\"\uF6A5\"]}]}";
        FontDigitReader.apply(JsonParser.parseString(font).getAsJsonObject(), map);
        if (!FontDigitReader.read("/kopanie \uF6A5", map).orElseThrow().equals("0")) throw new AssertionError();
        FontDigitReader.apply(JsonParser.parseString(font.replace("number_0", "number_7")).getAsJsonObject(), map);
        if (!FontDigitReader.read("/kopanie \uF6A5", map).orElseThrow().equals("7")) throw new AssertionError("pack priority");
        if (FontDigitReader.read("/kopanie \uF6A5\uF6A6", map).isPresent()) throw new AssertionError("unknown glyph guessed");
        if (!FontDigitReader.read("/kopanie 00123", map).orElseThrow().equals("00123")) throw new AssertionError("leading zeros");
        FontDigitReader.apply(JsonParser.parseString(font.replace("font_number_0", "other")).getAsJsonObject(), map);
        if (FontDigitReader.read("/kopanie \uF6A5", map).isPresent()) throw new AssertionError("stale lower pack mapping");
        System.out.println("Font digits: live pack overrides, unknown rejection, leading zeros passed");
    }
}
