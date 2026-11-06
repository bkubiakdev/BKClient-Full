package dev.bkclient.utility.render.shader;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Shader {
    private int programId;
    private final java.util.Map<String, Integer> uniformLocations = new java.util.HashMap<>();

    public Shader(String folder, String name) {
        String vertexPath = "/assets/bkclient/shaders/" + folder + "/" + name + ".vsh";
        String fragmentPath = "/assets/bkclient/shaders/" + folder + "/" + name + ".fsh";

        int vertexId = 0, fragmentId = 0;
        boolean linked = false;
        try {
            vertexId = createShader(vertexPath, GL20.GL_VERTEX_SHADER);
            fragmentId = createShader(fragmentPath, GL20.GL_FRAGMENT_SHADER);
            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, vertexId);
            GL20.glAttachShader(programId, fragmentId);
            GL20.glLinkProgram(programId);
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
                throw new RuntimeException("Failed to link shader: " + GL20.glGetProgramInfoLog(programId));
            }
            linked = true;
        } finally {
            if (vertexId != 0) GL20.glDeleteShader(vertexId);
            if (fragmentId != 0) GL20.glDeleteShader(fragmentId);
            if (!linked) delete();
        }
    }

    private int createShader(String path, int type) {
        try {
            String source = readFile(path);
            int id = GL20.glCreateShader(type);
            boolean compiled = false;
            try {
                shaderSource(id, source);
                GL20.glCompileShader(id);
                if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == 0) {
                    throw new RuntimeException("Failed to compile shader: " + path + "\n" + GL20.glGetShaderInfoLog(id));
                }
                compiled = true;
                return id;
            } finally {
                if (!compiled) GL20.glDeleteShader(id);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }

    /**
     * Some AMD drivers (atio6axx.dll) scan the source with 16-byte SSE reads and crash when the text ends right at a
     * page boundary. The source is therefore passed NUL-terminated inside a zero-padded buffer.
     */
    private static void shaderSource(int id, String source) {
        byte[] bytes = source.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.ByteBuffer text = MemoryUtil.memCalloc(bytes.length + 64);
        PointerBuffer pointers = MemoryUtil.memAllocPointer(1);
        try {
            text.put(bytes).clear();
            pointers.put(0, MemoryUtil.memAddress(text));
            GL20.nglShaderSource(id, 1, MemoryUtil.memAddress(pointers), 0L);
        } finally {
            MemoryUtil.memFree(pointers);
            MemoryUtil.memFree(text);
        }
    }

    private String readFile(String path) throws IOException {
        InputStream stream = Shader.class.getResourceAsStream(path);
        if (stream == null) throw new IOException("Shader not found: " + path);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public int getProgramId() { return programId; }
    public void bind()   { GL20.glUseProgram(programId); }
    public void unbind() { GL20.glUseProgram(0); }
    public void delete() {
        if (programId != 0) GL20.glDeleteProgram(programId);
        programId = 0;
        uniformLocations.clear();
    }

    private int uniformLocation(String name) {
        return uniformLocations.computeIfAbsent(name, key -> GL20.glGetUniformLocation(programId, key));
    }

    public void setUniform1i(String name, int value) {
        int loc = uniformLocation(name);
        if (loc != -1) GL20.glUniform1i(loc, value);
    }
    public void setUniform1f(String name, float value) {
        int loc = uniformLocation(name);
        if (loc != -1) GL20.glUniform1f(loc, value);
    }
    public void setUniform2f(String name, Vector2f v) {
        int loc = uniformLocation(name);
        if (loc != -1) GL20.glUniform2f(loc, v.x, v.y);
    }
    public void setUniform3f(String name, Vector3f v) {
        int loc = uniformLocation(name);
        if (loc != -1) GL20.glUniform3f(loc, v.x, v.y, v.z);
    }
    public void setUniformBool(String name, boolean value) {
        setUniform1i(name, value ? 1 : 0);
    }

    public void setUniform4f(String name, float x, float y, float z, float w) {
        int loc = uniformLocation(name);
        if (loc != -1) GL20.glUniform4f(loc, x, y, z, w);
    }

    public void setUniformMatrix4f(String name, org.joml.Matrix4f matrix) {
        int loc = uniformLocation(name);
        if (loc == -1) return;
        float[] values = new float[16];
        matrix.get(values);
        GL20.glUniformMatrix4fv(loc, false, values);
    }

    public void setUniform4fv(String name, float[] values) {
        int loc = uniformLocation(name);
        if (loc != -1) GL20.glUniform4fv(loc, values);
    }

    public void setUniform1fv(String name, float[] values) {
        int loc = uniformLocation(name);
        if (loc != -1) GL20.glUniform1fv(loc, values);
    }
}
