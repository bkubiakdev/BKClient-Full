package dev.bkclient.compat;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL33C.*;

/** Isolates material animation from perspective using the actual production shader pair. */
public final class ArmorGlassGpuTest {
    private static final int SIZE = 128;

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) throw new AssertionError(glGetShaderInfoLog(shader));
        return shader;
    }

    private static int texture(int size, float[] pixels) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, size, size, 0, GL_RGBA, GL_FLOAT, pixels);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return texture;
    }

    private static float[] render(int program, Matrix4f view, float time, float shine) {
        // Compensate the view only in projection: identical surface samples, different view directions.
        // Thus this checks material attachment, without confusing perspective with a moving highlight.
        glUniformMatrix4fv(glGetUniformLocation(program, "ModelViewMat"), false, view.get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(program, "ProjMat"), false, new Matrix4f(view).invert().get(new float[16]));
        glUniform1f(glGetUniformLocation(program, "Time"), time);
        glUniform4f(glGetUniformLocation(program, "Material"), .38f, 6f, shine, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        float[] pixels = new float[SIZE * SIZE * 4];
        glReadPixels(0, 0, SIZE, SIZE, GL_RGBA, GL_FLOAT, pixels);
        if (glGetError() != GL_NO_ERROR) throw new AssertionError("GL error");
        for (float value : pixels) if (!Float.isFinite(value)) throw new AssertionError("Non-finite material output");
        return pixels;
    }

    private static float difference(float[] a, float[] b) {
        float maximum = 0;
        for (int i = 0; i < a.length; i++) maximum = Math.max(maximum, Math.abs(a[i] - b[i]));
        return maximum;
    }

    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW init");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        long window = GLFW.glfwCreateWindow(SIZE, SIZE, "Armor sheen regression", 0, 0);
        if (window == 0) throw new AssertionError("GL context");
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            glViewport(0, 0, SIZE, SIZE);
            glBindVertexArray(glGenVertexArrays());
            Path shaders = Path.of("src/main/resources/assets/bkclient/shaders/core/armor_glass");
            int vertex = compile(GL_VERTEX_SHADER, Files.readString(shaders.resolve("glass.vsh")));
            int fragment = compile(GL_FRAGMENT_SHADER, Files.readString(shaders.resolve("glass.fsh")));
            int program = glCreateProgram();
            glAttachShader(program, vertex);
            glAttachShader(program, fragment);
            glBindAttribLocation(program, 0, "Position");
            glBindAttribLocation(program, 1, "UV0");
            glBindAttribLocation(program, 2, "UV1");
            glBindAttribLocation(program, 3, "Normal");
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == 0) throw new AssertionError(glGetProgramInfoLog(program));
            glUseProgram(program);

            glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
            glBufferData(GL_ARRAY_BUFFER, new float[]{-1,-1,0, 0,0, 1,-1,0, 1,0, -1,1,0, 0,1, 1,1,0, 1,1}, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 20, 0L);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 20, 12L);
            glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
            glBufferData(GL_ARRAY_BUFFER, new int[]{0,0, 1,0, 0,1, 1,1}, GL_STATIC_DRAW);
            glEnableVertexAttribArray(2);
            glVertexAttribIPointer(2, 2, GL_INT, 8, 0L);
            glVertexAttrib3f(3, 0, 0, 1);

            int output = texture(SIZE, null);
            glBindFramebuffer(GL_FRAMEBUFFER, glGenFramebuffers());
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, output, 0);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) throw new AssertionError("FBO");
            glActiveTexture(GL_TEXTURE0);
            texture(1, new float[]{1, 1, 1, 1});
            glUniform1i(glGetUniformLocation(program, "Sampler0"), 0);
            glActiveTexture(GL_TEXTURE1);
            texture(1, new float[]{.08f, .10f, .14f, 1});
            glUniform1i(glGetUniformLocation(program, "Sampler1"), 1);
            glUniform2f(glGetUniformLocation(program, "Viewport"), SIZE, SIZE);
            glUniform3f(glGetUniformLocation(program, "Tint"), .82f, .78f, 1);

            Matrix4f front = new Matrix4f().translation(0, 0, -4);
            Matrix4f[] views = {
                new Matrix4f().translation(1.7f, -.6f, -2.5f),
                new Matrix4f().translation(-2f, 1f, -7f).rotateY(.9f),
                new Matrix4f().translation(0, 0, -4).rotateX(-.65f).rotateY(-1.1f),
                new Matrix4f().translation(.3f, -.2f, -3).rotateZ(.6f)
            };
            for (float time : new float[]{0, .5f, 3f, 19.25f}) {
                float[] baseline = render(program, front, time, 1.2f);
                for (Matrix4f view : views) {
                    float difference = difference(baseline, render(program, view, time, 1.2f));
                    if (difference > .0002f) throw new AssertionError("Camera moved sheen/opacity: max channel delta=" + difference + ", time=" + time);
                }
            }
            float animated = difference(render(program, front, 0, 1.2f), render(program, front, 3, 1.2f));
            if (animated < .05f) throw new AssertionError("Flow no longer animates");
            float disabled = difference(render(program, front, 0, 0), render(program, front, 3, 0));
            if (disabled > .0002f) throw new AssertionError("Shine=0 leaves animated highlights");
            System.out.println("PASS: 16 camera/time comparisons; sheen animates; Shine=0 disables it; finite output and no GL errors. GPU=" + glGetString(GL_RENDERER));
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }
}
