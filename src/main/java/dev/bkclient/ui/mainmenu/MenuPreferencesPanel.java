package dev.bkclient.ui.mainmenu;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.animation.base.*;
import dev.bkclient.BKClientInit;

final class MenuPreferencesPanel {
    private boolean open;
    private float right,panelX,panelY;
    private final Animation fade=new Animation(220L,0,Easing.FIGMA_EASE_IN_OUT);
    void close(){open=false;}
    boolean isOpen(){return open;}
    private long lastSample;
    private boolean darkInk;
    private void sampleBackground(UIContext ctx,int width,int height){
        long now=System.nanoTime();if(now-lastSample<250_000_000L)return;lastSample=now;
        ctx.draw();
        var fb=net.minecraft.client.MinecraftClient.getInstance().getFramebuffer();
        float sx=fb.textureWidth/(float)Math.max(1,width),sy=fb.textureHeight/(float)Math.max(1,height);
        float brightness=0;
        for(int dx:new int[]{-5,0,5}){
            var color=ColorRGBA.fromPixel(Math.clamp((right-12+dx)*sx,0,fb.textureWidth-1),
                Math.clamp(fb.textureHeight-18*sy-1,0,fb.textureHeight-1));
            brightness+=color.getRed()*.2126f+color.getGreen()*.7152f+color.getBlue()*.0722f;
        }
        brightness/=3;
        if(brightness>165)darkInk=true;else if(brightness<140)darkInk=false;
    }
    void draw(UIContext ctx,int width,int height){
        right=width-10;panelX=Math.max(8,right-152);panelY=32;
        sampleBackground(ctx,width,height);
        boolean pl=dev.bkclient.systems.localization.Localizator.getCurrentLanguage()==dev.bkclient.systems.localization.Language.PL_PL;
        var font=Fonts.REGULAR.getFont(8);
        ColorRGBA ink=darkInk?ColorRGBA.BLACK:ColorRGBA.WHITE;
        boolean hovered=ctx.getMouseX()>=right-24&&ctx.getMouseX()<=right&&ctx.getMouseY()>=6&&ctx.getMouseY()<=30;
        if(hovered)dev.bkclient.utility.game.cursor.CursorUtility.set(dev.bkclient.utility.game.cursor.CursorType.HAND);
        ctx.draw();LucideIcons.MORE.draw(ctx,right-18,12,12,ink.withAlpha(hovered||open?255:205));
        fade.update(open);float a=fade.getValue();if(a<.001f)return;
        ColorRGBA paper=darkInk?new ColorRGBA(247,247,247):new ColorRGBA(19,19,19);
        ctx.drawRoundedRect(panelX,panelY,152,12+MenuPreferences.Option.values().length*20,BorderRadius.all(6),paper.withAlpha(245*a));
        int i=0;
        for(var option:MenuPreferences.Option.values()){
            float y=panelY+6+i++*20;
            ctx.drawText(font,pl?option.pl:option.en,panelX+10,y+6,ink.mulAlpha(a));
            ctx.drawRoundedRect(panelX+132,y+5,10,10,BorderRadius.all(2),ink.withAlpha((option.enabled?100:45)*a));
            ctx.drawRoundedRect(panelX+133,y+6,8,8,BorderRadius.all(1),paper.mulAlpha(a));
            if(option.enabled){ctx.draw();LucideIcons.CHECK.draw(ctx,panelX+132,y+5,10,ink.mulAlpha(a));}
        }
    }
    boolean click(double x,double y,int button){
        if(x>=right-24&&x<=right&&y>=6&&y<=30){if(button==0)open=!open;return true;}
        if(!open)return false;
        if(button==0 && x>=panelX&&x<=panelX+152&&y>=panelY+6&&y<panelY+6+MenuPreferences.Option.values().length*20){
            int row=(int)((y-panelY-6)/20);
            var option=MenuPreferences.Option.values()[row];option.enabled=!option.enabled;
            BKClientInit.getInstance().getFileManager().writeFile("client");
        }else close();
        return true;
    }
}
