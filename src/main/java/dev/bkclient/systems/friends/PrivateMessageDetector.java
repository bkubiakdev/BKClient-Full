package dev.bkclient.systems.friends;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Conservative parsing of incoming server whispers; never search the message body for names. */
public final class PrivateMessageDetector {
    private static final String NAME = "([A-Za-z0-9_]{1,16})";
    private static final String RANK = "(?:\\[[^\\]\\r\\n]{1,24}\\]\\s*)*";
    private static final Pattern ARROW = Pattern.compile("^\\[?" + RANK + NAME
        + "\\s*(?:->|»|→|>>|➜)\\s*([A-Za-z0-9_]{1,16})\\s*\\]?\\s*[:»]?\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM = Pattern.compile("^\\[?(?:from|od)\\s+" + RANK + NAME
        + "\\s*(?:\\]\\s*:?|:)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHISPER = Pattern.compile("^" + RANK + NAME
        + "\\s+(?:whispers to you|whispers|szepcze do ciebie, mówiąc|szepcze do ciebie|szepcze)\\s*[:,]\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    private PrivateMessageDetector() {}

    public static String findFriend(String raw, String self, Collection<String> friends) {
        Incoming incoming = parseIncoming(raw, self);
        return incoming == null ? null : matchFriend(incoming.sender(), self, friends);
    }

    public record Incoming(String sender, String body) {}

    public static List<String> replyCommands(String recipient, String body) {
        if (recipient == null || !recipient.matches("[A-Za-z0-9_]{1,16}") || body == null || body.isBlank()) return List.of();
        String prefix = "msg " + recipient + " ";
        int limit = 256 - prefix.length();
        List<String> commands = new ArrayList<>();
        for (int start = 0; start < body.length();) {
            int end = Math.min(body.length(), start + limit);
            if (end < body.length() && Character.isHighSurrogate(body.charAt(end - 1))) end--;
            commands.add(prefix + body.substring(start, end));
            start = end;
        }
        return commands;
    }

    public static Incoming parseIncoming(String raw, String self) {
        if (raw == null || self == null || raw.length() > 8192) return null;
        String text = raw.replaceAll("(?i)§[0-9a-fk-orx]", "").replace('\u00a0', ' ').strip();
        String sender = null;
        String body = null;
        var arrow = ARROW.matcher(text);
        if (arrow.matches()) {
            String recipient = arrow.group(2);
            if (!(recipient.equalsIgnoreCase(self) || recipient.equalsIgnoreCase("me")
                || recipient.equalsIgnoreCase("you") || recipient.equalsIgnoreCase("ja") || recipient.equalsIgnoreCase("ty"))) return null;
            sender = arrow.group(1);
            body = arrow.group(3);
        } else {
            for (Pattern pattern : new Pattern[]{FROM, WHISPER}) {
                var match = pattern.matcher(text);
                if (match.matches()) { sender = match.group(1); body = match.group(2); break; }
            }
        }
        return sender == null || sender.equalsIgnoreCase(self) || body == null || body.isBlank()
            ? null : new Incoming(sender, body.strip());
    }

    public static String matchFriend(String sender, String self, Collection<String> friends) {
        if (sender == null || sender.equalsIgnoreCase(self)) return null;
        for (String friend : friends) if (friend.equalsIgnoreCase(sender)) return friend;
        return null;
    }
}
