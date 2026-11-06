package dev.bkclient.systems.file.impl;

import dev.bkclient.BKClientInit;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.file.ClientFile;
import dev.bkclient.systems.file.SafeJsonFile;
import dev.bkclient.systems.file.api.FileInfo;
import dev.bkclient.systems.modules.constructions.swinganim.SwingManager;
import dev.bkclient.systems.modules.constructions.swinganim.SwingPhase;
import dev.bkclient.ui.mainmenu.AccountManager;
import dev.bkclient.ui.mainmenu.MenuBackground;
import dev.bkclient.systems.modules.constructions.swinganim.presets.SwingPreset;
import dev.bkclient.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import dev.bkclient.systems.modules.modules.other.AutoAuth;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.theme.Theme;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.hud.HudElement;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;
import net.minecraft.client.MinecraftClient;

@FileInfo(name = "client")
public class ClientDataFile extends ClientFile implements IMinecraft {
    private JsonObject storedJson = new JsonObject();
    @Override
   public void write() {
      try {
         ensureCurrentSessionAccount();
         JsonObject json = this.storedJson.deepCopy();
            if (mc.getSession() != null) {
                json.addProperty("username", mc.getSession().getUsername());
            }
            json.addProperty("theme", BKClientInit.getInstance().getThemeManager().getCurrentTheme().name());
            json.add("themes", this.getThemesJsonObject());
            json.addProperty("swing", BKClientInit.getInstance().getSwingManager().getCurrent());
            json.add("hudElements", this.getHudElementsJsonArray());
            json.add("favoriteModules", dev.bkclient.systems.modules.ModuleFavorites.save());
            json.add("friends", this.getFriendsJsonArray());
            json.add("presencePrivacy", dev.bkclient.systems.friends.PresencePrivacy.save());
            json.add("waypoints", BKClientInit.getInstance().getWayPointsManager().save());
            json.add("colorPickerPresets", this.getColorPickerPresetsJsonArray());
            json.add("password", this.getPassword());
            ConfigFile currentConfig = BKClientInit.getInstance().getConfigManager().getCurrent();
            if (currentConfig != null) {
                json.addProperty("lastConfig", currentConfig.getFileName());
            } else {
                json.remove("lastConfig");
            }

            json.add("accounts", this.getAccountsJsonArray());
            json.addProperty("menuBackground", BKClientInit.getInstance().getSelectedMenuBackground());
            json.add("menuPreferences", dev.bkclient.ui.mainmenu.MenuPreferences.save());

            SafeJsonFile.write(this.file.toPath(), json, ClientDataFile::validStructure);
            this.storedJson = json.deepCopy();
        } catch (Exception e) {
            System.err.println("Error saving client data: " + e.getMessage());
            e.printStackTrace();
        }
    }

   @Override
   public void read() {
      dev.bkclient.systems.friends.PresencePrivacy.load(null);
      boolean previousSuppress = dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave;
      dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave = true;
      try {
         JsonObject object = dev.bkclient.systems.config.CooldownsHudMigration.migrate(
             SafeJsonFile.read(this.getFile().toPath(), ClientDataFile::validStructure));
         this.storedJson = object.deepCopy();
         dev.bkclient.systems.modules.ModuleFavorites.load(object.get("favoriteModules"));
         if (object.has("waypoints") && object.get("waypoints").isJsonArray()) {
            BKClientInit.getInstance().getWayPointsManager().load(object.getAsJsonArray("waypoints"));
         }
         if (object.has("password")) {
            this.loadPass(object.getAsJsonArray("password"));
         }

         if (object.has("swing")) {
            String swing = object.get("swing").getAsString();
            SwingManager swingManager = BKClientInit.getInstance().getSwingManager();
            SwingPresetManager manager = BKClientInit.getInstance().getSwingPresetManager();

            for (SwingPreset value : BKClientInit.getInstance().getSwingManager().getPresets()) {
               if (value.getName().equals(swing)) {
                  swingManager.getBezier().start(value.getBezierStart()).end(value.getBezierEnd());
                  swingManager.getBack().enabled(value.isSwingBack());
                  swingManager.getSpeed().setCurrentValue(value.getSpeed());
                  SwingPhase start = swingManager.getStartPhase();
                  start.getAnchorX().setCurrentValue(value.getFrom().anchorX());
                  start.getAnchorY().setCurrentValue(value.getFrom().anchorY());
                  start.getAnchorZ().setCurrentValue(value.getFrom().anchorZ());
                  start.getMoveX().setCurrentValue(value.getFrom().moveX());
                  start.getMoveY().setCurrentValue(value.getFrom().moveY());
                  start.getMoveZ().setCurrentValue(value.getFrom().moveZ());
                  start.getRotateX().setCurrentValue(value.getFrom().rotateX());
                  start.getRotateY().setCurrentValue(value.getFrom().rotateY());
                  start.getRotateZ().setCurrentValue(value.getFrom().rotateZ());
                  SwingPhase end = swingManager.getEndPhase();
                  end.getAnchorX().setCurrentValue(value.getTo().anchorX());
                  end.getAnchorY().setCurrentValue(value.getTo().anchorY());
                  end.getAnchorZ().setCurrentValue(value.getTo().anchorZ());
                  end.getMoveX().setCurrentValue(value.getTo().moveX());
                  end.getMoveY().setCurrentValue(value.getTo().moveY());
                  end.getMoveZ().setCurrentValue(value.getTo().moveZ());
                  end.getRotateX().setCurrentValue(value.getTo().rotateX());
                  end.getRotateY().setCurrentValue(value.getTo().rotateY());
                  end.getRotateZ().setCurrentValue(value.getTo().rotateZ());
                  swingManager.setCurrent(swing);
               }
            }
         }

         if (object.has("theme")) {
            String themeName = object.get("theme").getAsString();

            try {
               Theme theme = Theme.valueOf(themeName);
               BKClientInit.getInstance().getThemeManager().setCurrentTheme(theme);
            } catch (IllegalArgumentException var16) {
               BKClientInit.getInstance().getThemeManager().setCurrentTheme(Theme.DARK);
            }
         }

         if (object.has("themes") && object.get("themes").isJsonObject()) {
            this.loadThemes(object.getAsJsonObject("themes"));
         }

         if (object.has("friends")) {
            JsonArray friendsArray = object.getAsJsonArray("friends");
            BKClientInit.getInstance().getFriendManager().clear();

            for (JsonElement friendElement : friendsArray) {
               BKClientInit.getInstance().getFriendManager().add(friendElement.getAsString());
            }
         }

         if (object.has("colorPickerPresets")) {
            this.loadColorPickerPresets(object.getAsJsonArray("colorPickerPresets"));
         }

         if (object.has("hudElements")) {
            for (JsonElement elemObj : object.getAsJsonArray("hudElements")) {
               JsonObject elementObject = elemObj.getAsJsonObject();
               String name = elementObject.get("name").getAsString();
               float x = elementObject.get("x").getAsFloat();
               float y = elementObject.get("y").getAsFloat();
               boolean showing = elementObject.get("showing").getAsBoolean();
               HudElement element = BKClientInit.getInstance().getHud().getElementByName(name);
               if (element != null) {
                  // Old drag/scale builds could serialize Float.MIN_VALUE. It is finite, but it
                  // effectively pins a HUD element into the screen edge and makes it look missing.
                  if (!Float.isFinite(x) || x != 0.0F && Math.abs(x) < Float.MIN_NORMAL) {
                     x = name.equalsIgnoreCase("hud.player") ? 10.0F : element.getX();
                  }
                  if (!Float.isFinite(y) || y != 0.0F && Math.abs(y) < Float.MIN_NORMAL) {
                     y = element.getY();
                  }
                  element.setX(x);
                  element.setY(y);
                  element.setShowing(showing);
                  if (elementObject.has("settings")) {
                     JsonObject settingsObject = elementObject.getAsJsonObject("settings");

                     for (Setting setting : element.getSettings()) {
                        if (settingsObject.has(setting.getName())) {
                           setting.load(settingsObject.get(setting.getName()));
                        }
                     }
                  }
               }
            }
         }

          if (object.has("lastConfig")) {
             String configName = object.get("lastConfig").getAsString();
             ConfigFile config = BKClientInit.getInstance().getConfigManager().getConfig(configName);
             if (config != null) {
                config.load();
             }
          }

          if (object.has("accounts")) {
             List<AccountManager.Account> loadedAccounts = new ArrayList<>();
             for (JsonElement elem : object.getAsJsonArray("accounts")) {
                JsonObject accObj = elem.getAsJsonObject();
                String username = accObj.get("username").getAsString();
                String uuid = accObj.has("uuid") ? accObj.get("uuid").getAsString() : UUID.randomUUID().toString();
                String typeStr = accObj.has("type") ? accObj.get("type").getAsString() : "LEGACY";
                Session.AccountType type;
                try {
                   type = Session.AccountType.valueOf(typeStr);
                } catch (Exception e) {
                   type = Session.AccountType.LEGACY;
                }
                loadedAccounts.add(new AccountManager.Account(username, uuid, type));
             }
             BKClientInit.getInstance().setAccounts(loadedAccounts);

             // The username is the selected account saved by write(). Restore the
             // actual Minecraft session after the account list has been loaded.
             // Previously a Session object was created and immediately discarded,
             // so the list survived but the selected account reverted on restart.
             // IAS highlights the account whose name matches MinecraftClient.session.
             // The launcher starts with its generic offline username, so restore the
             // last account selected in BKClient before the IAS screen is opened.
             if (object.has("username")) {
                String selectedUsername = object.get("username").getAsString();
                for (AccountManager.Account account : loadedAccounts) {
                   if (account.username.equals(selectedUsername)) {
                      restoreSession(account);
                      break;
                   }
                }
             }
          }

          dev.bkclient.ui.mainmenu.MenuPreferences.load(object.get("menuPreferences"));
          if (object.has("menuBackground")) {
             String backgroundId = object.get("menuBackground").getAsString();
             if (MenuBackground.fromUniqueId(backgroundId, null) != null || backgroundId.startsWith("file:")) {
                BKClientInit.getInstance().setSelectedMenuBackground(backgroundId);
             }
          }
          dev.bkclient.systems.friends.PresencePrivacy.load(object.get("presencePrivacy"));
      } catch (Exception var18) {
         var18.printStackTrace();
      } finally {
         dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave = previousSuppress;
      }
   }

   private static void restoreSession(AccountManager.Account account) {
      // IAS owns authenticated sessions and token refresh. Legacy BK metadata
      // contains no credentials and must never replace that session.
      if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ias")
            || account.type != AccountType.LEGACY
            || !MinecraftClient.getInstance().getSession().getAccessToken().isBlank()) return;
      UUID uuid;
      try {
         uuid = UUID.fromString(account.uuid);
      } catch (Exception ignored) {
         uuid = UUID.randomUUID();
      }
      AccountType type = account.type == null ? AccountType.LEGACY : account.type;
      try {
         Session session = new Session(account.username, uuid, "", Optional.empty(), Optional.empty(), type);
         ((dev.bkclient.mixin.minecraft.client.IMinecraftClient) MinecraftClient.getInstance()).setSession(session);
      } catch (RuntimeException error) {
         BKClientInit.LOGGER.warn("Could not restore selected account session", error);
      }
   }

   private static boolean validStructure(JsonObject json) {
      for (String key : new String[]{"hudElements", "friends", "waypoints", "password", "accounts", "colorPickerPresets"}) {
         if (json.has(key) && !json.get(key).isJsonArray()) return false;
      }
      if (json.has("themes") && !json.get("themes").isJsonObject()) return false;
      if (json.has("hudElements")) {
         for (JsonElement entry : json.getAsJsonArray("hudElements")) {
            if (!entry.isJsonObject()) return false;
            JsonObject hud = entry.getAsJsonObject();
            if (hud.has("settings") && !hud.get("settings").isJsonObject()) return false;
         }
      }
      return true;
   }

   private JsonArray getHudElementsJsonArray() {
      this.storedJson = dev.bkclient.systems.config.CooldownsHudMigration.migrate(this.storedJson);
      JsonArray hudElementsArray = new JsonArray();
      java.util.Map<String, JsonObject> storedElements = new java.util.LinkedHashMap<>();
      if (this.storedJson.has("hudElements") && this.storedJson.get("hudElements").isJsonArray()) {
         for (JsonElement value : this.storedJson.getAsJsonArray("hudElements")) {
            if (value.isJsonObject() && value.getAsJsonObject().has("name")) {
               JsonObject object = value.getAsJsonObject();
               storedElements.put(object.get("name").getAsString(), object.deepCopy());
            }
         }
      }

      for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
         JsonObject elementObject = storedElements.remove(element.getName());
         if (elementObject == null) elementObject = new JsonObject();
         elementObject.addProperty("name", element.getName());
         elementObject.addProperty("x", element.getX());
         elementObject.addProperty("y", element.getY());
         elementObject.addProperty("showing", element.isShowing());
         JsonObject settings = elementObject.has("settings") && elementObject.get("settings").isJsonObject()
            ? elementObject.getAsJsonObject("settings") : new JsonObject();
         for (var entry : this.getSettingsJsonObject(element).entrySet()) settings.add(entry.getKey(), entry.getValue());
         elementObject.add("settings", settings);
         hudElementsArray.add(elementObject);
      }

      storedElements.values().forEach(hudElementsArray::add);
      return hudElementsArray;
   }

   private JsonObject getSettingsJsonObject(HudElement element) {
      JsonObject settingsObject = new JsonObject();

      for (Setting setting : element.getSettings()) {
         settingsObject.add(setting.getName(), setting.save());
      }

      return settingsObject;
   }

   private JsonArray getAccountsJsonArray() {
       JsonArray accountsArray = new JsonArray();

       for (AccountManager.Account account : BKClientInit.getInstance().getAccounts()) {
          JsonObject accountObject = new JsonObject();
          accountObject.addProperty("username", account.username);
          accountObject.addProperty("uuid", account.uuid);
          accountObject.addProperty("type", account.type == null ? "LEGACY" : account.type.name());
          accountsArray.add(accountObject);
       }

       return accountsArray;
    }

    /**
     * Keep the account list in sync with the session supplied by the launcher or
     * an external account-switcher mod. Previously such a switch only changed
     * MinecraftClient.session, while client.bk kept an empty/stale account list.
     */
    private void ensureCurrentSessionAccount() {
       Session session = mc.getSession();
       if (session == null || session.getUsername() == null || session.getUsername().isBlank()) return;

       List<AccountManager.Account> accounts = BKClientInit.getInstance().getAccounts();
       if (accounts == null) accounts = new ArrayList<>();
       for (AccountManager.Account account : accounts) {
          if (account != null && account.username != null
                && account.username.equalsIgnoreCase(session.getUsername())) {
             return;
          }
       }

       String uuid = session.getUuidOrNull() == null
             ? UUID.nameUUIDFromBytes(session.getUsername().getBytes(StandardCharsets.UTF_8)).toString()
             : session.getUuidOrNull().toString();
       AccountType type = session.getAccountType() == null ? AccountType.LEGACY : session.getAccountType();
       accounts = new ArrayList<>(accounts);
       accounts.add(new AccountManager.Account(session.getUsername(), uuid, type));
       BKClientInit.getInstance().setAccounts(accounts);
    }

    private JsonArray getFriendsJsonArray() {
       JsonArray friendsJsonArray = new JsonArray();

       for (String friendsName : BKClientInit.getInstance().getFriendManager().listFriends()) {
          friendsJsonArray.add(friendsName);
       }

       return friendsJsonArray;
    }

   private JsonArray getPassword() {
      JsonArray passwordJsonArray = new JsonArray();

      for (Entry<String, String> pass : BKClientInit.getInstance().getModuleManager().getModule(AutoAuth.class).listPassword().entrySet()) {
         JsonObject passObject = new JsonObject();
         passObject.addProperty("nick", pass.getValue());
         passObject.addProperty("pass", pass.getKey());
         passwordJsonArray.add(passObject);
      }

      return passwordJsonArray;
   }

   private JsonArray getColorPickerPresetsJsonArray() {
      JsonArray presetsArray = new JsonArray();

      for (ColorPicker.Preset preset : ColorPicker.COLOR_PRESETS) {
         if (preset.isShowing()) {
            JsonObject presetObject = new JsonObject();
            ColorRGBA color = preset.getColor();
            presetObject.addProperty("red", color.getRed());
            presetObject.addProperty("green", color.getGreen());
            presetObject.addProperty("blue", color.getBlue());
            presetObject.addProperty("alpha", color.getAlpha());
            presetsArray.add(presetObject);
         }
      }

      return presetsArray;
   }

   private JsonObject getThemesJsonObject() {
      JsonObject themes = new JsonObject();

      for (Theme theme : Theme.values()) {
         JsonObject themeObject = new JsonObject();
         themeObject.add("text", this.colorToJson(theme.getTextColor()));
         themeObject.add("background", this.colorToJson(theme.getBackgroundColor()));
         themeObject.add("additional", this.colorToJson(theme.getAdditionalColor()));
         themeObject.add("outline", this.colorToJson(theme.getOutlineColor()));
         themeObject.add("flat", this.colorToJson(theme.getFlatColor()));
         themeObject.add("accent", this.colorToJson(theme.getAccentColor()));
         themeObject.add("icons", this.colorToJson(theme.getIconsColor()));
         themeObject.add("enabledModules", this.colorToJson(theme.getEnabledModulesColor()));
         themes.add(theme.name(), themeObject);
      }

      return themes;
   }

   private JsonObject colorToJson(ColorRGBA color) {
      JsonObject object = new JsonObject();
      object.addProperty("red", color.getRed());
      object.addProperty("green", color.getGreen());
      object.addProperty("blue", color.getBlue());
      object.addProperty("alpha", color.getAlpha());
      return object;
   }

   private void loadThemes(JsonObject themesObject) {
      for (Theme theme : Theme.values()) {
         if (themesObject.has(theme.name()) && themesObject.get(theme.name()).isJsonObject()) {
            JsonObject themeObject = themesObject.getAsJsonObject(theme.name());
            theme.setTextColor(this.readColor(themeObject, "text", theme.getTextColor()));
            theme.setBackgroundColor(this.readColor(themeObject, "background", theme.getBackgroundColor()));
            theme.setAdditionalColor(this.readColor(themeObject, "additional", theme.getAdditionalColor()));
            theme.setOutlineColor(this.readColor(themeObject, "outline", theme.getOutlineColor()));
            theme.setFlatColor(this.readColor(themeObject, "flat", theme.getFlatColor()));
            theme.setAccentColor(this.readColor(themeObject, "accent", theme.getAccentColor()));
            theme.setIconsColor(this.readColor(themeObject, "icons", theme.getIconsColor()));
            theme.setEnabledModulesColor(this.readColor(themeObject, "enabledModules", theme.getEnabledModulesColor()));
         }
      }
   }

   private ColorRGBA readColor(JsonObject themeObject, String name, ColorRGBA fallback) {
      if (themeObject.has(name) && themeObject.get(name).isJsonObject()) {
         JsonObject colorObject = themeObject.getAsJsonObject(name);
         if (colorObject.has("red") && colorObject.has("green") && colorObject.has("blue")) {
            float alpha = colorObject.has("alpha") ? colorObject.get("alpha").getAsFloat() : fallback.getAlpha();
            return new ColorRGBA(colorObject.get("red").getAsFloat(), colorObject.get("green").getAsFloat(), colorObject.get("blue").getAsFloat(), alpha);
         } else {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private void loadColorPickerPresets(JsonArray presetsArray) {
      List<ColorPicker.Preset> loadedPresets = new ArrayList<>();

      for (JsonElement presetElement : presetsArray) {
         JsonObject presetObject = presetElement.getAsJsonObject();
         float red = presetObject.get("red").getAsFloat();
         float green = presetObject.get("green").getAsFloat();
         float blue = presetObject.get("blue").getAsFloat();
         float alpha = presetObject.get("alpha").getAsFloat();
         ColorRGBA color = new ColorRGBA(red, green, blue, alpha);
         loadedPresets.add(new ColorPicker.Preset(color));
      }

      ColorPicker.setColorPresets(loadedPresets);
   }

   private void loadPass(JsonArray password) {
      for (JsonElement passElement : password) {
         JsonObject passObject = passElement.getAsJsonObject();
         String nick = passObject.get("nick").getAsString();
         String pass = passObject.get("pass").getAsString();
         BKClientInit.getInstance().getModuleManager().getModule(AutoAuth.class).put(nick, pass);
      }
   }
}
