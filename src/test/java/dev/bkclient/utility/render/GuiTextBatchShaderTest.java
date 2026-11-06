package dev.bkclient.utility.render;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL33C.*;

/** Actual MSDF shaders: same-column fading must match individual draws pixel for pixel. */
public final class GuiTextBatchShaderTest {
    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(128,128,"GUI batch test",0,0);
        if (window == 0) throw new AssertionError("GL context");
        int program=0, vao=0, vbo=0, ebo=0, texture=0;
        try {
            GLFW.glfwMakeContextCurrent(window); GL.createCapabilities();
            Path root = Path.of("src/main/resources/assets/bkclient/shaders");
            String common = Files.readString(root.resolve("include/common.glsl"));
            int vertex = compile(GL_VERTEX_SHADER, Files.readString(root.resolve("core/msdf_font/vertex.vsh")).replace("#moj_import <bkclient:common.glsl>",common));
            int fragment = compile(GL_FRAGMENT_SHADER, Files.readString(root.resolve("core/msdf_font/fragment.fsh")).replace("#moj_import <bkclient:common.glsl>",common));
            program=glCreateProgram(); glAttachShader(program,vertex); glAttachShader(program,fragment);
            glBindAttribLocation(program,0,"Position"); glBindAttribLocation(program,1,"Color"); glBindAttribLocation(program,2,"UV0");
            glLinkProgram(program); glDeleteShader(vertex); glDeleteShader(fragment);
            if(glGetProgrami(program,GL_LINK_STATUS)==0) throw new AssertionError(glGetProgramInfoLog(program));
            glUseProgram(program);
            float[] identity={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
            glUniformMatrix4fv(glGetUniformLocation(program,"ModelViewMat"),false,identity);
            glUniformMatrix4fv(glGetUniformLocation(program,"ProjMat"),false,new float[]{2f/128,0,0,0,0,2f/128,0,0,0,0,1,0,-1,-1,0,1});
            glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);
            glUniform1f(glGetUniformLocation(program,"Smoothness"),.5f);
            glUniform1f(glGetUniformLocation(program,"Range"),4);
            glUniform1f(glGetUniformLocation(program,"Thickness"),.05f);
            glUniform1i(glGetUniformLocation(program,"Outline"),0);
            glUniform1i(glGetUniformLocation(program,"EnableFadeout"),1);
            glUniform1f(glGetUniformLocation(program,"FadeoutStart"),.85f);
            glUniform1f(glGetUniformLocation(program,"FadeoutEnd"),1);
            glUniform1f(glGetUniformLocation(program,"TextPosX"),7);
            texture=glGenTextures(); glBindTexture(GL_TEXTURE_2D,texture);
            ByteBuffer atlas=BufferUtils.createByteBuffer(16*16*4);
            for(int y=0;y<16;y++) for(int x=0;x<16;x++) {
                byte distance=(byte)(Math.min(Math.min(x,15-x),Math.min(y,15-y))>2?220:40);
                atlas.put(distance).put(distance).put(distance).put((byte)255);
            }
            atlas.flip(); glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,16,16,0,GL_RGBA,GL_UNSIGNED_BYTE,atlas);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
            glUniform1i(glGetUniformLocation(program,"Sampler0"),0);
            vao=glGenVertexArrays(); glBindVertexArray(vao);
            vbo=glGenBuffers(); glBindBuffer(GL_ARRAY_BUFFER,vbo);
            ebo=glGenBuffers(); glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,new int[]{0,1,2,2,3,0,4,5,6,6,7,4},GL_STATIC_DRAW);
            glEnableVertexAttribArray(0); glVertexAttribPointer(0,3,GL_FLOAT,false,36,0L);
            glEnableVertexAttribArray(1); glVertexAttribPointer(1,4,GL_FLOAT,false,36,12L);
            glEnableVertexAttribArray(2); glVertexAttribPointer(2,2,GL_FLOAT,false,36,28L);
            glViewport(0,0,128,128); glDisable(GL_DEPTH_TEST); glDisable(GL_CULL_FACE);
            glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_SCISSOR_TEST); glScissor(8,8,105,100);
            for(float[] shape : new float[][]{{42,11,30},{22,7,60},{62,12,35},{49,9,40}}) {
                glUniform1f(glGetUniformLocation(program,"MaxWidth"),shape[2]);
                glUniform4f(glGetUniformLocation(program,"Radius"),shape[2],shape[2],shape[2],shape[2]);
                float[] vertices=new float[72];
                for(int q=0;q<2;q++) for(int i=0;i<4;i++) {
                    int j=q*36+i*9;
                    vertices[j]=7-2+(i>=2?shape[0]+4:0);
                    vertices[j+1]=22+q*31-2+(i==1||i==2?shape[1]+4:0);
                    vertices[j+3]=q==0?.9f:.2f; vertices[j+4]=.7f; vertices[j+5]=.3f; vertices[j+6]=.8f;
                    vertices[j+7]=i>=2?1:0; vertices[j+8]=i==1||i==2?1:0;
                }
                glBufferData(GL_ARRAY_BUFFER,vertices,GL_DYNAMIC_DRAW);
                byte[] single=render(false), batch=render(true);
                if(!Arrays.equals(single,batch)) throw new AssertionError("Batch changed AA pixels");
                boolean nonzero=false; for(byte pixel:batch) if(pixel!=0) {nonzero=true;break;}
                if(!nonzero) throw new AssertionError("Empty draw");
            }
            if(glGetError()!=GL_NO_ERROR) throw new AssertionError("GL error");
            System.out.println("PASS: 4 MSDF text extents, same-column batches preserve pixels, fade, clipping and colors");
        } finally {
            if(texture!=0) glDeleteTextures(texture); if(program!=0) glDeleteProgram(program); if(vbo!=0) glDeleteBuffers(vbo); if(ebo!=0) glDeleteBuffers(ebo); if(vao!=0) glDeleteVertexArrays(vao);
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
