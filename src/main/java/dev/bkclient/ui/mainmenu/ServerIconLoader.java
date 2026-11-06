package dev.bkclient.ui.mainmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import java.util.*;
import java.util.concurrent.*;

/** Menu-local status queries; DNS/connect run off the render thread. */
final class ServerIconLoader {
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(5, 5, 30, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(32), task -> {
            Thread thread = new Thread(task, "BK server icons"); thread.setDaemon(true); return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    static { WORKERS.allowCoreThreadTimeOut(true); }
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<String, byte[]> icons = new HashMap<>();
    private final Map<String, byte[]> dirty = new HashMap<>();
    private final Deque<ServerInfo> queue = new ArrayDeque<>();
    private final List<Request> requests = new ArrayList<>();
    private boolean active;
    private int generation;
    private long nextSave;

    private static final class Request {
        final ServerInfo server;
        final int generation;
        final MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        volatile boolean started, finished, cancelled;
        volatile Future<?> task;
        Request(ServerInfo server, int generation) { this.server = server; this.generation = generation; }
        void cancel() {
            cancelled = true;
            Future<?> pending = task;
            if (pending != null) {
                pending.cancel(true);
                if (pending instanceof Runnable queued) WORKERS.remove(queued);
            }
            pinger.cancel();
        }
    }

    void start() {
        if (active) return;
        active = true;
        ServerList saved = new ServerList(mc); saved.loadFile();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < saved.size(); i++) {
            ServerInfo entry = saved.get(i);
            if (entry.getFavicon() != null) icons.put(entry.address, entry.getFavicon());
            if (seen.add(entry.address)) queue.add(entry);
        }
    }

    /** Pings these exact entries, so their players, ping, MOTD and status fields fill in for the server browser. */
    void start(List<ServerInfo> servers) {
        stop();
        active = true;
        for (ServerInfo entry : servers) {
            if (entry.getFavicon() != null) icons.put(entry.address, entry.getFavicon());
            entry.setStatus(ServerInfo.Status.PINGING);
            entry.ping = -1L;
            queue.add(entry);
        }
    }

    byte[] favicon(ServerInfo server) {
        byte[] icon = icons.get(server.address);
        return icon != null ? icon : server.getFavicon();
    }

    void tick() {
        if (!active) return;
        long now = System.nanoTime();
        for (Iterator<Request> iterator = requests.iterator(); iterator.hasNext();) {
            Request request = iterator.next();
            if (request.started && !request.cancelled) {
                try { request.pinger.tick(); } catch (Exception ignored) { request.finished = true; }
            }
            if (request.finished || now >= request.deadline) {
                // No answer in time (or DNS/connect failed): show the server as offline instead of pinging forever.
                if (request.server.getStatus() == ServerInfo.Status.PINGING && request.server.players == null) {
                    request.server.setStatus(ServerInfo.Status.UNREACHABLE);
                }
                request.cancel(); iterator.remove();
            }
        }
        while (requests.size() < 5 && !queue.isEmpty()) {
            Request request = new Request(queue.removeFirst(), generation); requests.add(request);
            try { request.task = WORKERS.submit(() -> {
                try {
                    if (!request.cancelled) request.pinger.add(request.server,
                        () -> mc.execute(() -> received(request)), () -> request.finished = true);
                } catch (Exception ignored) { request.finished = true; }
                finally {
                    request.started = true;
                    // Closing the menu during DNS/connect must not leave a late connection alive.
                    if (request.cancelled) request.pinger.cancel();
                }
            }); } catch (RejectedExecutionException busy) {
                // A slow DNS resolver must not create an unbounded backlog or block rendering.
                request.finished = true;
            }
        }
        if (!dirty.isEmpty() && now >= nextSave) { save(); nextSave = now + TimeUnit.SECONDS.toNanos(1); }
    }

    private void received(Request request) {
        if (!active || request.generation != generation) return;
        byte[] png = request.server.getFavicon();
        if (png == null || Arrays.equals(icons.get(request.server.address), png)) return;
        icons.put(request.server.address, png);
        dirty.put(request.server.address, png);
    }

    private void save() {
        // Merge icons into the current list; preserve names, order and user edits.
        ServerList saved = new ServerList(mc); saved.loadFile(); boolean changed = false;
        for (int i = 0; i < saved.size(); i++) {
            ServerInfo entry = saved.get(i); byte[] png = dirty.get(entry.address);
            if (png != null && !Arrays.equals(entry.getFavicon(), png)) { entry.setFavicon(png); changed = true; }
        }
        if (changed) saved.saveFile();
        dirty.clear();
    }

    void stop() {
        active = false; generation++; queue.clear();
        for (Request request : requests) request.cancel();
        requests.clear();
        if (!dirty.isEmpty()) save();
        icons.clear();
    }
}
