package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.setting.ColorSetting;

/** Full palette and editable HEX, with the popup facing into the available canvas. */
final class InspectorColorRow extends ColorPickerRow {
   InspectorColorRow(ColorSetting color) { super(color); }
   @Override protected float[] popupPosition() {
      return popupPosition(true);
   }
}
