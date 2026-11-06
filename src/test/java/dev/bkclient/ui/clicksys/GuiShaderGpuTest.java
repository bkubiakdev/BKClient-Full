package dev.bkclient.ui.clicksys;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL33.*;
import java.nio.file.*;
import java.util.*;
/** Real driver compile/link/render test in an invisible window; not an in-game parity test. */
public final class GuiShaderGpuTest {
 static final Path ROOT=Path.of("src/main/resources/assets/bkclicksys/shaders/core");
 static int shader(int type,String filename)throws Exception {
  int shader=glCreateShader(type);glShaderSource(shader,Files.readString(ROOT.resolve(filename)));glCompileShader(shader);
  if(glGetShaderi(shader,GL_COMPILE_STATUS)==0)throw new AssertionError(filename+": "+glGetShaderInfoLog(shader));return shader;
 }
 static int program()throws Exception {
  int v=shader(GL_VERTEX_SHADER,"liquidglass_rect.vsh"),f=shader(GL_FRAGMENT_SHADER,"liquidglass_rect.fsh"),p=glCreateProgram();
  glAttachShader(p,v);glAttachShader(p,f);glBindAttribLocation(p,0,"Position");glBindAttribLocation(p,1,"UV0");glLinkProgram(p);
  if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new AssertionError(glGetProgramInfoLog(p));glDeleteShader(v);glDeleteShader(f);return p;
 }
 static void uniform(int p,String n,float...v){int l=glGetUniformLocation(p,n);switch(v.length){case 1->glUniform1f(l,v[0]);case 2->glUniform2f(l,v[0],v[1]);case 3->glUniform3f(l,v[0],v[1],v[2]);case 4->glUniform4f(l,v[0],v[1],v[2],v[3]);}}
 static void defaults(int p,int mask){
  glUseProgram(p);float[] identity={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
  glUniformMatrix4fv(glGetUniformLocation(p,"ModelViewMat"),false,identity);glUniformMatrix4fv(glGetUniformLocation(p,"ProjMat"),false,identity);
  glUniform1i(glGetUniformLocation(p,"Sampler0"),0);glUniform1i(glGetUniformLocation(p,"BoxCount"),0);
  uniform(p,"Resolution",256,192);uniform(p,"Size",256,192);uniform(p,"QuadSize",256,192);uniform(p,"ContentOffset",0,0);
  uniform(p,"Radius",18,18,18,18);uniform(p,"FillColor",.04F,.04F,.04F,1);uniform(p,"BorderColor",.2F,.2F,.2F,1);uniform(p,"BorderWidth",1);
  uniform(p,"GlassTint",.2F,.4F,1,1);uniform(p,"GlassTintStrength",.02F);uniform(p,"GlassRefraction",1);uniform(p,"GlassChromatic",1);uniform(p,"GlassBlur",.4F);uniform(p,"GlassIntensity",1);
  String[] first={"GlassFrosted","GlassPosterize","GlassPixelate","GlassWave","GlassInnerGlow"};
  for(int i=0;i<5;i++)uniform(p,first[i],(mask&(1<<i))==0?0:.5F);
  uniform(p,"GlassWaveSpeed",1);uniform(p,"GlassWaveFreq",.5F);uniform(p,"GlassInnerGlowSize",.5F);uniform(p,"GlassInnerGlowColor",.2F,.4F,1,1);
  uniform(p,"Time",1.37F);uniform(p,"GlobalAlpha",1);uniform(p,"GuiEffects",mask);uniform(p,"GlassEnabled",1);uniform(p,"PanelOpacity",.62F);
  uniform(p,"GuiCursor",128,96);uniform(p,"CursorSize",1);uniform(p,"CursorStrength",1);uniform(p,"RevealRadius",1);uniform(p,"RevealStrength",1);
  uniform(p,"AccentFrom",.6F,.1F,.8F);uniform(p,"AccentTo",.1F,.8F,.7F);uniform(p,"AccentTargets",0,0,0);uniform(p,"PaletteBackground",.04F,.04F,.04F);uniform(p,"PaletteSurface",.07F,.07F,.07F);uniform(p,"PaletteElevated",.1F,.1F,.1F);uniform(p,"GradientAngle",45);uniform(p,"GradientEnabled",0);
 }
 static int[] render(){glClear(GL_COLOR_BUFFER_BIT);glDrawArrays(GL_TRIANGLES,0,6);int[] pixels=new int[256*192];glReadPixels(0,0,256,192,GL_RGBA,GL_UNSIGNED_BYTE,pixels);if(glGetError()!=GL_NO_ERROR)throw new AssertionError("OpenGL error");return pixels;}
 public static void main(String[]args)throws Exception {
  if(!GLFW.glfwInit())throw new AssertionError("No GLFW context");
  GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);
  long window=GLFW.glfwCreateWindow(256,192,"BK GUI shader test",0,0);if(window==0)throw new AssertionError("No OpenGL window");
  try {
   GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();glViewport(0,0,256,192);
   int vao=glGenVertexArrays();glBindVertexArray(vao);int buffer=glGenBuffers();glBindBuffer(GL_ARRAY_BUFFER,buffer);
   glBufferData(GL_ARRAY_BUFFER,new float[]{-1,-1,0,0,0, 1,-1,0,1,0, 1,1,0,1,1, -1,-1,0,0,0, 1,1,0,1,1, -1,1,0,0,1},GL_STATIC_DRAW);
   glVertexAttribPointer(0,3,GL_FLOAT,false,20,0);glEnableVertexAttribArray(0);glVertexAttribPointer(1,2,GL_FLOAT,false,20,12);glEnableVertexAttribArray(1);
   int texture=glGenTextures();glBindTexture(GL_TEXTURE_2D,texture);int[] pattern=new int[256*192];for(int y=0;y<192;y++)for(int x=0;x<256;x++)pattern[y*256+x]=0xff000000|((x/16+y/16)%2==0?0x2080e0:0xe03050)|(x&15);
   glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,256,192,0,GL_RGBA,GL_UNSIGNED_BYTE,pattern);glGenerateMipmap(GL_TEXTURE_2D);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
   glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
   for(int reload=0;reload<2;reload++) {
    int p=program();defaults(p,0);int[] baseline=render();
    for(int effect=0;effect<16;effect++){defaults(p,1<<effect);int[] output=render();if(Arrays.equals(baseline,output))throw new AssertionError("Effect "+effect+" did not change pixels");}
    defaults(p,65535);render();
    for(float size:new float[]{24,128,1024}){uniform(p,"Size",size,size);uniform(p,"CursorSize",.3F);uniform(p,"RevealRadius",3);uniform(p,"GlassIntensity",0);render();uniform(p,"GlassIntensity",1);uniform(p,"GlassRefraction",3);uniform(p,"GlassChromatic",3);uniform(p,"GlassBlur",.8F);render();}
    defaults(p,0);uniform(p,"AccentTargets",1,1,1);uniform(p,"GradientEnabled",1);int[] gradient=render();if(Arrays.equals(baseline,gradient))throw new AssertionError("Gradient did not change pixels");
    glDeleteProgram(p);
   }
   glDeleteTextures(texture);glDeleteBuffers(buffer);glDeleteVertexArrays(vao);
   System.out.println("GPU: "+glGetString(GL_RENDERER)+"; compile/link, all 16 individual effects, combined effects, extremes, geometry resizing, gradient and program reload passed.");
  }finally{GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
 }
}
