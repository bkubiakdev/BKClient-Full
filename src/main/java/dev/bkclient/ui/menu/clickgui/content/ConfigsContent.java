package dev.bkclient.ui.menu.clickgui.content;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.config.ConfigManager;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.modules.modules.bkmods.config.AutoMarketConfigManager;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.ui.menu.clickgui.content.config.ConfigEntryCard;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.CloudConfigUtility;
import dev.bkclient.utility.game.ClientConstants;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.ScissorUtility;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Global and AutoMarket config management, functionally matching BK-Client in the Liquid UI. */
public final class ConfigsContent implements IMinecraft {
   private static final int CLOUD_KEY_LENGTH = 12;
   private static final float GAP = 9.0F;
   private static final float PADDING = 10.0F;
   private static final float HEADER_HEIGHT = 18.0F;
   private static final float INPUT_HEIGHT = 20.0F;
   private static final float BUTTON_OFFSET = 0;
   private static final float BUTTON_GAP = 4.0F;
   private static final float CARD_GAP = 2.0F;
   private final Animation[] buttonHover = {
      new Animation(150L, Easing.FIGMA_EASE_IN_OUT), new Animation(150L, Easing.FIGMA_EASE_IN_OUT),
      new Animation(150L, Easing.FIGMA_EASE_IN_OUT), new Animation(150L, Easing.FIGMA_EASE_IN_OUT)
   };

   private final List<ConfigEntryCard> globalCards = new ArrayList<>();
   private final List<ConfigEntryCard> marketCards = new ArrayList<>();
   private final TextField globalField = new TextField(Fonts.MEDIUM.getFont(6.5F));
   private final TextField marketField = new TextField(Fonts.MEDIUM.getFont(6.5F));
   private final ScrollHandler globalScroll = new ScrollHandler();
   private final ScrollHandler marketScroll = new ScrollHandler();
   private final TextField librarySearch = new TextField(Fonts.MEDIUM.getFont(6.5F));
   private boolean profiles;
   private float libraryX, libraryY;

   private boolean globalDirty = true;
   private boolean marketDirty = true;
   private String globalSignature = "";
   private String marketSignature = "";
   private String globalRenameFrom;
   private String marketRenameFrom;
   private long nextRefreshAt;

   private float globalListX, globalListY, globalListW, globalListH;
   private float marketListX, marketListY, marketListW, marketListH;
   private float globalFieldX, globalFieldY, globalFieldW;
   private float marketFieldX, marketFieldY, marketFieldW;
   private float globalAddX, globalAddW, globalImportX, globalImportW;
   private float marketAddX, marketAddW, marketImportX, marketImportW;

   public ConfigsContent() {
      this.globalField.setPreview("Config name or BK code...");
      this.marketField.setPreview("Profile name or BK code...");
      this.librarySearch.setPreview("Search libraryÃ¢â‚¬Â¦");
   }

   public void render(UIContext context, float x, float y, float width, float height, float alpha) {
      long now = System.currentTimeMillis();
      if (now >= this.nextRefreshAt) {
         this.globalDirty = true;
         this.marketDirty = true;
         this.nextRefreshAt = now + 1000L;
      }
      syncGlobal();
      syncMarket();

      libraryX = x + PADDING; libraryY = y + 5;
      globalCards.forEach(ConfigEntryCard::beginFrame); marketCards.forEach(ConfigEntryCard::beginFrame);
      globalListW = marketListW = globalFieldW = marketFieldW = globalAddW = marketAddW = globalImportW = marketImportW = 0;
      for (int i = 0; i < 2; i++) {
         float tx = libraryX + i * 67;
         context.drawRoundedRect(tx, libraryY, 62, 21, BorderRadius.all(6), Colors.getTextColor().withAlpha(profiles == (i == 1) ? 18 : 5));
         context.drawCenteredText(Fonts.MEDIUM.getFont(7), i == 0 ? "Configs" : "Profiles", tx + 31, libraryY + 7,
            profiles == (i == 1) ? Colors.getAccent() : Colors.getTextColor());
      }
      context.drawRoundedRect(x + width - 170, libraryY, 160, 21, BorderRadius.all(6), Colors.getTextColor().withAlpha(10));
      librarySearch.set(x + width - 163, libraryY + 4, 146, 13); librarySearch.setTextColor(Colors.getTextColor()); librarySearch.render(context);

      float panelY = y + 53;
      float panelHeight = height - 63;
      float panelWidth = width - PADDING * 2;
      renderPanel(context, !profiles, libraryX, panelY, panelWidth, panelHeight, alpha);

      for (ConfigEntryCard card : this.globalCards) if (card.shouldRenderMenu()) card.renderMenu(context, alpha);
      for (ConfigEntryCard card : this.marketCards) if (card.shouldRenderMenu()) card.renderMenu(context, alpha);
   }

   private void renderPanel(UIContext context, boolean global, float x, float y, float width, float height, float alpha) {
      String query = librarySearch.getBuiltText().strip().toLowerCase(java.util.Locale.ROOT);
      List<ConfigEntryCard> cards = (global ? this.globalCards : this.marketCards).stream()
         .filter(card -> card.getName().toLowerCase(java.util.Locale.ROOT).contains(query)).toList();
      ScrollHandler scroll = global ? this.globalScroll : this.marketScroll;
      String title = global ? "Saved configs" : "AutoMarket profiles";
      char titleIcon = global ? '9' : '!';
      Font titleFont = Fonts.SEMIBOLD.getFont(7.3F);
      float titleY = y - HEADER_HEIGHT + 3.0F;
      context.drawIcon(Fonts.ICONS, titleIcon, x + 1.0F, titleY - 1.0F, 8.5F,
         Colors.getAccent().withAlpha(235.0F * alpha));
      context.drawText(titleFont, title, x + 12.0F, titleY,
         Colors.getTextColor().withAlpha(205.0F * alpha));
      context.drawRightText(Fonts.MEDIUM.getFont(6.0F), Integer.toString(cards.size()), x + width - 2.0F, titleY + 0.5F,
         Colors.getTextColor().withAlpha(82.0F * alpha));

      context.drawClientRect(x, y, width, height, alpha, 0.0F, 9.0F);

      float inputY = y + 7;
      float listX = x + 7.0F;
      float listY = y + 36.0F;
      float listW = width - 14.0F;
      float listH = Math.max(0.0F, y + height - listY - 7.0F);
      storeListBounds(global, listX, listY, listW, listH);

      float scrollValue = (float)scroll.getValue();
      float stride = ConfigEntryCard.HEIGHT + CARD_GAP;
      for (ConfigEntryCard card : cards) card.beginFrame();
      ScissorUtility.push(context.getMatrices(), listX - 1.0F, listY, listW + 2.0F, listH);
      for (int i = 0; i < cards.size(); i++) {
         float cardY = listY + i * stride - scrollValue;
         if (cardY + ConfigEntryCard.HEIGHT >= listY - 1.0F && cardY <= listY + listH + 1.0F) {
            ConfigEntryCard card = cards.get(i);
            card.setPanelBounds(listY, y + height - 7.0F);
            card.render(context, listX, cardY, listW, alpha);
         }
      }
      ScissorUtility.pop();
      if (cards.isEmpty()) {
         Font emptyFont = Fonts.MEDIUM.getFont(7.0F);
         context.drawCenteredText(emptyFont, global ? "No saved configs" : "No saved profiles",
            x + width / 2.0F, listY + listH / 2.0F - emptyFont.height() / 2.0F,
            Colors.getTextColor().mulAlpha(0.45F * alpha));
      }
      float contentH = Math.max(0.0F, cards.size() * stride - CARD_GAP);
      scroll.setMax(Math.min(0.0F, listH - contentH));
      scroll.update();

      renderInputRow(context, global, x + 7.0F, inputY, width - 14.0F, alpha);
   }

   private void renderInputRow(UIContext context, boolean global, float x, float y, float width, float alpha) {
      TextField field = global ? this.globalField : this.marketField;
      float addW = 42;
      float importW = 49;
      float fieldW = width - addW - importW - 8;
      boolean focused = field.isFocused();
      boolean hovered = hit(context.getMouseX(), context.getMouseY(), x, y, fieldW, INPUT_HEIGHT);
      BorderRadius radius = BorderRadius.all(5.0F);
      context.drawRoundedRect(x, y, fieldW, INPUT_HEIGHT, radius,
         Colors.getBackgroundColor().withAlpha((focused || hovered ? 120.0F : 85.0F) * alpha));
      field.setAlpha(alpha);
      field.setTextColor(Colors.getTextColor());
      field.set(x + 7.0F, y + 1.0F, fieldW - 14.0F, INPUT_HEIGHT - 2.0F);
      field.render(context);

      float addX = x + fieldW + 4;
      float importX = addX + addW + BUTTON_GAP;
      boolean renaming = (global ? this.globalRenameFrom : this.marketRenameFrom) != null;
      drawButton(context, addX, y, addW, INPUT_HEIGHT, "icons/add.png", renaming ? "Rename" : "Add", alpha, global ? 0 : 2);
      drawButton(context, importX, y, importW, INPUT_HEIGHT, "icons/hud/copy.png", "Import", alpha, global ? 1 : 3);
      if (global) {
         this.globalFieldX = x; this.globalFieldY = y; this.globalFieldW = fieldW;
         this.globalAddX = addX; this.globalAddW = addW; this.globalImportX = importX; this.globalImportW = importW;
      } else {
         this.marketFieldX = x; this.marketFieldY = y; this.marketFieldW = fieldW;
         this.marketAddX = addX; this.marketAddW = addW; this.marketImportX = importX; this.marketImportW = importW;
      }
   }

   private void drawButton(UIContext context, float x, float y, float width, float height, String icon, String text, float alpha, int index) {
      boolean hovered = hit(context.getMouseX(), context.getMouseY(), x, y, width, height);
      this.buttonHover[index].update(hovered);
      float hover = this.buttonHover[index].getValue();
      if (hovered) CursorUtility.set(CursorType.HAND);
      context.drawRoundedRect(x, y, width, height, BorderRadius.all(6.0F),
         Colors.getTextColor().withAlpha(10.0F * hover * alpha));
      Font font = Fonts.REGULAR.getFont(8.0F);
      float iconSize = 8.0F;
      float totalWidth = iconSize + 2.5F + font.width(text);
      float contentX = x + (width - totalWidth) / 2.0F;
      context.drawTexture(BKClientInit.id(icon), contentX, y + (height - iconSize) / 2.0F, iconSize, iconSize,
         Colors.getTextColor().mulAlpha((0.75F + 0.25F * hover) * alpha));
      context.drawText(font, text, contentX + iconSize + 2.5F, y + (height - font.height()) / 2.0F,
         Colors.getTextColor().mulAlpha((0.75F + 0.25F * hover) * alpha));
   }

   private void syncGlobal() {
      if (!this.globalDirty) return;
      this.globalDirty = false;
      ConfigManager manager = BKClientInit.getInstance().getConfigManager();
      if (manager == null) return;
      manager.refresh();
      String active = manager.getCurrent() == null ? "autosave" : manager.getCurrent().getFileName();
      StringBuilder signature = new StringBuilder(active).append('|');
      for (ConfigFile config : manager.getConfigFiles()) {
         signature.append(config.getFileName()).append(':').append(config.getFile().lastModified()).append(';');
      }
      if (signature.toString().equals(this.globalSignature)) return;
      this.globalSignature = signature.toString();
      String open = openMenuName(this.globalCards);
      List<ConfigEntryCard> previousCards = new ArrayList<>(this.globalCards);
      this.globalCards.clear();
      for (ConfigFile config : manager.getConfigFiles()) {
         ConfigEntryCard existing = previousCards.stream().filter(card -> card.getName().equals(config.getDisplayName())).findFirst().orElse(null);
         if (existing != null) {
            existing.setActive(config.getFileName().equalsIgnoreCase(active));
            existing.setModified(config.getFile().lastModified());
            this.globalCards.add(existing);
            continue;
         }
         ConfigEntryCard card = new ConfigEntryCard(config.getDisplayName(), config.getFile().lastModified(),
            config.getFileName().equalsIgnoreCase(active),
            () -> loadGlobal(config), () -> beginRenameGlobal(config), () -> loadGlobal(config),
            () -> saveGlobal(config), () -> shareGlobal(config, false), () -> shareGlobal(config, true),
            () -> deleteGlobal(config));
         if (config.getDisplayName().equals(open)) card.setMenuOpen(true);
         this.globalCards.add(card);
      }
   }

   private void syncMarket() {
      if (!this.marketDirty) return;
      this.marketDirty = false;
      AutoMarket module = autoMarket();
      if (module == null || module.getConfigManager() == null) return;
      module.getConfigManager().refresh();
      String active = module.getActiveProfile();
      StringBuilder signature = new StringBuilder(String.valueOf(active)).append('|');
      for (AutoMarketConfigManager.ConfigInfo config : module.getConfigManager().getConfigs()) {
         signature.append(config.filename).append(':').append(config.date).append(';');
      }
      if (signature.toString().equals(this.marketSignature)) return;
      this.marketSignature = signature.toString();
      String open = openMenuName(this.marketCards);
      List<ConfigEntryCard> previousCards = new ArrayList<>(this.marketCards);
      this.marketCards.clear();
      for (AutoMarketConfigManager.ConfigInfo config : module.getConfigManager().getConfigs()) {
         ConfigEntryCard existing = previousCards.stream().filter(card -> card.getName().equals(config.name)).findFirst().orElse(null);
         if (existing != null) {
            existing.setActive(active != null && config.filename.equalsIgnoreCase(active));
            existing.setModified(config.date);
            this.marketCards.add(existing);
            continue;
         }
         ConfigEntryCard card = new ConfigEntryCard(config.name, config.date,
            active != null && config.filename.equalsIgnoreCase(active),
            () -> loadMarket(config), () -> beginRenameMarket(config), () -> loadMarket(config),
            () -> saveMarket(config), () -> shareMarket(config, false), () -> shareMarket(config, true),
            () -> deleteMarket(config));
         if (config.name.equals(open)) card.setMenuOpen(true);
         this.marketCards.add(card);
      }
   }

   public boolean handleClick(double mouseX, double mouseY, MouseButton button) {

      if (button == MouseButton.LEFT && hit(mouseX, mouseY, libraryX, libraryY, 129, 21)) {
         profiles = mouseX >= libraryX + 67; closeOtherMenus(null);
         globalField.setFocused(false); marketField.setFocused(false); librarySearch.clear(); librarySearch.setFocused(false);
         globalCards.forEach(ConfigEntryCard::beginFrame); marketCards.forEach(ConfigEntryCard::beginFrame);
         globalScroll.reset(); marketScroll.reset(); return true;
      }
      librarySearch.onMouseClicked(mouseX, mouseY, button);
      for (ConfigEntryCard card : this.globalCards) {
         if (card.isMenuOpen() && card.handleClick(mouseX, mouseY, button)) return true;
      }
      for (ConfigEntryCard card : this.marketCards) {
         if (card.isMenuOpen() && card.handleClick(mouseX, mouseY, button)) return true;
      }
      ConfigEntryCard clicked = handleCardClick(this.globalCards, mouseX, mouseY, button);
      if (clicked == null) clicked = handleCardClick(this.marketCards, mouseX, mouseY, button);
      if (clicked != null) {
         closeOtherMenus(clicked);
         return true;
      }
      if (button != MouseButton.LEFT) {
         closeOtherMenus(null);
         return false;
      }

      (profiles ? this.marketField : this.globalField).onMouseClicked(mouseX, mouseY, button);

      if (hit(mouseX, mouseY, this.globalAddX, this.globalFieldY + BUTTON_OFFSET, this.globalAddW, INPUT_HEIGHT)) {
         createGlobal(); return true;
      }
      if (hit(mouseX, mouseY, this.globalImportX, this.globalFieldY + BUTTON_OFFSET, this.globalImportW, INPUT_HEIGHT)) {
         importGlobal(); return true;
      }
      if (hit(mouseX, mouseY, this.marketAddX, this.marketFieldY + BUTTON_OFFSET, this.marketAddW, INPUT_HEIGHT)) {
         createMarket(); return true;
      }
      if (hit(mouseX, mouseY, this.marketImportX, this.marketFieldY + BUTTON_OFFSET, this.marketImportW, INPUT_HEIGHT)) {
         importMarket(); return true;
      }
      closeOtherMenus(null);
      return hit(mouseX, mouseY, this.globalFieldX, this.globalFieldY, this.globalFieldW, INPUT_HEIGHT)
         || hit(mouseX, mouseY, this.marketFieldX, this.marketFieldY, this.marketFieldW, INPUT_HEIGHT);
   }

   public void handleRelease() {
      librarySearch.onMouseReleased(0, 0, MouseButton.LEFT);
      for (ConfigEntryCard card : new ArrayList<>(this.globalCards)) card.handleRelease();
      for (ConfigEntryCard card : new ArrayList<>(this.marketCards)) card.handleRelease();
      this.globalField.onMouseReleased(0.0, 0.0, MouseButton.LEFT);
      this.marketField.onMouseReleased(0.0, 0.0, MouseButton.LEFT);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (librarySearch.isFocused()) { librarySearch.onKeyPressed(keyCode, scanCode, modifiers); globalScroll.reset(); marketScroll.reset(); closeOtherMenus(null); return true; }
      if (!this.globalField.isFocused() && !this.marketField.isFocused()) return false;
      if (keyCode == 256) {
         this.globalField.setFocused(false);
         this.marketField.setFocused(false);
         return true;
      }
      if (this.globalField.isFocused()) {
         this.globalField.onKeyPressed(keyCode, scanCode, modifiers);
         if (keyCode == 257) createGlobal();
         return true;
      }
      this.marketField.onKeyPressed(keyCode, scanCode, modifiers);
      if (keyCode == 257) createMarket();
      return true;
   }

   public boolean charTyped(char chr, int modifiers) {
      if (librarySearch.isFocused()) { globalScroll.reset(); marketScroll.reset(); closeOtherMenus(null); return librarySearch.charTyped(chr, modifiers); }
      if (this.globalField.isFocused()) return this.globalField.charTyped(chr, modifiers);
      if (this.marketField.isFocused()) return this.marketField.charTyped(chr, modifiers);
      return false;
   }

   public boolean onScroll(double mouseX, double mouseY, double amount) {
      if (hit(mouseX, mouseY, this.globalListX, this.globalListY, this.globalListW, this.globalListH)) {
         closeOtherMenus(null); this.globalScroll.scroll(amount); return true;
      }
      if (hit(mouseX, mouseY, this.marketListX, this.marketListY, this.marketListW, this.marketListH)) {
         closeOtherMenus(null); this.marketScroll.scroll(amount); return true;
      }
      return false;
   }

   private void loadGlobal(ConfigFile config) {
      ConfigManager manager = BKClientInit.getInstance().getConfigManager();
      if (manager == null || config == null) return;
      ConfigFile previous = manager.getAutoSaveConfig();
      if (previous != null && !previous.getFileName().equalsIgnoreCase(config.getFileName()) && !previous.save()) {
         notifyError("Configs", "Could not save the current config. Switch cancelled.");
         return;
      }
      config.load();
      this.globalDirty = true;
   }

   private void saveGlobal(ConfigFile config) {
      if (config == null) return;
      if (!config.save()) {
         notifyError("Configs", "Could not save config.");
         return;
      }
      notifySuccess("Configs", "Config saved.");
      this.globalDirty = true;
   }

   private void shareGlobal(ConfigFile config, boolean privateMode) {
      if (config == null) return;
      try {
         String content = Files.readString(config.getFile().toPath());
         JsonObject json = JsonParser.parseString(content).getAsJsonObject();
         json.addProperty("meta_name", ConfigManager.sanitizeConfigName(config.getFileName()));
         notifySuccess("Configs", "Uploading config...");
         CloudConfigUtility.uploadConfig(config.getFileName(), FileManager.GSON.toJson(json)).thenAccept(result ->
            mc.execute(() -> {
               if (result == null) {
                  notifyError("Configs", "Config upload failed.");
                  return;
               }
               String code = ClientConstants.CLOUD_PREFIX + result.key;
               if (privateMode && result.deletionKey != null) code += result.deletionKey;
               mc.keyboard.setClipboard(code);
               notifySuccess("Configs", privateMode ? "Private code copied." : "Public code copied.");
            }));
      } catch (Exception error) {
         BKClientInit.LOGGER.error("Failed to share config {}", config.getFileName(), error);
         notifyError("Configs", "Could not read this config.");
      }
   }

   private void deleteGlobal(ConfigFile config) {
      if (config == null || "autosave".equalsIgnoreCase(config.getFileName())) return;
      ConfigManager manager = BKClientInit.getInstance().getConfigManager();
      if (manager.getCurrent() != null && manager.getCurrent().getFileName().equalsIgnoreCase(config.getFileName())) {
         ConfigFile autosave = manager.getConfig("autosave");
         if (autosave != null) autosave.load();
      }
      config.delete();
      manager.refresh();
      this.globalDirty = true;
   }

   private void beginRenameGlobal(ConfigFile config) {
      if (config == null || "autosave".equalsIgnoreCase(config.getFileName())) return;
      this.globalRenameFrom = config.getFileName();
      this.globalField.setText(config.getDisplayName());
      this.globalField.setFocused(true);
   }

   private void renameGlobal(String oldName, String newName) {
      ConfigManager manager = BKClientInit.getInstance().getConfigManager();
      ConfigFile config = manager == null ? null : manager.getConfig(oldName);
      String clean = ConfigManager.sanitizeConfigName(newName);
      if (config == null || clean.equalsIgnoreCase(oldName)) return;
      File target = new File(config.getFile().getParentFile(), clean + ".bk");
      if (target.exists()) {
         notifyError("Configs", "That name already exists.");
         return;
      }
      try {
         manager.renameConfig(config, clean);
         notifySuccess("Configs", "Config renamed.");
      } catch (Exception error) {
         BKClientInit.LOGGER.error("Failed to rename config {}", oldName, error);
         notifyError("Configs", "Rename failed.");
      }
      manager.refresh();
      this.globalDirty = true;
   }

   private void createGlobal() {
      String name = this.globalField.getText().trim();
      if (name.isEmpty()) return;
      if (this.globalRenameFrom != null) {
         renameGlobal(this.globalRenameFrom, name);
         this.globalRenameFrom = null;
      } else {
         ConfigManager manager = BKClientInit.getInstance().getConfigManager();
         String clean = ConfigManager.sanitizeConfigName(name);
         ConfigFile existing = manager.getConfig(clean, true);
         if (!(existing == null ? manager.createConfig(clean) : existing.save())) {
            notifyError("Configs", "Could not save config.");
            return;
         }
      }
      this.globalField.clear();
      this.globalField.setFocused(false);
      this.globalDirty = true;
   }

   private void importGlobal() {
      String input=this.globalField.getText().trim();
      if(input.isEmpty())return;
      dev.bkclient.ui.clicksys.port.data.ConfigSync.getInstance().onConsumerString2(result->{
         if(result instanceof dev.bkclient.ui.clicksys.port.data.ConfigSync.Imported){this.globalField.clear();this.globalDirty=true;}
      },input);
   }
   private void loadMarket(AutoMarketConfigManager.ConfigInfo config) {
      AutoMarket module = autoMarket();
      if (module == null || config == null) return;
      String current = module.getActiveProfile();
      if (current != null && !current.equalsIgnoreCase(config.filename)) module.getConfigManager().save(current);
      module.setLoading(true);
      try {
         module.configName.setText(config.filename);
         module.setActiveProfile(config.filename);
         module.getConfigManager().load(config.filename);
      } finally {
         module.setLoading(false);
      }
      this.marketDirty = true;
   }

   private void saveMarket(AutoMarketConfigManager.ConfigInfo config) {
      AutoMarket module = autoMarket();
      if (module == null || config == null) return;
      module.configName.setText(config.filename);
      module.setActiveProfile(config.filename);
      module.getConfigManager().save(config.filename);
      notifySuccess("AutoMarket", "Profile saved.");
      this.marketDirty = true;
   }

   private void shareMarket(AutoMarketConfigManager.ConfigInfo config, boolean privateMode) {
      AutoMarket module = autoMarket();
      if (module == null || config == null) return;
      module.getConfigManager().exportToCloud(config.filename, privateMode);
   }

   private void deleteMarket(AutoMarketConfigManager.ConfigInfo config) {
      AutoMarket module = autoMarket();
      if (module == null || config == null) return;
      module.getConfigManager().delete(config.filename);
      this.marketDirty = true;
   }

   private void beginRenameMarket(AutoMarketConfigManager.ConfigInfo config) {
      if (config == null) return;
      this.marketRenameFrom = config.filename;
      this.marketField.setText(config.name);
      this.marketField.setFocused(true);
   }

   private void createMarket() {
      AutoMarket module = autoMarket();
      String name = this.marketField.getText().trim();
      if (module == null || name.isEmpty()) return;
      if (this.marketRenameFrom != null) {
         boolean renamed = module.getConfigManager().rename(this.marketRenameFrom, name);
         if (renamed) notifySuccess("AutoMarket", "Profile renamed.");
         else notifyError("AutoMarket", "Profile rename failed.");
         this.marketRenameFrom = null;
      } else {
         String clean = AutoMarketConfigManager.sanitizeProfileName(name);
         module.configName.setText(clean);
         module.setActiveProfile(clean);
         module.getConfigManager().save(clean);
      }
      module.getConfigManager().refresh();
      this.marketField.clear();
      this.marketField.setFocused(false);
      this.marketDirty = true;
   }

   private void importMarket() {
      AutoMarket module = autoMarket();
      String input = this.marketField.getText().trim();
      if (module == null || input.isEmpty()) return;
      module.getConfigManager().importFromCloud(input);
      this.marketField.clear();
      this.marketField.setFocused(false);
      this.marketDirty = true;
      invalidateMarketLater(1500L);
      invalidateMarketLater(4000L);
   }

   private ConfigEntryCard handleCardClick(List<ConfigEntryCard> cards, double mouseX, double mouseY, MouseButton button) {
      boolean global = cards == this.globalCards;
      if (!hit(mouseX, mouseY, global ? this.globalListX : this.marketListX, global ? this.globalListY : this.marketListY,
         global ? this.globalListW : this.marketListW, global ? this.globalListH : this.marketListH)) return null;
      for (ConfigEntryCard card : cards) if (card.handleClick(mouseX, mouseY, button)) return card;
      return null;
   }

   private void closeOtherMenus(ConfigEntryCard keep) {
      for (ConfigEntryCard card : this.globalCards) if (card != keep) card.setMenuOpen(false);
      for (ConfigEntryCard card : this.marketCards) if (card != keep) card.setMenuOpen(false);
   }

   private static String openMenuName(List<ConfigEntryCard> cards) {
      for (ConfigEntryCard card : cards) if (card.isMenuOpen()) return card.getName();
      return null;
   }

   private void storeListBounds(boolean global, float x, float y, float width, float height) {
      if (global) {
         this.globalListX = x; this.globalListY = y; this.globalListW = width; this.globalListH = height;
      } else {
         this.marketListX = x; this.marketListY = y; this.marketListW = width; this.marketListH = height;
      }
   }

   private AutoMarket autoMarket() {
      try {
         return BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
      } catch (RuntimeException ignored) {
         return null;
      }
   }

   private void invalidateMarketLater(long delayMs) {
      CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> mc.execute(() -> this.marketDirty = true));
   }

   private void notifySuccess(String title, String message) {
      BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS, title, message);
   }

   private void notifyError(String title, String message) {
      BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.ERROR, title, message);
   }

   private static boolean hit(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }
}
