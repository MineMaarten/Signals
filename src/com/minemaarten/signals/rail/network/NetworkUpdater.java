package com.minemaarten.signals.rail.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class NetworkUpdater<TPos extends IPosition<TPos>> {
    private final INetworkObjectProvider<TPos> objectProvider;
    private final Set<TPos> dirtyPositions = new HashSet<>(); //Positions that have possibly changed

    public NetworkUpdater(INetworkObjectProvider<TPos> objectProvider){
        this.objectProvider = objectProvider;
    }

    public void markDirty(TPos pos){
        dirtyPositions.add(pos);
    }

    /**
     * Updates the network, using the positions that have been reported dirty.
     * 1. positions already in the network marked dirty get re-acquired.
     * 2. Neighbors of the positions marked dirty get re-acquired, and possibly cause a recursive look-up. For example, a rail section that wasn't part of the network before now may, because of a gap being filled in with a new rail
     */
    public RailNetwork<TPos> updateNetwork(RailNetwork<TPos> network){
        if(dirtyPositions.isEmpty()) return network; //Nothing to update.

        Map<TPos, NetworkObject<TPos>> allObjects = new HashMap<>(network.railObjects.getAllNetworkObjects());

        //Remove all existing objects that were marked dirty.
        for(TPos dirtyPos : dirtyPositions) {
            allObjects.remove(dirtyPos);
        }

        //Re-acquire positions that were marked dirty, and possibly recursively look up other parts.
        Stack<TPos> toEvaluate = new Stack<>();
        dirtyPositions.forEach(pos -> toEvaluate.push(pos));
        while(!toEvaluate.isEmpty()) {
            TPos curPos = toEvaluate.pop();

            if(!allObjects.containsKey(curPos)) {
                NetworkObject<TPos> networkObject = objectProvider.provide(curPos);
                if(networkObject != null) {
                    allObjects.put(curPos, networkObject);

                    if(networkObject instanceof NetworkRail) {
                        for(TPos neighborPos : ((NetworkRail<TPos>)networkObject).getPotentialNeighborRailLocations()) {
                            toEvaluate.push(neighborPos);
                        }
                    }
                }
            }
        }

        dirtyPositions.clear();
        return new RailNetwork<TPos>(allObjects);
    }
}
