package com.minemaarten.signals.rail.network;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;

/**
 * A rail section is a collection of rails that are separated by signals.
 * It is that collection that may only contain one train at a time.
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailSection<TPos extends IPosition<TPos>> implements Iterable<NetworkRail<TPos>>{

    private final ImmutableMap<TPos, NetworkRail<TPos>> rails;
    private final RailObjectHolder<TPos> railObjects;

    public RailSection(RailObjectHolder<TPos> railObjects, Collection<NetworkRail<TPos>> rails){
        this.rails = ImmutableMap.<TPos, NetworkRail<TPos>> copyOf(rails.stream().collect(Collectors.toMap(n -> n.pos, n -> n)));
        this.railObjects = railObjects.subSelection(rails);
    }

    /**
     * Retrieves the currently present train on the block
     * @param trains
     * @return the only train that should be on this block, or null.
     */
    public Train<TPos> getTrain(Iterable<Train<TPos>> trains){
        for(Train<TPos> train : trains) {
            for(TPos trainPos : train.getPositions()) {
                if(rails.get(trainPos) != null) return train;
            }
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
            return rails.equals(((RailSection<TPos>)other).rails);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(){
        return rails.hashCode();
    }
}
