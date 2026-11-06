package dev.bkclient.utility.render;

import dev.bkclient.utility.render.shader.Shader;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL33C.*;

/** Opt-in hidden OpenGL context; does not start Minecraft. */
public final class ShaderLifecycleTest {
    public static void main(String[] args) {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW init failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(16, 16, "Shader lifecycle regression", 0, 0);
        if (window == 0) throw new AssertionError("GL context failed");
        try {
            GLFW.glfwMakeContextCurrent(window); GL.createCapabilities();
            for (String name : new String[]{"chroma", "solid", "balatro", "smoke", "stripes", "glow", "glass", "snow", "blend", "viewmodel_outline"}) verify("hand", name);
            for (String name : new String[]{"esp_down", "esp_up", "esp_combine"}) verify("bkengine", name);
            try { new Shader("missing", "missing"); throw new AssertionError("missing source accepted"); }
            catch (RuntimeException expected) { }
            verify("hand", "solid");
            if (glGetError() != GL_NO_ERROR) throw new AssertionError("GL lifecycle error");
            System.out.println("13 hand/ESP shader programs compiled; uniform lookup and repeated deletion passed");
        } finally { GL.setCapabilities(null); GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate(); }
    }
    private static void verify(String folder, String name) {
        Shader shader = new Shader(folder, name);
        int id = shader.getProgramId();
        try {
            if (!glIsProgram(id) || glGetProgrami(id, GL_LINK_STATUS) == 0) throw new AssertionError(name);
            shader.bind();
            for (int i=0; i<10; i++) shader.setUniform1f("missingUniformForLifecycleTest", 1);
            shader.unbind();
        } finally { shader.delete(); shader.delete(); }
        if (shader.getProgramId() != 0 || glIsProgram(id)) throw new AssertionError("program not released: " + name);
    }
}
