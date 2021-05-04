package com.personthecat.cavegenerator.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

/** A utility for mapping multiple values to a single key. */
public class MultiValueIdentityMap<K, V> extends IdentityHashMap<K, List<V>> {
    public void add(K k, V v) {
        if (!containsKey(k)) {
            put(k, new ArrayList<>());
        }
        get(k).add(v);
    }
}
