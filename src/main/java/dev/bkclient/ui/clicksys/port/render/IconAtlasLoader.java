package dev.bkclient.ui.clicksys.port.render;

import dev.bkclient.ui.clicksys.port.data.FontData;
import dev.bkclient.ui.clicksys.port.module.Feature;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Load one atlas on the render thread; its texture has an explicit owner. */
public class IconAtlasLoader {
   private String text;
   private Identifier identifier;
   private String text2;
   public IconAtlasLoader getIconAtlasLoaderByString(String name) { text2 = name; identifier = null; return this; }
   public IconAtlasLoader getIconAtlasLoaderByString2(String name) { text = name; return this; }
   static Identifier textureId(String name) { return Identifier.of("bkclicksys", "dynamic/fonts/" + name); }

   public IconAtlas getIconAtlas() {
      if (text == null || text.isEmpty()) return null;
      NativeImage image = null;
      NativeImageBackedTexture owned = null;
      boolean transferred = false;
      try (var json = IconAtlasLoader.class.getResourceAsStream("/assets/bkclicksys/fonts/" + text + ".json")) {
         if (json == null) return null;
         var source = com.google.gson.JsonParser.parseReader(new InputStreamReader(json, StandardCharsets.UTF_8)).getAsJsonObject();
         if (text2 != null) {
            try (var png = IconAtlasLoader.class.getResourceAsStream("/assets/bkclicksys/fonts/" + text + ".png")) {
               if (png == null) return null;
               image = NativeImage.read(NativeImage.Format.RGBA, png);
            }
            image = GuiFontCoverage.complete(text, source, image);
         }
         FontData data = new com.google.gson.Gson().fromJson(source, FontData.class);
         if (data == null || data.getGlyphMetrics() == null) return null;
         Map<Integer, AtlasSprite> glyphs = new HashMap<>();
         float width = data.getGlyphMetrics().getValue2(), height = data.getGlyphMetrics().getValue3();
         if (data.getList() != null) for (var glyph : data.getList()) {
            if (glyph != null) glyphs.put(glyph.getValue(), new AtlasSprite(glyph, width, height));
         }
         Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
         if (data.getList2() != null) for (var kerning : data.getList2()) {
            if (kerning != null) kernings.computeIfAbsent(kerning.getValue(), ignored -> new HashMap<>())
               .put(kerning.getValue2(), kerning.getValue3());
         }
         AbstractTexture texture;
         if (text2 != null) {
            owned = new NativeImageBackedTexture(image);
            owned.setFilter(true, false);
            texture = owned;
         } else {
            if (identifier == null) return null;
            texture = Feature.mc.getTextureManager().getTexture(identifier);
         }
         IconAtlas atlas = new IconAtlas(texture, data.getGlyphMetrics(), data.getFontMetrics(), glyphs, kernings);
         if (owned != null) Feature.mc.getTextureManager().registerTexture(textureId(text), owned);
         transferred = true;
         return atlas;
      } catch (Exception failure) {
         dev.bkclient.BKClientInit.LOGGER.warn("Failed to load GUI font {}", text, failure);
         return null;
      } finally {
         if (!transferred) {
            if (owned != null) owned.close();
            else if (image != null) image.close();
         }
      }
   }
}
