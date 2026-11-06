package dev.bkclient.protection.client;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.modules.modules.bkmods.automarket.AutoMarketScanner;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;
/** Per-screen state; keep business logic outside the mixin transformation boundary. */
public final class HandledScreenLogic implements IMinecraft {
   private net.minecraft.item.ItemStack[] bkclient$autoMarketStacks;

   private AutoMarketScanner.ScanResult[] bkclient$autoMarketResults;

   private int bkclient$autoMarketSyncId = -1;

   private long bkclient$autoMarketPriceVersion = -1L;

   public void render(DrawContext context, net.minecraft.text.Text title, int x, int y) {
      AutoMarket autoMarket = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
      if (autoMarket != null && autoMarket.isEnabled() && autoMarket.getAutoMarketController() != null
         && mc.player != null && mc.player.currentScreenHandler != null) {
         net.minecraft.screen.ScreenHandler handler = mc.player.currentScreenHandler;
         DefaultedList<Slot> slots = handler.slots;
         long priceVersion = autoMarket.getAutoMarketController().getPriceVersion();
         boolean priceChanged = priceVersion != this.bkclient$autoMarketPriceVersion;

         java.util.ArrayList<Slot> slotSnapshot = new java.util.ArrayList<>(slots);
         autoMarket.getAutoMarketController().onScreenRender(
            title, handler, slotSnapshot
         );
         autoMarket.getLobbyController().onScreenRender(
            title, handler, slotSnapshot
         );

         // With highlighting disabled, the purchase scanner still owns its matches.
         // Do not scan/copy every item just to draw an invisible decoration.
         boolean showHighlights = !autoMarket.highlightMode.is(autoMarket.highlightNone);
         if (!showHighlights && !autoMarket.getAutoMarketController().isActive()) {
            this.bkclient$autoMarketStacks = null;
            this.bkclient$autoMarketResults = null;
            autoMarket.getAutoMarketController().getCurrentMatches().clear();
         } else {
            if (handler.syncId != this.bkclient$autoMarketSyncId
               || this.bkclient$autoMarketStacks == null
               || this.bkclient$autoMarketStacks.length != slots.size()
               || priceChanged) {
               this.bkclient$autoMarketStacks = new net.minecraft.item.ItemStack[slots.size()];
               this.bkclient$autoMarketResults = new AutoMarketScanner.ScanResult[slots.size()];
               this.bkclient$autoMarketSyncId = handler.syncId;
               this.bkclient$autoMarketPriceVersion = priceVersion;
            }

            for (Slot slot : slots) {
               if (slot.inventory == mc.player.getInventory() || !slot.isEnabled()) continue;
               if (slot.id < 0 || slot.id >= this.bkclient$autoMarketStacks.length) continue;

               net.minecraft.item.ItemStack stack = slot.getStack();
               if (stack.isEmpty()) {
                  this.bkclient$autoMarketResults[slot.id] = null;
                  this.bkclient$autoMarketStacks[slot.id] = null;
                  autoMarket.getAutoMarketController().getCurrentMatches().remove(slot.id);
                  continue;
               }

               boolean changed = this.bkclient$autoMarketStacks[slot.id] == null
                  || !net.minecraft.item.ItemStack.areEqual(this.bkclient$autoMarketStacks[slot.id], stack);
               if (changed || priceChanged) {
                  this.bkclient$autoMarketResults[slot.id] = autoMarket.getAutoMarketController().getScanner()
                     .evaluateSlot(slot, autoMarket.getAutoMarketController().getEntryMap(), false);
                  this.bkclient$autoMarketStacks[slot.id] = stack.copy();
                  if (this.bkclient$autoMarketResults[slot.id] != null
                     && this.bkclient$autoMarketResults[slot.id].matched) {
                     autoMarket.getAutoMarketController().getCurrentMatches().add(slot.id);
                  } else {
                     autoMarket.getAutoMarketController().getCurrentMatches().remove(slot.id);
                  }
               }

               AutoMarketScanner.ScanResult result = this.bkclient$autoMarketResults[slot.id];
               if (showHighlights && result != null && result.matched) {
                  ColorRGBA color = result.cheap ? autoMarket.cheapColor.getColor() : autoMarket.expensiveColor.getColor();
                  this.bkclient$drawAutoMarketHighlight(context, x + slot.x, y + slot.y, color, autoMarket.highlightMode.is(autoMarket.highlightFill));
               }
            }
         }
      }

   }

   private void bkclient$drawAutoMarketHighlight(DrawContext context, int sx, int sy, ColorRGBA color, boolean fill) {
      dev.bkclient.ui.components.SlotHighlight.draw(context, sx, sy,
         fill ? color.withAlpha(90.0F).getRGB() : 0, color.withAlpha(255.0F).getRGB());
   }

   public boolean consumeClick(java.util.function.Predicate<Slot> hoveredSlot) {
      if (mc == null || mc.player == null) return false;
      long handle = mc.getWindow().getHandle();
      boolean ctrl = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL)
         || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_CONTROL);
      if (!ctrl) return false;

      AutoMarket autoMarket = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
      if (autoMarket == null || !autoMarket.isEnabled() || autoMarket.getAutoMarketController() == null) return false;

      Slot hovered = null;
      for (Slot slot : mc.player.currentScreenHandler.slots) {
         if (slot != null && slot.hasStack() && hoveredSlot.test(slot)) {
            hovered = slot;
            break;
         }
      }
      if (hovered == null) return false;

      boolean globalUnit = autoMarket.unitModeGlobal.is(autoMarket.unitModeVal);
      java.util.List<String> lines = autoMarket.getAutoMarketController().getScanner().debugCompare(
         hovered.getStack(), autoMarket.getAutoMarketController().getEntryMap(), globalUnit
      );
      for (String line : lines) {
         mc.player.sendMessage(net.minecraft.text.Text.literal(line), false);
      }
      return true;
   }

}
