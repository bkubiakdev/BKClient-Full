package dev.bkclient.utility.text;

import java.util.HashSet;
import java.util.Set;

/** One submission per code while a challenge remains visible, with server-confirmed resumption. */
public final class MiningChallengeGate {
    private final Set<String> sent = new HashSet<>();
    private long lastSeen, nextSend;
    private boolean waiting, confirmed;
    public String observe(boolean present, Set<String> codes, long now) {
        if (!present) {
            if (confirmed && now - lastSeen >= 1000) { confirmed = false; sent.clear(); }
            return null;
        }
        lastSeen = now;
        if (confirmed && (codes.isEmpty() || sent.containsAll(codes))) return null;
        confirmed = false;
        waiting = true;
        if (codes.size() != 1 || now < nextSend) return null;
        String code = codes.iterator().next();
        if (!code.matches("[0-9]{1,16}") || sent.contains(code) || sent.size() >= 16) return null;
        sent.add(code);
        nextSend = now + 3000;
        return "kopanie " + code;
    }
    public boolean confirm() {
        if (!waiting || sent.isEmpty()) return false;
        waiting = false;
        confirmed = true;
        return true;
    }
    public static boolean isSuccessMessage(String message) {
        return message.replaceAll("§[0-9a-fk-orA-FK-OR]", "").strip()
                .equalsIgnoreCase("Prawidłowy kod! Możesz dalej kopać!");
    }
    public boolean waiting() { return waiting; }
    public void reset() { waiting = false; confirmed = false; sent.clear(); lastSeen = 0; nextSend = 0; }
}
