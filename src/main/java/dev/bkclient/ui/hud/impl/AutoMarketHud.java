package dev.bkclient.ui.hud.impl;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.ui.hud.HudList;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;


/** BK AutoMarket content rendered entirely with the client's Liquid HUD components. */
public class AutoMarketHud extends HudList {
   private static final float WIDTH = 115.0F;
   private static final float HEADER_HEIGHT = 22.0F;
   private static final float ROW_HEIGHT = 18.0F;
   private static final float HISTORY_HEADER_HEIGHT = 15.0F;
   private static final float SIDE_PADDING = 7.0F;
   /** ColorRGBA is immutable, so one shared instance draws the same. */
   private static final ColorRGBA OLD_ROW_BACKGROUND = new ColorRGBA(21.0F, 21.0F, 21.0F, 128.0F);

   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");

   private final BooleanSetting bought = new BooleanSetting(this, "hud.market.bought").enable();
   private final BooleanSetting sold = new BooleanSetting(this, "hud.market.sold").enable();
   private final BooleanSetting listed = new BooleanSetting(this, "hud.market.listed").enable();
   private final BooleanSetting profit = new BooleanSetting(this, "hud.market.profit").enable();
   private final BooleanSetting wallet = new BooleanSetting(this, "hud.market.wallet").enable();
   private final dev.bkclient.systems.setting.settings.SliderSetting historyLimit = new dev.bkclient.systems.setting.settings.SliderSetting(this, "hud.market.history_limit").min(0).max(10).step(1).currentValue(3);
   private final BooleanSetting status = new BooleanSetting(this, "Status").enable();
   private final dev.bkclient.systems.setting.settings.ButtonSetting clearHistory =
      new dev.bkclient.systems.setting.settings.ButtonSetting(this, "hud.market.clear_history").action(() -> {
         var history = dev.bkclient.systems.modules.modules.bkmods.AutoMarketHistoryManager.getInstance();
         if (history == null) return;
         history.resetStats();
         nextSnapshot = 0;
         refreshSnapshot();
      });
   private List<Metric> liveMetrics = List.of(new Metric("Bought", "0"), new Metric("Sold", "0"),
      new Metric("Listed", "0"), new Metric("Profit", "$0"), new Metric("Wallet", "---"), new Metric("Status", "IDLE"));
   private List<HistoryRow> liveHistory = List.of();
   private long nextSnapshot;

   private dev.bkclient.systems.modules.modules.bkmods.AutoMarket market() {
      var client = dev.bkclient.BKClientInit.getInstance();
      return client == null || client.getModuleManager() == null ? null
         : client.getModuleManager().getModule(dev.bkclient.systems.modules.modules.bkmods.AutoMarket.class);
   }

   private void refreshSnapshot() {
      long now = net.minecraft.util.Util.getMeasuringTimeMs();
      if (now < nextSnapshot) return;
      nextSnapshot = now + 250;
      var history = dev.bkclient.systems.modules.modules.bkmods.AutoMarketHistoryManager.getInstance();
      var module = market();
      if (history == null) return;
      double balance = mc.world == null ? -1 : history.getWalletBalance();
      double result = history.getTotalProfit();
      String state = module == null || !module.isEnabled() ? "OFF"
         : net.minecraft.util.Formatting.strip(module.getAutoMarketController().getCurrentStatus());
      liveMetrics = List.of(new Metric("Bought", Integer.toString(history.getBoughtCount())),
         new Metric("Sold", Integer.toString(history.getSoldCount())),
         new Metric("Listed", Integer.toString(history.getListedCount())),
         new Metric("Profit", (result > 0 ? "+" : "") + money(result)),
         new Metric("Wallet", balance < 0 ? "---" : money(balance)), new Metric("Status", state));
      liveHistory = history.getHistory().stream().map(entry ->
         new HistoryRow(entry.stack(), entry.name(), entry.count(), money(entry.price()))).toList();
   }

   private static String money(double value) {
      return "$" + dev.bkclient.systems.modules.modules.bkmods.AutoMarketHistoryManager.formatPrice(value);
   }

   /** Derived lists are rebuilt only when their inputs (snapshot lists, toggles, limit) change. */
   private List<Metric> metricsCache, metricsSource;
   private int metricsMask = -1;
   private List<HistoryRow> historyCache, historySource;
   private int historyCacheLimit = -1;

   private List<Metric> metrics() {
      BooleanSetting[] settings = {bought, sold, listed, profit, wallet, status};
      int mask = 0;
      for (int i = 0; i < settings.length; i++) if (settings[i].isEnabled()) mask |= 1 << i;
      if (metricsCache != null && metricsSource == liveMetrics && metricsMask == mask) return metricsCache;
      java.util.ArrayList<Metric> result = new java.util.ArrayList<>();
      for (int i = 0; i < settings.length; i++) if ((mask & 1 << i) != 0) result.add(liveMetrics.get(i));
      metricsSource = liveMetrics;
      metricsMask = mask;
      return metricsCache = result;
   }
   private List<HistoryRow> history() {
      int limit = (int)historyLimit.getCurrentValue();
      if (historyCache != null && historySource == liveHistory && historyCacheLimit == limit) return historyCache;
      historySource = liveHistory;
      historyCacheLimit = limit;
      return historyCache = liveHistory.stream().limit(limit).toList();
   }
   private record Metric(String label, String value) {}
   private record HistoryRow(ItemStack stack, String name, int count, String price) {}
   public AutoMarketHud() {
      super("hud.automarket", "icons/hud/buy.png");
   }

   @Override
   public void update(UIContext context) {
      refreshSnapshot();
      this.width = WIDTH;
      if (HudTheme.isOld()) {
         boolean topStats = bought.isEnabled() || sold.isEnabled() || listed.isEnabled();
         this.height = listTop(21) + (topStats ? listStride(23) : 0)
            + (profit.isEnabled() ? listStride(19) : 0) + (wallet.isEnabled() ? listStride(24) : 0) + (status.isEnabled() ? listStride(19) : 0)
            + (history().isEmpty() ? 2 : 13 + history().size() * listStride(17) + 4);
      } else this.height = listTop(HEADER_HEIGHT) + metrics().size() * listStride(ROW_HEIGHT)
         + (history().isEmpty() ? 2 : HISTORY_HEADER_HEIGHT + history().size() * listStride(ROW_HEIGHT));
      super.update(context);
   }

   @Override
   protected void renderContent(UIContext context) {
      if (HudTheme.isOld()) {
         this.renderOldContent(context);
         return;
      }

      // The frame, blur, radius, header and icon are the same Liquid component
      // used by Keybinds, Effects, Friends and Cooldowns.
      super.renderContent(context);

      float y = this.y + listTop(HEADER_HEIGHT);
      for (Metric metric : metrics()) {
         renderMetricRow(context, metric, y);
         y += listStride(ROW_HEIGHT);
      }

      if (history().isEmpty()) return;
      renderHistoryHeader(context, y);
      y += HISTORY_HEADER_HEIGHT;

      for (HistoryRow row : history()) {
         renderHistoryRow(context, row, y);
         y += listStride(ROW_HEIGHT);
      }
   }

   private void renderOldContent(UIContext context) {
      HudTheme.drawPanel(context, this.x, this.y, this.width, this.height, this.animation.getValue(), this.dragAnim.getValue(), 8.0F);
      Font headerFont = Fonts.SEMIBOLD.getFont(7.0F);
      drawListHeader(context);
      float innerX = this.x + 8.0F;
      float innerWidth = this.width - 16.0F;
      float y = this.y + listTop(21.0F);
      var topStats = metrics().stream().filter(metric -> liveMetrics.indexOf(metric) >= 0 && liveMetrics.indexOf(metric) < 3).toList();
      if (!topStats.isEmpty()) {
         float statWidth = (innerWidth - 4 * (topStats.size() - 1)) / topStats.size();
         for (int i = 0; i < topStats.size(); i++) oldStatBox(context, innerX + i * (statWidth + 4), y, statWidth, topStats.get(i));
         y += listStride(23);
      }
      if (profit.isEnabled()) { oldInfoRow(context, innerX, y, innerWidth, 'B', liveMetrics.get(3)); y += listStride(19); }
      if (wallet.isEnabled()) {
         context.drawRoundedRect(innerX + 4, y, innerWidth - 8, .5F, BorderRadius.all(.25F), HudTheme.separatorColor());
         y += 6;
         oldInfoRow(context, innerX, y, innerWidth, 'C', liveMetrics.get(4)); y += listStride(18);
      }
      if (status.isEnabled()) { oldInfoRow(context, innerX, y, innerWidth, 'A', liveMetrics.get(5)); y += listStride(19); }
      if (history().isEmpty()) return;
      context.drawText(headerFont, "History", innerX + 2.0F,
         y + GuiUtility.getMiddleOfBox(headerFont.height(), 13.0F), HudTheme.textColor());
      context.drawRightText(headerFont, String.valueOf(history().size()), innerX + innerWidth - 1.0F,
         y + GuiUtility.getMiddleOfBox(headerFont.height(), 13.0F), Colors.getAccent());
      y += 13.0F;
      for (HistoryRow row : history()) {
         oldHistoryRow(context, innerX, y, innerWidth, row);
         y += listStride(17.0F);
      }
   }

   private void oldStatBox(UIContext context, float x, float y, float width, Metric metric) {
      ColorRGBA rowBackground = OLD_ROW_BACKGROUND;
      context.drawRoundedRect(x, y, width, 18.0F, BorderRadius.all(4.0F), rowBackground);
      Font valueFont = Fonts.SEMIBOLD.getFont(6.0F);
      Font labelFont = Fonts.MEDIUM.getFont(5.5F);
      context.drawText(valueFont, metric.value(), x + 4.0F, y + 3.5F, Colors.getAccent());
      context.drawText(labelFont, metric.label(), x + 4.0F, y + 11.0F, HudTheme.textColor().withAlpha(140.0F));
      context.drawRoundedRect(x + width - 5.5F, y + 3.0F, 2.5F, 2.5F, BorderRadius.all(1.25F), Colors.getAccent());
   }

   private void oldInfoRow(UIContext context, float x, float y, float width, char icon, Metric metric) {
      context.drawRoundedRect(x, y, width, 13.0F, BorderRadius.all(4.0F), OLD_ROW_BACKGROUND);
      Font font = Fonts.MEDIUM.getFont(6.0F);
      context.drawIcon(Fonts.ICONS, icon, x + 4.0F, y + 3.0F, 7.0F, Colors.getAccent());
      float textY = y + GuiUtility.getMiddleOfBox(font.height(), 13.0F);
      context.drawText(font, metric.label(), x + 14.0F, textY, HudTheme.textColor());
      context.drawRightText(font, metric.value(), x + width - 4.0F, textY, Colors.getAccent());
   }

   private void oldHistoryRow(UIContext context, float x, float y, float width, HistoryRow row) {
      context.drawRoundedRect(x, y, width, 13.0F, BorderRadius.all(4.0F), OLD_ROW_BACKGROUND);
      context.drawItem(row.stack(), x + 4.0F, y + 3.0F, 7.0F / 16.0F);
      Font font = Fonts.MEDIUM.getFont(6.0F);
      float textY = y + GuiUtility.getMiddleOfBox(font.height(), 13.0F);
      float priceWidth = font.width(row.price());
      String name = trimToWidth(font, row.name(), width - priceWidth - 31.0F);
      context.drawText(font, name, x + 14.0F, textY, HudTheme.textColor());
      context.drawText(font, " x" + row.count(), x + 14.0F + font.width(name), textY, Colors.getAccent());
      context.drawRightText(font, row.price(), x + width - 4.0F, textY, Colors.getAccent());
   }

   private void renderMetricRow(UIContext context, Metric metric, float y) {
      Font labelFont = Fonts.REGULAR.getFont(7.0F);
      Font valueFont = Fonts.MEDIUM.getFont(6.5F);
      drawRowSeparator(context, y);

      context.drawText(labelFont, metric.label(), this.x + SIDE_PADDING,
         y + GuiUtility.getMiddleOfBox(labelFont.height(), ROW_HEIGHT), HudTheme.textColor());
      drawValueBadge(context, valueFont, metric.value(), y);
   }

   private void renderHistoryHeader(UIContext context, float y) {
      Font font = Fonts.MEDIUM.getFont(6.5F);
      context.drawRect(this.x, y, this.width, 0.5F, HudTheme.textColor().withAlpha(12.0F));
      float textY = y + GuiUtility.getMiddleOfBox(font.height(), HISTORY_HEADER_HEIGHT);
      context.drawText(font, "History", this.x + SIDE_PADDING, textY, HudTheme.textColor());
      context.drawRightText(font, String.valueOf(history().size()), this.x + this.width - SIDE_PADDING,
         textY, Colors.getAccent());
   }

   private void renderHistoryRow(UIContext context, HistoryRow row, float y) {
      Font nameFont = Fonts.REGULAR.getFont(7.0F);
      Font valueFont = Fonts.MEDIUM.getFont(6.5F);
      drawRowSeparator(context, y);

      context.drawItem(row.stack(), this.x + 6.0F, y + 1.0F, 0.72F);
      float nameX = this.x + 22.0F;
      String count = " x" + row.count();
      float badgeWidth = valueFont.width(row.price()) + 7.0F;
      float available = this.x + this.width - badgeWidth - 8.0F - nameX - nameFont.width(count);
      String name = trimToWidth(nameFont, row.name(), available);
      float textY = y + GuiUtility.getMiddleOfBox(nameFont.height(), ROW_HEIGHT);
      context.drawText(nameFont, name, nameX, textY, HudTheme.textColor());
      context.drawText(nameFont, count, nameX + nameFont.width(name), textY, Colors.getAccent());
      drawValueBadge(context, valueFont, row.price(), y);
   }

   private void drawRowSeparator(UIContext context, float y) {
      context.drawRect(this.x, y, this.width, 0.5F, HudTheme.textColor().withAlpha(5.1F));
   }

   private void drawValueBadge(UIContext context, Font font, String value, float y) {
      float badgeWidth = font.width(value) + 7.0F;
      float badgeX = this.x + this.width - badgeWidth - 6.0F;
      context.drawRoundedRect(badgeX, y + 4.0F, badgeWidth, 10.0F, BorderRadius.all(4.0F),
         Colors.getAccent().withAlpha(190.0F));
      context.drawCenteredText(font, value, badgeX + badgeWidth / 2.0F,
         y + GuiUtility.getMiddleOfBox(font.height(), ROW_HEIGHT), HudTheme.strongTextColor());
   }

   private String trimToWidth(Font font, String value, float maxWidth) {
      if (font.width(value) <= maxWidth) return value;
      String trimmed = value;
      while (!trimmed.isEmpty() && font.width(trimmed + "...") > maxWidth) {
         trimmed = trimmed.substring(0, trimmed.length() - 1);
      }
      return trimmed + "...";
   }

   @Override
   public boolean show() {
      var module = market();
      return mc.currentScreen instanceof ChatScreen || this.alwaysDisplay.isEnabled() || module != null && module.isEnabled();
   }
}
