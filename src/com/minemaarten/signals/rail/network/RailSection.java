package com.minemaarten.signals.rail.network;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;

/**
 * A rail section is a collection of rails that are separated by signals.
 * It is that collection of rails that may only contain one train at a time.
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailSection<TPos extends IPosition<TPos>> implements Iterable<NetworkRail<TPos>>{

    private final ImmutableMap<TPos, NetworkRail<TPos>> rails;
    public final RailObjectHolder<TPos> railObjects;
    private final PosAABB<TPos> aabb;

    public RailSection(RailObjectHolder<TPos> railObjects, Collection<NetworkRail<TPos>> rails){
        this.rails = ImmutableMap.<TPos, NetworkRail<TPos>> copyOf(rails.stream().collect(Collectors.toMap(n -> n.pos, n -> n)));
        this.railObjects = railObjects.subSelection(rails);
        this.aabb = new PosAABB<>(railObjects.getRails().map(r -> r.pos).collect(Collectors.toList()));
    }

    /**
     * Retrieves the currently present train on the block, or mimicked, with a Rail Link hold delay
     * @param trains
     * @return the only train that should be on this block, or null.
     */
    public Train<TPos> getTrain(Collection<? extends Train<TPos>> trains){
        return trains.stream().filter(t -> t.isInAABB(aabb, true)).findFirst().orElse(null);
    }

    public Stream<TPos> getRailPositions(){
        return rails.keySet().stream();
    }

    public Stream<NetworkSignal<TPos>> getSignals(){
        return railObjects.getSignals();
    }

    /**
     * Returns true if the section contains a rail at the given pos.
     * @param pos
     * @return
     */
    public boolean containsRail(TPos pos){
        return rails.containsKey(pos);
    }

    public boolean isAdjacent(RailSection<TPos> section){
        return rails.keySet().stream().flatMap(TPos::allHorizontalNeighbors).anyMatch(section::containsRail);
    }

    @Override
    public Iterator<NetworkRail<TPos>> iterator(){
        return rails.values().iterator();
    }

    @Override
    public String toString(){
        return StringUtils.join(getRailPositions().collect(Collectors.toList()), ", ");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other){
        if(other instanceof RailSection) {
            boolean eq = rails.keySet().equals(((RailSection<TPos>)other).rails.keySet());
            return eq;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(){
        int hash = rails.keySet().hashCode();
        return hash;
    }
}
