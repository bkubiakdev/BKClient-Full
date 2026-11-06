package dev.bkclient;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.FabricLoader;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Starts Discord IPC before Minecraft initialises its client screens. */
public final class BKClientPreLaunch implements PreLaunchEntrypoint {
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("BKClient-DiscordRPC");
   private static java.util.concurrent.ScheduledFuture<?> presenceTask;
   public static volatile RandomAccessFile pipe;
   public static volatile boolean connected;
   public static volatile boolean enabled = true;
   // BKClient application ID from the working legacy client; keep separate from the launcher.
   public static final String CLIENT_ID = "1505968319226708089";
   public static final long START_TS = System.currentTimeMillis() / 1000L;
   public static ScheduledExecutorService executor;

   @Override
   public void onPreLaunch() {
      executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
         Thread thread = new Thread(runnable, "BKClient-DiscordRPC");
         thread.setDaemon(true);
         return thread;
      });
      executor.execute(BKClientPreLaunch::tryConnect);
   }

   public static void tryConnect() {
      if (!enabled || connected || executor == null || executor.isShutdown()) return;
      try {
         pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-0", "rw");
         sendFrame(0, "{\"v\":1,\"client_id\":\"" + CLIENT_ID + "\"}");
         String ready = readFrame();
         if (!"READY".equals(com.google.gson.JsonParser.parseString(ready).getAsJsonObject().get("evt").getAsString())) {
            throw new java.io.IOException("Discord did not acknowledge the handshake");
         }
         connected = true;
         sendPresence();
         if (!connected) return;
         LOGGER.info("Discord RPC connected to BKClient application {}", CLIENT_ID);
         if (presenceTask != null) presenceTask.cancel(false);
         presenceTask = executor.scheduleAtFixedRate(() -> {
            if (connected && enabled) sendPresence();
         }, 10L, 10L, TimeUnit.SECONDS);
      } catch (Exception error) {
         connected = false;
         LOGGER.warn("Discord RPC connection failed: {}", error.toString());
         closePipe();
         if (enabled && !executor.isShutdown()) {
            executor.schedule(BKClientPreLaunch::tryConnect, 30L, TimeUnit.SECONDS);
         }
      }
   }

   public static void sendPresence() {
      try {
         String payload = "{\"cmd\":\"SET_ACTIVITY\",\"nonce\":\"" + System.nanoTime()
            + "\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
            + ",\"activity\":{\"details\":\"Best optimization client\",\"state\":\""
            + rpcVersionAndModCount() + "\",\"timestamps\":{\"start\":" + START_TS
            + "},\"buttons\":[{\"label\":\"Download\",\"url\":\"https://changeme.net/mods\"},"
            + "{\"label\":\"Discord\",\"url\":\"https://discord.gg/t3Wn4YpnTW\"}]}}}";
         sendFrame(1, payload);
         readFrame();
      } catch (Exception error) {
         LOGGER.warn("Discord RPC activity failed: {}", error.toString());
         connected = false;
         if (presenceTask != null) presenceTask.cancel(false);
         closePipe();
         if (enabled && executor != null && !executor.isShutdown()) {
            executor.schedule(BKClientPreLaunch::tryConnect, 30L, TimeUnit.SECONDS);
         }
      }
   }

   private static String rpcVersionAndModCount() {
      int mods = FabricLoader.getInstance().getAllMods().size();
      return "1.21.4 (" + mods + " mods)";
   }

   public static void clearPresence() {
      try {
         String payload = "{\"cmd\":\"SET_ACTIVITY\",\"nonce\":\"" + System.nanoTime()
            + "\",\"args\":{\"pid\":" + ProcessHandle.current().pid() + ",\"activity\":null}}";
         sendFrame(1, payload);
         readFrame();
      } catch (Exception ignored) {
      }
   }

   private static void sendFrame(int operation, String json) throws Exception {
      if (pipe == null) throw new IllegalStateException("Discord pipe is closed");
      byte[] data = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      ByteBuffer buffer = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(operation).putInt(data.length).put(data);
      pipe.write(buffer.array());
   }

   private static String readFrame() throws Exception {
      byte[] header = new byte[8];
      pipe.readFully(header);
      int length = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
      if (length <= 0 || length > 1_048_576) throw new java.io.IOException("Invalid Discord frame length: " + length);
      byte[] body = new byte[length];
      pipe.readFully(body);
      String json = new String(body, java.nio.charset.StandardCharsets.UTF_8);
      com.google.gson.JsonObject response = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
      if (ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(0) == 2
            || (response.has("evt") && !response.get("evt").isJsonNull() && "ERROR".equals(response.get("evt").getAsString()))) {
         throw new java.io.IOException("Discord rejected RPC: " + json);
      }
      return json;
   }

   private static void closePipe() {
      try {
         if (pipe != null) pipe.close();
      } catch (Exception ignored) {
      }
      pipe = null;
   }

   public static void shutdown() {
      connected = false;
      enabled = false;
      if (executor != null) executor.shutdownNow();
      closePipe();
   }
}
