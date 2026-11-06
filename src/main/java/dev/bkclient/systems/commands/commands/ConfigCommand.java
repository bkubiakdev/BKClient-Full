package dev.bkclient.systems.commands.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.nio.file.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.CommandContext;
import dev.bkclient.systems.commands.ParameterValidator;
import dev.bkclient.systems.commands.ValidationResult;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.config.ConfigManager;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.notifications.ConfigSavedPopup;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.utility.game.CloudConfigUtility;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.game.XSK;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class ConfigCommand {
   private static final ParameterValidator<String> CONFIG_NAME = ValidationResult::ok;

   public Command command() {
      List<String> configNames = BKClientInit.getInstance().getConfigManager().getConfigFiles().stream().map(ConfigFile::getFileName).toList();
      return CommandBuilder.begin(
            "config",
            b -> b.aliases("cfg", "кфг", "конфиг")
               .desc("commands.config.description")
               .<ConfigCommand.Action>param(
                  "action",
                  p -> p.validator(text -> {
                        Optional<ConfigCommand.Action> action = ConfigCommand.Action.from(text);
                        return action.isPresent() ? ValidationResult.ok(action.get()) : ValidationResult.error(Localizator.translate("commands.config.invalid_action"));
                     })
                     .suggests(ConfigCommand.Action.allNames())
                )
               .<String>param("id", p -> p.optional().validator(CONFIG_NAME).suggests(configNames))
               .handler(this::handle)
         )
         .build();
   }

   private void handle(CommandContext ctx) {
      ConfigCommand.Action action = (ConfigCommand.Action)ctx.arguments().get(0);
      String id = (String)ctx.arguments().get(1);
      action.createHandler().accept(id);
   }

   private enum Action {
      SAVE("save", "create", "add", "сохранить", "ыфму"),
      REMOVE("delete", "remove", "del", "удалить", "вудуеу"),
      LIST("list", "дшые"),
      LOAD("load", "use", "использовать", "дщфв"),
      DIR("dir", "direction"),
      SHARE("share", "upload"),
      IMPORT("import", "loadcode");

      private final List<String> names;

      Action(String... names) {
         this.names = Arrays.stream(names).map(String::toLowerCase).collect(Collectors.toList());
      }

      private Consumer<String> createHandler() {
         return switch (this) {
            case SAVE -> this::saveConfig;
            case REMOVE -> s -> {
               if (s != null) {
                  ConfigFile config = BKClientInit.getInstance().getConfigManager().getConfig(s);
                  if (config != null) config.delete();
               }
            };
            case LIST -> s -> BKClientInit.getInstance().getConfigManager().listConfigs();
            case LOAD -> s -> {
               BKClientInit.getInstance().getConfigManager().refresh();
               if (s != null && BKClientInit.getInstance().getConfigManager().getConfig(s) != null) {
                  BKClientInit.getInstance().getConfigManager().getConfig(s).load();
               }
            };
            case DIR -> s -> BKClientInit.getInstance().getConfigManager().directionConfig();
            case SHARE -> this::shareConfig;
            case IMPORT -> this::importConfig;
         };
      }

      private void shareConfig(String configName) {
         if (configName == null) return;
         ConfigManager manager = BKClientInit.getInstance().getConfigManager();
         ConfigFile config = manager.getConfig(configName, true);
         if (config == null) {
            MessageUtility.error(Text.of("Config " + configName + " not found"));
            return;
         }
         try {
            String content = Files.readString(config.getFile().toPath());
            try {
               JsonObject json = JsonParser.parseString(content).getAsJsonObject();
               json.addProperty("meta_name", ConfigManager.sanitizeConfigName(configName));
               content = FileManager.GSON.toJson(json);
            } catch (RuntimeException ignored) {
            }
            String uploadContent = content;
            CloudConfigUtility.uploadConfig(configName, uploadContent).thenAccept(result -> {
               if (result == null) {
                  MessageUtility.error(Text.of("Config upload failed"));
                  return;
               }
               String code = XSK.CLOUD_PREFIX + result.key;
               MinecraftClient.getInstance().execute(() -> {
                  MinecraftClient.getInstance().keyboard.setClipboard(code);
                  MessageUtility.info(Text.of("Config published. Code copied: " + code));
               });
            });
         } catch (Exception error) {
            BKClientInit.LOGGER.error("Failed to share config {}", configName, error);
            MessageUtility.error(Text.of("Could not read config " + configName));
         }
      }

      private void importConfig(String input) {
         if (input == null || input.isBlank()) return;
         String raw = input.startsWith(XSK.CLOUD_PREFIX) ? input.substring(XSK.CLOUD_PREFIX.length()) : input;
         String code = raw;
         String deletionKey = null;
         if (raw.contains(XSK.BURN_SEP)) {
            String[] parts = raw.split(java.util.regex.Pattern.quote(XSK.BURN_SEP), 2);
            code = parts[0];
            deletionKey = parts[1];
         } else if (raw.length() > 12) {
            code = raw.substring(0, 12);
            deletionKey = raw.substring(12);
         }
         String finalCode = code;
         String finalDeletionKey = deletionKey;
         CloudConfigUtility.downloadConfig(finalCode, finalDeletionKey).thenAccept(content -> {
            if (content == null || content.isBlank()) {
               MessageUtility.error(Text.of("Failed to download config"));
               return;
            }
            MinecraftClient.getInstance().execute(() ->
               BKClientInit.getInstance().getConfigManager().importConfig("cloud_" + finalCode, content));
         });
      }

      private void saveConfig(String configName) {
         if (configName != null) {
            ConfigManager configManager = BKClientInit.getInstance().getConfigManager();
            ConfigFile existing = configManager.getConfig(configName, true);
            if (existing != null) {
               ConfigSavedPopup.getInstance().showOverwrite(configName, () -> {
                  if (existing.save()) ConfigSavedPopup.getInstance().show(configName);
               });
            } else {
               if (configManager.createConfig(configName)) ConfigSavedPopup.getInstance().show(configName);
            }
         }
      }

      static Optional<ConfigCommand.Action> from(String input) {
         String key = input.toLowerCase();
         return Arrays.stream(values()).filter(a -> a.names.contains(key)).findFirst();
      }

      static List<String> allNames() {
         return Arrays.stream(values()).map(a -> (String)a.names.getFirst()).collect(Collectors.toList());
      }
   }
}
