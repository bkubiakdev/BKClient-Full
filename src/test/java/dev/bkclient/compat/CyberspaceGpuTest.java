package dev.bkclient.compat;

import java.nio.file.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL33C.*;

/** Executes the production shader, including its longitude seam and camera poles. */
public final class CyberspaceGpuTest {
    private static final int W=256,H=144;
    private static int program,checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static int compile(int type,String source){int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);check(glGetShaderi(s,GL_COMPILE_STATUS)!=0,glGetShaderInfoLog(s));return s;}
    private static void f(String name,float value){glUniform1f(glGetUniformLocation(program,name),value);}
    private static void v(String name,float x,float y,float z){glUniform3f(glGetUniformLocation(program,name),x,y,z);}
    private static void camera(float yaw,float pitch){
        float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw),cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);
        v("uCamRight",cy,0,-sy);v("uCamUp",sy*sp,cp,cy*sp);v("uCamForward",-sy*cp,sp,-cy*cp);
    }
    private static float[] draw(){glDrawArrays(GL_TRIANGLES,0,6);float[] data=new float[W*H*4];glReadPixels(0,0,W,H,GL_RGBA,GL_FLOAT,data);
        check(glGetError()==0,"clean GL");for(int i=0;i<data.length;i++){check(Float.isFinite(data[i])&&data[i]>=0&&data[i]<=1,"bounded finite output");if(i%4==3)check(Math.abs(data[i]-.8f)<.0001,"opacity preserved");}return data;}
    private static double difference(float[] a,float[] b){double d=0;for(int i=0;i<a.length;i++)if(i%4!=3)d+=Math.abs(a[i]-b[i]);return d/(W*H*3);}
    private static float probe(float x,float y,float z){v("ProbeRay",x,y,z);return draw()[0];}
    private static void worldStreakShader(int unused)throws Exception {
        String base="src/main/resources/assets/bkclient/shaders/core/sky_effects/world_streak";
        int vs=compile(GL_VERTEX_SHADER,Files.readString(Path.of(base+".vsh")));
        int fs=compile(GL_FRAGMENT_SHADER,Files.readString(Path.of(base+".fsh")));
        program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);
        glBindAttribLocation(program,0,"Position");glBindAttribLocation(program,1,"UV0");glBindAttribLocation(program,2,"Color");
        glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);
        float[] identity={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
        glUniformMatrix4fv(glGetUniformLocation(program,"WorldView"),false,identity);
        glUniformMatrix4fv(glGetUniformLocation(program,"WorldProjection"),false,identity);
        int buffer=glGenBuffers();glBindBuffer(GL_ARRAY_BUFFER,buffer);
        glBufferData(GL_ARRAY_BUFFER,new float[]{-1,-1,0,-4,-4, 1,-1,0,4,-4, 1,1,0,4,4, -1,-1,0,-4,-4, 1,1,0,4,4, -1,1,0,-4,4},GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,false,20,0L);
        glEnableVertexAttribArray(1);glVertexAttribPointer(1,2,GL_FLOAT,false,20,12L);glVertexAttrib4f(2,.6f,.4f,1,.8f);
        f("Part",1);f("Brightness",.7f);f("SquareHead",0);
        var round=draw();f("SquareHead",1);var glow=draw();check(glow[(H/2*W+W/2)*4]<round[(H/2*W+W/2)*4],"cube uses separate glow and geometry");f("Part",2);glBufferData(GL_ARRAY_BUFFER,new float[]{-1,-1,0,-1,-1, 1,-1,0,1,-1, 1,1,0,1,1, -1,-1,0,-1,-1, 1,1,0,1,1, -1,1,0,-1,1},GL_STATIC_DRAW);var square=draw();check(difference(round,square)>.001,"round/square world heads differ");
        int corner=((int)(H*.6)*W+(int)(W*.6))*4;
        check(square[(H/2*W+(int)(W*.97))*4]>square[(H/2*W+W/2)*4],"cube has emissive rim");
        check(glow[((H/2)*W+W/2)*4]>glow[((H/2)*W+W/4)*4],"head has soft local halo");
        f("Part",0);var trail=draw();check(difference(square,trail)>.01,"separate tapered trail pass");
        float a=trail[(H/2*W+(int)(W*.55))*4],b=trail[(H/2*W+(int)(W*.6))*4];
        check(b>a,"trail brightens towards head");
        // Real depth buffer blocks fragments behind terrain, with no depth writes from particles.
        int depth=glGenRenderbuffers();glBindRenderbuffer(GL_RENDERBUFFER,depth);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT24,W,H);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depth);
        glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LEQUAL);glDepthMask(true);glClearDepth(.25);glClearColor(0,0,0,0);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glDepthMask(false);
        glDrawArrays(GL_TRIANGLES,0,6);float[] pixel=new float[4];glReadPixels(W/2,H/2,1,1,GL_RGBA,GL_FLOAT,pixel);check(pixel[0]==0&&pixel[3]==0,"occluded streak invisible");
        glDepthMask(true);glClearDepth(1);glClear(GL_DEPTH_BUFFER_BIT);glDepthMask(false);f("Part",2);glDrawArrays(GL_TRIANGLES,0,6);
        glReadPixels(W/2,H/2,1,1,GL_RGBA,GL_FLOAT,pixel);check(pixel[0]>.1,"streak visible in front of depth");
        float[] depthPixel=new float[1];glReadPixels(W/2,H/2,1,1,GL_DEPTH_COMPONENT,GL_FLOAT,depthPixel);check(depthPixel[0]==1,"streak does not write depth");
        check(glGetError()==0,"world shader clean GL");glDisable(GL_DEPTH_TEST);glDeleteRenderbuffers(depth);glDeleteBuffers(buffer);
    }
    public static void main(String[] args)throws Exception{
        check(GLFW.glfwInit(),"GLFW");GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);
        long window=GLFW.glfwCreateWindow(W,H,"Cyberspace shader verification",0,0);check(window!=0,"context");
        try{
            GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();glViewport(0,0,W,H);glBindVertexArray(glGenVertexArrays());
            int vertex=compile(GL_VERTEX_SHADER,"#version 150\nout vec2 vScreen;void main(){vec2 p[6]=vec2[6](vec2(-1,-1),vec2(1,-1),vec2(1,1),vec2(-1,-1),vec2(1,1),vec2(-1,1));vScreen=p[gl_VertexID];gl_Position=vec4(vScreen,1,1);}");
            int fragment=compile(GL_FRAGMENT_SHADER,Files.readString(Path.of("src/main/resources/assets/bkclient/shaders/core/sky_effects/sky_cyberspace.fsh")));
            program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);
            int texture=glGenTextures();glBindTexture(GL_TEXTURE_2D,texture);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA32F,W,H,0,GL_RGBA,GL_FLOAT,(float[])null);
            glBindFramebuffer(GL_FRAMEBUFFER,glGenFramebuffers());glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,texture,0);check(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE,"FBO");
            glUniform2f(glGetUniformLocation(program,"uResolution"),W,H);camera(0,.1f);f("uTime",0);f("uSpeed",1);f("uAlpha",.8f);f("uFov",70);f("uBrightness",1);
            v("uStarColor",.68f,.55f,1);v("uHorizonColor",.74f,.19f,1);f("uAurora",1);f("uStarAmount",1);f("uGlow",.65f);f("uFlow",0);f("uStars",1);
            var original=draw();f("uTime",600);check(difference(original,draw())<1e-7,"zero flow freezes all animation");
            f("uFlow",1);check(difference(original,draw())>.001,"flow animates horizon");f("uFlow",0);
            f("uAurora",0);check(difference(original,draw())>.005,"aurora control");f("uAurora",1);
            f("uGlow",0);check(difference(original,draw())>.005,"glow control");f("uGlow",.65f);
            v("uHorizonColor",.1f,1,.2f);check(difference(original,draw())>.01,"custom horizon color");v("uHorizonColor",.74f,.19f,1);
            f("uStars",0);check(difference(original,draw())>.00001,"stars can be disabled");f("uStars",1);
            camera((float)Math.PI/2-.00001f,.1f);var left=draw();camera((float)Math.PI/2+.00001f,.1f);check(difference(left,draw())<.001,"longitude seam continuous");
            for(float pitch:new float[]{(float)Math.PI/2,-(float)Math.PI/2}){camera(0,pitch);draw();}
            f("uFov",110);f("uStarAmount",2);camera(2,.5f);draw();
            worldStreakShader(vertex);
            System.out.println("CyberspaceGpuTest PASS "+checks+": controls, freeze, seam, poles, FOV and GL; "+glGetString(GL_RENDERER));
        }finally{GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
}
