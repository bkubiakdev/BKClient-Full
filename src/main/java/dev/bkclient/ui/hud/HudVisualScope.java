package dev.bkclient.ui.hud;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.DrawUtility;

/** Render-scoped settings: each actual panel receives the sweep in its own transformed coordinates. */
public final class HudVisualScope {
 private static HudElement element;
 private HudVisualScope() {}
 public static void begin(HudElement value) { element=value; }
 public static void end() { element=null; }
 public static boolean icons() { return element==null || element.interfaceIcons(); }
 public static float opacity() { return element==null?1:element.panelOpacity(); }
 public static float strength() { return element==null?0:element.selectionStrength()*Math.clamp(element.getAnimation().getValue()*element.getVisible().getValue(),0,1); }
 public static float progress() { return element==null?0:element.getLoadingAnim().getValue()*2.2F-.5F; }
 public static void panel(CustomDrawContext c,float x,float y,float w,float h,float radius,float corner,float alpha) {
  if(strength()<=.001F || w<=0 || h<=0)return;
  c.draw();
  DrawUtility.drawLoadingRect(c.getMatrices(),x,y,w,h,progress(),BorderRadius.all(radius),ColorRGBA.WHITE.withAlpha(100*strength()*alpha),corner);
 }
}
