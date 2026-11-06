package dev.bkclient.utility.render;

import java.net.URI;
import java.io.IOException;

public final class CustomCapeLinksTest {
    public static void main(String[] args) throws Exception {
        String url = "https://media1.tenor.com/m/example/animation.gif";
        if (!CustomCapeLinks.media("<meta content='" + url + "' property='og:image'>").toString().equals(url)) throw new AssertionError("Metadata order");
        if (!CustomCapeLinks.media("<meta name=\"twitter:image\" content=\"" + url + "?a=1&amp;b=2\">").toString().endsWith("?a=1&b=2")) throw new AssertionError("HTML entities");
        if (CustomCapeLinks.isTenor(URI.create("https://tenor.com.example.org/view/123"))) throw new AssertionError("Host boundary");
        for (String page : new String[]{"<img src='" + url + "'>", "<meta property='og:image' content='file:///a.gif'>", "<meta property='og:image' content='https://media.tenor.com/a.mp4'>"}) {
            try { CustomCapeLinks.media(page); throw new AssertionError("Unrelated media accepted"); }
            catch (IOException expected) { }
        }
        if (!CustomCapeLinks.media("<meta property='og:image' content='https://static.klipy.com/test.gif'>").getHost().equals("static.klipy.com")) throw new AssertionError("Other providers");
        if (args.length > 0) {
            byte[] bytes = CustomCapeLinks.download(URI.create(args[0]));
            var animation = CustomCapeImages.decode(bytes);
            if (animation.frames().size() < 2) throw new AssertionError("Expected animation");
            System.out.println("Live Tenor import: " + bytes.length + " bytes, " + animation.frames().size() + " frames, " + animation.durationMillis() + " ms");
        }
        System.out.println("Custom cape links: metadata, entities, host validation and missing GIF checks passed");
    }
}
