package dev.bkclient.ui.menu.rynek;

import static dev.bkclient.systems.localization.Localizator.translate;

import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.mixin.accessors.HandledScreenAccessor;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.NumberSetting;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import dev.bkclient.ui.components.popup.Popup;
import dev.bkclient.ui.components.popup.PopupComponent;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.inventory.ItemStackExtractor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.LoreComponent;

public class QuickAddScreen extends CustomScreen implements IScaledResolution, IMinecraft {
   private final Screen parentScreen;
   private final Popup popup;
   private final List<Setting> quickAddSettings = new ArrayList<>();

   public QuickAddScreen(Screen parentScreen, PriceEntry entry, ItemStack itemStack) {
      this.parentScreen = parentScreen;
      SettingsContainer container = () -> this.quickAddSettings;
      this.popup = new Popup(0.0F, 0.0F, 140.0F);
      this.popup.add(new QuickAddHeader(itemStack.copy()));

      new NumberSetting(container, "Buy price", entry.getMaxPrice(), false) {
         @Override
         public void setValue(double value) {
            if (!Double.isFinite(value) || value < 0.0) return;
            super.setValue(value);
            entry.setMaxPrice(value);
         }
      };
      new NumberSetting(container, "Sell price", entry.getSellPrice(), false) {
         @Override
         public void setValue(double value) {
            if (!Double.isFinite(value) || value < 0.0) return;
            super.setValue(value);
            entry.setSellPrice(value);
         }
      };
      for (Setting setting : this.quickAddSettings) {
         this.popup.setting(setting);
      }

      this.popup.separator();
      this.popup.button(translate("Add to registry"), LucideIcons.CHECK, component -> {
         entry.setBuyEnabled(true);
         entry.setSellEnabled(entry.getSellPrice() > 0.0);
         AutoMarket autoMarket = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
         if (autoMarket != null) {
            autoMarket.addEntry(entry);
         }
         String itemName = ItemStackExtractor.toReadablePlain(itemStack.getName());
         BKClientInit.getInstance().getNotificationManager().addNotificationOther(
            NotificationType.SUCCESS,
            "Auto-Market",
            translate("market.quick_add.saved", itemName)
         );
         mc.setScreen(this.parentScreen);
      });
      this.popup.button(translate("Cancel"), LucideIcons.CROSS, component -> mc.setScreen(this.parentScreen));
   }

   @Override
   public void render(UIContext context) {
      if (this.parentScreen != null) {
         this.parentScreen.render(context, -1, -1, context.getDelta());
      }
      context.drawRect(0.0F, 0.0F, sr.getScaledWidth(), sr.getScaledHeight(), Colors.BLACK.withAlpha(100.0F));

      float popupWidth = this.popup.getWidth();
      float popupHeight = Math.max(130.0F, this.popup.getHeight());
      float popupX;
      float popupY;
      if (this.parentScreen instanceof HandledScreen<?> handled) {
         HandledScreenAccessor accessor = (HandledScreenAccessor)handled;
         popupX = accessor.getX() + accessor.getBackgroundWidth() + 5.0F;
         popupY = accessor.getY() + accessor.getBackgroundHeight() / 2.0F - popupHeight / 2.0F;
         if (popupX + popupWidth > sr.getScaledWidth()) {
            popupX = sr.getScaledWidth() - popupWidth - 5.0F;
         }
         popupY = Math.clamp(popupY, 5.0F, Math.max(5.0F, sr.getScaledHeight() - popupHeight - 5.0F));
      } else {
         popupX = sr.getScaledWidth() / 2.0F - popupWidth / 2.0F;
         popupY = sr.getScaledHeight() / 2.0F - popupHeight / 2.0F;
      }

      this.popup.setX(popupX);
      this.popup.setY(popupY);
      this.popup.update(context);
      this.popup.render(context);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      this.popup.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.popup.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.popup.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         mc.setScreen(this.parentScreen);
         return true;
      }
      this.popup.onKeyPressed(keyCode, scanCode, modifiers);
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      return this.popup.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   @Override
   public void close() {
      mc.setScreen(this.parentScreen);
   }

   private static final class QuickAddHeader extends PopupComponent {
      private final ItemStack stack;
      private final List<String> loreLines = new ArrayList<>();
      private final String displayName;

      private QuickAddHeader(ItemStack stack) {
         this.stack = stack;
         this.displayName = stack.isEmpty() ? "" : ItemStackExtractor.toReadablePlain(stack.getName());
         if (!stack.isEmpty()) {
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
               for (Text line : lore.lines()) {
                  if (!ItemStackExtractor.shouldSkipLoreLine(line)) {
                     String readable = ItemStackExtractor.toReadablePlain(line);
                     if (!readable.isEmpty()) {
                        this.loreLines.add(readable);
                     }
                  }
               }
            }
         }
      }

      @Override
      protected void renderComponent(UIContext context) {
         if (!this.stack.isEmpty()) {
            context.drawItem(this.stack, this.x + 8.0F, this.y + 7.0F, 1.1F);
            context.drawText(Fonts.SEMIBOLD.getFont(8.0F), translate("Quick Add"), this.x + 30.0F, this.y + 6.0F,
               Colors.getAccent());
            context.drawFadeoutText(Fonts.MEDIUM.getFont(7.0F), this.displayName,
               this.x + 30.0F, this.y + 16.0F, Colors.getTextColor(), 0.8F, 1.0F, this.width - 35.0F);
            float loreY = this.y + 26.0F;
            for (int i = 0; i < Math.min(this.loreLines.size(), 3); i++) {
               context.drawFadeoutText(Fonts.REGULAR.getFont(6.0F), this.loreLines.get(i),
                  this.x + 30.0F, loreY, Colors.getTextColor().withAlpha(170.0F), 0.8F, 1.0F, this.width - 35.0F);
               loreY += 8.0F;
            }
         } else {
            context.drawText(Fonts.SEMIBOLD.getFont(10.0F), translate("Quick Add"), this.x + 10.0F, this.y + 10.0F,
               Colors.getTextColor());
         }
         context.drawRect(this.x, this.y + this.height - 0.5F, this.width, 0.5F,
            Colors.getTextColor().withAlpha(15.0F));
      }

      @Override
      public float getHeight() {
         return this.loreLines.isEmpty() ? 32.0F : 32.0F + Math.min(this.loreLines.size(), 3) * 8.0F;
      }
   }
}
