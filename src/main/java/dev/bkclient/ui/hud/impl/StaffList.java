package dev.bkclient.ui.hud.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.other.NameProtect;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.staff.StaffRankMatcher;
import dev.bkclient.ui.hud.HudList;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.ui.hud.components.HudValueRow;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.PlayerHeadSkins;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.world.GameMode;

public final class StaffList extends HudList {
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");
   private static final ColorRGBA ADMIN_RED = new ColorRGBA(245, 80, 90);
   private final Map<String, Row> staff = new LinkedHashMap<>();
   private long refreshed;
   private Object world;
   private List<Row> visibleRows = List.of();
   private int overflow;
   /** The sorted rows only change when refresh() ran, rows were dropped or the filter changed. */
   private boolean rowsDirty = true;
   private boolean lastOnlineOnly;
   private long presentCount;
   private final BooleanSetting showCount = new BooleanSetting(this, "hud.list.count");
   private final BooleanSetting showStatus = new BooleanSetting(this, "hud.list.status").enable();
   private final BooleanSetting onlineOnly = new BooleanSetting(this, "hud.list.online_only");
   @Override protected String headerTitle() {
      long count = presentCount;
      return super.headerTitle() + (showCount.isEnabled() ? " (" + count + ")" : "");
   }
   public StaffList() { super("hud.staff_list", "icons/hud/staff.png"); }

   private static ColorRGBA rankColor(Row row) {
      return row.rank.contains("ADMIN") || row.rank.contains("АДМИН") ? ADMIN_RED : Colors.getAccent();
   }

   private String statusLabel(Row row) {
      return !showStatus.isEnabled() ? "" : Localizator.translate(row.spectator ? "hud.staff.spectator_badge" : "hud.staff.online");
   }

   private void refresh() {
      if (world != mc.world) { staff.clear(); refreshed = 0; world = mc.world; rowsDirty = true; }
      if (mc.world == null || mc.getNetworkHandler() == null) {
         if (!staff.isEmpty()) { staff.clear(); rowsDirty = true; }
         return;
      }
      long now = System.currentTimeMillis();
      if (now - refreshed < 500) return;
      refreshed = now;
      rowsDirty = true;
      for (Row row : staff.values()) row.present = false;
      for (var entry : mc.getNetworkHandler().getPlayerList()) {
         String name = entry.getProfile().getName();
         var team = mc.world.getScoreboard().getScoreHolderTeam(name);
         String rank = StaffRankMatcher.match(team == null ? null : team.getPrefix().getString());
         if (rank == null && team != null) rank = StaffRankMatcher.match(team.getSuffix().getString());
         if (rank == null && entry.getDisplayName() != null)
            rank = StaffRankMatcher.matchDisplay(entry.getDisplayName().getString(), name);
         if (rank == null) continue;
         Row row = staff.computeIfAbsent(name, Row::new);
         row.rank = rank;
         row.tabName = StaffRankMatcher.tabName(entry.getDisplayName() == null ? null : entry.getDisplayName().getString(), name);
         row.spectator = entry.getGameMode() == GameMode.SPECTATOR;
         row.present = true;
      }
   }

   @Override public void update(UIContext context) {
      refresh();
      for (Row row : staff.values()) row.animation.update(row.present);
      if (staff.values().removeIf(row -> !row.present && row.animation.getValue() <= 0.001F)) rowsDirty = true;
      boolean filterOnline = onlineOnly.isEnabled();
      if (filterOnline != lastOnlineOnly) { lastOnlineOnly = filterOnline; rowsDirty = true; }
      if (rowsDirty) {
         rowsDirty = false;
         List<Row> sorted = staff.values().stream()
            .filter(row -> !filterOnline || row.present)
            .sorted(Comparator.comparing(row -> row.tabName, String.CASE_INSENSITIVE_ORDER)).toList();
         overflow = Math.max(0, sorted.size() - 12);
         visibleRows = sorted.stream().limit(12).toList();
         presentCount = staff.values().stream().filter(row -> row.present).count();
      }
      width = Math.max(HudTheme.isOld() ? 70 : 104,
         Fonts.MEDIUM.getFont(7).width(Localizator.translate(name)) + 28);
      height = listTop(HudValueRow.contentTop());
      for (Row row : visibleRows) {
         row.displayName = displayName(row.tabName);
         width = Math.max(width, HudValueRow.width(row.displayName, statusLabel(row), row.rank));
         height += listStride(HudValueRow.height()) * Math.clamp(row.animation.getValue(), 0, 1);
      }
      if (visibleRows.isEmpty() || overflow > 0) height += listStride(HudValueRow.height());
      if (HudTheme.isOld()) height += 6;
      super.update(context);
   }

   private String displayName(String name) {
      NameProtect protect = BKClientInit.getInstance().getModuleManager().getModule(NameProtect.class);
      return protect.isEnabled() ? protect.patchName(name) : name;
   }

   @Override protected void renderContent(UIContext context) {
      super.renderContent(context);
      var font = HudValueRow.labelFont();
      float rowY = y + listTop(HudValueRow.contentTop());
      float rowH = listStride(HudValueRow.height());
      for (Row row : visibleRows) {
         float alpha = Math.clamp(row.animation.getValue(), 0, 1);
         ScissorUtility.push(context.getMatrices(), x, rowY, width, rowH * alpha);
         try {
            HudValueRow.draw(context, x, rowY, width, row.displayName, statusLabel(row),
               HudTheme.textColor(), alpha, (headX, headY, size) -> {
                  float headSize = size * 0.75F;
                  float inset = (size - headSize) / 2;
                  DrawUtility.drawPlayerHeadWithRoundedShader(context.getMatrices(), PlayerHeadSkins.get(row.tabName),
                     headX + inset, headY + inset, headSize, BorderRadius.all(1.5F), ColorRGBA.WHITE.mulAlpha(alpha));
               }, row.rank, rankColor(row));
         } finally {
            ScissorUtility.pop();
         }
         if (context.getMouseX() >= x && context.getMouseX() <= x + width
            && context.getMouseY() >= rowY && context.getMouseY() <= rowY + rowH * alpha) {
            BKClientInit.getInstance().getHud().setDesc(Localizator.translate(row.spectator ? "hud.staff.spectator" : "hud.staff.online"));
         }
         rowY += rowH * alpha;
      }
      if (visibleRows.isEmpty() || overflow > 0) {
         String label = visibleRows.isEmpty() ? Localizator.translate("hud.staff.empty") : "+" + overflow;
         context.drawText(font, label, x + 7, rowY + 3, HudTheme.textColor().mulAlpha(0.55F));
      }
   }
   @Override public boolean show() {
      return !staff.isEmpty() || mc.currentScreen instanceof ChatScreen || alwaysDisplay.isEnabled();
   }
   private static final class Row {
      final String name;
      final Animation animation = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
      String rank = "", displayName, tabName;
      boolean spectator, present;
      Row(String name) { this.name = name; this.displayName = name; this.tabName = name; }
   }
}
