package dev.bkclient.utility.concurrent;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

/** Single-owner LRU cache. Eviction and replacement release resources immediately. */
public final class ResourceCache<K, V> {
    private record Entry<V>(V value, long bytes) { }
    private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(16, .75f, true);
    private final int maxEntries;
    private final long maxBytes;
    private final Consumer<V> release;
    private long bytes;

    public ResourceCache(int maxEntries, long maxBytes, Consumer<V> release) {
        if (maxEntries < 1 || maxBytes < 1) throw new IllegalArgumentException("Invalid cache budget");
        this.maxEntries = maxEntries;
        this.maxBytes = maxBytes;
        this.release = release;
    }

    public V get(K key) {
        Entry<V> entry = entries.get(key);
        return entry == null ? null : entry.value;
    }

    public void put(K key, V value, long weight) {
        if (weight < 0 || weight > maxBytes) throw new IllegalArgumentException("Resource exceeds cache budget");
        Entry<V> old = entries.remove(key);
        if (old != null) {
            bytes -= old.bytes;
            if (old.value != value) release.accept(old.value);
        }
        while (!entries.isEmpty() && (entries.size() >= maxEntries || bytes > maxBytes - weight)) {
            var iterator = entries.entrySet().iterator();
            Entry<V> evicted = iterator.next().getValue();
            iterator.remove();
            bytes -= evicted.bytes;
            release.accept(evicted.value);
        }
        entries.put(key, new Entry<>(value, weight));
        bytes += weight;
    }

    public void clear() {
        var values = java.util.List.copyOf(entries.values());
        entries.clear();
        bytes = 0;
        values.forEach(entry -> release.accept(entry.value));
    }

    public int size() { return entries.size(); }
    public long bytes() { return bytes; }
}
