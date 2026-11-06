package dev.bkclient.ui.clicksys.port.data;

import dev.bkclient.ui.clicksys.port.gui.widget.SettingField;

public record SettingRowBounds(SettingField row, float rx, float ry, float rw) {
   public float getRy() {
      return this.ry;
   }

   public float getRw() {
      return this.rw;
   }

   public float getRx() {
      return this.rx;
   }

   public SettingField getRow() {
      return this.row;
   }
}
