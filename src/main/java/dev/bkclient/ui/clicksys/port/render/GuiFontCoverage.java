package dev.bkclient.ui.clicksys.port.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.client.texture.NativeImage;

/** Adds missing Polish glyphs from the existing BK atlases once, when loading a GUI font. */
final class GuiFontCoverage {
    private static final String POLISH = "ĄĆĘŁŃÓŚŹŻąćęłńóśźż";
    private record Placement(JsonObject glyph, int left, int bottom, int width, int height, int x, int y) {}

    static NativeImage complete(String name, JsonObject data, NativeImage original) throws IOException {
        String fallback = switch (name) {
            case "a", "aa" -> "medium";
            case "b", "bb", "c", "cc" -> "semibold";
            default -> null;
        };
        if (fallback == null) return original;
        var present = new HashSet<Integer>();
        for (var glyph : data.getAsJsonArray("glyphs")) present.add(glyph.getAsJsonObject().get("unicode").getAsInt());
        if (POLISH.codePoints().allMatch(present::contains)) return original;
        String path = "/assets/bkclient/fonts/msdf/" + fallback;
        try (var json = GuiFontCoverage.class.getResourceAsStream(path + ".json");
             var png = GuiFontCoverage.class.getResourceAsStream(path + ".png")) {
            if (json == null || png == null) throw new IOException("Missing GUI fallback font " + fallback);
            var extra = JsonParser.parseReader(new InputStreamReader(json, StandardCharsets.UTF_8)).getAsJsonObject();
            try (var source = NativeImage.read(NativeImage.Format.RGBA, png)) {
                int x = original.getWidth() + 2, y = 2, columnWidth = 0;
                var placements = new ArrayList<Placement>();
                for (var element : extra.getAsJsonArray("glyphs")) {
                    var glyph = element.getAsJsonObject();int cp = glyph.get("unicode").getAsInt();
                    if (POLISH.indexOf(cp) < 0 || present.contains(cp)) continue;
                    var bounds = glyph.getAsJsonObject("atlasBounds");
                    int left = (int)Math.floor(bounds.get("left").getAsDouble());
                    int bottom = (int)Math.floor(bounds.get("bottom").getAsDouble());
                    int width = (int)Math.ceil(bounds.get("right").getAsDouble()) - left;
                    int height = (int)Math.ceil(bounds.get("top").getAsDouble()) - bottom;
                    if (y + height + 2 > original.getHeight()) { x += columnWidth + 2; y = 2; columnWidth = 0; }
                    placements.add(new Placement(glyph, left, bottom, width, height, x, y));
                    columnWidth = Math.max(columnWidth, width);y += height + 2;
                }
                if (placements.isEmpty()) return original;
                // Square atlas matches all existing GUI shader paths and adds only a narrow glyph strip.
                int edge = Math.max(Math.max(original.getWidth(), original.getHeight()), x + columnWidth + 2);
                var merged = new NativeImage(NativeImage.Format.RGBA, edge, edge, true);
                boolean complete = false;
                try {
                    for (int py=0;py<original.getHeight();py++) for (int px=0;px<original.getWidth();px++)
                        merged.setColorArgb(px, py + edge - original.getHeight(), original.getColorArgb(px,py));
                    float ratio = extra.getAsJsonObject("atlas").get("distanceRange").getAsFloat()
                        / data.getAsJsonObject("atlas").get("distanceRange").getAsFloat();
                    var next = data.deepCopy();
                    for (var p : placements) {
                        int top = source.getHeight() - p.bottom - p.height;
                        for (int py=0;py<p.height;py++) for (int px=0;px<p.width;px++)
                            merged.setColorArgb(p.x+px,p.y+py,remapRange(source.getColorArgb(p.left+px,top+py),ratio));
                        var glyph=p.glyph.deepCopy();var bounds=glyph.getAsJsonObject("atlasBounds");
                        float dx=p.x-p.left,dy=edge-p.y-p.height-p.bottom;
                        for (String key : new String[]{"left","right"}) bounds.addProperty(key,bounds.get(key).getAsFloat()+dx);
                        for (String key : new String[]{"bottom","top"}) bounds.addProperty(key,bounds.get(key).getAsFloat()+dy);
                        next.getAsJsonArray("glyphs").add(glyph);
                    }
                    next.getAsJsonObject("atlas").addProperty("width",edge);
                    next.getAsJsonObject("atlas").addProperty("height",edge);
                    data.add("atlas",next.get("atlas"));data.add("glyphs",next.get("glyphs"));
                    complete=true;original.close();return merged;
                } finally { if (!complete) merged.close(); }
            }
        }
    }

    private static int remapRange(int argb,float ratio) {
        int result=0;
        for(int shift=0;shift<32;shift+=8) {
            int value=Math.clamp(Math.round((((argb>>>shift)&255)-127.5f)*ratio+127.5f),0,255);
            result |= value<<shift;
        }
        return result;
    }
}
