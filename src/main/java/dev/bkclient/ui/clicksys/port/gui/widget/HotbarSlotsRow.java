package dev.bkclient.ui.clicksys.port.gui.widget;
import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.ListSetting;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.utility.colors.ColorRGBA;
import org.joml.Matrix4f;
public final class HotbarSlotsRow extends PanelWidget {
    private final ListSetting slots;
    public HotbarSlotsRow(ListSetting slots) { super(slots); this.slots=slots; }
    @Override public float getFloat5() { return 20; }
    @Override public void onFloatFloatFloatMatrix4f(float alpha,float mouseX,float mouseY,Matrix4f matrix) {
        getFloatByFloatMatrix4fFloat(value237-60,matrix,alpha);
        TextShader.onFloatFloatIntFloatFloatStringMatrix4f(value236,value235+value237-54,
            Theme.mutedFg(),12,alpha,slots.getInt()+"/9",matrix);
        TextShader.update2();
        LucideIcons.EXPAND.draw(matrix,value235+value237-16,value236,14,ColorRGBA.fromInt(Theme.foreground()).withAlpha(255*alpha));
        value238=getFloat5();
    }
    @Override public boolean isIntDoubleDouble(int button,double x,double y) {
        if(button!=0 || x<value235 || x>value235+value237 || y<value236 || y>value236+20)return false;
        if(net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof dev.bkclient.ui.clicksys.ClickSysScreen host)host.openSlots(slots);
        return true;
    }
}
