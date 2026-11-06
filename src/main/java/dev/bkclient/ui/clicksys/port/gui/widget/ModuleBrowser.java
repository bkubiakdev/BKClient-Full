package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.module.Module;
import java.util.List;
import java.util.Set;
import org.joml.Matrix4f;

/** Three columns of compact module cards and a detached settings inspector. */
public final class ModuleBrowser extends Widget {
   private final ContentArea modules = new ContentArea();
   private final SettingsInspector settings = new SettingsInspector(this::closeSettings);
   private Module selected;
   private boolean layoutDirty = true;
   private float laidOutX = Float.NaN;
   private float laidOutY = Float.NaN;
   private float laidOutWidth = Float.NaN;
   private float laidOutHeight = Float.NaN;

   public Module getSelectedModule() { return selected; }
   public SettingsInspector getSettingsArea() { return settings; }
   public boolean hasOpenSettings() {
      // Visibility may change while a module is selected; release the layout too.
      if (selected != null && !SettingsInspector.canShow(selected)) closeSettings();
      return selected != null;
   }
   public List<ModuleRow> getList() { return modules.getList(); }
   public float getValue249() { return modules.getValue249(); }
   public void setFloat(float value) { modules.setFloat(value); }
   public void setFloat2(float value) { modules.setFloat2(value); }
   public void onSet(Set<?> folded) { modules.onSet(folded); }
   public void onBoolean(boolean folded) { closeSettings(); modules.onBoolean(folded); }

   public void addList(List<Module> list) {
      layoutDirty = true;
      closeSettings();
      modules.addList(list);
      for (ModuleRow row : modules.getList()) {
         // Keep module cards compact; right click opens the detached settings panel.
         row.setSettingsAction(() -> setModule(row.getModule()), true);
      }
   }

   public void setModule(Module module) {
      if (module == null) { closeSettings(); return; }
      // Scroll the grid to the card and pulse its highlight, also for modules without settings to inspect.
      modules.setModule(module);
      // Reject empty/hidden/unsupported settings before selection changes the window size.
      if (!SettingsInspector.canShow(module) || module == selected) return;
      selected = module;
      layoutDirty = true;
      if (module != null) {
         settings.show(module);
         for (ModuleRow row : modules.getList()) {
            row.setSelected(row.getModule() == module);
         }
      }
      layout();
   }

   /** Opens {@code module} in the inspector and scrolls to {@code setting}, highlighting it. */
   public void focusSetting(Module module, dev.bkclient.ui.clicksys.port.setting.Setting setting) {
      setModule(module);
      if (selected == module && setting != null) settings.focus(setting);
   }

   public void closeSettings() {
      layoutDirty = true;
      for (ModuleRow row : modules.getList()) row.setSelected(false);
      settings.hide();
      selected = null;
      layout();
   }

   private void layout() {
      if (!layoutDirty && laidOutX == value235 && laidOutY == value236
            && laidOutWidth == value237 && laidOutHeight == value238) {
         return;
      }
      modules.setColumns(3);
      modules.onFloatFloat2(value236, value235);
      modules.onFloatFloat4(0, value237);
      modules.setValue238(value238);
      settings.position(value235 + value237 + 40, value236 - 49, value238 + 49);
      laidOutX = value235;
      laidOutY = value236;
      laidOutWidth = value237;
      laidOutHeight = value238;
      layoutDirty = false;
   }

   public void renderInspector(float alpha, float mouseY, float mouseX, Matrix4f matrix) {
      settings.onFloatFloatFloatMatrix4f(alpha, mouseY, mouseX, matrix);
   }

   public void onFloatFloat4(float expansion, float width) { if (value237 != width) layoutDirty = true; value237 = width; layout(); }

   @Override
   public void onFloatFloatFloatMatrix4f(float alpha, float mouseY, float mouseX, Matrix4f matrix) {
      layout();
      modules.onFloatFloatFloatMatrix4f(alpha, mouseY, mouseX, matrix);
   }

   public void onMatrix4fFloat(Matrix4f matrix, float alpha) {
      modules.onMatrix4fFloat(matrix, alpha);

   }

   @Override public boolean isIntDoubleDouble(int button, double x, double y) {
      if (selected != null && settings.isIntDoubleDouble(button, x, y)) return true;
      return modules.isIntDoubleDouble(button, x, y);
   }
   @Override public boolean isDoubleDoubleDouble(double delta, double x, double y) {
      return selected != null && settings.isDoubleDoubleDouble(delta, x, y)
         || modules.isDoubleDoubleDouble(delta, x, y);
   }
   @Override public boolean isDoubleDoubleInt(double x, double y, int button) {
      boolean handled = selected != null && settings.isDoubleDoubleInt(x, y, button);
      return modules.isDoubleDoubleInt(x, y, button) || handled;
   }
   @Override public boolean isDoubleDoubleIntDoubleDouble(double y, double x, int button, double dx, double dy) {
      return selected != null && settings.isDoubleDoubleIntDoubleDouble(y, x, button, dx, dy)
         || modules.isDoubleDoubleIntDoubleDouble(y, x, button, dx, dy);
   }
   @Override public boolean isIntIntInt(int a, int b, int c) {
      return selected != null && settings.isIntIntInt(a, b, c) || modules.isIntIntInt(a, b, c);
   }
   @Override public boolean isIntIntInt2(int a, int b, int c) {
      return selected != null && settings.isIntIntInt2(a, b, c) || modules.isIntIntInt2(a, b, c);
   }
   @Override public boolean isIntChar(int modifiers, char ch) {
      return selected != null && settings.isIntChar(modifiers, ch) || modules.isIntChar(modifiers, ch);
   }
}
