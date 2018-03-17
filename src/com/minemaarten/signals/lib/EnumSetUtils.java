package com.minemaarten.signals.lib;

import java.util.EnumSet;

public class EnumSetUtils{
    public static short toShort(EnumSet<?> set){
        int val = 0;
        for(Enum<?> e : set) {
            val |= 1 << e.ordinal();
        }
        return (short)val;
    }

    public static <T extends Enum<T>> EnumSet<T> toEnumSet(Class<T> clazz, T[] allValues, int val){
        EnumSet<T> ret = EnumSet.noneOf(clazz);
        for(T value : allValues) {
            if((val & (1 << value.ordinal())) != 0) {
                ret.add(value);
            }
        }
        return ret;
    }
}
