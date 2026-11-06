package dev.bkclient.systems.supabase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bkclient.BKClientInit;
import dev.bkclient.utility.concurrent.ClientAsyncExecutor;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/** One-shot, five-minute AirDrop reception. Legacy nickname/Supabase transport is disabled. */
public final class SupabaseService implements IMinecraft {
    private static final String API = "https://changeme.net/mods/api/airdrop/v2";
    private static final int MAX_CONFIG_BYTES = 262144;
    private static SupabaseService instance;
    private volatile boolean active;
    private volatile long generation, deadline;
    private volatile String code = "", token = "", status = "AirDrop off";
    private Timer timer;
    private String lastCheckedSender, lastCheckedConfig, pendingId, pendingData;
    private final java.util.concurrent.atomic.AtomicBoolean polling = new java.util.concurrent.atomic.AtomicBoolean();
    public static synchronized SupabaseService getInstance() {
        if (instance == null) instance = new SupabaseService();
        return instance;
    }
    public static void stopIfInitialized() { if (instance != null) instance.stopPolling(); }
    public boolean isActive() { return active; }
    public String getReceiveCode() { return code; }
    public String getStatus() { return status; }
    public long remainingSeconds() { return active ? Math.max(0, (deadline-System.nanoTime())/1_000_000_000L) : 0; }
    public void copyReceiveCode() {
        if (!code.isEmpty() && active) { mc.keyboard.setClipboard(code); }
    }
    public synchronized void startPolling() {
        if (!dev.bkclient.systems.DeferredFeatures.AIRDROP) return;
        if (active) return;
        active = true; code = ""; token = ""; status = "Creating receive code...";
        deadline = System.nanoTime() + 300_000_000_000L;
        long current = ++generation;
        timer = new Timer("BK-AirDrop-Expiry", true);
        timer.schedule(new TimerTask() { public void run() {
            runOnClient(() -> finish(current, "AirDrop expired - receiving off"));
        } }, 300000);
        ClientAsyncExecutor.execute(() -> {
            try {
                JsonObject response = request("/sessions", new JsonObject());
                String receivedToken=response.get("token").getAsString();
                synchronized (this) {
                    if (!active || generation != current) { cancelRemote(receivedToken); return; }
                    token=receivedToken; code=response.get("code").getAsString(); status="Receiving for 5 minutes";
                    timer.schedule(new TimerTask() { public void run() {
                        if (polling.compareAndSet(false, true)) ClientAsyncExecutor.execute(() -> {
                            try { poll(current); } finally { polling.set(false); }
                        });
                    } }, 1000, 8000);
                }

            } catch (Exception failure) {
                runOnClient(() -> finish(current, "Could not create AirDrop code - receiving off"));
            }
        });
    }
    public synchronized void stopPolling() {
        String oldToken=token;
        active=false; ++generation; code=""; token=""; deadline=0; status="AirDrop off";
        if(timer!=null){timer.cancel();timer=null;}
        cancelRemote(oldToken);
    }
    private void cancelRemote(String oldToken) {
        if(oldToken.isEmpty())return;
        ClientAsyncExecutor.execute(() -> { try { request("/cancel", tokenBody(oldToken)); } catch(Exception ignored) {} });
    }
    private void finish(long current, String reason) {
        if(!active || generation!=current)return;
        stopPolling();
        status=reason;

    }
    private void poll(long current) {
        if(!active || generation!=current)return;
        if(System.nanoTime()>=deadline){runOnClient(()->finish(current,"AirDrop expired - receiving off"));return;}
        try {
            JsonObject response=request("/receive",tokenBody(token));
            if(!response.has("transfer")){if(active && generation==current)status="Waiting for a config";return;}
            JsonObject transfer=response.getAsJsonObject("transfer");
            String data=transfer.get("config_data").getAsString();
            if(!validConfig(data))throw new java.io.IOException("Invalid config");
            String name=transfer.get("config_name").getAsString().replaceAll("[^\\p{L}\\p{N} _.-]", "").replace("..", "");
            if(name.isBlank())name="Received config";
            final String safeName=name;
            runOnClient(()->{
                if(!active || generation!=current || System.nanoTime()>=deadline)return;
                // Close reception before showing the confirmation. No further popups can arrive.
                finish(current,"Config received - AirDrop off");
                lastCheckedSender="One-time code"; lastCheckedConfig=safeName + "_received_" + System.currentTimeMillis();
                pendingId=transfer.get("id").getAsString(); pendingData=data;
                dev.bkclient.systems.notifications.AirdropNotification.getInstance().show(
                    pendingId,lastCheckedSender,lastCheckedConfig,data);
            });
        } catch(HttpFailure failure) {
            if(failure.status==410 || failure.status==404)runOnClient(()->finish(current,"AirDrop expired - receiving off"));
        } catch(Exception ignored) { if(active && generation==current)status="Connection lost - retrying..."; }
    }
    public static String normalizeCode(String value) { return value==null?"":value.replaceAll("\\s", "").toUpperCase(java.util.Locale.ROOT); }
    public static boolean validCode(String value) { return normalizeCode(value).matches("[A-Z0-9_-]{12}"); }
    private static boolean validConfig(String data) {
        if(data==null||data.getBytes(StandardCharsets.UTF_8).length>MAX_CONFIG_BYTES)return false;
        try { var j=JsonParser.parseString(data).getAsJsonObject();return j.has("modules")&&j.get("modules").isJsonArray(); }
        catch(Exception ignored){return false;}
    }
    public record CodeCheck(boolean ready, long ttlSeconds, String message) {}
    public void checkReceiveCode(String targetCode, Consumer<CodeCheck> completion) {
        if (!dev.bkclient.systems.DeferredFeatures.AIRDROP) { completion.accept(new CodeCheck(false, 0, "AirDrop disabled")); return; }
        if (!validCode(targetCode)) { completion.accept(new CodeCheck(false, 0, "Enter a 12-character code")); return; }
        var body = new JsonObject(); body.addProperty("code", normalizeCode(targetCode));
        ClientAsyncExecutor.execute(() -> {
            CodeCheck result;
            try {
                var response = request("/check", body);
                result = new CodeCheck(true, response.get("ttlSeconds").getAsLong(), "Ready - receiver code active");
            } catch (HttpFailure failure) {
                result = new CodeCheck(false, 0, failure.status == 429 ? "Too many checks - wait a minute" :
                    failure.status == 404 || failure.status == 410 ? "Code expired or already used" : "Service unavailable");
            } catch (Exception failure) { result = new CodeCheck(false, 0, "Connection failed - try again"); }
            final CodeCheck checked = result;
            runOnClient(() -> completion.accept(checked));
        });
    }
    public boolean hasPendingTransfer() { return pendingId != null; }
    public String getPendingConfigName() { return pendingId == null ? "" : lastCheckedConfig; }
    public void acceptPendingTransfer() {
        acceptAirdrop(pendingId, pendingData);
        dev.bkclient.systems.notifications.AirdropNotification.dismissIfInitialized();
    }
    public void rejectPendingTransfer() {
        rejectAirdrop(pendingId);
        dev.bkclient.systems.notifications.AirdropNotification.dismissIfInitialized();
        status = "Config rejected";
    }
    public void sendAirdrop(String targetCode,String name,String data){sendAirdrop(targetCode,name,data,ok->{});}
    public void sendAirdrop(String targetCode,String name,String data,Consumer<Boolean> completion){
        if (!dev.bkclient.systems.DeferredFeatures.AIRDROP) { completion.accept(false); return; }
        if(!validCode(targetCode)||!validConfig(data)){runOnClient(()->{message("Enter a valid 12-character receive code and config.");completion.accept(false);});return;}
        JsonObject body=new JsonObject();body.addProperty("code",normalizeCode(targetCode));body.addProperty("config_name",name);body.addProperty("config_data",data);
        ClientAsyncExecutor.execute(()->{
            boolean ok=false;String result;
            try{request("/send",body);ok=true;result="AirDrop sent. Receive code has been used.";}
            catch(HttpFailure failure){result=switch(failure.status){case 404,410 -> "Code expired or already used.";case 429 -> "Too many attempts. Try later.";case 413 -> "Config is too large.";default -> "AirDrop unavailable. Try later.";};}
            catch(Exception failure){result="Connection failed. Check with the recipient before retrying.";}
            final boolean success=ok;final String msg=result;
            runOnClient(()->{message(msg);completion.accept(success);});
        });
    }
    // Delivery is removed when reception stops. Acceptance/rejection stays local.
    public void updateStatus(String id,String status) { if (id != null && id.equals(pendingId)) { pendingId=null; pendingData=null; } }
    public void acceptAirdrop(String id,String data) {
        if (!dev.bkclient.systems.DeferredFeatures.AIRDROP) return;
        if(id==null || !id.equals(pendingId) || !java.util.Objects.equals(data,pendingData))return;
        try {
            var file=new dev.bkclient.systems.config.ConfigFile(lastCheckedConfig);
            file.loadFromJson(JsonParser.parseString(data).getAsJsonObject());
            file.save(); BKClientInit.getInstance().getConfigManager().setCurrent(file);
            status = "Config accepted: " + lastCheckedConfig;
            message(status);
        } catch(Exception failure) { message("Invalid config - could not apply AirDrop."); }
        finally { pendingId=null; pendingData=null; }
    }
    public void rejectAirdrop(String id) { updateStatus(id,"rejected"); }
    public String getLastCheckedSender(){return lastCheckedSender;}
    public String getLastCheckedConfig(){return lastCheckedConfig;}
    private static JsonObject tokenBody(String token){var b=new JsonObject();b.addProperty("token",token);return b;}
    private static JsonObject request(String path,JsonObject body)throws java.io.IOException {
        if (!dev.bkclient.systems.DeferredFeatures.AIRDROP) throw new java.io.IOException("AirDrop disabled");
        HttpURLConnection c=(HttpURLConnection)URI.create(API+path).toURL().openConnection();
        try{
            c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setRequestMethod("POST");c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);
            try(var out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int status=c.getResponseCode();if(status<200||status>=300)throw new HttpFailure(status);
            try(var in=c.getInputStream()){
                byte[] bytes=in.readNBytes(524289);if(bytes.length>524288)throw new java.io.IOException("Response too large");
                return JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            }
        }finally{c.disconnect();}
    }
    private static class HttpFailure extends java.io.IOException { final int status;HttpFailure(int status){super("AirDrop HTTP "+status);this.status=status;} }
    private static void message(String text){MessageUtility.info(Text.of(text));}
    private static void runOnClient(Runnable action){if(mc.isOnThread())action.run();else mc.execute(action);}
}
