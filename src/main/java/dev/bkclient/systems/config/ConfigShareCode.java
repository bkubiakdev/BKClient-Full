package dev.bkclient.systems.config;

import dev.bkclient.utility.game.ClientConstants;

/** Compatible with ClickGUI public and single-use BK sharing codes. */
public record ConfigShareCode(String key, String deletionKey) {
    public static ConfigShareCode parse(String input) {
        if (input == null) throw new IllegalArgumentException("Paste a BK code first.");
        String raw = input.trim();
        if (raw.startsWith(ClientConstants.CLOUD_PREFIX)) raw = raw.substring(ClientConstants.CLOUD_PREFIX.length());
        String key = raw, deletionKey = null;
        int separator = raw.indexOf(ClientConstants.BURN_SEP);
        if (separator >= 0) {
            key = raw.substring(0, separator);
            deletionKey = raw.substring(separator + ClientConstants.BURN_SEP.length());
        } else if (raw.length() > 12) {
            key = raw.substring(0, 12);
            deletionKey = raw.substring(12);
        }
        if (!key.matches("[A-Za-z0-9_-]{12}") || deletionKey != null && !deletionKey.matches("[A-Za-z0-9_-]{1,256}"))
            throw new IllegalArgumentException("Invalid BK code.");
        return new ConfigShareCode(key, deletionKey);
    }

    public String formatted() {
        return ClientConstants.CLOUD_PREFIX + key + (deletionKey == null ? "" : deletionKey);
    }
}
