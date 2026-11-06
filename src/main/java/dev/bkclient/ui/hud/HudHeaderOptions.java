package dev.bkclient.ui.hud;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;

/** Shared options for list-style HUD headers; Edge preserves the original icon placement. */
public final class HudHeaderOptions {
   public final BooleanSetting visible, icon;
   public final BooleanSetting joined, fullWidth;
   private final boolean tabbed;
   public final SliderSetting spacing;
   private final ModeSetting alignment, iconPosition;
   private final ModeSetting.Value left, center, right, edge, before, after;
   public HudHeaderOptions(SettingsContainer owner, String name) {
      tabbed = java.util.Set.of("hud.effects", "hud.staff_list", "hud.friends", "hud.keybinds").contains(name);
      visible = new BooleanSetting(owner, "hud.editor.header").enable();
      icon = new BooleanSetting(owner, "hud.editor.header_icon", () -> !visible.isEnabled()).enable();
      joined = tabbed ? new BooleanSetting(owner, "hud.editor.joined", () -> !visible.isEnabled()).enable() : null;
      fullWidth = tabbed ? new BooleanSetting(owner, "hud.editor.full_header", () -> !visible.isEnabled()) : null;
      alignment = new ModeSetting(owner, "hud.editor.alignment", () -> !visible.isEnabled());
      left = new ModeSetting.Value(alignment, "hud.editor.left").select();
      center = new ModeSetting.Value(alignment, "hud.editor.center");
      right = new ModeSetting.Value(alignment, "hud.editor.right");
      iconPosition = new ModeSetting(owner, name.equals("hud.effects") ? "hud.effects.icon_position" : "hud.header.icon_position", () -> !visible.isEnabled() || !icon.isEnabled());
      edge = new ModeSetting.Value(iconPosition, "hud.header.edge").select();
      before = new ModeSetting.Value(iconPosition, "hud.editor.left");
      after = new ModeSetting.Value(iconPosition, "hud.editor.right");
      if (tabbed) { center.select(); before.select(); }
      spacing = new SliderSetting(owner, "hud.editor.spacing").min(0).max(8).step(1).currentValue(0);
   }
   public float top(float normal) { return tabbed ? (visible.isEnabled() ? bodyTop() + 3 : 3) : visible.isEnabled() ? normal : 5; }
   public float stride(float normal) { return normal + spacing.getCurrentValue(); }
   public float minimumWidth(String title) { return Fonts.MEDIUM.getFont(7).width(title) + 32; }
   public boolean tabbed() { return tabbed; }
   private float headerHeight() { return HudTheme.isOld() ? 16 : 18; }
   private float bodyTop() { return !visible.isEnabled() ? 0 : headerHeight() + (joined.isEnabled() ? -2 : 4); }
   private float panelWidth(String title, float width) {
      return fullWidth.isEnabled() ? width : Math.min(width, Fonts.MEDIUM.getFont(7).width(title) + (icon.isEnabled() && HudVisualScope.icons() ? 24 : 12));
   }
   private float panelX(float x, float width, float panelWidth) {
      return x + (width - panelWidth) * (left.isSelected() ? 0 : right.isSelected() ? 1 : .5f);
   }
   public void drawPanels(UIContext c, float x, float y, float width, float height, String title, float alpha, float drag) {
      float hw = panelWidth(title, width), hx = panelX(x, width, hw), top = bodyTop();
      boolean body = height > headerHeight() + .5f || !visible.isEnabled();
      if (visible.isEnabled()) {
         if (joined.isEnabled() && body) {
            if (fullWidth.isEnabled()) HudTheme.drawPanel(c,x,y,width,height,alpha,drag,7);
            else HudTheme.drawJoinedPanel(c,x,y,width,height,hx-x,hw,headerHeight(),top,alpha,drag,HudVisualScope.strength(),HudVisualScope.progress());
         } else HudTheme.drawPanel(c,hx,y,hw,headerHeight(),alpha,drag,7);
      }
      if (body && (!visible.isEnabled() || !joined.isEnabled()))
         HudTheme.drawPanel(c,x,y+top,width,Math.max(1,height-top),alpha,drag,7);
   }
   public void draw(UIContext c, float x, float y, float width, String title, String texture) {
      if (!visible.isEnabled()) return;
      if (tabbed) { float hw = panelWidth(title, width); x = panelX(x, width, hw); width = hw; }
      boolean old = HudTheme.isOld();
      Font font = (old ? Fonts.SEMIBOLD : Fonts.MEDIUM).getFont(7);
      float size = 8, pad = tabbed ? 6 : old ? 8 : 7, titleWidth = font.width(title);
      boolean showIcon = icon.isEnabled() && HudVisualScope.icons();
      float groupWidth = titleWidth + (showIcon && !edge.isSelected() ? size + 4 : 0);
      float available = width - 2 * pad - (showIcon && edge.isSelected() ? size + 4 : 0);
      float start = center.isSelected() ? x + (width - groupWidth) / 2
         : right.isSelected() ? x + pad + available - groupWidth : x + pad;
      start = Math.clamp(start, x + pad, Math.max(x + pad, x + pad + available - groupWidth));
      float textX = start + (showIcon && before.isSelected() ? size + 4 : 0);
      c.drawText(font, title, textX, y + GuiUtility.getMiddleOfBox(font.height(), old ? 16 : 18) + .5F, HudTheme.textColor());
      if (showIcon) {
         float iconX = edge.isSelected() ? x + width - pad - size : before.isSelected() ? start : start + titleWidth + 4;
         c.drawTexture(BKClientInit.id(texture), iconX, y + (old ? 4 : 6), size, size,
            old ? Colors.getAccent() : HudTheme.textColor());
      }
   }
}
