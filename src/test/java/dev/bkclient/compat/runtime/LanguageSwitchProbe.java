package dev.bkclient.compat.runtime;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.localization.Language;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.ui.clicksys.ClickSysScreen;
import dev.bkclient.ui.clicksys.port.gui.screen.ClickGuiScreen;
import dev.bkclient.ui.clicksys.port.gui.widget.IconButton;
import dev.bkclient.ui.clicksys.port.gui.widget.QuickMenu;
import dev.bkclient.ui.clicksys.port.gui.widget.TopBar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.lwjgl.opengl.GL33C.*;

/** Separate test mod. Exercises the actual screen click and persistence in an isolated game profile. */
public final class LanguageSwitchProbe implements ClientModInitializer {
    private static int tick, checks;
    private static boolean done;
    private static Interface settings;
    private static ClickSysScreen screen;
    private static TopBar top;
    private static IconButton button;
    private static final boolean RESUME = Boolean.getBoolean("bk.language.resume");

    @Override public void onInitializeClient() { ClientTickEvents.END_CLIENT_TICK.register(LanguageSwitchProbe::tick); }
    private static Object field(Object o, String name) throws Exception {
        var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);
    }
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        checks++;System.out.println("LANGUAGE_CHECK " + name);
    }
    private static Path dir(MinecraftClient mc) { return mc.runDirectory.toPath().resolve("language-proof"); }
    private static void open(MinecraftClient mc) throws Exception {
        screen=new ClickSysScreen();mc.setScreen(screen);
        top=(TopBar)field(field(field(screen,"delegate"),"navBar"),"topBar");
        button=(IconButton)field(top,"iconButton4");
    }
    private static void click() {
        float scale=ClickGuiScreen.getValue235();
        screen.onMouseClicked((button.getValue235()+7)*scale,(button.getValue236()+7)*scale,MouseButton.LEFT);
        screen.onMouseReleased((button.getValue235()+7)*scale,(button.getValue236()+7)*scale,MouseButton.LEFT);
    }
    private static void assertLanguage(boolean polish, String reason) throws Exception {
        check(settings.getLanguage().getValue().getName().equals(polish?"Polish":"English"),reason+" setting");
        check(Localizator.getCurrentLanguage()==(polish?Language.PL_PL:Language.EN_US),reason+" active language");
        check(Localizator.translate("modules.settings.interface.language").equals(polish?"Język":"Language"),reason+" labels");
        check(field(button,"text").equals(polish?"Język: Polski":"Language: English"),reason+" tooltip");
    }
    private static String savedLanguage(MinecraftClient mc) throws Exception {
        var json=JsonParser.parseString(Files.readString(mc.runDirectory.toPath().resolve("BK-Client/configs/autosave.bk"))).getAsJsonObject();
        for(var entry:json.getAsJsonArray("modules")) {
            var module=entry.getAsJsonObject();
            if(module.get("name").getAsString().equals("Interface"))
                return module.getAsJsonObject("settings").get("modules.settings.interface.language").getAsString();
        }
        throw new AssertionError("Interface missing in autosave");
    }
    private static void shot(MinecraftClient mc, String name) throws Exception {
        var f=mc.getFramebuffer();int w=f.textureWidth,h=f.textureHeight,old=glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        var data=org.lwjgl.BufferUtils.createByteBuffer(w*h*4);
        com.mojang.blaze3d.platform.GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER,f.fbo);
        glReadPixels(0,0,w,h,GL_RGBA,GL_UNSIGNED_BYTE,data);
        com.mojang.blaze3d.platform.GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER,old);
        var image=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=((h-y-1)*w+x)*4;image.setRGB(x,y,(data.get(i)&255)<<16|(data.get(i+1)&255)<<8|data.get(i+2)&255);}
        javax.imageio.ImageIO.write(image,"png",dir(mc).resolve(name+".png").toFile());
        check(glGetError()==GL_NO_ERROR,"clean GL "+name);
    }
    private static void finish(MinecraftClient mc, String result) throws Exception {
        done=true;Files.writeString(dir(mc).resolve(RESUME?"resume-result.txt":"result.txt"),result);mc.scheduleStop();
    }
    private static void tick(MinecraftClient mc) {
        if(done)return;
        try {
            GLFW.glfwHideWindow(mc.getWindow().getHandle());
            if(mc.getOverlay()!=null)return;
            tick++;
            if(tick==1) {
                Files.createDirectories(dir(mc));
                settings=BKClientInit.getInstance().getModuleManager().getModule(Interface.class);
                if(!RESUME)check(settings.getLanguage().getValue().getName().equals("English"),"fresh profile starts in English");
                open(mc);
            }
            if(RESUME) {
                if(tick==30){assertLanguage(true,"fresh JVM restores Polish");check(savedLanguage(mc).equals("Polish"),"Polish persisted on disk");shot(mc,"03-after-restart");finish(mc,"PASS "+checks);}
                return;
            }
            if(tick==30){click();assertLanguage(true,"top bar EN to PL immediately");}
            if(tick==45){assertLanguage(true,"Polish remains after ticks");check(savedLanguage(mc).equals("Polish"),"top bar autosaves Polish");shot(mc,"01-polish");}
            if(tick==50){click();assertLanguage(false,"top bar PL to EN immediately");}
            if(tick==55){check(savedLanguage(mc).equals("English"),"top bar autosaves English");
                var quick=new QuickMenu();quick.onFloatFloat2(0,0);
                var method=QuickMenu.class.getDeclaredMethod("isIntDoubleDouble2",int.class,double.class,double.class);method.setAccessible(true);
                method.invoke(quick,0,190d,25d);assertLanguage(true,"quick menu agrees with top bar");
            }
            if(tick==60){
                settings.getLanguage().load(new JsonPrimitive("English"));assertLanguage(false,"profile load updates immediately");
                settings.getLanguage().load(new JsonPrimitive("polski"));assertLanguage(true,"legacy Polish name loads");
                settings.getLanguage().load(new JsonPrimitive("Russian"));assertLanguage(false,"legacy unsupported language falls back to English");
                for(int i=0;i<6;i++){click();assertLanguage(i%2==0,"repeated click "+i);}
                click();assertLanguage(true,"final Polish selection");
                check(Localizator.translate("Target priority.description").equals("Sposób wyboru nowego celu."),"short Aura description translated");
                open(mc);
            }
            if(tick==85){assertLanguage(true,"reopened GUI stays Polish");shot(mc,"02-reopened");check(savedLanguage(mc).equals("Polish"),"final Polish selection autosaved");finish(mc,"PASS "+checks);}
        } catch(Throwable t) {
            t.printStackTrace();try{Files.createDirectories(dir(mc));finish(mc,"FAIL "+t);}catch(Exception ignored){mc.scheduleStop();}
        }
    }
}
