package dev.bkclient.compat;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import java.nio.file.*;
import static org.lwjgl.opengl.GL33C.*;

/** Executes the real scan shader on a supersampled surface, including both turnarounds. */
public final class TargetScanGpuTest {
    private static final int W=96,H=192;
    private static int compile(int type,String source) {
        int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);
        if(glGetShaderi(s,GL_COMPILE_STATUS)==0)throw new AssertionError(glGetShaderInfoLog(s));return s;
    }
    private static int texture(int w,int h,float[] data) {
        int t=glGenTextures();glBindTexture(GL_TEXTURE_2D,t);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA32F,w,h,0,GL_RGBA,GL_FLOAT,data);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);return t;
    }
    private static float[] draw(int program,double angle) {
        glUniform4f(glGetUniformLocation(program,"Scan"),(float)(.5-.5*Math.cos(angle)),(float)Math.sin(angle),.3f,1);
        glClear(GL_COLOR_BUFFER_BIT);glDrawArrays(GL_TRIANGLE_STRIP,0,4);
        float[] rgba=new float[W*H*4];glReadPixels(0,0,W,H,GL_RGBA,GL_FLOAT,rgba);
        if(glGetError()!=GL_NO_ERROR)throw new AssertionError("GL error");
        for(float value:rgba)if(!Float.isFinite(value))throw new AssertionError("Invalid pixel");return rgba;
    }
    private static double energy(float[] pixels) {
        double sum=0;for(int i=3;i<pixels.length;i+=4)sum+=pixels[i];return sum;
    }
    private static double maximumDelta(float[] a,float[] b) {
        double max=0;for(int i=0;i<a.length;i++)max=Math.max(max,Math.abs(a[i]-b[i]));return max;
    }
    public static void main(String[] args)throws Exception {
        if(!GLFW.glfwInit())throw new AssertionError("GLFW");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);
        long window=GLFW.glfwCreateWindow(W,H,"Scan continuity and coverage",0,0);if(window==0)throw new AssertionError("Context");
        try {
            GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();glBindVertexArray(glGenVertexArrays());glViewport(0,0,W,H);
            Path shaders=Path.of("src/main/resources/assets/bkclient/shaders/core/target_scan");
            int v=compile(GL_VERTEX_SHADER,Files.readString(shaders.resolve("scan.vsh"))),f=compile(GL_FRAGMENT_SHADER,Files.readString(shaders.resolve("scan.fsh")));
            int p=glCreateProgram();glAttachShader(p,v);glAttachShader(p,f);glBindAttribLocation(p,0,"Position");glBindAttribLocation(p,1,"UV0");glLinkProgram(p);
            if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new AssertionError(glGetProgramInfoLog(p));glUseProgram(p);
            glBindBuffer(GL_ARRAY_BUFFER,glGenBuffers());glBufferData(GL_ARRAY_BUFFER,new float[]{-1,-1,0,0,0, 1,-1,0,1,0, -1,1,0,0,1, 1,1,0,1,1},GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,false,20,0L);glEnableVertexAttribArray(1);glVertexAttribPointer(1,2,GL_FLOAT,false,20,12L);
            int output=texture(W,H,null);glBindFramebuffer(GL_FRAMEBUFFER,glGenFramebuffers());glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,output,0);
            if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("FBO");
            // A diagonal silhouette tests partial pixel coverage, in addition to a tall front face.
            float[] mask=new float[W*H*16],depth=new float[mask.length];
            for(int y=0;y<H*2;y++)for(int x=0;x<W*2;x++) {
                int i=(y*W*2+x)*4;boolean covered=x>30+y/12&&x<W*2-20;
                mask[i]=.5f;mask[i+1]=.5f;mask[i+2]=1;mask[i+3]=covered?1:0;
                depth[i]=covered?.5f:1;depth[i+3]=1;
            }
            glActiveTexture(GL_TEXTURE0);texture(W*2,H*2,mask);glUniform1i(glGetUniformLocation(p,"Sampler0"),0);
            glActiveTexture(GL_TEXTURE1);texture(W*2,H*2,depth);glUniform1i(glGetUniformLocation(p,"Sampler1"),1);
            glUniformMatrix4fv(glGetUniformLocation(p,"InverseViewProjection"),false,new Matrix4f().get(new float[16]));
            glUniform4f(glGetUniformLocation(p,"OriginHeight"),0,-1,0,2);glUniform3f(glGetUniformLocation(p,"Tint"),.6f,.25f,1);
            glUniform1f(glGetUniformLocation(p,"Opacity"),1);glUniform2f(glGetUniformLocation(p,"PixelSize"),1f/W,1f/H);
            double midEnergy=energy(draw(p,Math.PI*.5));
            for(double turn:new double[]{0,Math.PI,Math.PI*2}) {
                var left=draw(p,turn-.001);var center=draw(p,turn);var right=draw(p,turn+.001);
                if(maximumDelta(left,right)>.01)throw new AssertionError("Abrupt trail reversal");
                if(energy(center)<midEnergy*.2)throw new AssertionError("Scan disappears at the end of the body");
            }
            int partial=0;
            var frame=draw(p,Math.PI*.5);
            for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
                int hits=0;for(int j=0;j<2;j++)for(int i=0;i<2;i++)if(mask[((y*2+j)*W*2+x*2+i)*4+3]>0)hits++;
                float alpha=frame[(y*W+x)*4+3];
                if(hits==0&&alpha!=0)throw new AssertionError("Scan leaks outside target");
                if(hits>0&&hits<4&&alpha>0){partial++;if(alpha>.95f*hits/4+.0001)throw new AssertionError("Incorrect edge coverage");}
            }
            if(partial<3)throw new AssertionError("No antialiased edge samples");
            System.out.println("PASS: continuous upper/lower reversals, visible endpoints, subpixel silhouette coverage, no target leakage, clean GL; "+glGetString(GL_RENDERER));
        } finally {GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
}
