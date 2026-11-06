package dev.bkclient.systems.setting;

import java.util.LinkedHashMap;
import java.util.List;

/** Keep only the latest pending write per destination during a continuous edit. */
public final class PendingGuiWrites {
    private final LinkedHashMap<Object,Runnable> pending = new LinkedHashMap<>();
    public synchronized boolean defer(Object key, Runnable action, boolean editing) {
        if (editing) { pending.put(key,action); return true; }
        pending.remove(key); // An immediate release save supersedes the queued one.
        return false;
    }
    public synchronized List<Runnable> drain() {
        var actions = List.copyOf(pending.values());
        pending.clear();
        return actions;
    }
}
