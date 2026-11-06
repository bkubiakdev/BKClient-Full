package dev.bkclient.utility.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Downsample during decoding, before allocating full-resolution pixels. */
public final class BoundedImageReader {
    private BoundedImageReader() {}
    public static BufferedImage read(File file, int maxDimension) throws IOException {
        if (maxDimension < 1) throw new IllegalArgumentException("Invalid image limit");
        try (var input = ImageIO.createImageInputStream(file)) {
            if (input == null) throw new IOException("Cannot open image");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported image");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1) throw new IOException("Invalid image dimensions");
                int sample = (int)Math.max(1, ((long)Math.max(width, height) + maxDimension - 1) / maxDimension);
                var parameters = reader.getDefaultReadParam();
                parameters.setSourceSubsampling(sample, sample, 0, 0);
                return reader.read(0, parameters);
            } finally { reader.dispose(); }
        }
    }
}
