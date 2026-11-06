package dev.bkclient.utility.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.lwjgl.opengl.GL33C.*;

/** Optional GPU regression: actual ported shaders with an isolated, synthetic hand mask. */
public final class FlameHandsShaderTest {
    private static final int SIZE = 128;
    private static final int[] programs = new int[3], textures = new int[5], fbos = new int[5];

    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW init failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(SIZE, SIZE, "BK Flame shader regression", 0, 0);
        if (window == 0) throw new AssertionError("GL context failed");
        int vao = 0, vbo = 0;
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            String[] stages = {"glow", "trail", "composite"};
            Path dir = Path.of("src/main/resources/assets/bkclient/shaders/hand");
            for (int i = 0; i < stages.length; i++) {
                int vertex = compile(GL_VERTEX_SHADER, Files.readString(dir.resolve("flame_" + stages[i] + ".vsh")));
                int fragment = compile(GL_FRAGMENT_SHADER, Files.readString(dir.resolve("flame_" + stages[i] + ".fsh")));
                programs[i] = glCreateProgram();
                glAttachShader(programs[i], vertex); glAttachShader(programs[i], fragment);
                glLinkProgram(programs[i]);
                check(glGetProgrami(programs[i], GL_LINK_STATUS) != 0, glGetProgramInfoLog(programs[i]));
                glDeleteShader(vertex); glDeleteShader(fragment);
            }
            vao = glGenVertexArrays(); glBindVertexArray(vao);
            vbo = glGenBuffers(); glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, new float[]{-1,-1,0, 1,-1,0, 1,1,0, -1,-1,0, 1,1,0, -1,1,0}, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0); glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
            glViewport(0, 0, SIZE, SIZE);
            glDisable(GL_DEPTH_TEST); glDisable(GL_BLEND); glDisable(GL_CULL_FACE);
            for (int i = 0; i < textures.length; i++) {
                textures[i] = glGenTextures(); glBindTexture(GL_TEXTURE_2D, textures[i]);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, SIZE, SIZE, 0, GL_RGBA, GL_FLOAT, 0L);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                fbos[i] = glGenFramebuffers(); glBindFramebuffer(GL_FRAMEBUFFER, fbos[i]);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textures[i], 0);
                check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE, "Incomplete target");
            }
            FloatBuffer empty = render(false, false, false);
            check(energy(empty) < 0.0001, "Empty hand mask changed the scene");
            FloatBuffer outline = render(true, false, false);
            check(energy(outline) > 1, "No flame/glow outside the hand");
            check(pixelEnergy(outline, 64, 60) < 0.0001, "Outline mode covered the item interior");
            check(pixelEnergy(outline, 2, 2) < 0.0001, "Flame leaked to distant background");
            FloatBuffer fill = render(true, true, false);
            check(pixelEnergy(fill, 64, 60) > 0.05, "Fill option did not cover the item");
            FloatBuffer green = render(true, true, true);
            int center = (60 * SIZE + 64) * 4;
            check(green.get(center + 1) > green.get(center) * 2, "Custom color mix ignored");
            check(glGetError() == GL_NO_ERROR, "OpenGL error");
            Path output = Path.of("build/flame-hands-gpu.png");
            BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
                int offset = (y * SIZE + x) * 4;
                int rgb = 0;
                for (int c = 0; c < 3; c++) rgb = (rgb << 8) | Math.round(Math.clamp(outline.get(offset + c), 0f, 1f) * 255);
                image.setRGB(x, SIZE - 1 - y, rgb);
            }
            ImageIO.write(image, "png", output.toFile());
            System.out.println("PASS: 3 Flame programs, empty mask, halo, item exclusion, fill, custom color, GL errors");
        } finally {
            if (vbo != 0) glDeleteBuffers(vbo);
            if (vao != 0) glDeleteVertexArrays(vao);
            for (int program : programs) if (program != 0) glDeleteProgram(program);
            for (int texture : textures) if (texture != 0) glDeleteTextures(texture);
            for (int fbo : fbos) if (fbo != 0) glDeleteFramebuffers(fbo);
            GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate();
        }
    }

    private static FloatBuffer render(boolean mask, boolean fill, boolean green) {
        glClearColor(0, 0, 0, 0);
        for (int fbo : fbos) { glBindFramebuffer(GL_FRAMEBUFFER, fbo); glClear(GL_COLOR_BUFFER_BIT); }
        FloatBuffer pixels = BufferUtils.createFloatBuffer(SIZE * SIZE * 4);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            boolean inside = mask && x >= 56 && x < 72 && y >= 24 && y < 102;
            pixels.put(inside ? 0.65f : 0).put(inside ? 0.25f : 0).put(inside ? 0.08f : 0).put(inside ? 1f : 0);
        }
        pixels.flip(); glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, SIZE, SIZE, GL_RGBA, GL_FLOAT, pixels);
        int history = 2, next = 3;
        for (int frame = 0; frame < 12; frame++) {
            pass(0, 1, fill, green, frame);
            sampler(0, "HandSampler", 0, 0); sampler(0, "MaskSampler", 1, 0); glDrawArrays(GL_TRIANGLES, 0, 6);
            pass(1, next, fill, green, frame);
            sampler(1, "TrailSampler", 0, history); sampler(1, "GlowSampler", 1, 1);
            sampler(1, "HandSampler", 2, 0); sampler(1, "MaskSampler", 3, 0); glDrawArrays(GL_TRIANGLES, 0, 6);
            int old = history; history = next; next = old;
        }
        pass(2, 4, fill, green, 12);
        sampler(2, "MaskSampler", 0, 0); sampler(2, "GlowSampler", 1, 1); sampler(2, "TrailSampler", 2, history);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        FloatBuffer result = BufferUtils.createFloatBuffer(SIZE * SIZE * 4);
        glReadPixels(0, 0, SIZE, SIZE, GL_RGBA, GL_FLOAT, result);
        return result;
    }

    private static void pass(int stage, int target, boolean fill, boolean green, int frame) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbos[target]); glUseProgram(programs[stage]);
        uniform(stage, "Screen", SIZE, SIZE, frame / 60f, fill ? 1 : 0);
        uniform(stage, "GlowParams", 15, 1.18f, 1.1f, 1);
        uniform(stage, "TrailParams", 0.94f, 28, 1.1f, 1.16f);
        uniform(stage, "FireParams", green ? 0 : 1, green ? 1 : 0.44f, 0.12f, 1.18f);
    }
    private static void uniform(int stage, String name, float a, float b, float c, float d) {
        glUniform4f(glGetUniformLocation(programs[stage], name), a, b, c, d);
    }
    private static void sampler(int stage, String name, int unit, int texture) {
        glActiveTexture(GL_TEXTURE0 + unit); glBindTexture(GL_TEXTURE_2D, textures[texture]);
        glUniform1i(glGetUniformLocation(programs[stage], name), unit);
    }
    private static int compile(int type, String source) {
        int shader = glCreateShader(type); glShaderSource(shader, source); glCompileShader(shader);
        check(glGetShaderi(shader, GL_COMPILE_STATUS) != 0, glGetShaderInfoLog(shader));
        return shader;
    }
    private static double energy(FloatBuffer pixels) {
        double sum = 0;
        for (int i = 0; i < pixels.capacity(); i++) {
            check(Float.isFinite(pixels.get(i)), "Non-finite output");
            if (i % 4 != 3) sum += pixels.get(i);
        }
        return sum;
    }
    private static float pixelEnergy(FloatBuffer pixels, int x, int y) {
        int p = (y * SIZE + x) * 4;
        return pixels.get(p) + pixels.get(p + 1) + pixels.get(p + 2);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
