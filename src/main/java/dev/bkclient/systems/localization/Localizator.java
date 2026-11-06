package dev.bkclient.systems.localization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import lombok.Generated;
import dev.bkclient.BKClientInit;

public final class Localizator {
   private static final Language DEFAULT_LANG = Language.EN_US;
   private static final Map<String, String> translations = new HashMap<>();
   private static final Map<String, String> displayAliases = new HashMap<>();
   private static Language currentLanguage = DEFAULT_LANG;

   public static void loadTranslations() {
      Map<String, String> english = readTranslations(DEFAULT_LANG);
      translations.clear();
      translations.putAll(english);
      if (currentLanguage != DEFAULT_LANG) {
         translations.putAll(readTranslations(currentLanguage));
      }
      displayAliases.clear();
      // Some older controls store English labels instead of resource keys. Resolve
      // those at display time without changing the persisted setting identifiers.
      english.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
         String translated = translations.get(entry.getKey());
         if (translated != null && !translated.equals(entry.getValue())) {
            displayAliases.putIfAbsent(entry.getValue(), translated);
         }
      });
   }

   private static Map<String, String> readTranslations(Language language) {
      String langFile = "/assets/" + BKClientInit.MOD_ID + "/lang/" + language.getCode() + ".lang";
      InputStream stream = Localizator.class.getResourceAsStream(langFile);
      if (stream == null) {
         throw new IllegalStateException("Language file not found: " + langFile);
      }

      Map<String, String> result = new HashMap<>();
      try (stream; BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
         String line;
         int lineNumber = 0;
         while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = removeComments(line).trim();
            if (line.isEmpty()) continue;
            int equalIndex = line.indexOf('=');
            if (equalIndex < 0) {
               BKClientInit.LOGGER.warn("Invalid language line {} in {}: {}", lineNumber, langFile, line);
               continue;
            }
            String key = line.substring(0, equalIndex).trim();
            if (key.isEmpty()) {
               BKClientInit.LOGGER.warn("Empty language key at line {} in {}", lineNumber, langFile);
               continue;
            }
            result.put(key, line.substring(equalIndex + 1).trim());
         }
      } catch (IOException exception) {
         throw new IllegalStateException("Failed to load language: " + language.getCode(), exception);
      }
      return result;
   }

   public static void setLanguage(@Nonnull Language language) {
      currentLanguage = language == Language.RU_RU ? DEFAULT_LANG : language;
      loadTranslations();
   }

   public static String translate(String key) {
      return key == null ? "" : translations.getOrDefault(key, displayAliases.getOrDefault(key, key));
   }

   public static String translate(String key, Object... args) {
      return String.format(translate(key), args);
   }

   public static String translateOrEmpty(String key) {
      return translations.getOrDefault(key, " ");
   }

   private static String removeComments(String line) {
      int commentIndex = line.indexOf('#');
      return commentIndex < 0 ? line : line.substring(0, commentIndex);
   }

   @Generated
   private Localizator() {
      throw new UnsupportedOperationException("Utility class");
   }

   @Generated
   public static Language getCurrentLanguage() {
      return currentLanguage;
   }
}
