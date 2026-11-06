package dev.bkclient.combatengine.bkengine.utils.math;
import dev.bkclient.combatengine.bkengine.utils.annotation.IMinecraft;
import lombok.experimental.UtilityClass;
@UtilityClass
public class ServerUtil implements IMinecraft {
    public String server = "Local   ";
    public void tick() { server = getServer(); }
    public boolean nullCheck() { return mc.player == null || mc.world == null; }
    private String getServer() {
        if (nullCheck() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null || mc.getNetworkHandler().getBrand() == null) return "Vanilla";
        String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        String brand = mc.getNetworkHandler().getBrand().toLowerCase();

        if (brand.contains("botfilter")) return "FunTime";
        else if (brand.contains("§6spooky§ccore")) return "SpookyTime";
        else if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) return "HolyWorld";
        else if (serverIp.contains("reallyworld")) return "ReallyWorld";
        return serverIp;
    }


    public boolean isCopyTime() { return server.equals("SpookyTime"); }
    public boolean isFunTime() { return server.equals("FunTime"); }
    public boolean isHolyWorld() { return server.equals("HolyWorld"); }
}
