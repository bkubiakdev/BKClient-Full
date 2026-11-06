package dev.bkclient.ui.menu.esp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/** F8 in the ESP editor records a bounded local GL trace; no account or world data. */
final class EspPreviewDiagnostics {
    private final ArrayList<String> frames=new ArrayList<>();
    private Path directory;
    private int frame;
    private int burstFrames;
    private long deadline,nextTrace,nextCapture;
    private JsonObject current;
    boolean active(){return directory!=null;}
    void modelChanged(){if(active())burstFrames=120;}
    void start(){
        if(active())return;
        try {
            directory=MinecraftClient.getInstance().runDirectory.toPath().resolve("BK-Client/debug/esp-"+System.currentTimeMillis());
            Files.createDirectories(directory);
            var metadata=new JsonObject();
            metadata.addProperty("vendor",GL11.glGetString(GL11.GL_VENDOR));
            metadata.addProperty("renderer",GL11.glGetString(GL11.GL_RENDERER));
            var mods=new JsonObject();
            net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().forEach(m->mods.addProperty(m.getMetadata().getId(),m.getMetadata().getVersion().getFriendlyString()));
            metadata.add("mods",mods);
            Files.writeString(directory.resolve("environment.json"),metadata.toString());
            frame=0;frames.clear();
            burstFrames=120;
            deadline=System.nanoTime()+8_000_000_000L;nextTrace=nextCapture=0;
            dev.bkclient.BKClientInit.LOGGER.info("ESP debug started: {}",directory);
        }catch(Exception e){directory=null;dev.bkclient.BKClientInit.LOGGER.warn("ESP debug could not start",e);}
    }
    void before(){if(active()&&System.nanoTime()>=nextTrace){nextTrace=System.nanoTime()+16_000_000L;current=new JsonObject();current.addProperty("frame",frame);current.add("before",state());}}
    void after(){if(active()&&current!=null){current.add("after",state());frames.add(current.toString());current=null;}}
    void finishFrame(){
        if(!active())return;
        var mc=MinecraftClient.getInstance();
        long now=System.nanoTime();
        // Consecutive frames catch alternate-frame corruption that tick-based screenshots miss.
        if(burstFrames>0||now>=nextCapture){burstFrames--;nextCapture=now+500_000_000L;ScreenshotRecorder.saveScreenshot(directory.toFile(),"frame-"+frame+".png",mc.getFramebuffer(),t->{});}
        frame++;
        if(now>=deadline)close();
    }
    void close(){
        if(!active())return;
        try {Files.write(directory.resolve("frames.jsonl"),frames);dev.bkclient.BKClientInit.LOGGER.info("ESP debug saved: {} ({} frames)",directory,frames.size());}
        catch(Exception e){dev.bkclient.BKClientInit.LOGGER.warn("ESP debug save failed",e);}
        finally {directory=null;frames.clear();current=null;}
    }
    private static JsonObject state(){
        var j=new JsonObject();
        j.addProperty("framebuffer",GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
        j.addProperty("depthTest",GL11.glIsEnabled(GL11.GL_DEPTH_TEST));
        j.addProperty("depthWrite",GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
        j.addProperty("depthFunc",GL11.glGetInteger(GL11.GL_DEPTH_FUNC));
        j.addProperty("blend",GL11.glIsEnabled(GL11.GL_BLEND));
        j.addProperty("cull",GL11.glIsEnabled(GL11.GL_CULL_FACE));
        j.addProperty("scissor",GL11.glIsEnabled(GL11.GL_SCISSOR_TEST));
        j.addProperty("polygonOffset",GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL));
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        var vp=new JsonArray();for(int v:viewport)vp.add(v);j.add("viewport",vp);
        double[] range=new double[2];GL11.glGetDoublev(GL11.GL_DEPTH_RANGE,range);
        var dr=new JsonArray();for(double v:range)dr.add(v);j.add("depthRange",dr);
        var color=new JsonArray();for(float v:RenderSystem.getShaderColor())color.add(v);j.add("shaderColor",color);
        var lights=new JsonArray();for(var v:dev.bkclient.mixin.accessors.RenderSystemLightsAccessor.bkclient$getShaderLights()){
            var light=new JsonArray();light.add(v.x);light.add(v.y);light.add(v.z);lights.add(light);
        }j.add("lights",lights);
        var matrix=new JsonArray();for(float v:RenderSystem.getModelViewMatrix().get(new float[16]))matrix.add(v);j.add("modelView",matrix);
        return j;
    }
}
