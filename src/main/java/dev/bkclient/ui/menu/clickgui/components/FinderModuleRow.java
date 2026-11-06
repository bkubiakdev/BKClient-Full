package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.components.SettingControls;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.TextUtility;

/** Compact V2 module tile with the shared native switch and settings inspector. */
public final class FinderModuleRow extends ClickGuiModuleCard {
   public static final float HEIGHT = 27;
   private final java.util.function.Consumer<ClickGuiModuleCard> opener;
   private final Animation enabled = new Animation(180L, Easing.QUARTIC_OUT);

   public FinderModuleRow(Module module, java.util.function.Consumer<ClickGuiModuleCard> opener) {
      super(module, opener);
      this.opener = opener;
      this.height = HEIGHT;
      enabled.setValue(module.isEnabled() ? 1 : 0);
   }

   @Override public float getHeight() { return HEIGHT; }

   @Override protected void renderComponent(UIContext c) {
      var p = ClickGuiV2Theme.current();
      var module = getModule();
      enabled.update(module.isEnabled());
      ColorRGBA surface = p.dark ? new ColorRGBA(39, 39, 42) : ColorRGBA.WHITE;
      if (isHovered(c)) surface = surface.mix(p.text, .035F);
      c.drawRoundedRect(x, y, width, HEIGHT, BorderRadius.all(6), surface);
      LucideIcons.COPY.draw(c, x+7, y+9, 9, p.text.mulAlpha(.85F));
      float textWidth = Math.max(12, width-91);
      c.drawFadeoutText(Fonts.MEDIUM.getFont(6.3F), module.getName(), x+22, y+6, p.text, .9F, 1, textWidth);
      String description = module.getDescription();
      if (description == null || description.startsWith("modules.descriptions.")) description = "Module settings";
      c.drawFadeoutText(Fonts.REGULAR.getFont(5.2F), description, x+22, y+15, p.muted, .85F, 1, textWidth);
      String key = isBinding() ? "..." : module.getKey()==-1 ? "NONE" : TextUtility.getKeyName(module.getKey());
      c.drawFadeoutText(Fonts.MEDIUM.getFont(5), key, x+width-65, y+11, p.muted, .9F, 1, 23);
      LucideIcons.MORE.draw(c, x+width-40, y+10, 8, p.text.mulAlpha(.8F));
      SettingControls.toggle(c, x+width-29, y+8.5F, enabled.getValue(), 0, 1);
   }

   @Override public void onCardMouseClicked(double mx, double my, MouseButton button) {
      if (!isHovered(mx, my)) return;
      if (button == MouseButton.MIDDLE || (button == MouseButton.LEFT && mx >= x+width-68 && mx < x+width-42)) {
         super.onCardMouseClicked(x+25, y+12, MouseButton.MIDDLE);
      } else if (button == MouseButton.LEFT && mx >= x+width-30) {
         getModule().toggle();
      } else if (button == MouseButton.LEFT || button == MouseButton.RIGHT) {
         opener.accept(this);
      }
   }
}
