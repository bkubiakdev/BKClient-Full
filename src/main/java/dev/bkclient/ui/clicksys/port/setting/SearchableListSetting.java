package dev.bkclient.ui.clicksys.port.setting;

import java.util.*;
import java.util.function.*;
import dev.bkclient.ui.clicksys.port.util.OptionSearchIndex;

/** Stable option keys are independent of their translated presentation. */
public final class SearchableListSetting extends ListSetting {
    private final List<String> keys;
    private final Function<String, String> label, searchText;
    private final Supplier<Object> version;
    private Object indexedVersion;
    private OptionSearchIndex index;
    private List<String> labels;
    private static long resourceGeneration;
    private long generation = -1;
    public SearchableListSetting(String name, String description, List<String> keys, List<String> selected,
                                 boolean multiple, int min, Function<String, String> label,
                                 Supplier<Object> version) {
        this(name, description, keys, selected, multiple, min, label, version, key -> key);
    }
    public SearchableListSetting(String name, String description, List<String> keys, List<String> selected,
                                 boolean multiple, int min, Function<String, String> label,
                                 Supplier<Object> version, Function<String, String> searchText) {
        super(name, description, keys, selected, multiple, multiple ? keys.size() : 1, min);
        this.keys = List.copyOf(keys); this.label = label; this.version = version; this.searchText = searchText;
    }
    public static void invalidateResources() { resourceGeneration++; }
    public void prepare() {
        Object currentVersion = version.get();
        if (index != null && Objects.equals(currentVersion, indexedVersion) && generation == resourceGeneration) return;
        indexedVersion = currentVersion; generation = resourceGeneration;
        labels = keys.stream().map(label).toList();
        List<String> searchable = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) searchable.add(searchText.apply(keys.get(i)) + " " + keys.get(i) + " " + labels.get(i));
        index = new OptionSearchIndex(searchable);
    }
    public int[] filter(String query) { prepare(); return index.filter(query); }
    public String label(int i) { if (index == null) return label.apply(keys.get(i)); prepare(); return labels.get(i); }
}
