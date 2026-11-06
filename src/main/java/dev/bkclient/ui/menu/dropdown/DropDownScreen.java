package dev.bkclient.ui.menu.dropdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.components.animated.AnimatedText;
import dev.bkclient.ui.components.textfield.FieldAction;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.ui.menu.api.MenuCategory;
import dev.bkclient.ui.menu.dropdown.components.MenuPanel;
import dev.bkclient.ui.menu.dropdown.components.module.ModuleComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.render.batching.impl.FontBatching;
import dev.bkclient.utility.render.batching.impl.IconBatching;
import dev.bkclient.utility.render.batching.impl.RectBatching;
import dev.bkclient.utility.sounds.ClientSounds;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;

public class DropDownScreen extends MenuScreen implements IMinecraft {
   private final Animation searchAnimation = new Animation(300L, Easing.BAKEK);
   private final Animation appendingAnim = new Animation(300L, Easing.BAKEK);
   private boolean closing;
   private List<MenuPanel> panels = new ArrayList<>();
   private float panelWidth;
   private float panelHeight;
   private String desc = "";
   private AnimatedText descText;
   private final List<ColorPicker> colorPickers = new ArrayList<>();
    private TextField searchField;

   protected void init() {
      this.descText = new AnimatedText(Fonts.REGULAR.getFont(10.0F), 10.0F, 300L, Easing.BAKEK).centered();
      this.closing = false;
      this.panelWidth = 115.0F;
      this.panelHeight = 240.0F;
      this.panels = Arrays.stream(MenuCategory.values()).map(MenuPanel::new).toList();

      for (MenuPanel panel : this.panels) {
         panel.setWidth(this.panelWidth);
         panel.setHeight(this.panelHeight);
         panel.onInit();
      }

      for (MenuCategory category : MenuCategory.values()) {
         category.initSpriteAnimation();
      }

      this.searchField = new TextField(Fonts.REGULAR.getFont(12.0F));
      HashMap<String, FieldAction> append = new HashMap<>();

      for (Module module : BKClientInit.getInstance().getModuleManager().getModules()) {
         FieldAction action = new FieldAction(
            module::toggle,
            () -> this.panels
               .forEach(panelx -> panelx.getModuleComponents().stream().filter(component -> component.getModule() == module).forEach(ModuleComponent::open))
         );
         append.put(module.getName().replace(" ", ""), action);
         append.put(module.getName(), action);
      }

      this.searchField.setAppend(append);
      super.init();
   }

   public void tick() {
      this.handleMovementKeys();
      super.tick();
   }

   @Override
   public void render(UIContext context) {
      this.menuAnimation.setEasing(Easing.LINEAR);
      this.menuAnimation.update(this.isClosing() ? 0.0F : 1.0F);
      this.menuAnimation.setDuration(this.isClosing() ? 300L : 500L);
      this.desc = "";
      float spacing = 10.0F;
      float x = (this.width - (this.panelWidth + spacing) * this.panels.size() + spacing) / 2.0F;
      float y = (this.height - this.panelHeight) / 2.0F;
      context.pushMatrix();
      float offset = 0.0F;

      for (MenuPanel menuPanel : this.panels) {
         menuPanel.setX(
            MathUtility.interpolate(x + offset, this.width / 2.0F - this.panelWidth / 2.0F, this.closing ? 1.0F - this.menuAnimation.getValue() : 0.0)
         );
         menuPanel.setY(y);
         menuPanel.setWidth(this.panelWidth);
         menuPanel.setHeight(this.panelHeight);
         offset += this.panelWidth + spacing;
      }

      for (MenuPanel menuPanel : this.panels) {
         menuPanel.renderBlur(context);
      }

      for (MenuPanel menuPanel : this.panels) {
         menuPanel.render(context);
      }

      IconBatching icon = new IconBatching(VertexFormats.POSITION_TEXTURE_COLOR, context.getMatrices());

      for (MenuPanel panel : this.panels) {
         panel.drawType(context);
      }

      icon.draw();

      for (MenuPanel panel : this.panels) {
         this.scissor(
            context,
            panel,
            () -> {
               FontBatching font = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, Fonts.REGULAR);
               panel.drawRegular8(context);
               font.draw();
               IconBatching icon1 = new IconBatching(VertexFormats.POSITION_TEXTURE_COLOR, context.getMatrices());
               panel.drawIcons(context);
               icon1.draw();
               RectBatching split = new RectBatching(VertexFormats.POSITION_COLOR, context.getMatrices());
               if (BKClientInit.getInstance().getThemeManager().getCurrentTheme() == null
                  || BKClientInit.getInstance().getThemeManager().getCurrentTheme().isSeparators()) {
                  panel.drawSplit(context);
               }

               split.draw();
            }
         );
      }

      context.popMatrix();
      if (this.menuAnimation.getValue() < 0.5F) {
         this.desc = "";
      }

      this.searchAnimation.update(this.searchField.isFocused());
      float f = this.menuAnimation.getValue() * this.searchAnimation.getValue();
      if (f > 0.0F) {
         if (Interface.showMinimalizm()) {
            context.drawBlurredRect(
               this.searchField.getX(),
               this.searchField.getY(),
               this.searchField.getWidth(),
               this.searchField.getHeight(),
               45.0F,
               BorderRadius.all(6.0F),
               ColorRGBA.WHITE.withAlpha(255.0F * f)
            );
         }

         if (Interface.showGlass()) {
            context.drawLiquidGlass(
               this.searchField.getX(),
               this.searchField.getY(),
               this.searchField.getWidth(),
               this.searchField.getHeight(),
               2.0F,
               0.08F,
               BorderRadius.all(6.0F),
               ColorRGBA.WHITE.withAlpha(255.0F * f)
            );
         }

         boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
            && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
          float glassOverlay = 0.2F;
         float searchFillAlpha = dark ? 0.9F - (0.9F - glassOverlay) * Interface.glass() : 0.7F;
         context.drawRoundedRect(
            this.searchField.getX(),
            this.searchField.getY(),
            this.searchField.getWidth(),
            this.searchField.getHeight(),
            BorderRadius.all(6.0F),
            Colors.getBackgroundColor().withAlpha(255.0F * searchFillAlpha * f)
         );
         this.searchField.set(this.width / 2.0F - this.searchField.getWidth() / 2.0F, this.height - 20 - 20.0F * f, 100.0F, 20.0F);
         this.searchField.setAlpha(f);
         this.searchField.setTextColor(Colors.getTextColor());
         this.searchField.render(context);
         this.appendingAnim.update(!this.searchField.getAppending().isBlank());
         context.drawCenteredText(
            Fonts.MEDIUM.getFont(11.0F),
            Localizator.translate("search.tooltip.tab"),
            this.width / 2.0F,
            this.height - 65 - 10.0F * f * this.appendingAnim.getValue(),
            ColorRGBA.WHITE.withAlpha(150.0F * f * this.appendingAnim.getValue())
         );
         context.drawCenteredText(
            Fonts.MEDIUM.getFont(11.0F),
            Localizator.translate("search.tooltip.enter"),
            this.width / 2.0F,
            this.height - 50 - 10.0F * f * this.appendingAnim.getValue(),
            ColorRGBA.WHITE.withAlpha(150.0F * f * this.appendingAnim.getValue())
         );
      } else {
         this.searchField.clear();
      }

      context.drawCenteredText(
         Fonts.MEDIUM.getFont(11.0F),
         Localizator.translate("search.tooltip"),
         this.width / 2.0F,
         this.height - 20 - 10.0F * this.menuAnimation.getValue() * (1.0F - this.searchAnimation.getValue()),
         ColorRGBA.WHITE.withAlpha(150.0F * this.menuAnimation.getValue() * (1.0F - this.searchAnimation.getValue()))
      );
      this.descText.pos(this.width / 2.0F, this.height / 2.0F - 150.0F);
      if (!this.desc.contains(".description")) {
         this.descText.update(this.desc);
         this.descText.render(context);
      }

      if (this.menuAnimation.getValue() > 0.0F) {
      }

      for (ColorPicker colorPicker : this.colorPickers) {
         colorPicker.render(context);
         if (!(mc.currentScreen instanceof DropDownScreen)) {
            colorPicker.setShowing(false);
         }
      }

      this.colorPickers.removeIf(popup -> popup.getAnimation().getValue() == 0.0F && !popup.isShowing());
   }

   private void handleMovementKeys() {
      if (dev.bkclient.ui.components.textfield.TextField.hasFocusedField()) {
         dev.bkclient.ui.components.textfield.TextField.releaseMovementKeys(); return;
      }
      if (mc.player != null && !this.isTyping()) {
         long windowHandle = mc.getWindow().getHandle();

         for (KeyBinding key : new KeyBinding[]{
            mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey
         }) {
            int keyCode = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode();
            key.setPressed(InputUtil.isKeyPressed(windowHandle, keyCode));
         }

         if (mc.player.getAbilities().flying) {
            int keyCode = InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode();
            mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(windowHandle, keyCode));
         }
      }
   }

   private boolean isTyping() {
      return mc.currentScreen != null && TextField.LAST_FIELD != null && TextField.LAST_FIELD.isFocused();
   }

   public boolean isBindingModule() {
      return this.panels.stream().flatMap(panel -> panel.getModuleComponents().stream()).anyMatch(ModuleComponent::isBindingMode);
   }

   private void scissor(UIContext context, MenuPanel panel, Runnable runnable) {
      panel.scale(context);
      panel.push(context);
      runnable.run();
      ScissorUtility.pop();
      RenderUtility.end(context.getMatrices());
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (!BKClientInit.getInstance().getHud().getIsland().handleClick((float)mouseX, (float)mouseY, button.getButtonIndex())) {
         for (ColorPicker colorPicker : this.colorPickers) {
            boolean isPick = colorPicker.isPick();
            colorPicker.onMouseClicked(mouseX, mouseY, button);
            if (colorPicker.isHovered(mouseX, mouseY) || isPick) {
               return;
            }

            colorPicker.setShowing(false);
         }

         for (MenuPanel panel : this.panels) {
            if (panel.isHovered(mouseX, mouseY) || panel.hasOpenDropdown()) {
               panel.onMouseClicked(mouseX, mouseY, button);
            }
         }

         if (this.searchField.isFocused() && button != MouseButton.MIDDLE) {
            this.searchField.onMouseClicked(mouseX, mouseY, button);
         }

         super.onMouseClicked(mouseX, mouseY, button);
      }
   }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {

      for (ColorPicker colorPicker : this.colorPickers) {
         colorPicker.onMouseReleased(mouseX, mouseY, button);
      }

      for (MenuPanel panel : this.panels) {
         panel.onMouseReleased(mouseX, mouseY, button);
      }

      if (this.searchField.isFocused()) {
         this.searchField.onMouseReleased(mouseX, mouseY, button);
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      for (MenuPanel panel : this.panels) {
         panel.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      for (ColorPicker colorPicker : this.colorPickers) {
         colorPicker.onKeyPressed(keyCode, scanCode, modifiers);
      }

      if (this.searchField != null && !this.searchField.isFocused() && Screen.hasControlDown() && keyCode == 70) {
         this.searchField.setFocused(true);
      }

      for (MenuPanel panel : this.panels) {
         panel.onKeyPressed(keyCode, scanCode, modifiers);
      }

      if (this.searchField.isFocused() && !this.isBindingModule()) {
         this.searchField.onKeyPressed(keyCode, scanCode, modifiers);
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.searchField.isFocused() && !this.isBindingModule()) {
         this.searchField.charTyped(chr, modifiers);
      }

      for (MenuPanel panel : this.panels) {
         panel.charTyped(chr, modifiers);
      }

      return super.charTyped(chr, modifiers);
   }

   public void close() {
      this.closing = true;
      BKClientInit.getInstance().getModuleManager().getModule(MenuModule.class).disable();
      Sounds soundsModule = BKClientInit.getInstance().getModuleManager().getModule(Sounds.class);
      if (soundsModule.isEnabled()) {
         ClientSounds.CLICKGUI_OPEN.play(soundsModule.getVolume().getCurrentValue(), 1.0F);
      }

      BKClientInit.getInstance().getFileManager().writeFile("client");
      if (BKClientInit.getInstance().getConfigManager().getCurrent() != null) {
         BKClientInit.getInstance().getConfigManager().getCurrent().save();
      }

      BKClientInit.getInstance().getThemeManager().flushSave();
      if (TextField.LAST_FIELD != null) {
         TextField.LAST_FIELD.setFocused(false);
      }

      super.close();
   }

   public boolean shouldPause() {
      return false;
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   @Generated
   public Animation getSearchAnimation() {
      return this.searchAnimation;
   }

   @Generated
   public Animation getAppendingAnim() {
      return this.appendingAnim;
   }

   @Generated
   @Override
   public boolean isClosing() {
      return this.closing;
   }

   @Generated
   public List<MenuPanel> getPanels() {
      return this.panels;
   }

   @Generated
   public float getPanelWidth() {
      return this.panelWidth;
   }

   @Generated
   public float getPanelHeight() {
      return this.panelHeight;
   }

   @Generated
   public String getDesc() {
      return this.desc;
   }

   @Generated
   public AnimatedText getDescText() {
      return this.descText;
   }

   @Generated
   public List<ColorPicker> getColorPickers() {
      return this.colorPickers;
   }

   @Generated
   @Override
   public void setClosing(boolean closing) {
      this.closing = closing;
   }

   @Generated
   public void setDesc(String desc) {
      this.desc = desc;
   }

   @Generated
   public TextField getSearchField() {
      return this.searchField;
   }

}
