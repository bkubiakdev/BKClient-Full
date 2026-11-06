package dev.bkclient.systems.config;

import com.google.gson.JsonObject;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.file.FileManager;

import java.io.File;
import java.io.IOException;

public class WebhookConfig {
    private static final File FILE = new File(FileManager.DIRECTORY, "webhooks.json");
    private static String webhookUrl = "";

    public static void load() {
        if (!FILE.exists())
            return;
        try {
            JsonObject json = dev.bkclient.systems.file.SafeJsonFile.read(FILE.toPath());
            if (json.has("webhookUrl")) {
                webhookUrl = json.get("webhookUrl").getAsString();
            }
        } catch (IOException e) {
            BKClientInit.LOGGER.error("Failed to load webhook config", e);
        }
    }

    public static void save(String url) {
        webhookUrl = url;
        JsonObject json = new JsonObject();
        json.addProperty("webhookUrl", url);
        try {
            dev.bkclient.systems.file.SafeJsonFile.write(FILE.toPath(), json);
        } catch (IOException e) {
            BKClientInit.LOGGER.error("Failed to save webhook config", e);
        }
    }

    public static String getUrl() {
        return webhookUrl;
    }
}

