package com.minemaarten.signals.rail;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.minemaarten.signals.rail.network.IPosition;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.NetworkSignal;

/**
 * Helper class to allow querying network objects. Designed to be immutable.
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailObjectHolder<TPos extends IPosition<TPos>> {
    private ImmutableMap<TPos, NetworkObject<TPos>> allNetworkObjects;

    public RailObjectHolder(List<NetworkObject<TPos>> allNetworkObjects){
        this.allNetworkObjects = ImmutableMap.<TPos, NetworkObject<TPos>> copyOf(allNetworkObjects.stream().collect(Collectors.toMap(n -> n.pos, n -> n)));
    }

    public NetworkObject<TPos> get(TPos pos){
        return allNetworkObjects.get(pos);
    }

    public <T extends NetworkObject<TPos>> Stream<T> networkObjectsOfType(Class<T> clazz){
        return allNetworkObjects.values().stream().filter(o -> clazz.isAssignableFrom(o.getClass())).map(clazz::cast);
    }

    public Stream<NetworkRail<TPos>> getRails(){
        return allNetworkObjects.values().stream().filter(o -> o instanceof NetworkRail).map(o -> (NetworkRail<TPos>)o);
    }

    public Stream<NetworkRail<TPos>> getNeighborRails(Collection<TPos> potentialNeighbors){
        return ofType(NetworkRail.class, potentialNeighbors.stream().map(n -> allNetworkObjects.get(n)));
    }

    public Stream<NetworkSignal<TPos>> getNeighborSignals(Collection<TPos> potentialNeighbors){
        return ofType(NetworkSignal.class, potentialNeighbors.stream().map(n -> allNetworkObjects.get(n)));
    }

    /**
     * Filter by the requested type and cast the remaining items.
     * @param type
     * @param stream
     * @return
     */
    private static <T> Stream<T> ofType(Class<? extends T> type, Stream<? super T> stream){
        return stream.filter(el -> el != null && type.isAssignableFrom(el.getClass())).map(type::cast);
    }

    @Override
    public boolean equals(Object other){
        if(other instanceof RailObjectHolder) {
            return ((RailObjectHolder<?>)other).allNetworkObjects.equals(allNetworkObjects);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(){
        return allNetworkObjects.hashCode();
    }
}
