package com.personthecat.cavegenerator.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class MultiValueHashMap<K, V> extends HashMap<K, List<V>> {
    public void add(K k, V v) {
        if (!containsKey(k)) {
            put(k, new ArrayList<>());
        }
        get(k).add(v);
    }

    public void forEachInner(BiConsumer<K, V> fn) {
        for (Map.Entry<K, List<V>> entry : this.entrySet()) {
            for (V v : entry.getValue()) {
                fn.accept(entry.getKey(), v);
            }
        }
    }
}
