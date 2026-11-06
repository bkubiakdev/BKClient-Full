package dev.bkclient.systems.config;

import dev.bkclient.utility.game.XSK;

public final class ConfigShareCodeTest {
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        String key = "AbC123xyZ_90", secret = "deleteKey-1234";
        ConfigShareCode plain = ConfigShareCode.parse("  " + XSK.CLOUD_PREFIX + key + "\n");
        require(plain.key().equals(key) && plain.deletionKey() == null);
        ConfigShareCode privateCode = new ConfigShareCode(key, secret);
        require(ConfigShareCode.parse(privateCode.formatted()).equals(privateCode));
        require(ConfigShareCode.parse(key + XSK.BURN_SEP + secret).equals(privateCode));
        require(ConfigShareCode.parse(key).equals(plain));
        for (String invalid : new String[]{"", "short", "../bad/path12", "https://example.com/config", key + XSK.BURN_SEP, key + "bad key"}) {
            try { ConfigShareCode.parse(invalid); throw new AssertionError("Accepted invalid code: " + invalid); }
            catch (IllegalArgumentException expected) { }
        }
        System.out.println("BK sharing codes: public/private round trips, legacy separator and invalid input passed");
    }
}
