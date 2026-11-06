package dev.bkclient.utility.render;

import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.w3c.dom.Node;

/** Bounded PNG/GIF decoding. No Minecraft or GPU access on the worker. */
public final class CustomCapeImages {
    public static final int WIDTH = 352, HEIGHT = 272, BYTES = WIDTH * HEIGHT * 4;
    public record Frame(byte[] rgba, int millis) {}
    public record Animation(List<Frame> frames, long durationMillis) {
        public int index(long elapsed) {
            long time = Math.floorMod(elapsed, durationMillis);
            for (int i = 0; i < frames.size(); i++) {
                time -= frames.get(i).millis();
                if (time < 0) return i;
            }
            return 0;
        }
    }
    private CustomCapeImages() {}
    private static Node child(Node node, String name) {
        if (node == null) return null;
        for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling())
            if (c.getNodeName().equals(name)) return c;
        return null;
    }
    private static String attr(Node node, String name, String fallback) {
        if (node == null || node.getAttributes().getNamedItem(name) == null) return fallback;
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }
    private static int number(Node node, String name, int fallback) {
        return Integer.parseInt(attr(node, name, Integer.toString(fallback)));
    }
    private static void dimensions(int w, int h) throws IOException {
        if (w < 1 || h < 1 || w > 2048 || h > 2048) throw new IOException("Image size must be 1..2048 pixels");
    }
    public static Animation decode(byte[] bytes) throws IOException {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Use a PNG or GIF file");
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                boolean gif = format.equalsIgnoreCase("gif");
                if (!gif && !format.equalsIgnoreCase("png")) throw new IOException("Use a PNG or GIF file");
                reader.setInput(input);
                int count = reader.getNumImages(true);
                if (count < 1 || count > 240) throw new IOException("Maximum 240 animation frames");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                int background = 0, backgroundIndex = -1;
                if (gif) {
                    Node stream = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
                    Node descriptor = child(stream, "LogicalScreenDescriptor");
                    Node table = child(stream, "GlobalColorTable");
                    backgroundIndex = number(table, "backgroundColorIndex", -1);
                    if (table != null) for (Node entry = table.getFirstChild(); entry != null; entry = entry.getNextSibling()) {
                        if (number(entry, "index", -2) == backgroundIndex) {
                            background = 0xff000000 | number(entry, "red", 0) << 16
                                    | number(entry, "green", 0) << 8 | number(entry, "blue", 0);
                        }
                    }
                    width = number(descriptor, "logicalScreenWidth", width);
                    height = number(descriptor, "logicalScreenHeight", height);
                }
                dimensions(width, height);
                BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                List<Frame> frames = new ArrayList<>();
                long duration = 0;
                for (int i = 0; i < count; i++) {
                    dimensions(reader.getWidth(i), reader.getHeight(i));
                    Node metadata = gif ? reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0") : null;
                    Node descriptor = child(metadata, "ImageDescriptor"), control = child(metadata, "GraphicControlExtension");
                    int x = number(descriptor, "imageLeftPosition", 0), y = number(descriptor, "imageTopPosition", 0);
                    int delay = gif ? Math.max(20, number(control, "delayTime", 10) * 10) : 1000;
                    String disposal = attr(control, "disposalMethod", "none");
                    BufferedImage previous = null;
                    if (disposal.equals("restoreToPrevious")) {
                        previous = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                        var copy = previous.createGraphics(); copy.drawImage(canvas, 0, 0, null); copy.dispose();
                    }
                    BufferedImage frame = reader.read(i);
                    if (x < 0 || y < 0 || x + frame.getWidth() > width || y + frame.getHeight() > height)
                        throw new IOException("Invalid GIF frame bounds");
                    var graphics = canvas.createGraphics(); graphics.drawImage(frame, x, y, null); graphics.dispose();
                    frames.add(new Frame(map(canvas), delay)); duration += delay;
                    if (disposal.equals("restoreToPrevious")) canvas = previous;
                    else if (disposal.equals("restoreToBackgroundColor")) {
                        graphics = canvas.createGraphics();
                        boolean transparentBackground = attr(control, "transparentColorFlag", "FALSE").equalsIgnoreCase("TRUE")
                                && number(control, "transparentColorIndex", -2) == backgroundIndex;
                        graphics.setComposite(AlphaComposite.Src);
                        graphics.setColor(new java.awt.Color(transparentBackground ? 0 : background, true));
                        graphics.fillRect(x, y, frame.getWidth(), frame.getHeight()); graphics.dispose();
                    }
                }
                return new Animation(List.copyOf(frames), duration);
            } finally { reader.dispose(); }
        }
    }
    /** Keeps standard 64x32 atlases; stretches ordinary images onto both faces. */
    static byte[] map(BufferedImage source) {
        BufferedImage atlas = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_ARGB);
        var g = atlas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (source.getWidth() % 64 == 0 && source.getWidth() == source.getHeight() * 2) {
            g.drawImage(source, 0, 0, 1024, 512, null);
        } else {
            g.setColor(java.awt.Color.BLACK); g.fillRect(0, 0, 1024, 512);
            g.drawImage(source, 16, 16, 160, 256, null);
            g.drawImage(source, 192, 16, 160, 256, null);
        }
        g.dispose();
        byte[] rgba = new byte[BYTES];
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            int argb = atlas.getRGB(x, y), offset = (y * WIDTH + x) * 4;
            rgba[offset] = (byte)(argb >> 16); rgba[offset + 1] = (byte)(argb >> 8);
            rgba[offset + 2] = (byte)argb; rgba[offset + 3] = (byte)(argb >> 24);
        }
        return rgba;
    }
}
