package com.personthecat.cavegenerator.util;

import org.apache.logging.log4j.util.TriConsumer;

import java.util.HashMap;
import java.util.Map;

public class DualHashMap<K1, K2, V> extends HashMap<K1, HashMap<K2, V>> {

    public void put(K1 k1, K2 k2, V v) {
        this.computeIfAbsent(k1, k -> new HashMap<>()).put(k2, v);
    }

    public boolean has(K1 k1, K2 k2) {
        final HashMap<K2, V> inner = this.get(k1);
        if (inner == null) {
            return false;
        }
        return inner.containsKey(k2);
    }

    public void forEachInner(TriConsumer<K1, K2, V> fn) {
        for (Map.Entry<K1, HashMap<K2, V>> e1 : this.entrySet()) {
            for (Map.Entry<K2, V> e2 : e1.getValue().entrySet()) {
                fn.accept(e1.getKey(), e2.getKey(), e2.getValue());
            }
        }
    }
}
