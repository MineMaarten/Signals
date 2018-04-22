package com.minemaarten.signals.rail.network;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * A rail section is a collection of rails that are separated by signals.
 * It is that collection of rails that may only contain one train at a time.
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailSection<TPos extends IPosition<TPos>> implements Iterable<NetworkRail<TPos>>,
        IAdjacentCheckable<RailSection<TPos>>{

    private final ImmutableMap<TPos, NetworkRail<TPos>> rails;
    public final RailObjectHolder<TPos> railObjects;
    private final Set<TPos> allRailNeighbors;

    public RailSection(RailObjectHolder<TPos> railObjects, Collection<NetworkRail<TPos>> rails){
        this.rails = ImmutableMap.<TPos, NetworkRail<TPos>> copyOf(rails.stream().collect(Collectors.toMap(n -> n.pos, n -> n)));
        this.railObjects = railObjects.subSelection(rails);
        allRailNeighbors = calculateRailNeighbors();
    }

    private ImmutableSet<TPos> calculateRailNeighbors(){
        ImmutableSet.Builder<TPos> builder = ImmutableSet.builder();
        for(TPos railPos : rails.keySet()) {
            for(TPos neighbor : railPos.allHorizontalNeighbors()) {
                if(!rails.containsKey(neighbor)) { //Only neighbors, not actual rails
                    builder.add(neighbor);
                }
            }
        }
        return builder.build();
    }

    /**
     * Retrieves the currently present train on the block, or mimicked, with a Rail Link hold delay
     * @param trains
     * @return the only train that should be on this block, or null.
     */
    public Train<TPos> getTrain(Iterable<? extends Train<TPos>> trains){
        for(Train<TPos> train : trains) {
            for(TPos trainPos : train.getPositions()) {
                if(rails.get(trainPos) != null) return train;
            }
            if(train.getRailLinkHolds().stream().anyMatch(pos -> rails.get(pos) != null)) return train;
        }
        return null;
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

    @Override
    public boolean isAdjacent(RailSection<TPos> section){
        for(TPos railPos : section.rails.keySet()) {
            if(allRailNeighbors.contains(railPos)) return true;
        }
        return false;
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
