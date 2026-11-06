package dev.bkclient.systems.airdrop;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.config.ConfigManager;
import dev.bkclient.utility.interfaces.IMinecraft;
import fi.iki.elonen.NanoHTTPD;
import lombok.Generated;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class ConfigUploadServer extends NanoHTTPD implements IMinecraft {
   private final File directory = new File(dev.bkclient.systems.file.FileManager.DIRECTORY, "configs");
   private String name;
   private boolean render;

   public ConfigUploadServer() throws IOException {
      super(5656);
      if (!this.directory.exists() && !this.directory.mkdirs()) {
         throw new IOException("Could not create the config directory: " + this.directory);
      }
      this.start(5000, false);
      BKClientInit.LOGGER.info("Config upload server started on port 5656; directory: {}", this.directory.getAbsolutePath());
   }

   @Override
   public Response serve(IHTTPSession session) {
      if (Method.POST.equals(session.getMethod())) {
         try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String temporaryPath = files.get("file");
            if (temporaryPath == null) {
               return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No config file was provided");
            }
            String originalName = session.getParms().get("file");
            if (originalName == null || originalName.isBlank()) originalName = "client.bk";
            originalName = new File(originalName).getName();
            if (!originalName.endsWith(".bk") && !originalName.endsWith(".sw")) {
               return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Expected a .bk or legacy .sw config");
            }
            String configName = ConfigManager.sanitizeConfigName(originalName.substring(0, originalName.lastIndexOf('.')));
            originalName = configName + ".bk";
            this.name = originalName;
            this.render = true;

            File destination = new File(this.directory, originalName);
            Files.copy(Path.of(temporaryPath), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            mc.execute(() -> {
               ConfigManager manager = BKClientInit.getInstance().getConfigManager();
               manager.refresh();
               ConfigFile config = manager.getConfig(configName);
               if (config != null) config.load();
            });
            return newFixedLengthResponse("Config uploaded; queued for loading in the client");
         } catch (Exception exception) {
            BKClientInit.LOGGER.error("Config upload failed", exception);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Upload failed: " + exception.getMessage());
         }
      }

      this.render = false;
      String html = """
         <!doctype html><html lang="en"><head><meta charset="UTF-8">
         <meta name="viewport" content="width=device-width,initial-scale=1">
         <title>BK-Client Config Upload</title><style>
         body{font-family:system-ui,sans-serif;display:grid;place-items:center;min-height:100vh;margin:0;background:#f2f2f2}
         form{display:flex;flex-direction:column;gap:15px;background:white;padding:24px;border-radius:14px;box-shadow:0 4px 18px #0002}
         input{font-size:1rem}input[type=submit]{padding:12px;border:0;border-radius:8px;background:#287cff;color:white;cursor:pointer}
         </style></head><body><form method="POST" enctype="multipart/form-data">
         <h1>Upload a BK-Client config</h1><input type="file" name="file" accept=".bk,.sw" required>
         <input type="submit" value="Send to client"></form></body></html>
         """;
      return newFixedLengthResponse(Response.Status.OK, "text/html", html);
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public boolean isRender() {
      return this.render;
   }
}
