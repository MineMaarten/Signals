package com.minemaarten.signals.lib;

import java.util.IdentityHashMap;

public class IdentityHashSet<T> extends IdentityHashMap<T, Object>{
    private static final long serialVersionUID = 8073683307808310158L;
    private static final Object dummy = new Object();

    public boolean contains(T key){
        return containsKey(key);
    }

    public void add(T key){
        put(key, dummy);
    }
}
