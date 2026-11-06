package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.systems.cosmetics.CosmeticCatalog;
import dev.bkclient.systems.cosmetics.CosmeticEntry;
import dev.bkclient.systems.cosmetics.CosmeticManager;
import dev.bkclient.systems.cosmetics.CosmeticSlot;
import dev.bkclient.systems.cosmetics.render.CosmeticRenderer;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.ui.clicksys.ClickSysSounds;
import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.ui.clicksys.port.enums.SoundEvent;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.utility.colors.ColorRGBA;
import java.util.List;
import org.joml.Matrix4f;

/** Cosmetic wardrobe in ClickSys logical coordinates, including its input and clipping. */
public final class CosmeticsPage extends Widget {
   private static final float PADDING = 24, GAP = 12, CARD_HEIGHT = 218, HEADER_HEIGHT = 140;
   private static final float CHIP_WIDTH = 142, CHIP_HEIGHT = 34;
   private CosmeticSlot slot = CosmeticSlot.BACKPACK;
   private final Tween scroll = EasingPresets.getTweenByFloatFloat(0, .18F);
   private float scrollTarget;
   private float renderedScroll;
   private boolean draggingScroll;
   private float scrollGrab;

   private float chipWidth() { return Math.min(CHIP_WIDTH, (value237 - PADDING * 2 - 24) / 4); }

   private int columns() { return Math.max(1, Math.min(4, (int) ((value237 - PADDING * 2) / 230))); }
   private float cardWidth() { return (value237 - PADDING * 2 - 10 - GAP * (columns() - 1)) / columns(); }
   private float gridTop() { return value236 + HEADER_HEIGHT; }
   private float gridHeight() { return Math.max(1, value238 - HEADER_HEIGHT - 30); }
   private float contentHeight() {
      int rows = (CosmeticCatalog.entries(slot).size() + columns()) / columns();
      return rows * (CARD_HEIGHT + GAP) - GAP;
   }
   private float maxScroll() { return Math.max(0, contentHeight() - gridHeight()); }
   private float thumbHeight() { return Math.max(36, gridHeight() * gridHeight() / Math.max(gridHeight(), contentHeight())); }
   private float thumbY() { return gridTop() + (gridHeight() - thumbHeight()) * renderedScroll / Math.max(1, maxScroll()); }
   private float scrollbarX() { return value235 + value237 - PADDING + 2; }

   @Override
   public void onFloatFloatFloatMatrix4f(float alpha, float mouseY, float mouseX, Matrix4f matrix) {
      scrollTarget = Math.clamp(scrollTarget, 0, maxScroll());
      scroll.setFloat2(scrollTarget);
      renderedScroll = Math.clamp(scroll.getFloat(), 0, maxScroll());
      var manager = CosmeticManager.getInstance();
      List<CosmeticEntry> entries = CosmeticCatalog.entries(slot);
      float x = value235 + PADDING;
      ScissorStack.onFloatFloatFloatFloat(value237, value238, value236, value235);
      try {
         text(matrix, tr("cosmetics.wardrobe"), x, value236 + 22, 22, Theme.foreground(), alpha);
         clippedText(matrix, tr(slot == CosmeticSlot.CHARACTER ? "cosmetics.characters_description" : "cosmetics.description"), x, value236 + 49, value237 - PADDING * 2, 13, Theme.mutedFg(), alpha);
         for (CosmeticSlot candidate : CosmeticSlot.values()) {
            float chipX = x + candidate.ordinal() * (chipWidth() + 8);
            boolean selected = slot == candidate;
            boolean hover = contains(mouseX, mouseY, chipX, value236 + 74, chipWidth(), CHIP_HEIGHT);
            box(matrix, chipX, value236 + 74, chipWidth(), CHIP_HEIGHT, 9,
               selected ? Theme.primary() : Theme.elevated(), alpha * (selected ? .18F : hover ? .85F : .5F));
            icon(matrix, slotIcon(candidate), chipX + 12, value236 + 83, 16,
               selected ? Theme.primary() : Theme.mutedFg(), alpha);
            clippedText(matrix, slotName(candidate), chipX + 36, value236 + 84, chipWidth() - 42, 13,
               selected ? Theme.primary() : Theme.foreground(), alpha);
         }
         CosmeticEntry equipped = manager.equipped(slot);
         String status = tr("cosmetics.equipped") + ": " + (equipped == null ? tr("cosmetics.none") : equipped.name());
         float statusX = x;
         clippedText(matrix, status, statusX, value236 + 118, value235 + value237 - PADDING - statusX,
            12, Theme.mutedFg(), alpha);

         // Leave room for the card border and scissor rounding above the first row.
         ScissorStack.onFloatFloatFloatFloat(value237 - PADDING * 2 - 6, gridHeight() + 2, gridTop() - 2, x);
         // Each card draws its background right before the preview, so every preview starts from the same GL state.
         CosmeticRenderer.beginPreviews();
         try {
            for (int index = 0; index <= entries.size(); index++) {
               float cx = x + index % columns() * (cardWidth() + GAP);
               float cy = gridTop() + index / columns() * (CARD_HEIGHT + GAP) - renderedScroll;
               if (cy + CARD_HEIGHT < gridTop() || cy > gridTop() + gridHeight()) continue;
               CosmeticEntry entry = index == 0 ? null : entries.get(index - 1);
               boolean selected = entry == null ? equipped == null : manager.isEquipped(entry);
               boolean hover = contains(mouseX, mouseY, cx, cy, cardWidth(), CARD_HEIGHT)
                  && mouseY >= gridTop() && mouseY < gridTop() + gridHeight();
               drawCard(matrix, entry, cx, cy, selected, hover, alpha);
            }
         } finally {
            CosmeticRenderer.endPreviews();
            ScissorStack.update();
         }
         if (maxScroll() > 0) {
            box(matrix, scrollbarX(), gridTop(), 3, gridHeight(), 1.5F, Theme.elevated(), alpha * .6F);
            box(matrix, scrollbarX() - 1, thumbY(), 5, thumbHeight(), 2.5F, Theme.primary(), alpha * .65F);
         }
         boolean storageError = manager.persistenceError() != null;
         text(matrix, storageError ? tr("cosmetics.storage_error") : Localizator.translate("cosmetics.collection_count", entries.size()), x,
            value236 + value238 - 18, 11, storageError ? Theme.warning() : Theme.mutedFg(), alpha);
      } finally {
         ScissorStack.update();
      }
   }

   private void drawCard(Matrix4f matrix, CosmeticEntry entry, float x, float y, boolean selected, boolean hover, float alpha) {
      float width = cardWidth();
      ShapeShader.onFloatFloatFloatMatrix4fFloatIntFloatFloatIntFloat(1, width, alpha, matrix, y,
         hover ? Theme.elevated() : Theme.surface(), 12, x, selected ? Theme.primary() : Theme.border(), CARD_HEIGHT);
      box(matrix, x + 8, y + 8, width - 16, 148, 9, Theme.background(), alpha * .6F);
      if (entry == null) {
         icon(matrix, LucideIcons.CROSS, x + width / 2 - 22, y + 52, 44, Theme.mutedFg(), alpha * .65F);
         centeredText(matrix, tr("cosmetics.default_look"), x + width / 2, y + 112, 12, Theme.mutedFg(), alpha);
      } else {
         TextShader.update2();
         // The preview shares the same model and texture as the equipped cosmetic.
         CosmeticRenderer.renderPreview(entry, matrix, x + 16, y + 12, width - 32, 136, alpha);
      }
      if (selected) {
         box(matrix, x + width - 32, y + 16, 20, 20, 6, Theme.primary(), alpha);
         icon(matrix, LucideIcons.CHECK, x + width - 29, y + 19, 14, Theme.background(), alpha);
      }
      clippedText(matrix, entry == null ? tr("cosmetics.none") : entry.name(), x + 14, y + 168,
         width - 28, 15, Theme.foreground(), alpha);
      String action = selected ? tr(entry == null ? "cosmetics.selected" : "cosmetics.click_remove") : tr("cosmetics.click_equip");
      text(matrix, action, x + 14, y + 193, 12, selected ? Theme.primary() : Theme.mutedFg(), alpha);
   }

   @Override
   public boolean isIntDoubleDouble(int button, double x, double y) {
      if (button != 0 || !isDoubleDouble(x, y)) return false;
      for (CosmeticSlot candidate : CosmeticSlot.values()) {
         if (contains(x, y, value235 + PADDING + candidate.ordinal() * (chipWidth() + 8), value236 + 74, chipWidth(), CHIP_HEIGHT)) {
            if (slot != candidate) {
               slot = candidate;
               draggingScroll = false;
               scrollTarget = renderedScroll = 0;
               scroll.setFloat(0);
               ClickSysSounds.play(SoundEvent.CATEGORY_SWITCH);
            }
            return true;
         }
      }
      if (maxScroll() > 0 && contains(x, y, scrollbarX() - 6, gridTop(), 14, gridHeight())) {
         draggingScroll = true;
         scrollGrab = contains(x, y, scrollbarX() - 6, thumbY(), 14, thumbHeight()) ? (float) y - thumbY() : thumbHeight() / 2;
         dragScroll(y);
         return true;
      }
      if (y < gridTop() || y >= gridTop() + gridHeight()) return true;
      List<CosmeticEntry> entries = CosmeticCatalog.entries(slot);
      for (int index = 0; index <= entries.size(); index++) {
         float cx = value235 + PADDING + index % columns() * (cardWidth() + GAP);
         float cy = gridTop() + index / columns() * (CARD_HEIGHT + GAP) - renderedScroll;
         if (!contains(x, y, cx, cy, cardWidth(), CARD_HEIGHT)) continue;
         var manager = CosmeticManager.getInstance();
         CosmeticEntry entry = index == 0 ? null : entries.get(index - 1);
         boolean remove = entry == null || manager.isEquipped(entry);
         if (remove) manager.unequip(slot); else manager.equip(entry);
         ClickSysSounds.play(remove ? SoundEvent.TOGGLE_OFF : SoundEvent.TOGGLE_ON);
         return true;
      }
      return true;
   }

   @Override
   public boolean isDoubleDoubleDouble(double amount, double x, double y) {
      if (!isDoubleDouble(x, y)) return false;
      float previous = scrollTarget;
      scrollTarget = Math.clamp(scrollTarget - (float) amount * 60, 0, maxScroll());
      if (previous != scrollTarget) ClickSysSounds.scroll();
      return true;
   }

   @Override
   public boolean isDoubleDoubleIntDoubleDouble(double y, double x, int button, double dx, double dy) {
      if (!draggingScroll || button != 0) return false;
      dragScroll(y);
      return true;
   }

   private void dragScroll(double y) {
      scrollTarget = Math.clamp(((float) y - gridTop() - scrollGrab) / Math.max(1, gridHeight() - thumbHeight()) * maxScroll(), 0, maxScroll());
      renderedScroll = scrollTarget;
      scroll.setFloat(scrollTarget);
   }

   @Override
   public boolean isDoubleDoubleInt(double x, double y, int button) {
      if (button != 0 || !draggingScroll) return false;
      draggingScroll = false;
      return true;
   }

   private static boolean contains(double x, double y, float bx, float by, float width, float height) {
      return x >= bx && x < bx + width && y >= by && y < by + height;
   }
   private static String tr(String key) { return Localizator.translate(key); }
   private static String slotName(CosmeticSlot slot) {
      return tr(switch (slot) {
         case BACKPACK -> "cosmetics.backpacks";
         case WINGS -> "cosmetics.wings";
         case MELEE -> "cosmetics.melee";
         case CHARACTER -> "cosmetics.characters";
      });
   }
   private static LucideIcons slotIcon(CosmeticSlot slot) {
      return switch (slot) {
         case BACKPACK -> LucideIcons.PACKAGE;
         case WINGS -> LucideIcons.WAVES;
         case MELEE -> LucideIcons.SWORDS;
         case CHARACTER -> LucideIcons.USER;
      };
   }
   private static void box(Matrix4f matrix, float x, float y, float width, float height, float radius, int color, float alpha) {
      ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(radius, x, color, matrix, height, width, alpha, y);
   }
   private static void icon(Matrix4f matrix, LucideIcons icon, float x, float y, float size, int color, float alpha) {
      TextShader.update2();
      icon.draw(matrix, x, y, size, ColorRGBA.fromInt(color).mulAlpha(alpha));
   }
   private static void text(Matrix4f matrix, String label, float x, float y, float size, int color, float alpha) {
      TextShader.onMatrix4fStringFloatFloatFloatIntFloat(matrix, label, x, y, size, color, alpha);
   }
   private static void centeredText(Matrix4f matrix, String label, float centerX, float y, float size, int color, float alpha) {
      text(matrix, label, centerX - TextShader.getFloatByStringFloat(label, size) / 2, y, size, color, alpha);
   }
   private static void clippedText(Matrix4f matrix, String label, float x, float y, float width, float size, int color, float alpha) {
      if (width <= 0) return;
      ScissorStack.onFloatFloatFloatFloat(width, size + 5, y - 1, x);
      try { text(matrix, label, x, y, size, color, alpha); }
      finally { ScissorStack.update(); }
   }
}
