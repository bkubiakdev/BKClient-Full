package dev.bkclient.utility.game;

/**
 * String obfuscation utility.
 * Strings are encoded at build time using XS#e(String) and decoded at runtime using XS#d(String).
 */
public final class XS {

    // Key material \u2014 each byte is XORed with the corresponding char of XS.class.getName()
    // at class-init time, so no plaintext key ever appears as a literal in the constant pool.
    private static final byte K0 = (byte) 0x26;
    private static final byte K1 = (byte) 0x2E;
    private static final byte K2 = (byte) 0x35;
    private static final byte K3 = (byte) 0x42;
    private static final byte K4 = (byte) 0x0B;
    private static final byte K5 = (byte) 0x0E;
    private static final byte K6 = (byte) 0x0D;
    private static final byte K7 = (byte) 0x18;
    private static final byte K8 = (byte) 0x5B;
    private static final byte K9 = (byte) 0x55;
    private static final byte KA = (byte) 0x5C;
    private static final byte KB = (byte) 0x42;

    private static final byte[] KEY;

    static {
        // Must stay stable after obfuscation; encoded constants were generated with this seed.
        // Encoded constants came from the BK source package; keep that seed stable after relocation.
        byte[] n = "dev.bkclient.utility.game.XS".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] stored = { K0, K1, K2, K3, K4, K5, K6, K7, K8, K9, KA, KB };
        KEY = new byte[stored.length];
        for (int i = 0; i < stored.length; i++) {
            KEY[i] = (byte) (stored[i] ^ n[i % n.length]);
        }
    }

    private XS() {}

    /**
     * Decode an obfuscated string encoded with XS#e(String).
     * Input is a hex string (2 chars per byte).
     */
    public static String d(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ((Character.digit(hex.charAt(i * 2), 16) << 4)
                    + Character.digit(hex.charAt(i * 2 + 1), 16));
        }
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte) (bytes[i] ^ KEY[i % KEY.length]);
        }
        return new String(result, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Encode a plain string to a hex-encoded XOR-obfuscated form.
     * Used at build time to generate constants \u2014 not called at runtime in production.
     */
    public static String e(String plain) {
        byte[] bytes = plain.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte) (bytes[i] ^ KEY[i % KEY.length]);
        }
        StringBuilder sb = new StringBuilder(result.length * 2);
        for (byte b : result) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}

