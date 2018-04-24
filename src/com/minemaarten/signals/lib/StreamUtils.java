package com.minemaarten.signals.lib;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.reflect.TypeToken;

public class StreamUtils{
    private static final Map<TypeToken<?>, Class<?>> tokenToRawType = new ConcurrentHashMap<>();

    public static Class<?> getRawType(TypeToken<?> token){
        return tokenToRawType.computeIfAbsent(token, t -> t.getRawType());
    }

    /**
     * Filter by the requested type and cast the remaining items.
     * @param type
     * @param stream
     * @return
     */
    public static <T> Stream<T> ofType(Class<? extends T> type, Stream<? super T> stream){
        return stream.filter(el -> el != null && type.isAssignableFrom(el.getClass())).map(type::cast);
    }

    public static <T> Stream<T> ofType(Class<? extends T> type, Iterable<? super T> iterable){
        return ofType(type, StreamSupport.stream(iterable.spliterator(), false));
    }

    @SuppressWarnings("unchecked")
    public static <T> Stream<T> ofType(TypeToken<T> type, Stream<? super T> stream){
        return stream.filter(el -> el != null && getRawType(type).isAssignableFrom(el.getClass())).map(el -> (T)getRawType(type).cast(el));
    }

    public static <T> Stream<T> ofInterface(Class<? extends T> inter, Stream<?> stream){
        return stream.filter(el -> el != null && inter.isAssignableFrom(el.getClass())).map(inter::cast);
    }

    public static <T> Stream<T> ofInterface(Class<? extends T> inter, Iterable<?> iterable){
        return ofInterface(inter, StreamSupport.stream(iterable.spliterator(), false));
    }
}
