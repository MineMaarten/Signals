package com.minemaarten.signals.rail.network;

import java.util.List;

public class NetworkCache<TPos extends IPosition<TPos>> {
    private final INetworkObject<TPos> thisObj;
    private RailObjectHolder<TPos> objectNeighbors;

    public NetworkCache(INetworkObject<TPos> thisObj){
        this.thisObj = thisObj;
    }

    public RailObjectHolder<TPos> getObjectNeighbors(RailNetwork<TPos> network){
        if(objectNeighbors == null && thisObj instanceof NetworkRail) {
            List<TPos> potentialNeighbors = ((NetworkRail<TPos>)thisObj).getPotentialNeighborObjectLocations();
            objectNeighbors = new RailObjectHolder<TPos>(potentialNeighbors.stream().map(n -> network.railObjects.get(n)).filter(n -> n != null));
        }
        return objectNeighbors;
    }
}
