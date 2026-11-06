package dev.bkclient.utility.render;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** Resolves explicit image metadata on share pages without executing scripts. */
public final class CustomCapeLinks {
    private static final int LIMIT = 16 * 1024 * 1024;
    private static final Pattern META = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w:-]+)\\s*=\\s*([\"'])(.*?)\\2", Pattern.DOTALL);
    private CustomCapeLinks() {}
    public static boolean isTenor(URI uri) {
        String host = uri.getHost();
        return host != null && (host.equalsIgnoreCase("tenor.com") || host.equalsIgnoreCase("www.tenor.com"));
    }
    public static URI media(String html) throws IOException {
        URI png = null;
        var tags = META.matcher(html);
        while (tags.find()) {
            String property = "", content = "";
            var attrs = ATTRIBUTE.matcher(tags.group());
            while (attrs.find()) {
                String key = attrs.group(1).toLowerCase(Locale.ROOT);
                if (key.equals("property") || key.equals("name")) property = attrs.group(3);
                if (key.equals("content")) content = attrs.group(3).replace("&amp;", "&");
            }
            if (!property.equalsIgnoreCase("og:image") && !property.equalsIgnoreCase("twitter:image")) continue;
            try {
                URI result = URI.create(content);
                String host = result.getHost();
                if (("https".equalsIgnoreCase(result.getScheme()) || "http".equalsIgnoreCase(result.getScheme())) && host != null) {
                    String path = result.getPath().toLowerCase(Locale.ROOT);
                    if (path.endsWith(".gif")) return result;
                    if (path.endsWith(".png")) png = result;
                }
            } catch (IllegalArgumentException ignored) { }
        }
        if (png != null) return png;
        throw new IOException("No downloadable PNG/GIF found on this page.");
    }
    public static byte[] download(URI uri) throws IOException {
        boolean resolvedPage = false;
        for (int hops = 0; hops < 8; hops++) {
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                throw new IOException("Only HTTP or HTTPS links are supported.");
            var connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 BKClient-CustomCape/1.0");
            connection.setInstanceFollowRedirects(false);
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    String next = connection.getHeaderField("Location");
                    if (next == null) throw new IOException("Server returned an empty redirect.");
                    uri = uri.resolve(next); continue;
                }
                if ("challenge".equalsIgnoreCase(connection.getHeaderField("cf-mitigated")))
                    throw new IOException("This website blocks automatic downloads.");
                if (status != 200) throw new IOException("Server returned HTTP " + status + ". Try another link.");
                if (connection.getContentLengthLong() > LIMIT) throw new IOException("File exceeds 16 MB.");
                byte[] bytes;
                try (InputStream input = connection.getInputStream()) {
                    bytes = input.readNBytes(LIMIT + 1);
                }
                if (bytes.length > LIMIT) throw new IOException("File exceeds 16 MB.");
                String type = connection.getContentType();
                String beginning = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.UTF_8).stripLeading().toLowerCase(Locale.ROOT);
                boolean html = (type != null && type.toLowerCase(Locale.ROOT).contains("text/html"))
                        || beginning.startsWith("<!doctype html") || beginning.startsWith("<html");
                if (html) {
                    if (!resolvedPage) {
                        uri = media(new String(bytes, StandardCharsets.UTF_8)); resolvedPage = true; continue;
                    }
                    throw new IOException("No downloadable PNG/GIF found at this link.");
                }
                return bytes;
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many redirects.");
    }
}
