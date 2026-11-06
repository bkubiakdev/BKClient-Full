package dev.bkclient.systems.staff;

import java.util.List;

public final class StaffRankMatcherTest {
   public static void main(String[] args) {
      for (String prefix : List.of("§c[ADMIN] ", "[Helper]", "[VIP] [Moderator]", "[Head-Admin]",
         "[Właściciel]", "[Pomocnik]", "[Administracja]", "[Support]", "[Модератор]", "[Хелпер]",
         "[ＡＤＭＩＮ]", "[ᴀᴅᴍɪɴ]", "[ʜᴇʟᴘᴇʀ]", "[ꜱᴜᴘᴘᴏʀᴛ]", "[\ua509]", "[\ua521]")) {
         check(StaffRankMatcher.match(prefix) != null, "Detect rank: " + prefix);
      }
      for (String prefix : List.of("", "[VIP]", "[SVIP]", "[MVP+]", "[YouTuber]", "[Media]", "AdminFan",
         "NotModerator", "helper_123", "modpack", "Shopping", "STAFF123", "§a[Gracz]", "[Warrior]")) {
         check(StaffRankMatcher.match(prefix) == null, "Reject non-staff: " + prefix);
      }
      check(StaffRankMatcher.match(null) == null, "Null prefix");
      check("MOD+".equals(StaffRankMatcher.match("[Mod+]")), "Preserve rank suffix");
      check("HELPER".equals(StaffRankMatcher.match("[\ua509]")), "BKClient rank glyph");
      check("HELPER".equals(StaffRankMatcher.matchDisplay("§bHELPER §flocalfurian", "localfurian")), "Helper TAB badge");
      check("HELPER".equals(StaffRankMatcher.matchDisplay("LOCALFURIAN [HELPER]", "localfurian")), "Case and suffix rank");
      check(StaffRankMatcher.matchDisplay("AdminFan", "AdminFan") == null, "Player name is not a rank");
      for (String rank : List.of("CEO", "ᴄᴇᴏ", "Head Admin", "Junior Helper", "Trial Helper", "Senior Moderator", "Co-Owner", "Founder", "Developer", "Technik", "Zarząd", "Admin+", "Helper+")) {
         check(StaffRankMatcher.matchDisplay("["+rank+"] localfurian", "localfurian") != null, "Prefix "+rank);
         check(StaffRankMatcher.matchDisplay("LOCALFURIAN ["+rank+"]", "localfurian") != null, "Suffix "+rank);
      }
      for (String name : List.of("CEOPlayer", "Developer123", "helper_123"))
         check(StaffRankMatcher.matchDisplay(name, name) == null, "No rank from nick "+name);
      check("S-MOD".equals(StaffRankMatcher.matchDisplay("\u7fbd localfurian", "localfurian")), "Server glyph S-MOD");
      check("DEV".equals(StaffRankMatcher.matchDisplay("\u7446 localfurian", "localfurian")), "Server glyph DEV");
      check("HELPER".equals(StaffRankMatcher.matchDisplay("\u7448 localfurian", "localfurian")), "Server glyph HELPER");
      check("T-MOD".equals(StaffRankMatcher.matchDisplay("\u305c localfurian", "localfurian")), "Server glyph T-MOD");
      check("ADMIN".equals(StaffRankMatcher.matchDisplay("\u052a localfurian", "localfurian")), "Server glyph ADMIN");
      check("GM".equals(StaffRankMatcher.matchDisplay("\ua007 localfurian", "localfurian")), "Server glyph GM");
      check("CEO".equals(StaffRankMatcher.matchDisplay("\u793c localfurian", "localfurian")), "Server glyph CEO");
      check("MOD".equals(StaffRankMatcher.matchDisplay("\u73c8 localfurian", "localfurian")), "Server glyph MOD");
      check("EKIPA".equals(StaffRankMatcher.matchDisplay("\u4f5c localfurian", "localfurian")), "Server glyph EKIPA");
      check("T-HELPER".equals(StaffRankMatcher.matchDisplay("\u9f96 localfurian", "localfurian")), "Server glyph T-HELPER");
      check("localfurrier".equals(StaffRankMatcher.tabName("\u7448 localfurrier", "RAP6fa63beb")), "TAB alias instead of incognito profile");
      check("localfurrier".equals(StaffRankMatcher.tabName("§bHELPER §flocalfurrier 00", "RAP6fa63beb")), "Text rank and counter stripped");
      check("localfurrier".equals(StaffRankMatcher.tabName("[Head Admin] localfurrier", "RAP6fa63beb")), "Bracketed rank stripped");
      check("Admin".equals(StaffRankMatcher.tabName("HELPER Admin", "Admin")), "Real profile named Admin retained");
      check("fallback".equals(StaffRankMatcher.tabName(null, "fallback")), "Missing TAB fallback");
      System.out.println("Staff rank detection: Polish, English, Cyrillic, small caps and false positives passed");
   }
   private static void check(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
}
