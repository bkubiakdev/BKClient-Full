package dev.bkclient.utility.network;


import com.google.gson.Gson;
import dev.bkclient.utility.game.ScoreboardUtility;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.bkclient.utility.concurrent.ClientAsyncExecutor;

/**
 * Asynchronous Discord Webhook delivery utility.
 * Components V2 notifications; preserves server-confirmed purchase timing.
 */
public final class DiscordWebhook {
    private static final Gson GSON = new Gson();

    private DiscordWebhook() {
    }

    public static void send(String webhookUrl, Object bodyObject) {
        if (webhookUrl == null || webhookUrl.isEmpty() || !webhookUrl.startsWith("http"))
            return;

        ClientAsyncExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = java.net.URI.create(executionUrl(webhookUrl)).toURL();
                connection = (HttpURLConnection) url.openConnection();
                configure(connection);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setDoOutput(true);

                String json = GSON.toJson(bodyObject);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                checkResponse(connection);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public static void sendTestNotification(String webhookUrl, java.util.function.Consumer<Boolean> completion) {
        Map<String, Object> body = createBaseBody();
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "\uD83D\uDED2 Zakup udany \u00b7 PODGL\u0104D TESTOWY");
        notification.put("color", 0x2ECC71);
        notification.put("fields", List.of(
                createField("Przedmiot", "`Buty Hadesa \u2605 14`", false),
                createField("Ilo\u015b\u0107", "`1 szt.`", true),
                createField("Cena", "`11 000$`", true),
                createField("Stan konta", "`248 500$`", true),
                createField("Enchanty", "```Protection IV\nUnbreaking III\nMending```", false),
                createField("Lore", "```Przykladowy zakup BKClient.\nTo test wygladu, bez prawdziwej transakcji.```", false),
                createField("Serwer", "`demo.bkclient.local`", false)));
        addComponents(body, notification);
        ClientAsyncExecutor.execute(() -> completion.accept(sendSync(webhookUrl, body)));
    }

    public static void sendPurchaseNotification(String webhookUrl, String itemName, String price, String server,
            int amount, String enchants, String lore, String ignoredBalance) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

        ClientAsyncExecutor.schedule(() -> {
            String balance = ScoreboardUtility.getFormattedBalance();
            Map<String, Object> body = createBaseBody();

            
            Map<String, Object> embed = new HashMap<>();

            embed.put("title", "\uD83D\uDED2 Pomy\u015Blnie zakupiono przedmiot!");
            embed.put("color", 0x2ECC71); // Emerald Green

            List<Map<String, Object>> fields = new ArrayList<>();
            fields.add(createField("Przedmiot", "`" + itemName + "`", true));
            fields.add(createField("Ilo\u015B\u0107", "`" + amount + " szt.`", true));
            fields.add(createField("Cena", "`" + price + "$`", true));
            fields.add(createField("Stan konta", "`" + balance + "`", true));

            if (enchants != null && !enchants.isEmpty()) {
                fields.add(createField("Enchanty", "```" + truncate(enchants, 900) + "```", false));
            }

            if (lore != null && !lore.isEmpty()) {
                fields.add(createField("Lore", "```" + truncate(lore, 900) + "```", false));
            }

            fields.add(createField("Serwer", "`" + server + "`", false));
            embed.put("fields", fields);

            addComponents(body, embed);

            sendSync(webhookUrl, body);
        }, 3, java.util.concurrent.TimeUnit.SECONDS);
    }

    public static void sendSaleNotification(String webhookUrl, String earnings, String server, String itemName,
            String ignoredBalance) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

        ClientAsyncExecutor.schedule(() -> {
            String balance = ScoreboardUtility.getFormattedBalance();
            Map<String, Object> body = createBaseBody();

            
            Map<String, Object> embed = new HashMap<>();

            embed.put("title", "\uD83D\uDCB0 Przedmiot sprzedany!");
            embed.put("color", 0xF1C40F); // Sunflower Yellow

            List<Map<String, Object>> fields = new ArrayList<>();
            if (itemName != null && !itemName.isEmpty()) {
                fields.add(createField("Przedmiot", "`" + itemName + "`", true));
            }
            fields.add(createField("Zarobek", "`" + earnings + "$`", true));
            fields.add(createField("Stan konta", "`" + balance + "`", true));
            fields.add(createField("Serwer", "`" + server + "`", true));

            embed.put("fields", fields);
            addComponents(body, embed);

            sendSync(webhookUrl, body);
        }, 3, java.util.concurrent.TimeUnit.SECONDS);
    }

    public static void sendListingNotification(String webhookUrl, String itemName, String price, String server,
            int amount) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

        Map<String, Object> body = createBaseBody();
        
        Map<String, Object> embed = new HashMap<>();

        embed.put("title", "\uD83D\uDCDC Przedmiot wystawiony!");
        embed.put("color", 0x18DCFF); // Vibrant Light Blue

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("Przedmiot", "`" + itemName + "`", true));
        fields.add(createField("Ilo\u015B\u0107", "`" + amount + " szt.`", true));
        fields.add(createField("Cena", "`" + price + "$`", true));
        fields.add(createField("Serwer", "`" + server + "`", true));

        embed.put("fields", fields);
        addComponents(body, embed);

        send(webhookUrl, body);
    }

    public static void sendErrorNotification(String webhookUrl, String reason, String server, String itemName,
            String price) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

        Map<String, Object> body = createBaseBody();
        
        Map<String, Object> embed = new HashMap<>();

        embed.put("title", "\u26A0\uFE0F B\u0142\u0105d automatyzacji!");
        embed.put("color", 0xE74C3C); // Alizarin Red

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("Pow\u00F3d", "`" + reason + "`", true));
        if (itemName != null && !itemName.isEmpty()) {
            fields.add(createField("Przedmiot", "`" + itemName + "`", true));
        }
        if (price != null && !price.isEmpty()) {
            fields.add(createField("Cena", "`" + price + "`", true));
        }
        fields.add(createField("Serwer", "`" + server + "`", true));

        embed.put("fields", fields);
        addComponents(body, embed);

        send(webhookUrl, body);
    }

    /**
     * Sends a Discord webhook message with an embedded PNG screenshot file.
     * Uses multipart/form-data with payload_json + files[0].
     */
    public static void sendWithScreenshot(String webhookUrl, Map<String, Object> body, byte[] pngBytes, String fileName) {
        if (webhookUrl == null || webhookUrl.isEmpty() || pngBytes == null)
            return;

        ClientAsyncExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String boundary = "----BKClientInitBoundary" + Long.toHexString(System.currentTimeMillis());
                String payloadJson = new Gson().toJson(body);

                URL url = java.net.URI.create(executionUrl(webhookUrl)).toURL();
                conn = (HttpURLConnection) url.openConnection();
                configure(conn);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setDoOutput(true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                // Part 1: payload_json
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"payload_json\"\r\n");
                dos.writeBytes("Content-Type: application/json\r\n\r\n");
                dos.write(payloadJson.getBytes(StandardCharsets.UTF_8));
                dos.writeBytes("\r\n");

                // Part 2: file
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"files[0]\"; filename=\"" + fileName + "\"\r\n");
                dos.writeBytes("Content-Type: image/png\r\n\r\n");
                dos.write(pngBytes);
                dos.writeBytes("\r\n");

                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();

                byte[] multipartData = baos.toByteArray();
                conn.setRequestProperty("Content-Length", String.valueOf(multipartData.length));
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(multipartData);
                }
                checkResponse(conn);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static void sendPurchaseNotificationWithScreenshot(String webhookUrl, String itemName, String price,
            String server, int amount, String enchants, String lore, byte[] screenshotPng) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

        ClientAsyncExecutor.schedule(() -> {
            String balance = ScoreboardUtility.getFormattedBalance();
            Map<String, Object> body = createBaseBody();

            
            Map<String, Object> embed = new HashMap<>();

            embed.put("title", "\uD83D\uDED2 Pomy\u015Blnie zakupiono przedmiot!");
            embed.put("color", 0x2ECC71);

            List<Map<String, Object>> fields = new ArrayList<>();
            fields.add(createField("Przedmiot", "`" + itemName + "`", true));
            fields.add(createField("Ilo\u015B\u0107", "`" + amount + " szt.`", true));
            fields.add(createField("Cena", "`" + price + "$`", true));
            fields.add(createField("Stan konta", "`" + balance + "`", true));

            if (enchants != null && !enchants.isEmpty()) {
                fields.add(createField("Enchanty", "```" + truncate(enchants, 900) + "```", false));
            }
            if (lore != null && !lore.isEmpty()) {
                fields.add(createField("Lore", "```" + truncate(lore, 900) + "```", false));
            }
            fields.add(createField("Serwer", "`" + server + "`", false));

            if (screenshotPng != null) {
                embed.put("image", Map.of("url", "attachment://screen.png"));
            }

            embed.put("fields", fields);
            addComponents(body, embed);

            if (screenshotPng != null) {
                sendWithScreenshot(webhookUrl, body, screenshotPng, "screen.png");
            } else {
                sendSync(webhookUrl, body);
            }
        }, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public static void sendFullInventoryNotification(String webhookUrl, String server) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

        Map<String, Object> body = createBaseBody();
        
        Map<String, Object> embed = new HashMap<>();

        embed.put("title", "\u26A0\uFE0F Ekwipunek pe\u0142ny!");
        embed.put("color", 0xFF4757); // Red

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("Status", "Automatyzacja zatrzymana", false));
        fields.add(createField("Serwer", "`" + server + "`", false));

        embed.put("fields", fields);
        addComponents(body, embed);

        send(webhookUrl, body);
    }


    private static Map<String, Object> createBaseBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("username", "BK-Client Notifications");
        body.put("avatar_url", "https://i.imgur.com/DLs7dkY.png");
        return body;
    }

    /** Builds only non-interactive components, supported by ordinary incoming webhooks. */
    @SuppressWarnings("unchecked")
    static void addComponents(Map<String, Object> body, Map<String, Object> notification) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(text("-# BKCLIENT / AUTOMARKET"));
        parts.add(text("## " + notification.get("title")));
        parts.add(Map.of("type", 14, "divider", true, "spacing", 1));
        for (Map<String, Object> field : (List<Map<String, Object>>) notification.get("fields")) {
            String value = String.valueOf(field.get("value"));
            parts.add(text("**" + field.get("name") + "**\n" + truncate(value, 1100)));
        }
        if (notification.get("image") instanceof Map<?, ?> image) {
            parts.add(gallery(String.valueOf(image.get("url")), "Zrzut ekranu po zakupie"));
            body.put("attachments", List.of(Map.of("id", 0, "filename", "screen.png")));
        }
        parts.add(Map.of("type", 14, "divider", true, "spacing", 1));
        parts.add(Map.of("type", 9,
                "components", List.of(text("-# Autor: [BK-Client \u00b7 changeme.net](https://changeme.net/mods)"
                        + "\n-# <t:" + java.time.Instant.now().getEpochSecond() + ":f>"
                        + "\n[Wesprzyj projekt](https://donate.bkubiak.dev)")),
                "accessory", Map.of("type", 11,
                        "media", Map.of("url", "https://i.imgur.com/2fffrrr.jpeg"),
                        "description", "Logo BKClient")));
        body.put("flags", 1 << 15);
        body.put("allowed_mentions", Map.of("parse", List.of()));
        body.put("components", List.of(Map.of("type", 17,
                "accent_color", notification.get("color"), "components", parts)));
    }

    private static Map<String, Object> text(String content) {
        return Map.of("type", 10, "content", content);
    }

    private static Map<String, Object> gallery(String url, String description) {
        return Map.of("type", 12, "items", List.of(Map.of(
                "media", Map.of("url", url), "description", description)));
    }

    static String executionUrl(String webhookUrl) {
        java.net.URI uri = java.net.URI.create(webhookUrl);
        String query = uri.getRawQuery();
        List<String> parameters = new ArrayList<>();
        if (query != null) {
            for (String parameter : query.split("&")) {
                String key = parameter.split("=", 2)[0];
                if (!key.equals("with_components") && !key.equals("wait")) parameters.add(parameter);
            }
        }
        parameters.add("with_components=true");
        parameters.add("wait=true");
        String base = webhookUrl.split("[?#]", 2)[0];
        return base + "?" + String.join("&", parameters);
    }

    private static void checkResponse(HttpURLConnection connection) throws java.io.IOException {
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            // Do not log the connection URL: it contains the webhook token.
            throw new java.io.IOException("BKClient webhook rejected: HTTP " + status);
        }
        try (java.io.InputStream response = connection.getInputStream()) {
            response.transferTo(OutputStream.nullOutputStream());
        }
    }

    private static Map<String, Object> createField(String name, String value, boolean inline) {
        Map<String, Object> field = new HashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", inline);
        return field;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength) + "...";
    }

    private static boolean sendSync(String webhookUrl, Object bodyObject) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return false;
        HttpURLConnection connection = null;
        try {
            URL url = java.net.URI.create(executionUrl(webhookUrl)).toURL();
            connection = (HttpURLConnection) url.openConnection();
            configure(connection);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setDoOutput(true);
            String json = GSON.toJson(bodyObject);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            checkResponse(connection);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void configure(HttpURLConnection connection) {
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setUseCaches(false);
    }
}
