package com.minemaarten.signals.rail.network;

import static com.minemaarten.signals.lib.StreamUtils.ofType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

/**
 * Helper class to allow querying network objects. Designed to be immutable.
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailObjectHolder<TPos extends IPosition<TPos>> implements Iterable<NetworkObject<TPos>>{
    private final ImmutableMap<TPos, NetworkObject<TPos>> allNetworkObjects;

    public RailObjectHolder(List<NetworkObject<TPos>> allNetworkObjects){
        HashMap<TPos, NetworkObject<TPos>> mutableNetworkMap = new HashMap<>(allNetworkObjects.stream().collect(Collectors.toMap((NetworkObject<TPos> n) -> n.pos, n -> n)));

        //Filter invalid signals, signals that are placed next to intersections, or not next to rails
        for(NetworkObject<TPos> obj : allNetworkObjects) {
            if(obj instanceof NetworkSignal) {
                NetworkSignal<TPos> signal = (NetworkSignal<TPos>)obj;
                NetworkObject<TPos> railObj = mutableNetworkMap.get(signal.getRailPos());
                if(railObj instanceof NetworkRail) {
                    NetworkRail<TPos> rail = (NetworkRail<TPos>)railObj;
                    long neighborCount = ofType(NetworkRail.class, rail.getPotentialNeighborRailLocations().stream().map(mutableNetworkMap::get)).count();
                    if(neighborCount > 2) {
                        mutableNetworkMap.remove(signal.pos); //Invalid: Attached to an intersection.
                    }
                } else {
                    mutableNetworkMap.remove(signal.pos); //Invalid: Not attached to a rail.
                }
            }
        }

        this.allNetworkObjects = ImmutableMap.copyOf(mutableNetworkMap);
    }

    public NetworkObject<TPos> get(TPos pos){
        return allNetworkObjects.get(pos);
    }

    public <T extends NetworkObject<TPos>> Stream<T> networkObjectsOfType(Class<T> clazz){
        return ofType(clazz, allNetworkObjects.values().stream());
    }

    public Stream<NetworkRail<TPos>> getRails(){
        return ofType(NetworkRail.class, allNetworkObjects.values().stream());
    }

    public Stream<NetworkRail<TPos>> getNeighborRails(Collection<TPos> potentialNeighbors){
        return ofType(NetworkRail.class, potentialNeighbors.stream().map(n -> allNetworkObjects.get(n)));
    }

    public Stream<NetworkSignal<TPos>> getSignals(){
        return ofType(NetworkSignal.class, allNetworkObjects.values().stream());
    }

    public Stream<NetworkSignal<TPos>> getNeighborSignals(Collection<TPos> potentialNeighbors){
        return ofType(NetworkSignal.class, potentialNeighbors.stream().map(n -> allNetworkObjects.get(n)));
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

    @Override
    public Iterator<NetworkObject<TPos>> iterator(){
        return allNetworkObjects.values().iterator();
    }
}
