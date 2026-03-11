package cn.liboshuai.pulsix.engine.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CollectionCopier {

    private CollectionCopier() {
    }

    public static <T> List<T> copyList(List<? extends T> items) {
        return items == null ? null : new ArrayList<>(items);
    }

    public static <K, V> Map<K, V> copyMap(Map<? extends K, ? extends V> items) {
        return items == null ? null : new LinkedHashMap<>(items);
    }

    public static <K, V> Map<K, List<V>> copyMapOfLists(Map<? extends K, ? extends List<V>> items) {
        if (items == null) {
            return null;
        }
        Map<K, List<V>> copied = new LinkedHashMap<>();
        items.forEach((key, value) -> copied.put(key, copyList(value)));
        return copied;
    }

}
