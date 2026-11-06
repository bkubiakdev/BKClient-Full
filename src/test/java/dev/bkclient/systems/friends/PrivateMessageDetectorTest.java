package dev.bkclient.systems.friends;

import java.util.List;

public final class PrivateMessageDetectorTest {
    public static void main(String[] args) {
        for (String raw : List.of("[Alice -> Me] hello", "[Alice » Ty] hello", "[Alice -> Local] hello",
                "[From Alice] hello", "Od Alice: hello", "Alice whispers to you: hello",
                "Alice szepcze do ciebie: hello", "§a[VIP] Alice whispers to you: hello")) {
            var found = PrivateMessageDetector.parseIncoming(raw, "Local");
            check(found != null && found.sender().equals("Alice") && found.body().equals("hello"), "Parse: " + raw);
        }
        for (String raw : List.of("[Local -> Alice] hello", "[Alice -> Bob] hello", "<Alice> hello",
                "Server: Alice whispers to you: hello", "[From Alice]", "", "[From Alice]   ")) {
            check(PrivateMessageDetector.parseIncoming(raw, "Local") == null, "Reject: " + raw);
        }
        check(PrivateMessageDetector.findFriend("[From Alice] hello", "Local", List.of("aLiCe")).equals("aLiCe"), "Friend match");
        check(PrivateMessageDetector.findFriend("[From Bob] hello", "Local", List.of("Alice")) == null, "Non-friend filter");
        String body = "/spawn " + "hello 🙂 ".repeat(100);
        List<String> commands = PrivateMessageDetector.replyCommands("Alice", body);
        StringBuilder restored = new StringBuilder();
        for (String command : commands) {
            check(command.startsWith("msg Alice ") && command.length() <= 256, "Private chunk");
            check(!Character.isHighSurrogate(command.charAt(command.length() - 1)), "Unicode boundary");
            restored.append(command.substring("msg Alice ".length()));
        }
        check(restored.toString().equals(body), "Lossless chunking");
        check(PrivateMessageDetector.replyCommands("Alice /spawn", "hello").isEmpty(), "Invalid recipient");
        check(PrivateMessageDetector.replyCommands("Alice", " ").isEmpty(), "Empty reply");
        System.out.println("Private message parsing and reply checks passed");
    }

    private static void check(boolean result, String label) {
        if (!result) throw new AssertionError(label);
    }
}
