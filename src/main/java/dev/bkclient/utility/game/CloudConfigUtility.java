package dev.bkclient.utility.game;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import dev.bkclient.BKClientInit;
import dev.bkclient.utility.concurrent.ClientAsyncExecutor;

public final class CloudConfigUtility {

    public static final class CloudResult {
        public final String key;
        public final String deletionKey;

        public CloudResult(String key, String deletionKey) {
            this.key = key;
            this.deletionKey = deletionKey;
        }
    }

    public static CompletableFuture<CloudResult> uploadConfig(String name, String content) {
        return ClientAsyncExecutor.supply(() -> {
            HttpURLConnection conn = null;
            try {
                String protectedContent = xor(compress(content));
                java.net.URI uri = java.net.URI.create(ClientConstants.POST_URL);
                java.net.URL url = uri.toURL();
                conn = open(url);
                conn.setRequestMethod("POST");
                conn.setRequestProperty(ClientConstants.CONTENT_TYPE, "text/plain");
                conn.setRequestProperty(ClientConstants.USER_AGENT_HDR, ClientConstants.USER_AGENT_VAL + BKClientInit.VERSION);
                ClientTelemetryUtility.applyClientHeaders(conn);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = protectedContent.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                if (conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) {
                    throw new RuntimeException("Failed to upload config: HTTP " + conn.getResponseCode());
                }

                try (InputStream is = conn.getInputStream()) {
                    String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
                    String key = responseJson.get(ClientConstants.JSON_KEY_FIELD).getAsString();
                    String delKey = responseJson.has(ClientConstants.JSON_DEL_KEY) ? responseJson.get(ClientConstants.JSON_DEL_KEY).getAsString() : null;
                    return new CloudResult(key, delKey);
                }
            } catch (Exception e) {
                BKClientInit.LOGGER.error("Failed to share config", e);
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static CompletableFuture<Boolean> deleteConfig(String key, String deletionKey) {
        return ClientAsyncExecutor.supply(() -> {
            HttpURLConnection conn = null;
            try {
                java.net.URI uri = java.net.URI.create(ClientConstants.BASE_URL + key);
                java.net.URL url = uri.toURL();
                conn = open(url);
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty(ClientConstants.HTTP_DEL_HDR, deletionKey);
                conn.setRequestProperty(ClientConstants.USER_AGENT_HDR, ClientConstants.USER_AGENT_VAL + BKClientInit.VERSION);

                return conn.getResponseCode() >= 200 && conn.getResponseCode() < 300;
            } catch (Exception e) {
                BKClientInit.LOGGER.error("Failed to delete cloud config", e);
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static CompletableFuture<String> downloadConfig(String code) {
        return downloadConfig(code, null);
    }

    public static CompletableFuture<String> downloadConfig(String code, String deletionKey) {
        return ClientAsyncExecutor.supply(() -> {
            HttpURLConnection conn = null;
            try {
                java.net.URI uri = java.net.URI.create(ClientConstants.BASE_URL + code);
                java.net.URL url = uri.toURL();
                conn = open(url);
                conn.setRequestMethod("GET");
                conn.setRequestProperty(ClientConstants.USER_AGENT_HDR, ClientConstants.USER_AGENT_VAL + BKClientInit.VERSION);
                if (deletionKey != null && !deletionKey.isEmpty()) {
                    conn.setRequestProperty(ClientConstants.HTTP_DEL_HDR, deletionKey);
                }

                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed to download config: HTTP " + conn.getResponseCode());
                }

                try (InputStream is = conn.getInputStream()) {
                    String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    return decompressIfNeeded(xor(response));
                }
            } catch (Exception e) {
                BKClientInit.LOGGER.error("Failed to load cloud config", e);
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static HttpURLConnection open(java.net.URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setUseCaches(false);
        return connection;
    }

    private static String compress(String input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(input.getBytes(StandardCharsets.UTF_8));
        }
        return ClientConstants.GZIP_PREFIX + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private static String decompressIfNeeded(String input) throws IOException {
        if (!input.startsWith(ClientConstants.GZIP_PREFIX)) {
            return input;
        }

        byte[] compressed = Base64.getDecoder().decode(input.substring(ClientConstants.GZIP_PREFIX.length()));
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String xor(String input) {
        String k = ClientConstants.XOR_KEY_VAL;
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            output.append((char) (input.charAt(i) ^ k.charAt(i % k.length())));
        }
        return output.toString();
    }

    private CloudConfigUtility() {
    }
}
