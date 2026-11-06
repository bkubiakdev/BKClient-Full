package dev.bkclient.compat;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import java.nio.file.*;
import static org.lwjgl.opengl.GL33C.*;

/** Checks the actual postprocess shader, including depth reconstruction and a moving world anchor. */
public final class WorldSaturationGpuTest {
    private static final int W=128,H=128;
    private static int program,checks;
    private static final float R=.22f,G=.72f,B=.36f,A=.7f;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static int compile(int type,String source){int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);
        check(glGetShaderi(s,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(s));return s;}
    private static int texture(float[] data){int t=glGenTextures();glBindTexture(GL_TEXTURE_2D,t);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA32F,W,H,0,GL_RGBA,GL_FLOAT,data);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);return t;}
    private static void i(String name,int value){glUniform1i(glGetUniformLocation(program,name),value);}
    private static void f(String name,float value){glUniform1f(glGetUniformLocation(program,name),value);}
    private static void pair(String name,float x,float y){glUniform2f(glGetUniformLocation(program,name),x,y);}
    private static void center(float x,float y,float z){glUniform3f(glGetUniformLocation(program,"CenterRelative"),x,y,z);}
    private static float[] draw(){glDrawArrays(GL_TRIANGLES,0,6);float[] pixels=new float[W*H*4];glReadPixels(0,0,W,H,GL_RGBA,GL_FLOAT,pixels);
        check(glGetError()==GL_NO_ERROR,"clean GL");for(int p=0;p<pixels.length;p++){check(Float.isFinite(pixels[p]),"finite output");if(p%4==3)check(Math.abs(pixels[p]-A)<1e-6,"alpha preserved");}return pixels;}
    private static float channel(float[] pixels,int x,int y,int c){return pixels[(y*W+x)*4+c];}
    private static float chroma(float[] pixels,int x,int y){return channel(pixels,x,y,1)-channel(pixels,x,y,0);}
    private static void close(float actual,float expected,String name){check(Math.abs(actual-expected)<.0001,name+": "+actual+" != "+expected);}
    public static void main(String[] args)throws Exception{
        check(GLFW.glfwInit(),"GLFW");GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);
        long window=GLFW.glfwCreateWindow(W,H,"World saturation test",0,0);check(window!=0,"GL context");
        try{
            GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();glBindVertexArray(glGenVertexArrays());glViewport(0,0,W,H);
            Path path=Path.of("src/main/resources/assets/bkclient/shaders/ambience");
            int v=compile(GL_VERTEX_SHADER,Files.readString(path.resolve("saturation.vsh"))),frag=compile(GL_FRAGMENT_SHADER,Files.readString(path.resolve("saturation.fsh")));
            program=glCreateProgram();glAttachShader(program,v);glAttachShader(program,frag);glLinkProgram(program);
            check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);
            int output=texture(null);glBindFramebuffer(GL_FRAMEBUFFER,glGenFramebuffers());glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,output,0);
            check(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE,"FBO");
            float[] color=new float[W*H*4],depth=new float[color.length];
            for(int y=0;y<H;y++)for(int x=0;x<W;x++){int p=(y*W+x)*4;color[p]=R;color[p+1]=G;color[p+2]=B;color[p+3]=A;depth[p]=y==H-1?1:.5f;}
            glActiveTexture(GL_TEXTURE0);texture(color);i("Sampler0",0);glActiveTexture(GL_TEXTURE1);texture(depth);i("Sampler1",1);
            // Looking down onto a flat XZ plane, spanning -16..16 blocks.
            glUniformMatrix4fv(glGetUniformLocation(program,"InvViewProj"),false,new Matrix4f().rotationX((float)Math.PI/2).scale(16).get(new float[16]));
            i("Radial",0);i("Spherical",0);i("IncludeSky",1);f("GlobalSaturation",1);pair("NearFar",1,0);pair("RadiusTransition",4,6);center(0,0,0);
            var natural=draw();close(channel(natural,64,64,0),R,"100% unchanged");close(channel(natural,64,64,1),G,"100% unchanged green");
            f("GlobalSaturation",0);var gray=draw();close(chroma(gray,64,64),0,"global gray");close(channel(gray,64,64,0),R*.2126f+G*.7152f+B*.0722f,"luminance");
            f("GlobalSaturation",2);var vivid=draw();check(chroma(vivid,64,64)>chroma(natural,64,64),"200% vivid");
            i("Radial",1);var ring=draw();close(chroma(ring,64,64),G-R,"near remains colorful");close(chroma(ring,120,64),0,"outside gray");
            check(chroma(ring,92,64)>0&&chroma(ring,92,64)<G-R,"smooth transition band");
            float previous=G-R;for(int x=65;x<126;x++){float current=chroma(ring,x,64);check(current<=previous+.00001,"monotonic radius");check(previous-current<.04,"no hard edge");previous=current;}
            close(chroma(ring,64,H-1),0,"sky gets far saturation");i("IncludeSky",0);var noSky=draw();close(chroma(noSky,64,H-1),G-R,"sky excluded");
            center(11,0,0);var moved=draw();close(chroma(moved,104,64),G-R,"radius follows world anchor");close(chroma(moved,64,64),0,"old center outside moved radius");
            center(0,100,0);var column=draw();close(chroma(column,64,64),G-R,"cylinder independent of height");i("Spherical",1);var ball=draw();close(chroma(ball,64,64),0,"sphere includes height");
            center(0,0,0);i("Spherical",0);pair("NearFar",0,1);var inverse=draw();close(chroma(inverse,64,64),0,"inverse inside gray");close(chroma(inverse,120,64),G-R,"inverse outside colorful");
            pair("NearFar",1,0);pair("RadiusTransition",8,.5f);var thin=draw();close(chroma(thin,64,64),G-R,"minimum feather valid");
            pair("RadiusTransition",4,64);var wide=draw();check(chroma(wide,120,64)>.3,"wide feather valid");
            System.out.println("WorldSaturationGpuTest PASS "+checks+": radius, smooth edge, moved anchor, column/sphere, sky, inverse, alpha, global and vivid; "+glGetString(GL_RENDERER));
        }finally{GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
}
