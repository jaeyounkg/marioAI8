package ch.idsia.utils;

import java.util.HashMap;

public class HashMapWithDefault<K, V> extends HashMap<K, V> {

    private final V defaultValue;

    public HashMapWithDefault(V defaultValue) {
        super();
        this.defaultValue = defaultValue;
    }

    public V get(Object key) {
        V value = super.get(key);
        if (value == null) {
            put((K) key, defaultValue);
            return defaultValue;
        }
        return value;
    }

}
