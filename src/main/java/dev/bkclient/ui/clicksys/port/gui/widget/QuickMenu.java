package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.ColorSupplier;
import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.data.ThemeConfig;
import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.ui.clicksys.port.enums.ThemePalette;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.clicksys.port.render.SvgShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import org.joml.Matrix4f;

/** SettingsMenu dimensions and spacing from SystemSRC; BK actions and persistence. */
public class QuickMenu extends ButtonWidget {
   private Runnable runnable;
   private boolean listening, panicPage;
   private final Tween theme=EasingPresets.getTweenByFloatFloat(1,.22F);
   private final Tween[] hover={EasingPresets.getTween(),EasingPresets.getTween(),EasingPresets.getTween()};
   public QuickMenu(){value237=220;value238=250;}
   private MenuModule menu(){return dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(MenuModule.class);}
   private void box(Matrix4f m,float x,float y,float w,float h,float radius,int color,float alpha){
      ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(radius,x,color,m,h,w,alpha,y);
   }
   private void icon(Matrix4f m,CategoryType icon,float x,float y,float alpha){
      SvgShader.onFloatIntMatrix4fFloatCategoryTypeFloatFloatFloat(alpha,Theme.foreground(),m,y,icon,14,x,14);
   }
   private void label(Matrix4f m,String text,float x,float y,float size,float alpha){
      TextShader.onMatrix4fStringFloatFloatFloatIntFloat(m,dev.bkclient.systems.localization.Localizator.translate(text),x,y,size,Theme.foreground(),alpha);
   }
   @Override protected void onFloatFloatFloatMatrix4f3(float my,float alpha,float mx,Matrix4f matrix){
      // Animate opacity and the selection pill; hitboxes share the exact drawn coordinates.
      float x=value235,y=value236;
      dev.bkclient.ui.clicksys.ClickSysMaterial.panel(matrix,x,y,value237,value238,20,Theme.background(),Theme.border(),alpha);
      if(panicPage){
         label(matrix,"Panic & Clean",x+16,y+18,14,alpha);
         label(matrix,"Disable BK for this session",x+16,y+48,12,alpha);
         label(matrix,"Keep configs and files",x+16,y+68,12,alpha);
         row(matrix,0,"Disable client",x+6,y+104,mx,my,alpha);
         row(matrix,1,"Back",x+6,y+152,mx,my,alpha);return;
      }
      theme.setFloat2(ThemeConfig.getThemePalette()==ThemePalette.INSTANCE2?1:0);
      box(matrix,x+6,y+8,162,42,14,Theme.surface(),alpha);
      box(matrix,x+9+theme.getFloat()*81,y+11,75,36,12,Theme.elevated(),alpha);
      icon(matrix,CategoryType.SUN,x+39,y+22,alpha);
      icon(matrix,CategoryType.MOON,x+120,y+22,alpha);
      box(matrix,x+174,y+8,40,42,14,Theme.surface(),alpha);
      icon(matrix,CategoryType.LANGUAGE,x+187,y+22,alpha);
      box(matrix,x+6,y+56,208,42,14,Theme.surface(),alpha);
      box(matrix,x+9,y+59,94,36,12,Theme.elevated(),alpha);
      label(matrix,menu().modifierSymbols.isEnabled()?"^  /  +":"Ctrl Alt Shift",x+17,y+72,11,alpha);
      label(matrix,"SHIFT ALT CTRL",x+110,y+73,10,alpha*.65F);
      row(matrix,0,"Panic & Clean",x+6,y+104,mx,my,alpha);
      row(matrix,1,"ClickGui",x+6,y+152,mx,my,alpha);
      String key=listening?"...":menu().bindLabel();
      key=dev.bkclient.ui.clicksys.port.data.TextTrimmer.getStringByFloatStringFloat2(108,key,11);
      float width=TextShader.getFloatByStringFloat(key,11);
      label(matrix,key,x+202-width,y+168,11,alpha*.7F);
      row(matrix,2,"AutoMarket",x+6,y+200,mx,my,alpha);
   }
   private void row(Matrix4f m,int i,String text,float x,float y,float mx,float my,float alpha){
      hover[i].setFloat2(mx>=x&&mx<x+208&&my>=y&&my<y+42?1:0);
      int color=dev.bkclient.ui.clicksys.port.data.AnimatedInt.getIntByIntFloatInt(Theme.elevated(),hover[i].getFloat(),Theme.surface());
      box(m,x,y,208,42,14,color,alpha);label(m,text,x+10,y+14,14,alpha);
   }
   @Override protected boolean isIntDoubleDouble2(int button,double mx,double my){
      if(button!=0)return true;
      float x=(float)mx-value235,y=(float)my-value236;
      if(x<6||x>214)return true;
      if(panicPage){
         if(y>=104&&y<146){update4();dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.other.Panic.class).enable();}
         else if(y>=152&&y<194)panicPage=false;
         return true;
      }
      if(y>=8&&y<50){
         if(x>=174){
            dev.bkclient.BKClientInit.getInstance().getModuleManager()
               .getModule(dev.bkclient.systems.modules.modules.visuals.Interface.class).toggleLanguage();
         }else{ThemeConfig.setThemePalette(x<87?ThemePalette.INSTANCE:ThemePalette.INSTANCE2);ThemeConfig.syncInterfaceSelection();}
         AbstractSetting.saveConfig();return true;
      }
      if(y>=56&&y<98){
         if(x<=103){menu().modifierSymbols.toggle();AbstractSetting.saveConfig();}
         else listening=true;
         return true;
      }
      if(y>=104&&y<146){listening=false;panicPage=true;return true;}
      if(y>=152&&y<194){listening=true;return true;}
      if(y>=200&&y<242){update4();if(runnable!=null)runnable.run();}
      return true;
   }
   @Override public boolean isIntIntInt2(int a,int modifiers,int key){
      if(!listening)return super.isIntIntInt2(a,modifiers,key);
      if(key!=256){menu().setMenuBind(key==261?-1:key,modifiers);AbstractSetting.saveConfig();}
      listening=false;return true;
   }
   @Override protected void update7(){listening=false;panicPage=false;}
   @Override public void update2(){listening=false;panicPage=false;super.update2();}
   public void setRunnable(Runnable action){runnable=action;}
   @Override public void setColorSupplier2(ColorSupplier anchor){
      super.setColorSupplier2(()->{
         float[] p=anchor.get();var window=net.minecraft.client.MinecraftClient.getInstance().getWindow();
         float scale=Math.max(.01F,dev.bkclient.ui.clicksys.port.gui.screen.ClickGuiScreen.getValue235());
         float maxX=Math.max(8,window.getScaledWidth()/scale-value237-8),maxY=Math.max(8,window.getScaledHeight()/scale-value238-8);
         return new float[]{Math.clamp(p[0],8,maxX),Math.clamp(p[1]-value238/2,8,maxY)};
      });
   }
}
