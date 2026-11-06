package dev.bkclient.ui.hud.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.ui.hud.HudList;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.ui.hud.components.CooldownSnapshot;
import dev.bkclient.ui.hud.components.HudValueRow;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.ScissorUtility;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

/** Cooldown layout assembled from BKClient's HUD material, badges and rings. */
public final class Cooldowns extends HudList {
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");
   private final BooleanSetting preview = new BooleanSetting(this, "hud.cooldowns2.preview");
   private final BooleanSetting header = new BooleanSetting(this, "hud.editor.header").enable();
   private final BooleanSetting headerIcon = new BooleanSetting(this, "hud.editor.header_icon", () -> !header.isEnabled()).enable();
   private final BooleanSetting fullHeader = new BooleanSetting(this, "hud.editor.full_header", () -> !header.isEnabled());
   private final BooleanSetting joined = new BooleanSetting(this, "hud.editor.joined", () -> !header.isEnabled()).enable();
   private final ModeSetting alignment = new ModeSetting(this, "hud.editor.alignment", () -> !header.isEnabled());
   private final ModeSetting.Value left = new ModeSetting.Value(alignment, "hud.editor.left");
   private final ModeSetting.Value center = new ModeSetting.Value(alignment, "hud.editor.center").select();
   private final ModeSetting.Value right = new ModeSetting.Value(alignment, "hud.editor.right");
   private final BooleanSetting itemIcons = new BooleanSetting(this, "hud.editor.item_icons").enable();
   private final BooleanSetting rings = new BooleanSetting(this, "hud.editor.rings").enable();
   private final BooleanSetting badges = new BooleanSetting(this, "hud.editor.badges").enable();
   private final SliderSetting opacity = new SliderSetting(this, "hud.editor.opacity").min(.1f).max(1).step(.05f).currentValue(1);
   private final SliderSetting spacing = new SliderSetting(this, "hud.editor.spacing").min(0).max(8).step(1).currentValue(0);
   private final ModeSetting order = new ModeSetting(this, "hud.editor.order");
   private final ModeSetting.Value byItem = new ModeSetting.Value(order, "hud.editor.order_item");
   private final ModeSetting.Value soonest = new ModeSetting.Value(order, "hud.editor.order_time");
   private final CooldownSnapshot snapshot = new CooldownSnapshot();
   private final Map<Identifier, Row> rows = new LinkedHashMap<>();
   private List<Row> visibleRows = List.of();
   private Object playerIdentity;
   private boolean previewActive;
   private boolean hasContent;
   private boolean enteringContent;
   private static final float HEADER_HEIGHT = 18, BODY_TOP = 22, BODY_PADDING = 3;

   private final Identifier headerIconId = BKClientInit.id("icons/hud/clock.png");

   public Cooldowns() { super("hud.cooldowns", "icons/hud/clock.png"); }

   private List<CooldownSnapshot.Entry> readRows() {
      if (playerIdentity != mc.player) {
         playerIdentity = mc.player; snapshot.clear(); rows.clear();
      }
      List<CooldownSnapshot.Entry> live = List.of();
      if (mc.player != null && CooldownSnapshot.hasEntries(mc.player.getItemCooldownManager())) {
         List<ItemStack> inventory = new ArrayList<>();
         for (int slot = 0; slot < mc.player.getInventory().size(); slot++) inventory.add(mc.player.getInventory().getStack(slot));
         inventory.add(mc.player.getOffHandStack());
         live = snapshot.read(mc.player.getItemCooldownManager(), inventory);
      }
      // Same as read() with no tracked group: the label cache is emptied.
      else if (mc.player != null) snapshot.clear();
      boolean sample = preview.isEnabled() || live.isEmpty() && mc.currentScreen instanceof ChatScreen;
      if (previewActive != sample) { previewActive = sample; }
      if (!sample) return live;
      return List.of(
         new CooldownSnapshot.Entry(Identifier.ofVanilla("ender_pearl"), Items.ENDER_PEARL, Items.ENDER_PEARL.getName().getString(), 100, 160),
         new CooldownSnapshot.Entry(Identifier.ofVanilla("chorus_fruit"), Items.CHORUS_FRUIT, Items.CHORUS_FRUIT.getName().getString(), 46, 100));
   }

   @Override public void update(UIContext context) {
      var entries = readRows();
      boolean previouslyHadContent = hasContent;
      hasContent = !entries.isEmpty();
      enteringContent = hasContent && !previouslyHadContent;
      // Fade the complete last layout out; do not collapse into a header first.
      if (!hasContent && !alwaysDisplay.isEnabled() && !(mc.currentScreen instanceof ChatScreen)
            && !visibleRows.isEmpty()) {
         super.update(context);
         return;
      }
      if (enteringContent) rows.clear();
      var active = new java.util.HashSet<Identifier>();
      for (var entry : entries) {
         active.add(entry.group());
         rows.computeIfAbsent(entry.group(), ignored -> new Row(entry)).entry = entry;
      }
      for (var row : rows.values()) {
         if (enteringContent) row.reveal.setValue(1);
         row.reveal.update(active.contains(row.entry.group()));
      }
      rows.entrySet().removeIf(entry -> !active.contains(entry.getKey()) && entry.getValue().reveal.getValue() <= .001f);
      visibleRows = new ArrayList<>(rows.values());
      if (soonest.isSelected()) visibleRows.sort(java.util.Comparator.comparingInt(row -> row.entry.remainingTicks()));
      else visibleRows.sort(java.util.Comparator.comparing(row -> row.entry.group().toString()));
      width = Math.max(108, headerWidth());
      height = header.isEnabled() ? HEADER_HEIGHT : 1;
      if (!visibleRows.isEmpty()) {
         height = bodyTop() + BODY_PADDING * 2;
         for (Row row : visibleRows) {
            width = Math.max(width, HudValueRow.timerWidth(row.entry.label(), row.entry.time()));
            height += rowHeight() * Math.clamp(row.reveal.getValue(), 0, 1);
         }
      }
      // The outer HUD visibility animation owns entry. Start with the final geometry,
      // rather than animating both row heights and panel height from the empty header.
      if (enteringContent) {
         snapLayoutSize(width, height);
      }
      super.update(context);
   }

   private String title() { return Localizator.translate("hud.cooldowns"); }
   private float headerWidth() { return Fonts.MEDIUM.getFont(7).width(title()) + (headerIcon.isEnabled() ? 25 : 12); }
   private float bodyTop() { return !header.isEnabled() ? 0 : joined.isEnabled() ? HEADER_HEIGHT - 2 : BODY_TOP; }
   private float rowHeight() { return HudValueRow.timerHeight() + spacing.getCurrentValue(); }
   @Override protected float getContextMenuWidth() { return 150; }
   @Override protected boolean compactSettings() { return true; }


   @Override protected void renderContent(UIContext context) {
      float alpha = this.animation.getValue() * opacity.getCurrentValue();
      float headerWidth = fullHeader.isEnabled() ? width : headerWidth();
      float headerX = x + (width - headerWidth) * (left.isSelected() ? 0 : right.isSelected() ? 1 : .5f);
      if (header.isEnabled()) {
      if (joined.isEnabled() && !visibleRows.isEmpty()) {
         if (fullHeader.isEnabled()) HudTheme.drawPanel(context, x, y, width, height, alpha, dragAnim.getValue(), 7);
         else HudTheme.drawJoinedPanel(context, x, y, width, height, headerX - x, headerWidth,
            HEADER_HEIGHT, bodyTop(), alpha, dragAnim.getValue(), dev.bkclient.ui.hud.HudVisualScope.strength(), dev.bkclient.ui.hud.HudVisualScope.progress());
      } else HudTheme.drawPanel(context, headerX, y, headerWidth, HEADER_HEIGHT, alpha, dragAnim.getValue(), 7);
      var font = Fonts.MEDIUM.getFont(7);
      float textX = headerX + 6 + (headerWidth - headerWidth()) * (left.isSelected() ? 0 : right.isSelected() ? 1 : .5f);
      if (headerIcon.isEnabled()) context.drawTexture(headerIconId, textX, y + 5, 8, 8, HudTheme.textColor().mulAlpha(.7f * alpha));
      context.drawText(font, title(), textX + (headerIcon.isEnabled() ? 12 : 0), y + GuiUtility.getMiddleOfBox(font.height(), HEADER_HEIGHT), HudTheme.textColor().mulAlpha(alpha));
      }
      if (visibleRows.isEmpty()) return;
      if (!header.isEnabled() || !joined.isEnabled()) HudTheme.drawPanel(context, x, y + bodyTop(), width, Math.max(1, height - bodyTop()), alpha, dragAnim.getValue(), 7);
      float rowY = y + bodyTop() + BODY_PADDING;
      for (Row row : visibleRows) {
         float reveal = Math.clamp(row.reveal.getValue(), 0, 1);
         float rowHeight = rowHeight() * reveal;
         ScissorUtility.push(context.getMatrices(), x + 2, rowY, width - 4, rowHeight);
         try {
            HudValueRow.drawTimer(context, x, rowY + spacing.getCurrentValue() / 2, width, row.entry.label(), row.entry.time(), row.entry.fraction(), alpha * reveal,
               (iconX, iconY, size) -> context.drawItem(row.entry.item(), iconX, iconY, size / 16), itemIcons.isEnabled(), rings.isEnabled(), badges.isEnabled());
         } finally { ScissorUtility.pop(); }
         rowY += rowHeight;
      }
   }

   @Override public boolean show() {
      return preview.isEnabled() || hasContent || mc.currentScreen instanceof ChatScreen || alwaysDisplay.isEnabled();
   }
   private static final class Row {
      private CooldownSnapshot.Entry entry;
      private final Animation reveal = new Animation(180L, Easing.FIGMA_EASE_IN_OUT);
      private Row(CooldownSnapshot.Entry entry) { this.entry = entry; }
   }
}
