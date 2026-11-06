package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.data.SettingWidgetFactory;
import dev.bkclient.ui.clicksys.port.data.TextTrimmer;
import dev.bkclient.ui.clicksys.port.module.Module;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.joml.Matrix4f;

/** Detached settings window. All coordinates, including animated hitboxes, are in clicksys units. */
public class SettingsInspector extends Widget {
   protected boolean utilityPage;
   private List<PanelWidget> rows = new ArrayList<>(), previous = List.of();
   private Module module, previousModule;
   private final ToggleButton toggle = new ToggleButton(false);
   private final IconButton close = new IconButton(CategoryType.CLOSE, 16);
   private float scroll, previousScroll, contentHeight, maxHeight = 700, anchorX, anchorY;
   private long changedAt;
   private boolean closing;
   private float openingFrom, opacity;
   /** Setting picked in the search bar: the inspector scrolls to its row once laid out and highlights it briefly. */
   private Setting focused; private long focusedAt; private boolean focusScrolled;

   public SettingsInspector(Runnable onClose) {
      value237 = 334;
      close.setText("Close settings"); close.setRunnable(onClose);
      toggle.setRunnable(() -> { if (module != null) module.setEnabled(toggle.isFlag4()); });
   }
   public static boolean canShow(Module module) {
      return module != null && module.getSettings().stream().anyMatch(setting ->
         setting != module.getKeybindSetting() && setting.isVisible() && SettingWidgetFactory.supports(setting));
   }

   public float getValue249() { return scroll; }
   public void position(float x, float y, float maxHeight) { anchorX = x; anchorY = y; this.maxHeight = maxHeight; }
   private float progress() { return Math.clamp((System.nanoTime() - changedAt) / 180_000_000f, 0, 1); }
   private float eased() { float t = progress(); return 1 - (1-t)*(1-t)*(1-t); }
   public void show(Module next) {
      if (module == next && !closing) return;
      cleanup(rows);
      previous = closing ? List.of() : rows; previousModule = closing ? null : module; previousScroll = scroll;
      module = next; rows = new ArrayList<>(); scroll = 0; closing = false;
      openingFrom = opacity; changedAt = System.nanoTime();
      for (Setting setting : next.getSettingsCopy()) {
         if (setting == next.getKeybindSetting()) continue;
         PanelWidget row;
         if (setting instanceof dev.bkclient.ui.clicksys.port.setting.RangeSliderSetting range) row = new RangeSliderRow(range);
         else if (setting instanceof SliderSetting slider) row = new SliderRow(slider);
         else if (setting instanceof ColorSetting color) row = new InspectorColorRow(color);
         else if (setting instanceof ChoiceSetting choice) row = SettingWidgetFactory.getPanelWidgetBySetting10(choice);
         else if (setting instanceof BooleanSetting bool) row = new BooleanRow(bool);
         else row = SettingWidgetFactory.getPanelWidgetBySetting10(setting);
         if (row != null) rows.add(row);
      }
   }
   /** Scrolls to {@code setting} after {@link #show} and highlights its row for about 1.5 s. */
   public void focus(Setting setting) {
      focused = setting; focusedAt = System.nanoTime(); focusScrolled = false;
   }
   private float highlight() {
      if (focused == null) return 0;
      float t = (System.nanoTime() - focusedAt) / 1_500_000_000f;
      if (t >= 1) { focused = null; return 0; }
      return t < .2f ? t / .2f : 1 - (t - .2f) / .8f;
   }
   private void scrollToFocused() {
      if (focused == null || focusScrolled) return;
      float y = 0, viewport = value238 - 60;
      for (PanelWidget row : rows) {
         if (!row.getSetting().isVisible()) continue;
         float height = row.getFloat4() + 20;
         if (row.getSetting() == focused) {
            // Center the row when it does not fit at the current offset.
            if (y < scroll || y + height > scroll + viewport) scroll = y - Math.max(0, (viewport - height) / 2);
            scroll = Math.clamp(scroll, 0, Math.max(0, contentHeight - viewport));
            focusScrolled = true;
            return;
         }
         y += height;
      }
      focused = null;
   }
   public void hide() {
      focused = null;
      cleanup(rows); previous = List.of(); previousModule = null;
      openingFrom = opacity; closing = true; changedAt = System.nanoTime();
   }
   private static void cleanup(List<PanelWidget> rows) {
      for (PanelWidget row : rows) { row.update2(); row.update3(); }
   }
   private boolean hasVisibleSettings() {
      return rows.stream().anyMatch(row -> row.getSetting().isVisible());
   }
   private float measure(List<PanelWidget> items) {
      float h = 0;
      for (PanelWidget row : items) if (row.getSetting().isVisible()) {
         row.setFloat(value237 - 48); h += row.getFloat4() + 20;
      }
      return Math.max(36, h);
   }
   @Override public void onFloatFloatFloatMatrix4f(float alpha, float my, float mx, Matrix4f matrix) {
      if (module == null) return;
      if (!hasVisibleSettings()) {
         cleanup(rows);
         previous = List.of(); previousModule = null;
         opacity = 0; openingFrom = 0; value238 = 0;
         return;
      }
      float t = eased();
      opacity = closing ? openingFrom * (1-t) : openingFrom + (1-openingFrom)*t;
      if (opacity <= .001f && closing) { module = null; rows.clear(); return; }
      value235 = anchorX + (previousModule == null ? (1-opacity)*8 : 0); value236 = anchorY;
      contentHeight = measure(rows);
      float oldHeight = previousModule == null ? contentHeight : measure(previous);
      float height = oldHeight + (contentHeight-oldHeight)*t;
      value238 = Math.min(maxHeight, height + 60);
      scroll = Math.clamp(scroll, 0, Math.max(0, contentHeight - (value238-60)));
      if (progress() >= 1) scrollToFocused();
      float a = alpha * opacity;
      dev.bkclient.ui.clicksys.ClickSysMaterial.panel(matrix, value235, value236, value237, value238, 14, Theme.surface(), Theme.border(), a);
      if (previousModule != null && t < 1) title(previousModule, matrix, a*(1-t));
      title(module, matrix, a*(previousModule == null ? 1 : t));
      if (!utilityPage) {
      toggle.onFloatFloat2(value236+18, value235+value237-76); toggle.setBoolean2(module.isEnabled());
      toggle.onFloatFloatFloatMatrix4f(a, my, mx, matrix);
      close.onFloatFloat2(value236+18, value235+value237-32); close.onFloatFloatFloatMatrix4f(a, my, mx, matrix);
      }
      ScissorStack.onFloatFloatFloatFloat(value237-2, Math.max(0,value238-54), value236+46, value235+1);
      try {
         if (previousModule != null && t < 1) renderRows(previous, previousScroll, a*(1-t), my, mx, matrix);
         renderRows(rows, scroll, a*(previousModule == null ? 1 : t), my, mx, matrix);
      } finally { ScissorStack.update(); }
      if (progress() >= 1) { previous = List.of(); previousModule = null; }
      if (contentHeight > value238-60) {
         float track = value238-64, thumb = Math.max(24, track*(value238-60)/contentHeight);
         float sy = value236+50 + (track-thumb)*scroll/Math.max(1,contentHeight-(value238-60));
         ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(1, value235+value237-6, Theme.border(), matrix, thumb, 2, a, sy);
      }
   }
   private void title(Module owner, Matrix4f matrix, float alpha) {
      String title = TextTrimmer.getStringByFloatStringFloat2(value237-112, dev.bkclient.systems.localization.Localizator.translate(owner.getName()), 17);
      TextShader.onFloatStringFloatFloatIntFloatMatrix4f(alpha, title, 17, value235+16, Theme.foreground(), value236+17, matrix);
   }
   private void renderRows(List<PanelWidget> items, float offset, float alpha, float my, float mx, Matrix4f matrix) {
      float y = value236+52-offset;
      for (PanelWidget row : items) {
         if (!row.getSetting().isVisible()) continue;
         float height = row.getFloat4(); row.onFloatFloat2(y+7, value235+24);
         if (y+height+14 >= value236+46 && y <= value236+value238) {
            if (items == rows && row.getSetting() == focused && focusScrolled) {
               float glow = highlight();
               if (glow > 0) ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(10, value235+12, Theme.primary(), matrix, height+14, value237-24, alpha*glow*.28f, y);
            }
            row.onFloatFloatFloatMatrix4f(alpha, my, mx, matrix);
            if (items == rows && alpha > .95f && !WidgetState.check() && mx >= value235+12 && mx <= value235+value237-12
                && my >= y && my <= y+height+14 && my >= value236+46 && my <= value236+value238-8) {
               String description = row.getSetting().getDisplayDescription();
               if (description != null && !description.isBlank())
                  HeaderPainter.onFloatFloatStringFloat(value235+12, y, description, value237-24);
            }
         }
         y += height+20;
      }
   }
   public boolean contains(double x, double y) { return module != null && !closing && hasVisibleSettings() && isDoubleDouble(x,y); }
   private boolean route(Predicate<PanelWidget> event) {
      if (module == null || closing || !hasVisibleSettings()) return false;
      for (PanelWidget row : rows) if (row.getSetting().isVisible() && event.test(row)) return true;
      return false;
   }
   @Override public boolean isIntDoubleDouble(int button, double x, double y) {
      if (!contains(x,y)) return false;
      if (!utilityPage && (close.isIntDoubleDouble(button,x,y) || toggle.isIntDoubleDouble(button,x,y))) return true;
      if (y >= value236+46 && y <= value236+value238-8) route(row -> row.isIntDoubleDouble(button,x,y));
      return true;
   }
   @Override public boolean isDoubleDoubleDouble(double delta,double x,double y) {
      if (!contains(x,y)) return false;
      if (!route(row -> row.isDoubleDoubleDouble(delta,x,y))) {
         float previousScroll = scroll;
         scroll = Math.clamp(scroll-(float)delta*48,0,Math.max(0,contentHeight-(value238-60)));
         if (previousScroll != scroll) dev.bkclient.ui.clicksys.ClickSysSounds.scroll();
      }
      return true;
   }
   @Override public boolean isDoubleDoubleInt(double x,double y,int button) {
      return !utilityPage && module != null && !closing && hasVisibleSettings() && toggle.isDoubleDoubleInt(x,y,button)
         || route(row -> row.isDoubleDoubleInt(x,y,button));
   }
   @Override public boolean isDoubleDoubleIntDoubleDouble(double y,double x,int button,double dx,double dy) {
      return !utilityPage && module != null && !closing && hasVisibleSettings() && toggle.isDoubleDoubleIntDoubleDouble(y,x,button,dx,dy)
         || route(row -> row.isDoubleDoubleIntDoubleDouble(y,x,button,dx,dy));
   }
   @Override public boolean isIntIntInt(int a,int b,int c) { return route(row -> row.isIntIntInt(a,b,c)); }
   @Override public boolean isIntIntInt2(int a,int b,int c) { return route(row -> row.isIntIntInt2(a,b,c)); }
   @Override public boolean isIntChar(int modifiers,char ch) { return route(row -> row.isIntChar(modifiers,ch)); }
}
