package dev.bkclient.ui.clicksys;

import dev.bkclient.framework.base.*;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.config.ConfigImport;
import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.gui.widget.ActionRow;
import dev.bkclient.ui.clicksys.port.gui.widget.TextField;
import dev.bkclient.ui.clicksys.port.gui.widget.ChoiceRow;
import dev.bkclient.ui.clicksys.port.setting.ChoiceSetting;
import dev.bkclient.ui.clicksys.port.data.ChoiceOption;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.ActionSetting;
import net.minecraft.client.gui.screen.Screen;
import org.joml.Matrix4f;
import java.util.function.Consumer;

/** Native ClickSys controls, hosted as an input-blocking modal by ClickSysScreen. */
public final class ConfigImportScreen extends CustomScreen {
    private static final float W=340,H=254;
    private final Screen parent;
    private final ConfigImport.Draft draft;
    private final Consumer<String> completed;
    private final TextField name=new TextField(308,32,"Config name...",14,10,0);
    private final ChoiceSetting importMode=new ChoiceSetting("", "", new ChoiceOption("Save only"), new ChoiceOption("Save & load"), false);
    private final ChoiceRow load=new ChoiceRow(importMode,true);
    private final ActionRow cancel,confirm;
    private boolean finished;
    private String error="";
    private float x,y,scale=1;
    public ConfigImportScreen(Screen parent,ConfigImport.Draft draft,Consumer<String> completed){
        this.parent=parent;this.draft=draft;this.completed=completed;name.onString2(draft.name());
        ActionSetting cancelAction=new ActionSetting("Cancel","");cancelAction.setRunnable(this::close);cancel=new ActionRow(cancelAction,false,false);
        ActionSetting confirmAction=new ActionSetting("Import","");confirmAction.setRunnable(this::confirm);confirm=new ActionRow(confirmAction,true);
        load.setValue237(308);cancel.setValue237(148);confirm.setValue237(148);
    }
    public static void open(Screen parent,ConfigImport.Draft draft,Consumer<String> completed){
        var modal=new ConfigImportScreen(parent,draft,completed);
        if(parent instanceof ClickSysScreen host)host.setImportModal(modal);
        else net.minecraft.client.MinecraftClient.getInstance().setScreen(modal);
    }
    @Override public void render(UIContext context){
        scale=Math.max(.01f,Math.min(.65f,Math.min((width-24f)/W,(height-24f)/H)));
        x=(width/scale-W)/2;y=(height/scale-H)/2;
        float previousScale=dev.bkclient.ui.clicksys.port.gui.screen.ClickGuiScreen.getValue235();
        if(parent instanceof ClickSysScreen host){
            var panel=host.getConfigsPage();
            scale=Math.max(.01f,Math.min(previousScale*1.35f,Math.min((panel.getValue237()-32)*previousScale/W,(panel.getValue238()-32)*previousScale/H)));
            x=(panel.getValue235()+panel.getValue237()/2)*previousScale/scale-W/2;
            y=(panel.getValue236()+panel.getValue238()/2)*previousScale/scale-H/2;
        }
        float mx=context.getMouseX()/scale,my=context.getMouseY()/scale;
        context.draw();context.fill(0,0,width,height,0x88000000);context.draw();
        Matrix4f matrix=new Matrix4f().scale(scale,scale,1);
        ClickSysMaterial.begin(context);
        dev.bkclient.ui.clicksys.port.gui.widget.ScissorStack.update2();
        dev.bkclient.ui.clicksys.port.gui.screen.ClickGuiScreen.setValue235(scale);
        try{
            // Deliberately no outer border, in either theme.
            ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(12,x,Theme.surface(),matrix,H,W,1,y);
            text(matrix,dev.bkclient.systems.localization.Localizator.translate("Import configuration"),x+16,y+18,17,Theme.foreground());
            text(matrix,trim(draft.kind().label+" / "+draft.server(),308),x+16,y+46,13,Theme.mutedFg());

            text(matrix,trim(draft.summary(),308),x+16,y+65,13,Theme.mutedFg());
            text(matrix,dev.bkclient.systems.localization.Localizator.translate("Save as"),x+16,y+92,13,Theme.mutedFg());
            name.onFloatFloat2(y+110,x+16);name.onFloatFloatFloatMatrix4f(1,my,mx,matrix);
            load.onFloatFloat2(y+153,x+16);load.onFloatFloatFloatMatrix4f(1,my,mx,matrix);

            if(!error.isEmpty())text(matrix,trim(dev.bkclient.systems.localization.Localizator.translate(error),308),x+16,y+188,11,Theme.danger());
            cancel.onFloatFloat2(y+206,x+16);confirm.onFloatFloat2(y+206,x+176);
            cancel.onFloatFloatFloatMatrix4f(1,my,mx,matrix);confirm.onFloatFloatFloatMatrix4f(1,my,mx,matrix);
            TextShader.update2();context.draw();
        }finally{
            dev.bkclient.ui.clicksys.port.gui.widget.ScissorStack.update2();
            dev.bkclient.ui.clicksys.port.gui.screen.ClickGuiScreen.setValue235(previousScale);
            ClickSysMaterial.end();
        }
    }
    private static String trim(String text,float width){return dev.bkclient.ui.clicksys.port.data.TextTrimmer.getStringByFloatStringFloat2(width,text,13);}
    private static void text(Matrix4f matrix,String value,float x,float y,float size,int color){TextShader.onFloatStringFloatFloatIntFloatMatrix4f(1,value,size,x,color,y,matrix);}
    @Override public void onMouseClicked(double mx,double my,MouseButton button){
        mx/=scale;my/=scale;int index=button.getButtonIndex();name.isIntDoubleDouble(index,mx,my);
        if(cancel.isIntDoubleDouble(index,mx,my)||confirm.isIntDoubleDouble(index,mx,my))return;
        load.isIntDoubleDouble(index,mx,my);
    }
    private void confirm(){
        if(finished)return;
        try{String imported=ConfigImport.confirm(draft,name.getString(),importMode.isFlag3());finished=true;dismiss();completed.accept(imported);}
        catch(Exception e){error=e.getMessage()==null?"Could not import configuration.":e.getMessage();}
    }
    private void dismiss(){if(parent instanceof ClickSysScreen host)host.setImportModal(null);else client.setScreen(parent);}
    public void cancelPending(){if(!finished){finished=true;completed.accept(null);}}
    @Override public void onMouseReleased(double mx,double my,MouseButton button){load.isDoubleDoubleInt(mx/scale,my/scale,button.getButtonIndex());}
    @Override public void onMouseDragged(double mx,double my,MouseButton button,double dx,double dy){load.isDoubleDoubleIntDoubleDouble(my/scale,mx/scale,button.getButtonIndex(),dx/scale,dy/scale);}
    @Override public boolean charTyped(char c,int modifiers){return name.isIntChar(modifiers,c);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==256){close();return true;}return name.isIntIntInt2(scan,modifiers,key);}
    @Override public void close(){if(finished)return;cancelPending();dismiss();}
    @Override public void removed(){cancelPending();super.removed();}
    @Override public boolean shouldPause(){return false;}
}
