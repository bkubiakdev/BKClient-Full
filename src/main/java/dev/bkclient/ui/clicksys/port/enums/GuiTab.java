package dev.bkclient.ui.clicksys.port.enums;

public enum GuiTab {
   MODULES,
   CONFIGS,
   FRIENDS,
   FAVORITES,
   WAYPOINTS,
   THEMES,
   AUTOMARKET,
   COSMETICS;

   private static final GuiTab[] guiTabArray = getGuiTabArray();

   private static GuiTab[] getGuiTabArray() {
      return new GuiTab[]{MODULES, CONFIGS, FRIENDS, FAVORITES, WAYPOINTS, THEMES, AUTOMARKET, COSMETICS};
   }

   public static GuiTab getGuiTabByString(String text) {
      return Enum.valueOf(GuiTab.class, text);
   }
}
