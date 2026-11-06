package dev.bkclient.ui.hud.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.ui.hud.HudList;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.ui.hud.components.HudValueRow;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.render.ScissorUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Identifier;

/** Compact Liquid bind list; all displayed live states come from the module manager. */
public class KeyBinds extends HudList {
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");
   private final ModeSetting display = new ModeSetting(this, "hud.binds.display");
   private final ModeSetting.Value activeBinds = new ModeSetting.Value(display, "hud.binds.active_binds").select();
   private final ModeSetting.Value allBinds = new ModeSetting.Value(display, "hud.binds.all_binds");
   private final ModeSetting.Value activeModules = new ModeSetting.Value(display, "hud.binds.active_modules");
   private final BooleanSetting icons = new BooleanSetting(this, "hud.binds.icons").enable();
   private final BooleanSetting preview = new BooleanSetting(this, "hud.binds.preview");
   private final Map<Module, Row> rows = new IdentityHashMap<>();
   private final List<Row> previewRows = List.of(new Row("AutoTotem", "I", ModuleCategory.COMBAT),
      new Row("NoSlow", "N", ModuleCategory.MOVEMENT));
   private List<Row> visibleRows = List.of();
   private int activeCount, overflow;
   private final dev.bkclient.systems.setting.settings.SliderSetting rowLimit = new dev.bkclient.systems.setting.settings.SliderSetting(this, "hud.list.limit").min(1).max(30).step(1).currentValue(12);
   private final ModeSetting sorting = new ModeSetting(this, "hud.list.sort");
   private final ModeSetting.Value sortName = new ModeSetting.Value(sorting, "hud.list.sort_name").select();
   private final ModeSetting.Value sortKey = new ModeSetting.Value(sorting, "hud.binds.sort_key");
   private final ModeSetting.Value sortActive = new ModeSetting.Value(sorting, "hud.binds.sort_active");
   private final BooleanSetting badges = new BooleanSetting(this, "hud.editor.badges").enable();

   public KeyBinds() { super("hud.keybinds", "icons/hud/keyboard.png"); }
   private float rowHeight() { return listStride(HudValueRow.height()); }
   private float headerHeight() { return listTop(HudValueRow.contentTop()); }
   private Font nameFont() { return HudValueRow.labelFont(); }

   private boolean shouldInclude(Module module) {
      if (module instanceof MenuModule) return false;
      boolean bound = module.getKey() >= 0;
      if (allBinds.isSelected()) return bound;
      if (activeModules.isSelected()) return module.isEnabled();
      return module.isEnabled() && bound;
   }

   private static String keyLabel(int key) {
      if (key < 0) return "ON";
      // The shared formatter has Russian mouse labels; use short, language-neutral keycaps here.
      if (key <= 7) return "M" + (key + 1);
      return TextUtility.getKeyName(key);
   }

   /** Inputs of the last sorted list; the sort only runs again when one of them changed. */
   private List<Row> lastCandidates = List.of();
   private boolean lastPreview;
   private int lastSortMode = -1, lastMaxRows = -1, frameStamp;

   @Override public void update(UIContext context) {
      List<Module> modules = BKClientInit.getInstance().getModuleManager().getModules();
      int frame = ++frameStamp;
      boolean changed = false;
      List<Row> candidates = new ArrayList<>();
      activeCount = 0;
      for (Module module : modules) {
         if (module instanceof MenuModule) continue;
         boolean include = shouldInclude(module);
         Row row = rows.get(module);
         if (row == null && !include) continue;
         if (row == null) {
            row = new Row(module.getName(), keyLabel(module.getKey()), module.getCategory());
            row.keyLabelCode = module.getKey();
            rows.put(module, row);
            changed = true;
         }
         row.seen = frame;
         String label = module.getName();
         if (!java.util.Objects.equals(label, row.label)) changed = true;
         row.label = label;
         boolean enabled = module.isEnabled();
         if (enabled != row.enabled) changed = true;
         row.enabled = enabled;
         row.keyCode = module.getKey();
         // keyLabel is a pure function of the key code: only re-derive it when the bind changed.
         if (include && row.keyLabelCode != row.keyCode) {
            String key = keyLabel(row.keyCode);
            if (!key.equals(row.key)) changed = true;
            row.key = key;
            row.keyLabelCode = row.keyCode;
         }
         row.reveal.update(include);
         row.active.update(row.enabled);
         if (include && row.enabled) activeCount++;
         if (row.reveal.getValue() > 0.001F) candidates.add(row);
      }
      // Rows of modules that left the manager (every listed module with a row was stamped above).
      rows.values().removeIf(row -> row.seen != frame);
      boolean previewOn = preview.isEnabled();
      if (previewOn) {
         activeCount = previewRows.size();
         for (Row row : previewRows) {
            row.enabled = true;
            row.reveal.update(true);
            row.active.update(true);
         }
      } else {
         for (Row row : previewRows) { row.reveal.setValue(0); row.active.setValue(0); }
      }
      int sortMode = sortKey.isSelected() ? 1 : sortActive.isSelected() ? 2 : 0;
      int maxRows = (int)rowLimit.getCurrentValue();
      if (changed || previewOn != lastPreview || sortMode != lastSortMode || maxRows != lastMaxRows
            || !candidates.equals(lastCandidates)) {
         lastCandidates = candidates;
         lastPreview = previewOn;
         lastSortMode = sortMode;
         lastMaxRows = maxRows;
         List<Row> sorted = new ArrayList<>(candidates);
         sorted.sort(Comparator.comparing(row -> row.label, String.CASE_INSENSITIVE_ORDER));
         if (previewOn) sorted = new ArrayList<>(previewRows);
         if (sortMode == 1) sorted.sort(Comparator.comparing((Row row) -> row.key, String.CASE_INSENSITIVE_ORDER).thenComparing(row -> row.label));
         else if (sortMode == 2) sorted.sort(Comparator.comparing((Row row) -> !row.enabled).thenComparing(row -> row.label, String.CASE_INSENSITIVE_ORDER));
         overflow = Math.max(0, sorted.size() - maxRows);
         visibleRows = List.copyOf(sorted.subList(0, Math.min(maxRows, sorted.size())));
      }
      width = Math.max(HudTheme.isOld() ? 70 : 104,
         Fonts.MEDIUM.getFont(7).width(Localizator.translate(name)) + 28);
      height = headerHeight();
      for (Row row : visibleRows) {
         width = Math.max(width, HudValueRow.width(row.label, row.key) - (rowIcons() ? 0 : HudTheme.isOld() ? 9.5F : 16));
         height += rowHeight() * Math.clamp(row.reveal.getValue(), 0, 1);
      }
      width = Math.min(width, 240);
      if (visibleRows.isEmpty() || overflow > 0) height += rowHeight();
      if (HudTheme.isOld()) height += 6;
      super.update(context);
   }

   @Override protected void renderContent(UIContext context) {
      super.renderContent(context);
      float rowY = y + headerHeight();
      for (Row row : visibleRows) {
         float h = rowHeight() * Math.clamp(row.reveal.getValue(), 0, 1);
         ScissorUtility.push(context.getMatrices(), x + 4, rowY, width - 8, h);
         try { drawRow(context, row, rowY); }
         finally { ScissorUtility.pop(); }
         rowY += h;
      }
      if (visibleRows.isEmpty() || overflow > 0) {
         Font font = nameFont();
         String empty = visibleRows.isEmpty() ? Localizator.translate("hud.binds.empty") : "+" + overflow;
         context.drawFadeoutText(font, empty, x + 9, rowY + (rowHeight() - font.height()) / 2,
            HudTheme.textColor().mulAlpha(0.45F), 0.9F, 1, width - 18);
      }
   }

   private boolean rowIcons() { return icons.isEnabled() && dev.bkclient.ui.hud.HudVisualScope.icons(); }

   private void drawRow(UIContext context, Row row, float rowY) {
      float reveal = Math.clamp(row.reveal.getValue(), 0, 1);
      float active = Math.clamp(row.active.getValue(), 0, 1);
      float opacity = reveal * (0.4F + 0.6F * active);
      HudValueRow.draw(context, x, rowY, width, row.label, row.key, HudTheme.textColor(), opacity,
         rowIcons() ? (iconX, iconY, size) -> context.drawTextureSmooth(row.icon, iconX, iconY, size, size,
            HudTheme.textColor().mulAlpha(opacity)) : null, null, null, badges.isEnabled());
   }

   @Override protected float getContextMenuWidth() { return 165; }
   @Override public boolean show() {
      return preview.isEnabled() || !visibleRows.isEmpty() || activeCount > 0
         || mc.currentScreen instanceof ChatScreen || alwaysDisplay.isEnabled();
   }
   private static final class Row {
      String label, key;
      boolean enabled;
      int keyCode = -1;
      int keyLabelCode = Integer.MIN_VALUE;
      int seen;
      final Identifier icon;
      final Animation reveal = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
      final Animation active = new Animation(180L, Easing.FIGMA_EASE_IN_OUT);
      Row(String label, String key, ModuleCategory category) {
         this.label = label;
         this.key = key;
         this.icon = BKClientInit.id("icons/category/" + category.name().toLowerCase(Locale.ROOT) + ".png");
      }
   }
}
