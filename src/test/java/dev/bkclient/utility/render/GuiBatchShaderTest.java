package dev.bkclient.utility.render;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL33C.*;

/** Actual rectangle shaders: verify individual versus batched screen-AA quads pixel for pixel. */
public final class GuiBatchShaderTest {
    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(128,128,"GUI batch test",0,0);
        if (window == 0) throw new AssertionError("GL context");
        int program=0, vao=0, vbo=0, ebo=0;
        try {
            GLFW.glfwMakeContextCurrent(window); GL.createCapabilities();
            Path root = Path.of("src/main/resources/assets/bkclient/shaders");
            String common = Files.readString(root.resolve("include/common.glsl"));
            int vertex = compile(GL_VERTEX_SHADER, Files.readString(root.resolve("core/position_color.vsh")).replace("#moj_import <bkclient:common.glsl>",common));
            int fragment = compile(GL_FRAGMENT_SHADER, Files.readString(root.resolve("core/rectangle/fragment.fsh")).replace("#moj_import <bkclient:common.glsl>",common));
            program=glCreateProgram(); glAttachShader(program,vertex); glAttachShader(program,fragment);
            glBindAttribLocation(program,0,"Position"); glBindAttribLocation(program,1,"Color");
            glLinkProgram(program); glDeleteShader(vertex); glDeleteShader(fragment);
            if(glGetProgrami(program,GL_LINK_STATUS)==0) throw new AssertionError(glGetProgramInfoLog(program));
            glUseProgram(program);
            float[] identity={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
            glUniformMatrix4fv(glGetUniformLocation(program,"ModelViewMat"),false,identity);
            glUniformMatrix4fv(glGetUniformLocation(program,"ProjMat"),false,new float[]{2f/128,0,0,0,0,2f/128,0,0,0,0,1,0,-1,-1,0,1});
            glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);
            glUniform1f(glGetUniformLocation(program,"Smoothness"),-2);
            vao=glGenVertexArrays(); glBindVertexArray(vao);
            vbo=glGenBuffers(); glBindBuffer(GL_ARRAY_BUFFER,vbo);
            ebo=glGenBuffers(); glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,new int[]{0,1,2,2,3,0,4,5,6,6,7,4},GL_STATIC_DRAW);
            glEnableVertexAttribArray(0); glVertexAttribPointer(0,3,GL_FLOAT,false,28,0L);
            glEnableVertexAttribArray(1); glVertexAttribPointer(1,4,GL_FLOAT,false,28,12L);
            glViewport(0,0,128,128); glDisable(GL_DEPTH_TEST); glDisable(GL_CULL_FACE);
            glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_SCISSOR_TEST); glScissor(8,8,105,100);
            for(float[] shape : new float[][]{{22,11,5.5f},{7.4f,7.4f,3.7f},{22,12,6},{9,9,4.5f}}) {
                glUniform2f(glGetUniformLocation(program,"Size"),shape[0]+4,shape[1]+4);
                glUniform4f(glGetUniformLocation(program,"Radius"),shape[2],shape[2],shape[2],shape[2]);
                float[] vertices=new float[56];
                for(int q=0;q<2;q++) for(int i=0;i<4;i++) {
                    int j=q*28+i*7;
                    vertices[j]=7+q*47-2+(i>=2?shape[0]+4:0);
                    vertices[j+1]=22+q*31-2+(i==1||i==2?shape[1]+4:0);
                    vertices[j+3]=q==0?.9f:.2f; vertices[j+4]=.7f; vertices[j+5]=.3f; vertices[j+6]=.8f;
                }
                glBufferData(GL_ARRAY_BUFFER,vertices,GL_DYNAMIC_DRAW);
                byte[] single=render(false), batch=render(true);
                if(!Arrays.equals(single,batch)) throw new AssertionError("Batch changed AA pixels");
                boolean nonzero=false; for(byte pixel:batch) if(pixel!=0) {nonzero=true;break;}
                if(!nonzero) throw new AssertionError("Empty draw");
            }
            if(glGetError()!=GL_NO_ERROR) throw new AssertionError("GL error");
            System.out.println("PASS: 4 toggle shapes, individual and batched AA pixels identical, clipping and colors preserved");
        } finally {
            if(program!=0) glDeleteProgram(program); if(vbo!=0) glDeleteBuffers(vbo); if(ebo!=0) glDeleteBuffers(ebo); if(vao!=0) glDeleteVertexArrays(vao);
            GL.setCapabilities(null); GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate();
        }
    }
    private static byte[] render(boolean batch) {
        glClearColor(0,0,0,0); glClear(GL_COLOR_BUFFER_BIT);
        if(batch) glDrawElements(GL_TRIANGLES,12,GL_UNSIGNED_INT,0L);
        else {glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_INT,0L);glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_INT,24L);}
        ByteBuffer pixels=BufferUtils.createByteBuffer(128*128*4); glReadPixels(0,0,128,128,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
        byte[] result=new byte[pixels.remaining()];pixels.get(result);return result;
    }
    private static int compile(int type,String source) {
        int id=glCreateShader(type);glShaderSource(id,source);glCompileShader(id);
        if(glGetShaderi(id,GL_COMPILE_STATUS)==0) throw new AssertionError(glGetShaderInfoLog(id));return id;
    }
}
