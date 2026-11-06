package dev.bkclient.systems.emoji;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.Identifier;

public class EmojiManager {

    private static final Map<String, EmojiData> EMOJIS = new LinkedHashMap<>();

    static {
        register("grin", "grinning-face_1f600.png");
        register("happy", "grinning-face-with-big-eyes_1f603.png");
        register("smile", "grinning-face-with-smiling-eyes_1f604.png");
        register("sweat_smile", "grinning-face-with-sweat_1f605.png");
        register("lol", "grinning-squinting-face_1f606.png");
        register("beaming", "beaming-face-with-smiling-eyes_1f601.png");
        register("joy", "face-with-tears-of-joy_1f602.png");
        register("rofl", "rolling-on-the-floor-laughing_1f923.png");
        register("slight_smile", "slightly-smiling-face_1f642.png");
        register("upside_down", "upside-down-face_1f643.png");
        register("wink", "winking-face_1f609.png");
        register("blush", "smiling-face-with-smiling-eyes_1f60a.png");
        register("heart_eyes", "smiling-face-with-heart-eyes_1f60d.png");
        register("kiss", "face-blowing-a-kiss_1f618.png");
        register("tongue", "face-with-tongue_1f61b.png");
        register("squint_tongue", "squinting-face-with-tongue_1f61d.png");
        register("wink_tongue", "winking-face-with-tongue_1f61c.png");
        register("money", "money-mouth-face_1f911.png");
        register("hug", "hugging-face_1f917.png");
        register("thinking", "thinking-face_1f914.png");
        register("shush", "shushing-face_1f92b.png");
        register("oops", "face-with-hand-over-mouth_1f92d.png");
        register("shocked", "face-with-open-eyes-and-hand-over-mouth_1fae2.png");
        register("peek", "face-with-peeking-eye_1fae3.png");
        register("melting", "melting-face_1fae0.png");
        register("salute", "saluting-face_1fae1.png");
        register("star_struck", "star-struck_1f929.png");
        register("halo", "smiling-face-with-halo_1f607.png");
        register("hearts", "smiling-face-with-hearts_1f970.png");
        register("tear_smile", "smiling-face-with-tear_1f972.png");
        register("zany", "zany-face_1f92a.png");
        register("kiss_face", "kissing-face_1f617.png");
        register("kiss_closed", "kissing-face-with-closed-eyes_1f61a.png");
        register("kiss_smile", "kissing-face-with-smiling-eyes_1f619.png");
        register("smiling", "smiling-face_263a-fe0f.png");
        register("yummy", "face-savoring-food_1f60b.png");
    }

    private static void register(String code, String fileName) {
         EMOJIS.put(code, new EmojiData(code, fileName, Identifier.of("bkclient", "emojis/faces/" + fileName)));
    }

    public static Map<String, EmojiData> getAll() {
        return EMOJIS;
    }

    public static EmojiData getByCode(String code) {
        return EMOJIS.get(code);
    }

    public static boolean isEmojiCode(String code) {
        return EMOJIS.containsKey(code);
    }

    public static final class EmojiData {
        private final String code;
        private final String fileName;
        private final Identifier texture;

        EmojiData(String code, String fileName, Identifier texture) {
            this.code = code;
            this.fileName = fileName;
            this.texture = texture;
        }

        public String code() { return code; }
        public String fileName() { return fileName; }
        public Identifier texture() { return texture; }
    }
}
