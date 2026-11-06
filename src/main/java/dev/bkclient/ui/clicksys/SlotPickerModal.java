package dev.bkclient.ui.clicksys;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.ListSetting;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.utility.colors.ColorRGBA;
import org.joml.Matrix4f;

/** Small modal; events are captured by ClickSysScreen while open. */
final class SlotPickerModal {
    private final ListSetting slots;
    private final Runnable close;
    private float x, y, scale = 1;
    private static final float W = 302, H = 102;
    SlotPickerModal(ListSetting slots, Runnable close) { this.slots = slots; this.close = close; }
    void render(UIContext context, int width, int height) {
        scale = Math.max(.1F, Math.min(dev.bkclient.ui.clicksys.port.gui.screen.ClickGuiScreen.getValue235(),
            Math.min((width-16F)/W,(height-16F)/H)));
        x = (width/scale-W)/2; y = (height/scale-H)/2;
        context.draw(); context.fill(0,0,width,height,0x55000000); context.draw();
        Matrix4f matrix = new Matrix4f().scale(scale,scale,1);
        ClickSysMaterial.begin(context);
        try {
            ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(10,x,Theme.surface(),matrix,H,W,1,y);
            text(matrix,"Slot Picker - " + slots.getInt() + "/9",x+14,y+13,14,Theme.foreground());
            for(int i=0;i<9;i++) {
                float cx=x+14+i*31, cy=y+43;
                ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(4,cx,slots.isInt(i)?Theme.primary():Theme.elevated(),matrix,22,22,1,cy);
                text(matrix,Integer.toString(i+1),cx+7,y+75,12,Theme.mutedFg());
            }
            TextShader.update2(); context.draw();
            LucideIcons.CROSS.draw(matrix,x+W-28,y+12,14,ColorRGBA.fromInt(Theme.foreground()));
            for(int i=0;i<9;i++) if(slots.isInt(i)) LucideIcons.CHECK.draw(matrix,x+18+i*31,y+47,14,ColorRGBA.WHITE);
        } finally { ClickSysMaterial.end(); }
    }
    private static void text(Matrix4f matrix,String text,float x,float y,float size,int color) {
        TextShader.onFloatStringFloatFloatIntFloatMatrix4f(1,text,size,x,color,y,matrix);
    }
    void click(double mouseX,double mouseY,int button) {
        double mx=mouseX/scale,my=mouseY/scale;
        if(mx<x || mx>x+W || my<y || my>y+H) { close.run(); return; }
        if(button!=0)return;
        if(mx>=x+W-34 && my<y+34) { close.run(); return; }
        for(int i=0;i<9;i++) if(mx>=x+14+i*31 && mx<=x+36+i*31 && my>=y+40 && my<=y+92) {slots.onInt(i);return;}
    }
}
