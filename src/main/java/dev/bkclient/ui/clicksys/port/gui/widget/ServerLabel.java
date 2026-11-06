package dev.bkclient.ui.clicksys.port.gui.widget;
import org.joml.Matrix4f;
import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.render.TextShader;
/** Player head in compact mode; privacy-aware nickname in expanded mode. */
public class ServerLabel extends SliderWidget {
    public ServerLabel() { super("", 208.0F, 32.0F); }
    @Override public void onFloatFloatFloatMatrix4f(float alpha, float mouseY, float mouseX, Matrix4f matrix) {
        float expanded=Math.clamp(1-value239,0,1);
        dev.bkclient.ui.clicksys.ClickSysMaterial.avatar(matrix, value235 + 8 * expanded, value236, alpha);
        if(expanded<=.01F)return;
        var mc=net.minecraft.client.MinecraftClient.getInstance();
        String name=mc.player!=null ? mc.player.getGameProfile().getName() : mc.getSession().getUsername();
        var streamer=dev.bkclient.systems.modules.modules.other.StreamerMode.getInstance();
        if(streamer!=null && streamer.isEnabled() && streamer.hideSelf.isEnabled()) {
            name=streamer.fakeName.getText();
            if(name==null || name.isBlank())name="Protected";
        } else name=dev.bkclient.systems.modules.modules.other.NameProtect.protectDisplayText(name);
        ScissorStack.onFloatFloatFloatFloat(Math.max(0,value237-48),32,value236,value235+48);
        try {TextShader.onFloatStringFloatFloatIntFloatMatrix4f(alpha*expanded,name,14,value235+48,Theme.foreground(),value236+10,matrix);}
        finally {ScissorStack.update();}
    }
}
