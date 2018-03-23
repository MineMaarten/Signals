package com.minemaarten.signals.rail.network;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableMap;

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
     * Gets the required updates to the network, using the positions that have been reported dirty.
     * 1. positions already in the network marked dirty get re-acquired.
     * 2. Neighbors of the positions marked dirty get re-acquired, and possibly cause a recursive look-up. For example, a rail section that wasn't part of the network before now may, because of a gap being filled in with a new rail
     * @return Returns the changed objects, where removals are indicated with NetworkObject instanceof IRemovalMarker
     */
    public Collection<NetworkObject<TPos>> getNetworkUpdates(RailNetwork<TPos> network){
        if(dirtyPositions.isEmpty()) return Collections.emptyList(); //Nothing to update.

        Map<TPos, NetworkObject<TPos>> changedObjects = new HashMap<>();
        Map<TPos, NetworkObject<TPos>> allObjects = new HashMap<>(network.railObjects.getAllNetworkObjects());

        //Remove all existing objects that were marked dirty.
        for(TPos dirtyPos : dirtyPositions) {
            if(allObjects.remove(dirtyPos) != null) {
                changedObjects.put(dirtyPos, objectProvider.provideRemovalMarker(dirtyPos));
            }
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

                    NetworkObject<TPos> prevObj = network.railObjects.get(curPos);
                    if(!networkObject.equals(prevObj)) { //Only mark stuff changed that actually changed
                        changedObjects.put(curPos, networkObject);
                    } else {
                        changedObjects.remove(curPos); //Remove any possible removal markers that were inserted.
                    }

                    if(networkObject instanceof NetworkRail) {
                        for(TPos neighborPos : ((NetworkRail<TPos>)networkObject).getPotentialNeighborRailLocations()) {
                            toEvaluate.push(neighborPos);
                        }
                    }
                }
            }
        }

        dirtyPositions.clear();

        return changedObjects.values();
    }

    public RailNetwork<TPos> applyUpdates(RailNetwork<TPos> network, Collection<NetworkObject<TPos>> changedObjects){
        if(changedObjects.isEmpty()) return network;

        Map<TPos, NetworkObject<TPos>> allObjects = new HashMap<>(network.railObjects.getAllNetworkObjects());

        for(NetworkObject<TPos> changedObject : changedObjects) {
            if(changedObject instanceof IRemovalMarker) {
                allObjects.remove(changedObject.pos);
            } else {
                allObjects.put(changedObject.pos, changedObject);
            }
        }

        return new RailNetwork<TPos>(ImmutableMap.copyOf(allObjects));
    }
}
