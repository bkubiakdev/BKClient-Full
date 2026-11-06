package dev.bkclient.systems.staff;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Matches rank labels, never substrings of player names (e.g. AdminFan). */
public final class StaffRankMatcher {
   private static final Pattern COLORS = Pattern.compile("(?i)§[0-9a-fk-orx]");
   private static final Pattern RANK = Pattern.compile(
      "(?<![\\p{L}\\p{N}_])(?:(?:head|junior|senior|trial|jr|sr|co|t|s)[ ._-]*)?"
      + "(?:administrator|administracja|admin|owner|właściciel|wlasciciel|właściciela|wlasciciela|"
      + "moderator|moder|mod|helper|pomocnik|support|staff|opiekun|op|ceo|founder|założyciel|zalozyciel|"
      + "gm|ekipa|manager|menadżer|menadzer|developer|dev|technik|zarząd|zarzad|"
      + "администратор|админ|модератор|модер|хелпер|поддержка|куратор|стаж[её]р)"
      + "\\+?(?![\\p{L}\\p{N}_])");
   private StaffRankMatcher() { }
   /** Display alias is separate from the profile key used to track the entry. */
   public static String tabName(String display, String profileName) {
      if (display == null || display.isBlank()) return profileName;
      String plain = COLORS.matcher(display).replaceAll("");
      Pattern names = Pattern.compile("(?<![A-Za-z0-9_])[A-Za-z0-9_]{1,16}(?![A-Za-z0-9_])");
      var original = names.matcher(plain);
      while (original.find()) if (original.group().equalsIgnoreCase(profileName)) return original.group();
      plain = plain.replaceAll("\\[[^\\]]*\\]", " ");
      plain = Pattern.compile(RANK.pattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(plain).replaceAll(" ");
      var aliases = names.matcher(plain);
      while (aliases.find()) {
         String alias = aliases.group();
         if (!alias.matches("[0-9]+")) return alias;
      }
      return profileName;
   }
   public static String matchDisplay(String display, String playerName) {
      if (display == null) return null;
      String text = COLORS.matcher(display).replaceAll("");
      if (playerName != null && !playerName.isBlank())
         text = Pattern.compile(Pattern.quote(playerName), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text).replaceAll(" ");
      return match(text);
   }
   public static String match(String prefix) {
      if (prefix == null || prefix.isBlank()) return null;
      String plain = COLORS.matcher(Normalizer.normalize(prefix, Normalizer.Form.NFKC)).replaceAll("")
         .replace("\ua509", "helper").replace("\ua513", "moder").replace("\ua517", "moder")
         .replace("\ua521", "moder+").replace("\ua525", "moder")
         // Verified bitmap rank glyphs from the server resource pack (2026-09-29).
         .replace("\u7fbd", " s-mod ")
         .replace("\u7446", " dev ")
         .replace("\u7448", " helper ")
         .replace("\u305c", " t-mod ")
         .replace("\u052a", " admin ")
         .replace("\ua007", " gm ")
         .replace("\u793c", " ceo ")
         .replace("\u73c8", " mod ")
         .replace("\u4f5c", " ekipa ")
         .replace("\u9f96", " t-helper ")
         .replaceAll("[\\p{Cf}]", "").toLowerCase(Locale.ROOT);
      String smallCaps = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡʏᴢ";
      String latin = "abcdefghijklmnopqrstuvwyz";
      for (int i = 0; i < smallCaps.length(); i++) plain = plain.replace(smallCaps.charAt(i), latin.charAt(i));
      var found = RANK.matcher(plain);
      return found.find() ? found.group().trim().toUpperCase(Locale.ROOT) : null;
   }
}
