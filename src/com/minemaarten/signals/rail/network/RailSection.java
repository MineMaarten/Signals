package com.minemaarten.signals.rail.network;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A rail section is a collection of rails that are separated by signals.
 * It is that collection that may only contain one train at a time.
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailSection<TPos> {

    private Map<TPos, NetworkObject<TPos>> allNetworkObjects;

    public RailSection(Map<TPos, NetworkObject<TPos>> allNetworkObjects){
        this.allNetworkObjects = allNetworkObjects;
    }

    /**
     * Retrieves the currently present train on the block
     * @param trains
     * @return the only train that should be on this block, or null.
     */
    public Train<TPos> getTrain(List<Train<TPos>> trains){
        for(Train<TPos> train : trains) {
            TPos trainPos = train.getPos();
            if(allNetworkObjects.containsKey(trainPos)) return train;
        }
        return null;
    }

    public Stream<TPos> getRailPositions(){
        return allNetworkObjects.entrySet().stream().filter(o -> o.getValue() instanceof NetworkRail).map(o -> o.getKey());
    }

    /**
     * Returns true if the section contains a rail at the given pos.
     * @param pos
     * @return
     */
    public boolean containsRail(TPos pos){
        return allNetworkObjects.get(pos) instanceof NetworkRail;
    }
}
