package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.data.AnimatedInt;
import dev.bkclient.ui.clicksys.port.data.TextTrimmer;
import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;
import java.util.function.Consumer;
import org.joml.Matrix4f;

/** Local FriendCard adaptation. Identity always remains the BK nickname, never the alias. */
public class FriendRow extends ListRow {
   public static final float COLLAPSED_HEIGHT = 62, EXPANDED_HEIGHT = 112, RADIUS = 18;
   private String name, alias = "", group, date;
   private final Tween hover = EasingPresets.getTweenByFloatFloat2(0, .15F);
   private final Tween color = EasingPresets.getTweenByFloatFloat2(0, .22F);
   private final ScrollState actions;
   private Consumer<String> remove, copy;

   public FriendRow(String name, String group, String date) {
      this.name = name; this.group = group; this.date = date;
      value237 = 300; value238 = COLLAPSED_HEIGHT;
      actions = new ScrollState(36,4)
         .getScrollStateByStringRunnableFloatFloatCategoryType("Remove friend", () -> {
            if (remove != null) remove.accept(this.name);
         }, 14, 14, CategoryType.TRASH)
         .getScrollStateByStringRunnableFloatFloatCategoryType("Copy nickname", () -> {
            if (copy != null) copy.accept(this.name);
         }, 14, 14, CategoryType.COPY);
   }
   public void setConsumer(Consumer<String> value) { remove = value; }
   public void setConsumer2(Consumer<String> value) { copy = value; }
   public String getText() { return name; }
   public void onStringStringStringString(String name, String alias, String group, String date) {
      this.name = name; this.alias = alias == null ? "" : alias;
      this.group = group == null ? "" : group; this.date = date;
   }
   @Override public boolean check() { return !isFlag4() && super.check(); }
   @Override protected ScrollState getScrollState() { return actions; }
   @Override protected boolean isIntDoubleDouble2(int button, double y, double x) {
      return button == 0 && isDoubleDouble(x,y);
   }
   @Override protected void onFloatMatrix4fFloatFloat(float alpha, Matrix4f matrix, float my, float mx) {
      value238 = COLLAPSED_HEIGHT;
      boolean over = isDoubleDouble(mx,my) && !WidgetState.check();
      hover.setFloat2(over ? 1 : 0); color.setFloat2(over ? 1 : 0);
      int fill = AnimatedInt.getIntByIntFloatInt(Theme.elevated(), color.getFloat()*.65F, Theme.surface());
      dev.bkclient.ui.clicksys.ClickSysMaterial.panel(matrix,value235,value236,value237,value238,RADIUS,fill,Theme.border(),alpha);
      float x = value235+12, y = value236+12;
      dev.bkclient.ui.clicksys.ClickSysMaterial.friendAvatar(matrix,name,x,y,38,alpha);
      text(matrix,name,x+48,y+2,16,Math.max(30,value237-150),Theme.foreground(),alpha);
      var mc = net.minecraft.client.MinecraftClient.getInstance();
      var presence = dev.bkclient.utility.game.ClientTelemetryUtility.getFriendPresence(name);
      boolean sameServer = mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerList().stream()
         .anyMatch(entry -> entry.getProfile().getName().equalsIgnoreCase(name));
      boolean online = sameServer || presence != null;
      String status = online ? "Online" : "Offline";
      float badgeX = x + 48, badgeY = y + 23, badgeWidth = online ? 60 : 64;
      dev.bkclient.ui.clicksys.port.render.ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(
         2, badgeX + 6, online ? 0xFF68CA8D : 0xFFFFFFFF, matrix, 4, 4, alpha, badgeY + 7);
      text(matrix,status,badgeX+15,badgeY+3,11,badgeWidth-19,Theme.foreground(),alpha);
      String details = group == null || group.isBlank() || "Friend".equalsIgnoreCase(group) ? "" : group;
      if (online) {
         String server = sameServer
            ? dev.bkclient.utility.game.server.ServerUtility.getCurrentServerAddress("Online") : presence.server();
         details = details.isBlank() ? server : details + " · " + server;
      }
      float detailsWidth = value237 - 150 - badgeWidth - 6;
      if (detailsWidth > 16) text(matrix,details,badgeX+badgeWidth+6,badgeY+3,11,detailsWidth,Theme.mutedFg(),alpha);
      actions.getFloatByFloatFloatFloatMatrix4fFloatFloat(my,mx,alpha,matrix,value236+13,value235+value237-12);
   }
   private void text(Matrix4f matrix,String value,float x,float y,float size,float width,int color,float alpha) {
      String clipped=TextTrimmer.getStringByFloatStringFloat2(width,value==null?"":value,size);
      TextShader.onFloatFloatIntFloatFloatStringMatrix4f(y,x,color,size,alpha,clipped,matrix);
   }
}
