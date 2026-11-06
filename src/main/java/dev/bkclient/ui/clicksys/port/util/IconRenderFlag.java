package dev.bkclient.ui.clicksys.port.util;

public final class IconRenderFlag {
   private static boolean flag;

   private IconRenderFlag() {
   }

   public static void setFlag() {
      flag = false;
   }

   public static void setFlag2() {
      flag = true;
   }

   public static boolean isFlag() {
      return flag;
   }
}
