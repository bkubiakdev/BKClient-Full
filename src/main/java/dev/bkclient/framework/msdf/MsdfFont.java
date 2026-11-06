package dev.bkclient.framework.msdf;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import java.util.concurrent.ConcurrentHashMap;

import dev.bkclient.systems.modules.modules.other.NameProtect;
import dev.bkclient.systems.modules.modules.other.StreamerMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class MsdfFont {
   private static final int MAX_WIDTH_CACHE_ENTRIES = 4096;
   private static final int MAX_FONT_CACHE_ENTRIES = 128;
   private final String name;
   private final Identifier atlasIdentifier;
   private final FontData.AtlasData atlas;
   private final FontData.MetricsData metrics;
   private final Int2ObjectOpenHashMap<MsdfGlyph> glyphs;
   private final Int2ObjectOpenHashMap<Int2FloatOpenHashMap> kernings;
   /** Guarded by itself; primitive keys/values avoid Long/Float boxing per lookup. NaN = absent. */
   private final Long2FloatOpenHashMap widthCache = new Long2FloatOpenHashMap();
   private final ConcurrentHashMap<Integer, Font> fontCache = new ConcurrentHashMap<>();

   private MsdfFont(
      String name,
      Identifier atlasIdentifier,
      FontData.AtlasData atlas,
      FontData.MetricsData metrics,
      Int2ObjectOpenHashMap<MsdfGlyph> glyphs,
      Int2ObjectOpenHashMap<Int2FloatOpenHashMap> kernings
   ) {
      this.name = name;
      this.atlasIdentifier = atlasIdentifier;
      this.atlas = atlas;
      this.metrics = metrics;
      this.glyphs = glyphs;
      this.kernings = kernings;
      this.widthCache.defaultReturnValue(Float.NaN);
   }

   public int getTextureId() {
      // Resource reloads may replace the texture instance.
      return MinecraftClient.getInstance().getTextureManager().getTexture(this.atlasIdentifier).getGlId();
   }

   public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color) {
      int prevChar = -1;
      boolean skipNext = false;

      for (int i = 0; i < text.length(); i++) {
         char rawC = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (rawC == 167) {
            skipNext = true;
         } else {
            char c = normalizeCharacter(rawC);
            MsdfGlyph glyph = this.glyphs.get(c);
            if (glyph == null) {
               char fb = fallbackLatin(c);
               glyph = this.glyphs.get(fb);
            }
            if (glyph != null) {
               Int2FloatOpenHashMap kerning = this.kernings.get(prevChar);
               if (kerning != null) {
                  x += kerning.get(c) * size;
               }

               x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
               prevChar = c;
            }
         }
      }
   }

   public static char normalizeCharacter(char c) {
      return switch (c) {
         case '\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2015', '\u2212' -> '-';
         case '\u2018', '\u2019', '\u201A', '\u201B', '\u00B4', '`' -> '\'';
         case '\u201C', '\u201D', '\u201E', '\u201F', '\u00AB', '\u00BB' -> '"';
         case '\u2026' -> '.';
         case '\u00A0', '\u2002', '\u2003', '\u2009' -> ' ';
         case 'і' -> 'i';
         case 'І' -> 'I';
         default -> c;
      };
   }

   public static char fallbackLatin(char c) {
      return switch (c) {
         case 'ą' -> 'a'; case 'Ą' -> 'A';
         case 'ć' -> 'c'; case 'Ć' -> 'C';
         case 'ę' -> 'e'; case 'Ę' -> 'E';
         case 'ł' -> 'l'; case 'Ł' -> 'L';
         case 'ń' -> 'n'; case 'Ń' -> 'N';
         case 'ó' -> 'o'; case 'Ó' -> 'O';
         case 'ś' -> 's'; case 'Ś' -> 'S';
         case 'ź', 'ż' -> 'z'; case 'Ź', 'Ż' -> 'Z';
         default -> c;
      };
   }

   /** Same result as {@code text.replace("і", "i").replace("І", "I")}, without scanning twice when absent. */
   private static String replaceCyrillicI(String text) {
      for (int i = 0; i < text.length(); i++) {
         char ch = text.charAt(i);
         if (ch == 'і' || ch == 'І') {
            return text.replace("і", "i").replace("І", "I");
         }
      }
      return text;
   }

   public float getWidthOld(String text, float size) {
      text = replaceCyrillicI(text);
      int prevChar = -1;
      float width = 0.0F;
      boolean skipNext = false;
      NameProtect nameProtectModule = BKClientInit.getInstance().getModuleManager().getModule(NameProtect.class);
      if (nameProtectModule.isEnabled()) {
         text = nameProtectModule.patchName(text);
      }
      StreamerMode streamerMode = BKClientInit.getInstance().getModuleManager().getModule(StreamerMode.class);
      if (streamerMode != null && streamerMode.isEnabled()) {
         text = streamerMode.filterText(text);
      }

      for (int i = 0; i < text.length(); i++) {
         char rawC = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (rawC == 167) {
            skipNext = true;
         } else {
            char c = normalizeCharacter(rawC);
            MsdfGlyph glyph = this.glyphs.get(c);
            if (glyph == null) {
               glyph = this.glyphs.get(fallbackLatin(c));
            }
            if (glyph != null) {
               Int2FloatOpenHashMap kerning = this.kernings.get(prevChar);
               if (kerning != null) {
                  width += kerning.get(c) * size;
               }

               width += glyph.getWidth(size) + 0.25F;
               prevChar = c;
            }
         }
      }

      return width;
   }

   private static long widthKey(String s, float size, boolean np) {
      int h = s.hashCode();
      return h & 4294967295L ^ (long)Float.floatToIntBits(size) << 32 ^ (np ? -7046029254386353131L : 0L);
   }

   public float getWidth(String text, float size) {
      text = replaceCyrillicI(text);
      NameProtect nameProtectModule = BKClientInit.getInstance().getModuleManager().getModule(NameProtect.class);
      boolean np = nameProtectModule.isEnabled();
      if (np) {
         text = nameProtectModule.patchName(text);
      }

      long key = widthKey(text, size, np);
      float cached;
      synchronized (this.widthCache) {
         cached = this.widthCache.get(key);
      }
      if (!Float.isNaN(cached)) {
         return cached;
      }

      float w = this.getWidthOld(text, size);
      synchronized (this.widthCache) {
         if (this.widthCache.size() >= MAX_WIDTH_CACHE_ENTRIES) {
            this.widthCache.clear();
         }
         this.widthCache.put(key, w);
      }
      return w;
   }

   public void clearWidthCache() {
      synchronized (this.widthCache) {
         this.widthCache.clear();
      }
   }

   public float getTextWidth(Text text, float size) {
      return this.getWidth(text.getString(), size);
   }

   public Font getFont(float size) {
      int key = Float.floatToIntBits(size);
      if (this.fontCache.size() >= MAX_FONT_CACHE_ENTRIES && !this.fontCache.containsKey(key)) {
         this.fontCache.clear();
      }
      return this.fontCache.computeIfAbsent(key, ignored -> new Font(this, size));
   }

   public MsdfGlyph getGlyph(int unicode) {
      return this.glyphs.get(unicode);
   }

   public String getName() {
      return this.name;
   }

   public FontData.AtlasData getAtlas() {
      return this.atlas;
   }

   public FontData.MetricsData getMetrics() {
      return this.metrics;
   }

   public static MsdfFont.Builder builder() {
      return new MsdfFont.Builder();
   }

   public static class Builder {
      private String name = "?";
      private Identifier dataIdentifer;
      private Identifier atlasIdentifier;

      private Builder() {
      }

      public MsdfFont.Builder name(String name) {
         this.name = name;
         return this;
      }

      public MsdfFont.Builder data(String dataFileName) {
         this.dataIdentifer = Identifier.of(BKClientInit.MOD_ID, "fonts/msdf/" + dataFileName + ".json");
         return this;
      }

      public MsdfFont.Builder atlas(String atlasFileName) {
         this.atlasIdentifier = Identifier.of(BKClientInit.MOD_ID, "fonts/msdf/" + atlasFileName + ".png");
         return this;
      }

      public MsdfFont build() {
         FontData data = ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
         AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(this.atlasIdentifier);
         if (data == null) {
            throw new RuntimeException(
               "Failed to read font data file: "
                  + this.dataIdentifer.toString()
                  + "; Are you sure this is json file? Try to check the correctness of its syntax."
            );
         }

         RenderSystem.recordRenderCall(() -> texture.setFilter(true, false));
         float aWidth = data.atlas().width();
         float aHeight = data.atlas().height();
         Int2ObjectOpenHashMap<MsdfGlyph> glyphs = new Int2ObjectOpenHashMap<>();
         for (FontData.GlyphData glyphData : data.glyphs()) {
            // Collectors.toMap semantics: duplicate unicode is an error.
            if (glyphs.putIfAbsent(glyphData.unicode(), new MsdfGlyph(glyphData, aWidth, aHeight)) != null) {
               throw new IllegalStateException("Duplicate key " + glyphData.unicode());
            }
         }
         Int2ObjectOpenHashMap<Int2FloatOpenHashMap> kernings = new Int2ObjectOpenHashMap<>();
         data.kernings().forEach(kerning -> {
            Int2FloatOpenHashMap map = kernings.get(kerning.leftChar());
            if (map == null) {
               map = new Int2FloatOpenHashMap();
               map.defaultReturnValue(0.0F);
               kernings.put(kerning.leftChar(), map);
            }

            map.put(kerning.rightChar(), kerning.advance());
         });
         return new MsdfFont(this.name, this.atlasIdentifier, data.atlas(), data.metrics(), glyphs, kernings);
      }
   }
}
