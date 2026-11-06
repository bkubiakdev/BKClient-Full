package dev.bkclient.compat;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import java.nio.file.*;
import static org.lwjgl.opengl.GL33.*;

/** Runs the production star function on a planar surface at different pixel footprints. */
public final class TerrainStarsGpuTest {
    private static int compile(int type, String source) {
        int s=glCreateShader(type); glShaderSource(s,source); glCompileShader(s);
        if(glGetShaderi(s,GL_COMPILE_STATUS)==0) throw new AssertionError(glGetShaderInfoLog(s));
        return s;
    }
    private static double mean(int program, float span, float density, float size, float angle) {
        glUniform2f(glGetUniformLocation(program,"span"),span,span*angle);
        glUniform4f(glGetUniformLocation(program,"bkStarsParams"),1.25f,1,size,density);
        glDrawArrays(GL_TRIANGLES,0,3);
        float[] pixels=new float[256*256*4];glReadPixels(0,0,256,256,GL_RGBA,GL_FLOAT,pixels);
        double sum=0;
        for(int i=0;i<pixels.length;i+=4){if(!Float.isFinite(pixels[i]) || pixels[i]<0)throw new AssertionError("Invalid pixel");sum+=pixels[i];}
        if(glGetError()!=GL_NO_ERROR)throw new AssertionError("GL error");
        return sum/(256*256);
    }
    public static void main(String[] args)throws Exception {
        if(!GLFW.glfwInit())throw new AssertionError("GLFW");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);
        long w=GLFW.glfwCreateWindow(256,256,"Stars minification test",0,0);
        if(w==0)throw new AssertionError("GL context");
        try {
            GLFW.glfwMakeContextCurrent(w);GL.createCapabilities();glBindVertexArray(glGenVertexArrays());glViewport(0,0,256,256);
            int tex=glGenTextures();glBindTexture(GL_TEXTURE_2D,tex);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA32F,256,256,0,GL_RGBA,GL_FLOAT,(java.nio.ByteBuffer)null);
            int fbo=glGenFramebuffers();glBindFramebuffer(GL_FRAMEBUFFER,fbo);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,tex,0);
            if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("FBO");
            String v="#version 330\nuniform vec2 span;out vec3 bkStarsPosition;out float bkDistance;void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(p*2.0-1.0,0,1);bkStarsPosition=vec3((p.x-.5)*span.x,0,(p.y-.5)*span.y);bkDistance=0;}";
            String body=Files.readString(Path.of("src/main/resources/assets/bkclient/shaders/sodium/terrain_fragment.glsl"));
            int vs=compile(GL_VERTEX_SHADER,v),fs=compile(GL_FRAGMENT_SHADER,"#version 330\n"+body+"\nout vec4 color;void main(){color=vec4(vec3(bkSurfaceStars()),1);}");
            int p=glCreateProgram();glAttachShader(p,vs);glAttachShader(p,fs);glLinkProgram(p);
            if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new AssertionError(glGetProgramInfoLog(p));glUseProgram(p);
            for(float size:new float[]{.25f,1,2})for(float density:new float[]{1,3,6}) {
                double near=mean(p,4,density,size,1),far=mean(p,24,density,size,1),grazing=mean(p,4,density,size,10);
                System.out.printf("size=%.2f density=%.0f near=%.6f far=%.6f grazing=%.6f%n",size,density,near,far,grazing);
                if(near<=0)throw new AssertionError("Close stars disappeared");
                if(far>near*1.5 || grazing>near*1.5)throw new AssertionError("Minification increases star energy");
            }
            System.out.println("PASS: production Stars shader; minification/grazing angles and size/density extremes, GPU="+glGetString(GL_RENDERER));
        } finally {GLFW.glfwDestroyWindow(w);GLFW.glfwTerminate();}
    }
}
