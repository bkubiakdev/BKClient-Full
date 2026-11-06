package dev.bkclient.ui.menu.clickgui.content.config;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.ui.components.SettingControls;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

import java.text.SimpleDateFormat;
import java.util.Date;

/** A Liquid-style config row with the same actions as BK-Client's config card. */
public final class ConfigEntryCard {
   public static final float HEIGHT = 30.0F;
   private static final float MENU_WIDTH = 100.0F;
   private static final float MENU_PADDING = 5.0F;
   private static final float MENU_ROW_HEIGHT = 19.0F;
   private static final float MENU_HEIGHT = MENU_PADDING * 2.0F + MENU_ROW_HEIGHT * 6.0F + 1.0F;
   private static final String[] ACTIONS = {"Rename", "Load", "Save", "Public", "Private", "Delete"};
   private static final String[] ACTION_ICONS = {"icons/library/rename.png", "icons/library/load.png", "icons/library/save.png",
      "icons/library/public.png", "icons/library/private.png", "icons/library/delete.png"};

   private final String name;
   private long modified;
   private boolean active;
   private final Runnable onActivate;
   private final Runnable onRename;
   private final Runnable onLoad;
   private final Runnable onSave;
   private final Runnable onPublicShare;
   private final Runnable onPrivateShare;
   private final Runnable onDelete;
   private final Animation activeAnimation = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation menuAnimation = new Animation(160L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hoverAnimation = new Animation(150L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation pressAnimation = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
   private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

   private boolean menuOpen;
   private Runnable onAirDrop;
   public void setOnAirDrop(Runnable action) { this.onAirDrop = action; }
   private boolean pressed;
   private boolean visible;
   private float x;
   private float y;
   private float width;
   private float panelBottom = Float.MAX_VALUE;
   private float panelTop;
   private float menuButtonX;
   private float menuButtonY;
   private float toggleX;
   private float toggleY;

   public ConfigEntryCard(String name, long modified, boolean active, Runnable onActivate, Runnable onRename,
                          Runnable onLoad, Runnable onSave, Runnable onPublicShare, Runnable onPrivateShare,
                          Runnable onDelete) {
      this.name = name;
      this.modified = modified;
      this.active = active;
      this.onActivate = onActivate;
      this.onRename = onRename;
      this.onLoad = onLoad;
      this.onSave = onSave;
      this.onPublicShare = onPublicShare;
      this.onPrivateShare = onPrivateShare;
      this.onDelete = onDelete;
      this.activeAnimation.setValue(active ? 1.0F : 0.0F);
   }

   public void render(UIContext context, float x, float y, float width, float alpha) {
      this.visible = true;
      this.x = x;
      this.y = y;
      this.width = width;
      this.activeAnimation.update(this.active ? 1.0F : 0.0F);
      this.menuAnimation.update(this.menuOpen ? 1.0F : 0.0F);
      boolean hovered = hit(context.getMouseX(), context.getMouseY(), x, y, width, HEIGHT);
      this.hoverAnimation.update(hovered);
      this.pressAnimation.update(this.pressed);
      boolean dark = ClickGuiV2Theme.active() ? ClickGuiV2Theme.current().dark : BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
         && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
      ColorRGBA cardColor = dark ? ColorRGBA.WHITE.withAlpha(12.0F * this.hoverAnimation.getValue() * alpha)
         : ColorRGBA.BLACK.withAlpha(5.0F * this.hoverAnimation.getValue() * alpha);
      context.drawSquircle(x, y, width, HEIGHT, 4.0F, BorderRadius.all(8.0F), cardColor);

      float centerY = y + HEIGHT / 2.0F;
      float textX = x + 40.0F;
      Font nameFont = Fonts.MEDIUM.getFont(7.0F);
      Font dateFont = Fonts.MEDIUM.getFont(6.0F);
      context.drawText(nameFont, trim(name, nameFont, Math.max(20.0F, width - (onAirDrop == null ? 82.0F : 104.0F))), textX,
         centerY - nameFont.height() - 1.0F, Colors.getTextColor().withAlpha(225.0F * alpha));
      context.drawText(dateFont, this.dateFormat.format(new Date(this.modified)), textX, centerY + 2.0F,
         Colors.getTextColor().withAlpha(82.0F * alpha));

      this.toggleX = x + 7.0F;
      this.toggleY = centerY - 5.0F;
      SettingControls.toggle(context, this.toggleX, this.toggleY, this.activeAnimation.getValue(), this.pressAnimation.getValue(), alpha);

      this.menuButtonX = x + width - 24.0F;
      this.menuButtonY = centerY - 8.0F;
      if (onAirDrop != null) {
         boolean airHover = hit(context.getMouseX(), context.getMouseY(), x + width - 47, centerY - 9, 20, 18);
         context.drawTextureSmooth(BKClientInit.id("icons/airdrop-receive.png"), x + width - 44, centerY - 6, 12, 12,
            (airHover ? Colors.getAccent() : Colors.getTextColor()).mulAlpha(alpha));
         if (airHover) CursorUtility.set(CursorType.HAND);
      }
      boolean menuHovered = hit(context.getMouseX(), context.getMouseY(), this.menuButtonX, this.menuButtonY, 18.0F, 16.0F);
      if (menuHovered || hit(context.getMouseX(), context.getMouseY(), this.toggleX - 2.0F, this.toggleY - 3.0F, 30.0F, 16.0F)) {
         CursorUtility.set(CursorType.HAND);
      }
      if (menuHovered || this.menuOpen) {
         context.drawRoundedRect(this.menuButtonX, this.menuButtonY, 18.0F, 16.0F, BorderRadius.all(5.0F),
            Colors.getTextColor().withAlpha(10.0F * alpha));
      }
      Font dots = Fonts.SEMIBOLD.getFont(7.0F);
      context.drawCenteredText(dots, "...", this.menuButtonX + 9.0F,
         this.menuButtonY + (16.0F - dots.height()) / 2.0F - 1.0F,
         Colors.getTextColor().withAlpha((menuHovered ? 190.0F : 105.0F) * alpha));
   }

   public void renderMenu(UIContext context, float alpha) {
      float visibility = this.menuAnimation.getValue() * alpha;
      if (visibility <= 0.002F) return;
      float px = this.x + this.width - MENU_WIDTH;
      float py = menuY();
      context.drawClientRect(px, py, MENU_WIDTH, MENU_HEIGHT, visibility, 0.0F, 7.0F);

      Font font = Fonts.REGULAR.getFont(8.0F);
      for (int i = 0; i < ACTIONS.length; i++) {
         float rowY = py + MENU_PADDING + i * MENU_ROW_HEIGHT;
         boolean hovered = hit(context.getMouseX(), context.getMouseY(), px + 3.0F, rowY, MENU_WIDTH - 6.0F, MENU_ROW_HEIGHT);
         if (hovered) {
            CursorUtility.set(CursorType.HAND);
            context.drawRoundedRect(px + 3.0F, rowY, MENU_WIDTH - 6.0F, MENU_ROW_HEIGHT,
               BorderRadius.all(4.0F), Colors.getTextColor().withAlpha(10.0F * visibility));
         }
         ColorRGBA color = i == ACTIONS.length - 1 ? new ColorRGBA(255.0F, 83.0F, 93.0F) : Colors.getTextColor();
         context.drawText(font, ACTIONS[i], px + 24.0F, rowY + (MENU_ROW_HEIGHT - font.height()) / 2.0F,
            color.withAlpha((hovered ? 245.0F : 155.0F) * visibility));
         float iconSize = 10.0F;
         context.drawTextureSmooth(BKClientInit.id(ACTION_ICONS[i]), px + 8.0F,
            rowY + (MENU_ROW_HEIGHT - iconSize) / 2.0F, iconSize, iconSize,
            color.withAlpha((hovered ? 235.0F : 165.0F) * visibility));
         if (i == 4) {
            context.drawRect(px + 7.0F, rowY + MENU_ROW_HEIGHT, MENU_WIDTH - 14.0F, 0.5F,
               Colors.getSeparatorColor().mulAlpha(visibility));
         }
      }
   }

   public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
      if (button != MouseButton.LEFT && button != MouseButton.RIGHT) return false;
      if (!this.visible && !this.menuOpen) return false;
      if (!menuOpen && onAirDrop != null && button == MouseButton.LEFT && hit(mouseX, mouseY, x + width - 47, y + 6, 20, 18)) {
         onAirDrop.run(); return true;
      }
      if (this.menuOpen) {
         float px = this.x + this.width - MENU_WIDTH;
         float py = menuY();
         if (hit(mouseX, mouseY, px, py, MENU_WIDTH, MENU_HEIGHT)) {
            int row = (int)Math.floor((mouseY - py - MENU_PADDING) / MENU_ROW_HEIGHT);
            if (row < 0 || row >= ACTIONS.length) return true;
            runAction(row);
            this.menuOpen = false;
            return true;
         }
         this.menuOpen = false;
         return false;
      }
      if (button == MouseButton.RIGHT && hit(mouseX, mouseY, this.x, this.y, this.width, HEIGHT)) {
         this.menuOpen = true;
         return true;
      }
      if (button == MouseButton.LEFT && hit(mouseX, mouseY, this.menuButtonX, this.menuButtonY, 18.0F, 16.0F)) {
         this.menuOpen = true;
         return true;
      }
      if (button == MouseButton.LEFT && hit(mouseX, mouseY, this.toggleX - 2.0F, this.toggleY - 3.0F, 30.0F, 16.0F)) {
         this.pressed = true;
         if (this.onActivate != null) this.onActivate.run();
         return true;
      }
      return false;
   }

   private void runAction(int row) {
      Runnable action = switch (row) {
         case 0 -> this.onRename;
         case 1 -> this.onLoad;
         case 2 -> this.onSave;
         case 3 -> this.onPublicShare;
         case 4 -> this.onPrivateShare;
         default -> this.onDelete;
      };
      if (action != null) action.run();
   }

   public void handleRelease() {
      if (!this.pressed) return;
      this.pressed = false;
   }

   private float menuY() {
      float below = this.y + HEIGHT + 2.0F;
      float preferred = below + MENU_HEIGHT > this.panelBottom ? this.y - MENU_HEIGHT - 2.0F : below;
      return Math.max(this.panelTop, Math.min(preferred, this.panelBottom - MENU_HEIGHT));
   }

   private static String trim(String text, Font font, float maxWidth) {
      if (font.width(text) <= maxWidth) return text;
      String value = text;
      while (!value.isEmpty() && font.width(value + "...") > maxWidth) value = value.substring(0, value.length() - 1);
      return value + "...";
   }

   private static boolean hit(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public void setPanelBounds(float panelTop, float panelBottom) {
      this.panelTop = panelTop;
      this.panelBottom = panelBottom;
   }

   public void beginFrame() {
      this.visible = false;
   }

   public void setActive(boolean active) {
      this.active = active;
   }

   public void setModified(long modified) {
      this.modified = modified;
   }

   public String getName() {
      return this.name;
   }

   public boolean isMenuOpen() {
      return this.menuOpen;
   }

   public boolean shouldRenderMenu() {
      return this.menuOpen || this.menuAnimation.getValue() > 0.002F;
   }

   public void setMenuOpen(boolean menuOpen) {
      this.menuOpen = menuOpen;
   }
}
