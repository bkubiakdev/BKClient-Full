package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Language;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.RenderUtility;
import net.minecraft.util.Identifier;

import java.util.List;

/** Small corner launcher with a self-contained Liquid account/settings card. */
public final class CornerSettingsPanel implements IMinecraft {
   private static final float TRIGGER_SIZE = 20.0F;
   private static final float PANEL_WIDTH = 176.0F;
   private static final float PANEL_HEIGHT = 225.0F;
   private static final float HEADER_HEIGHT = 40.0F;
   private static final float ROW_HEIGHT = 21.0F;
   private static final long SESSION_STARTED_AT = System.currentTimeMillis();

   private static final String[] ICONS = {
      "language", "menu_scale", "quality", "friends", "sync", "discord"
   };
   private static final String[] LABELS = {
      "Language", "GUI Scale", "Render Quality", "Friends", "Sync", "Discord RPC"
   };

   private final Animation openAnimation = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation triggerHover = new Animation(180L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation syncAnimation = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation discordAnimation = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation[] rowHover = new Animation[LABELS.length];
   private boolean open;
   private boolean friendsView;
   private final Animation presenceAnimation = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation serverAnimation = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation friendsAnimation = new Animation(240L, Easing.FIGMA_EASE_IN_OUT);
   private final dev.bkclient.utility.gui.ScrollHandler friendsScroll = new dev.bkclient.utility.gui.ScrollHandler();
   private GuiScalePicker scalePicker;
   private float triggerX;
   private float triggerY;
   private float panelX;
   private float panelY;

   public CornerSettingsPanel() {
      for (int i = 0; i < this.rowHover.length; ++i) {
         this.rowHover[i] = new Animation(160L, Easing.FIGMA_EASE_IN_OUT);
      }
   }

   public void reset() {
      this.open = false;
      this.friendsView = false;
      this.friendsAnimation.setValue(0);
      this.friendsScroll.reset();
      this.scalePicker = null;
      this.openAnimation.setValue(0.0F);
   }

   public void renderScalePicker(UIContext context, float alpha) {
      if (this.scalePicker == null) return;
      this.scalePicker.render(context, alpha);
      if (this.scalePicker.isDead()) {
         this.scalePicker = null;
      }
   }

   public boolean hasScalePicker() {
      return this.scalePicker != null && !this.scalePicker.isDead();
   }

   public boolean onScalePickerClicked(double mouseX, double mouseY, MouseButton button) {
      return this.scalePicker != null && this.scalePicker.onClick(mouseX, mouseY, button);
   }

   public boolean onScalePickerScroll(double verticalAmount) {
      return this.scalePicker != null && this.scalePicker.onScroll(verticalAmount);
   }

   public boolean onScalePickerKeyPressed(int keyCode) {
      return this.scalePicker != null && this.scalePicker.onKeyPressed(keyCode);
   }

   public void render(UIContext context, float windowX, float bottomY, float alpha) {
      this.triggerX = windowX + 8.0F;
      this.triggerY = bottomY + 7.0F;
      this.panelX = windowX + 8.0F;
      this.panelY = this.triggerY + TRIGGER_SIZE + 2.0F;
      if (this.panelY + PANEL_HEIGHT > mc.getWindow().getScaledHeight() - 6.0F) {
         this.panelY = Math.max(6.0F, this.triggerY - PANEL_HEIGHT - 6.0F);
      }

      this.drawTrigger(context, alpha);
      float progress = this.openAnimation.update(this.open ? 1.0F : 0.0F);
      if (progress <= 0.005F) return;

      float panelAlpha = alpha * progress;
      RenderUtility.scale(
         context.getMatrices(), this.panelX + 10.0F, this.panelY + 7.0F,
         0.97F + 0.03F * progress
      );
      this.drawSurface(context, panelAlpha);
      this.drawAccountHeader(context, panelAlpha);
      float friendsProgress = this.friendsAnimation.update(this.friendsView ? 1.0F : 0.0F);
      dev.bkclient.utility.render.ScissorUtility.push(context.getMatrices(), this.panelX, this.panelY + HEADER_HEIGHT + 1,
         PANEL_WIDTH, PANEL_HEIGHT - HEADER_HEIGHT - 3);
      if (friendsProgress < 0.999F) {
         context.getMatrices().push();
         context.getMatrices().translate(-12 * friendsProgress, 0, 0);
         this.drawSettings(context, panelAlpha * (1 - friendsProgress));
         context.getMatrices().pop();
      }
      if (friendsProgress > 0.001F) {
         context.getMatrices().push();
         context.getMatrices().translate(12 * (1 - friendsProgress), 0, 0);
         this.drawFriends(context, panelAlpha * friendsProgress);
         context.getMatrices().pop();
      }
      dev.bkclient.utility.render.ScissorUtility.pop();
      RenderUtility.end(context.getMatrices());
   }

   private void drawTrigger(UIContext context, float alpha) {
      boolean hovered = hovered(
         this.triggerX, this.triggerY, TRIGGER_SIZE, TRIGGER_SIZE,
         context.getMouseX(), context.getMouseY()
      );
      float hover = this.triggerHover.update(hovered ? 1.0F : 0.0F);
      BorderRadius radius = BorderRadius.all(TRIGGER_SIZE / 2.0F);
      context.drawLiquidGlass(
         this.triggerX, this.triggerY, TRIGGER_SIZE, TRIGGER_SIZE,
         2.0F, 0.08F, radius,
         ColorRGBA.WHITE.withAlpha((24.0F + hover * 28.0F) * alpha)
      );
      context.drawRoundedRect(
         this.triggerX, this.triggerY, TRIGGER_SIZE, TRIGGER_SIZE, radius,
         Colors.getBackgroundColor().withAlpha((135.0F + hover * 28.0F) * alpha)
      );
      context.drawRoundedBorder(
         this.triggerX, this.triggerY, TRIGGER_SIZE, TRIGGER_SIZE, 0.5F, radius,
         Colors.getTextColor().withAlpha((14.0F + hover * 18.0F) * alpha)
      );
      context.drawTextureSmooth(
         BKClientInit.id("textures/gui/bk-logo.png"),
         this.triggerX + 4.0F, this.triggerY + 4.0F, 12.0F, 12.0F,
         Colors.getTextColor().mulAlpha((0.72F + hover * 0.22F) * alpha)
      );

      float badgeX = this.triggerX + 12.5F;
      float badgeY = this.triggerY + 12.5F;
      context.drawRoundedRect(
         badgeX, badgeY, 7.0F, 7.0F, BorderRadius.all(3.5F),
         Colors.getAccent().withAlpha(235.0F * alpha)
      );
      context.drawTextureSmooth(
         solar("settings"), badgeX + 2.0F, badgeY + 2.0F, 4.0F, 4.0F,
         Colors.WHITE.mulAlpha(alpha)
      );
   }

   private void drawSurface(UIContext context, float alpha) {
      context.drawClientRect(this.panelX, this.panelY, PANEL_WIDTH, PANEL_HEIGHT, alpha, 0.0F, 9.0F);
   }
   private void drawAccountHeader(UIContext context, float alpha) {
      float avatarX = this.panelX + 8.0F;
      float avatarY = this.panelY + 6.0F;
      context.drawRoundedRect(
         avatarX, avatarY, 22.0F, 22.0F, BorderRadius.all(11.0F),
         Colors.getAccent().withAlpha(42.0F * alpha)
      );
      context.drawRoundedBorder(
         avatarX, avatarY, 22.0F, 22.0F, 0.6F, BorderRadius.all(11.0F),
         Colors.getAccent().withAlpha(105.0F * alpha)
      );
      context.drawTextureSmooth(
         BKClientInit.id("textures/gui/bk-logo.png"),
         avatarX + 4.0F, avatarY + 4.0F, 14.0F, 14.0F,
         Colors.getTextColor().mulAlpha(0.92F * alpha)
      );

      Font nameFont = Fonts.SEMIBOLD.getFont(7.0F);
      Font subFont = Fonts.REGULAR.getFont(5.5F);
      String username = mc.getSession() == null ? "BK-Client" : mc.getSession().getUsername();
      username = dev.bkclient.systems.modules.modules.other.NameProtect.protectDisplayText(username);
      context.drawText(
         nameFont, trim(username, nameFont, 78.0F),
         this.panelX + 36.0F, this.panelY + 9.0F,
         Colors.getTextColor().mulAlpha(0.93F * alpha)
      );
      context.drawText(
         subFont, "BK-Client · " + sessionTime(),
         this.panelX + 36.0F, this.panelY + 22.0F,
         Colors.getTextColor().mulAlpha(0.43F * alpha)
      );
      context.drawTextureSmooth(
         solar("settings"), this.panelX + PANEL_WIDTH - 17.0F, this.panelY + 13.0F,
         7.0F, 7.0F, Colors.getTextColor().mulAlpha(0.4F * alpha)
      );
      context.drawRect(
         this.panelX + 7.0F, this.panelY + HEADER_HEIGHT,
         PANEL_WIDTH - 14.0F, 0.5F,
         Colors.getTextColor().withAlpha(13.0F * alpha)
      );
   }

   private void drawSettings(UIContext context, float alpha) {
      Interface settings = settings();
      float rowsY = this.panelY + HEADER_HEIGHT + 5.0F;
      this.syncAnimation.update(settings.getSync().isEnabled() ? 1.0F : 0.0F);
      this.discordAnimation.update(settings.getDiscordRpc().isEnabled() ? 1.0F : 0.0F);

      Font labelFont = Fonts.MEDIUM.getFont(6.5F);
      Font valueFont = Fonts.MEDIUM.getFont(6.0F);
      for (int i = 0; i < LABELS.length; ++i) {
         float rowY = rowsY + i * ROW_HEIGHT;
         boolean isHovered = hovered(
            this.panelX + 5.0F, rowY, PANEL_WIDTH - 10.0F, ROW_HEIGHT,
            context.getMouseX(), context.getMouseY()
         );
         float hover = this.rowHover[i].update(isHovered ? 1.0F : 0.0F);
         if (hover > 0.001F) {
            context.drawRoundedRect(
               this.panelX + 5.0F, rowY + 1.0F, PANEL_WIDTH - 10.0F, ROW_HEIGHT - 2.0F,
               BorderRadius.all(5.0F), Colors.getTextColor().withAlpha(7.0F * hover * alpha)
            );
         }

         context.drawTextureSmooth(
            solar(ICONS[i]), this.panelX + 10.0F, rowY + (ROW_HEIGHT - 9) / 2, 9.0F, 9.0F,
            Colors.getTextColor().mulAlpha((0.65F + 0.25F * hover) * alpha)
         );
         context.drawText(
            labelFont, LABELS[i], this.panelX + 26.0F, rowY + (ROW_HEIGHT - labelFont.height()) / 2,
            Colors.getTextColor().mulAlpha((0.85F + 0.15F * hover) * alpha)
         );

         if (i == 4) {
            this.drawToggle(context, rowY, this.syncAnimation.getValue(), alpha);
         } else if (i == 5) {
            this.drawToggle(context, rowY, this.discordAnimation.getValue(), alpha);
         } else {
            String value = switch (i) {
               case 0 -> languageName();
               case 1 -> menuScaleName(settings);
               case 2 -> settings.getRenderQuality().getValue().getName();
               case 3 -> String.valueOf(BKClientInit.getInstance().getFriendManager().listFriends().size());
               default -> "";
            };
            float arrowX = this.panelX + PANEL_WIDTH - 12.0F;
            context.drawRightText(
               valueFont, value, arrowX - 7.0F, rowY + (ROW_HEIGHT - valueFont.height()) / 2,
               Colors.getTextColor().mulAlpha((0.62F + 0.25F * hover) * alpha)
            );
            context.drawTextureSmooth(
               solar("arrow_right"), arrowX, rowY + (ROW_HEIGHT - 6) / 2, 6.0F, 6.0F,
               Colors.getTextColor().mulAlpha((0.26F + 0.34F * hover) * alpha)
            );
         }
      }
   }

   private void drawFriends(UIContext context, float alpha) {
      float contentY = this.panelY + HEADER_HEIGHT + 5;
      Font font = Fonts.MEDIUM.getFont(6.5F);
      boolean hover = hovered(panelX + 5, contentY, PANEL_WIDTH - 10, ROW_HEIGHT, context.getMouseX(), context.getMouseY());
      context.drawRoundedRect(panelX + 5, contentY, PANEL_WIDTH - 10, ROW_HEIGHT, BorderRadius.all(5),
         Colors.getTextColor().withAlpha((hover ? 16 : 6) * alpha));
      context.drawTextureSmooth(solar("arrow_left"), panelX + 10, contentY + 7, 7, 7, Colors.getTextColor().mulAlpha(alpha));
      context.drawText(font, "Friends", panelX + 26, contentY + (ROW_HEIGHT - font.height()) / 2, Colors.getTextColor().mulAlpha(alpha));
      List<String> friends = BKClientInit.getInstance().getFriendManager().listFriends();
      context.drawRightText(font, String.valueOf(friends.size()), panelX + PANEL_WIDTH - 12,
         contentY + (ROW_HEIGHT - font.height()) / 2, Colors.getTextColor().mulAlpha(0.55F * alpha));
      float privacyY = contentY + ROW_HEIGHT + 2;
      boolean enabled = dev.bkclient.systems.friends.PresencePrivacy.isEnabled();
      boolean shareServer = !dev.bkclient.systems.friends.PresencePrivacy.isHidden();
      boolean polish = Localizator.getCurrentLanguage() == Language.PL_PL;
      context.drawText(font, polish ? "Obecność Friends" : "Friends presence", panelX + 10, privacyY + 7, Colors.getTextColor().mulAlpha(alpha));
      this.drawToggle(context, privacyY, this.presenceAnimation.update(enabled ? 1 : 0), alpha);
      context.drawText(font, polish ? "Pokaż serwer" : "Share server", panelX + 10, privacyY + ROW_HEIGHT + 7, Colors.getTextColor().mulAlpha((enabled ? 1 : .35F) * alpha));
      this.drawToggle(context, privacyY + ROW_HEIGHT, this.serverAnimation.update(shareServer ? 1 : 0), (enabled ? 1 : .35F) * alpha);
      String privacyStatus = !enabled ? (polish ? "Wysyłka wyłączona" : "Presence is off")
         : dev.bkclient.utility.game.ClientTelemetryUtility.isPrivacyServiceReady()
            ? (polish ? "Połączono z usługą Friends" : "Connected to Friends service")
            : (polish ? "Oczekiwanie na usługę Friends" : "Waiting for Friends service");
      context.drawText(Fonts.REGULAR.getFont(5), privacyStatus, panelX + 10, privacyY + ROW_HEIGHT * 2 + 3, Colors.getTextColor().mulAlpha(.5F * alpha));
      float listY = privacyY + ROW_HEIGHT * 2 + 14;
      float listH = panelY + PANEL_HEIGHT - 7 - listY;
      friendsScroll.setMax(Math.min(0, listH - friends.size() * 23));
      friendsScroll.update();
      if (friends.isEmpty()) {
         context.drawTextureSmooth(solar("friends"), panelX + PANEL_WIDTH / 2 - 10, listY + 16, 20, 20, Colors.getTextColor().mulAlpha(0.35F * alpha));
         context.drawCenteredText(font, "No friends yet", panelX + PANEL_WIDTH / 2, listY + 44, Colors.getTextColor().mulAlpha(0.65F * alpha));
         return;
      }
      dev.bkclient.utility.render.ScissorUtility.push(context.getMatrices(), panelX + 5, listY, PANEL_WIDTH - 10, listH);
      for (int i = 0; i < friends.size(); i++) {
         float rowY = listY + i * 23 - (float)friendsScroll.getValue();
         if (rowY + 23 < listY || rowY > listY + listH) continue;
         String name = friends.get(i);
         var entry = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerList().stream()
            .filter(p -> p.getProfile().getName().equalsIgnoreCase(name)).findFirst().orElse(null);
         if (entry != null) {
            var skin = entry.getSkinTextures().texture();
            dev.bkclient.utility.render.DrawUtility.drawPlayerHeadWithRoundedShader(context.getMatrices(), skin, panelX + 10, rowY + 5, 12, BorderRadius.all(3), Colors.WHITE.mulAlpha(alpha));
            dev.bkclient.utility.render.DrawUtility.drawPlayerHatLayerWithRoundedShader(context.getMatrices(), skin, panelX + 10, rowY + 5, 12, BorderRadius.all(3), Colors.WHITE.mulAlpha(alpha));
         } else context.drawTextureSmooth(BKClientInit.id("icons/hud/player.png"), panelX + 10, rowY + 5, 12, 12, Colors.getTextColor().mulAlpha(0.6F * alpha));
         context.drawText(font, trim(name, font, PANEL_WIDTH - 48), panelX + 29, rowY + (23 - font.height()) / 2, Colors.getTextColor().mulAlpha(0.9F * alpha));
      }
      dev.bkclient.utility.render.ScissorUtility.pop();
      if (friends.size() * 23 > listH) {
         float thumb = Math.max(14, listH * listH / (friends.size() * 23));
         float progress = Math.clamp((float)friendsScroll.getValue() / (friends.size() * 23 - listH), 0, 1);
         context.drawRoundedRect(panelX + PANEL_WIDTH - 7, listY + progress * (listH - thumb), 1.5F, thumb, BorderRadius.all(0.75F), Colors.getTextColor().mulAlpha(0.35F * alpha));
      }
   }

   public boolean onScroll(double mouseX, double mouseY, double amount) {
      if (!open || !hovered(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, mouseX, mouseY)) return false;
      if (friendsView) friendsScroll.scroll(amount);
      return true;
   }
   private void drawToggle(UIContext context, float rowY, float value, float alpha) {
      dev.bkclient.ui.components.SettingControls.toggle(context,
         this.panelX + PANEL_WIDTH - 35.0F, rowY + (ROW_HEIGHT - 10.0F) / 2.0F, value, 0.0F, alpha);
   }
   public boolean onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (hovered(this.triggerX, this.triggerY, TRIGGER_SIZE, TRIGGER_SIZE, mouseX, mouseY)) {
         if (button == MouseButton.LEFT) {
            this.open = !this.open;
            if (!this.open) this.friendsView = false;
         }
         return true;
      }
      if (!this.open) return false;

      if (!hovered(this.panelX, this.panelY, PANEL_WIDTH, PANEL_HEIGHT, mouseX, mouseY)) {
         this.open = false;
         this.friendsView = false;
         return false;
      }

      float rowsY = this.panelY + HEADER_HEIGHT + 5.0F;
      if (Math.abs(this.friendsAnimation.getValue() - (this.friendsView ? 1.0F : 0.0F)) > 0.05F) return true;
      if (this.friendsView) {
         if (button == MouseButton.LEFT && hovered(
            this.panelX + 5.0F, rowsY, PANEL_WIDTH - 10.0F, ROW_HEIGHT, mouseX, mouseY
         )) {
            this.friendsView = false;
         } else if (button == MouseButton.LEFT) {
            float privacyY = rowsY + ROW_HEIGHT + 2;
            boolean changed = false;
            if (hovered(panelX + 5, privacyY, PANEL_WIDTH - 10, ROW_HEIGHT, mouseX, mouseY)) {
               dev.bkclient.systems.friends.PresencePrivacy.setEnabled(!dev.bkclient.systems.friends.PresencePrivacy.isEnabled());
               changed = true;
            } else if (dev.bkclient.systems.friends.PresencePrivacy.isEnabled()
                  && hovered(panelX + 5, privacyY + ROW_HEIGHT, PANEL_WIDTH - 10, ROW_HEIGHT, mouseX, mouseY)) {
               dev.bkclient.systems.friends.PresencePrivacy.setHidden(!dev.bkclient.systems.friends.PresencePrivacy.isHidden());
               changed = true;
            }
            if (changed) {
               BKClientInit.getInstance().getFileManager().writeFile("client");
               dev.bkclient.utility.game.ClientTelemetryUtility.privacyChanged();
            }
         }
         return true;
      }

      int row = (int)Math.floor((mouseY - rowsY) / ROW_HEIGHT);
      if (row < 0 || row >= LABELS.length) return true;
      int direction = button == MouseButton.RIGHT ? -1 : 1;
      Interface settings = settings();
      switch (row) {
         case 0 -> cycle(settings.getLanguage(), direction);
         case 1 -> {
            if (button == MouseButton.LEFT) {
               if (this.scalePicker != null && !this.scalePicker.isDead()) {
                  this.scalePicker.close();
               } else {
                  this.scalePicker = new GuiScalePicker(settings.getMenuScale());
               }
            }
         }
         case 2 -> cycle(settings.getRenderQuality(), direction);
         case 3 -> {
            if (button == MouseButton.LEFT) { this.friendsView = true; this.friendsScroll.reset(); }
         }
         case 4 -> settings.getSync().toggle();
         case 5 -> settings.getDiscordRpc().toggle();
         default -> {
         }
      }
      return true;
   }

   private static void cycle(ModeSetting setting, int direction) {
      List<ModeSetting.Value> values = setting.getValues();
      if (values.isEmpty()) return;
      int index = values.indexOf(setting.getValue());
      setting.setValue(values.get(Math.floorMod(index + direction, values.size())));
   }

   private static void cycle(SliderSetting setting, int direction) {
      float next = setting.getCurrentValue() + setting.getStep() * direction;
      if (next > setting.getMax() + 0.001F) next = setting.getMin();
      if (next < setting.getMin() - 0.001F) next = setting.getMax();
      setting.setCurrentValue(next);
   }

   private static Interface settings() {
      return BKClientInit.getInstance().getModuleManager().getModule(Interface.class);
   }

   private static Identifier solar(String name) {
      return BKClientInit.id("textures/gui/solar/" + name + ".png");
   }

   private static String languageName() {
      Language language = Localizator.getCurrentLanguage();
      return switch (language) {
         case PL_PL -> "Polski";
         default -> "English";
      };
   }

   private static String menuScaleName(Interface settings) {
      float value = settings.getMenuScale().getCurrentValue();
      return value <= 0.0F ? "Auto" : Math.round(value) + "%";
   }

   private static String sessionTime() {
      long minutes = Math.max(0L, (System.currentTimeMillis() - SESSION_STARTED_AT) / 60_000L);
      long hours = minutes / 60L;
      return hours + "h " + minutes % 60L + "m";
   }

   private static boolean hovered(float x, float y, float width, float height, double mouseX, double mouseY) {
      return GuiUtility.isHovered(x, y, width, height, mouseX, mouseY);
   }

   private static String trim(String text, Font font, float maxWidth) {
      if (font.width(text) <= maxWidth) return text;
      String suffix = "...";
      while (!text.isEmpty() && font.width(text + suffix) > maxWidth) {
         text = text.substring(0, text.length() - 1);
      }
      return text + suffix;
   }
}
