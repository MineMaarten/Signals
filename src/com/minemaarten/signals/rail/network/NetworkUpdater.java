package com.minemaarten.signals.rail.network;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableMap;
import com.minemaarten.signals.lib.Log;

public class NetworkUpdater<TPos extends IPosition<TPos>> {
    private static final int MAX_UPDATES_PER_TICK = 500;
    private final INetworkObjectProvider<TPos> objectProvider;
    private final Set<TPos> dirtyPositions = new HashSet<>(); //Positions that have possibly changed
    private boolean wasVeryBusy, isVeryBusy;

    private Map<TPos, NetworkObject<TPos>> changedObjects = new HashMap<>(); //Global var to prevent putting pressure on GC
    private Set<TPos> allPositions = new HashSet<>(); //Global var to prevent putting pressure on GC

    public NetworkUpdater(INetworkObjectProvider<TPos> objectProvider){
        this.objectProvider = objectProvider;
    }

    public void markDirty(TPos pos){
        dirtyPositions.add(pos);
    }

    public boolean didJustTurnBusy(){
        if(!wasVeryBusy && isVeryBusy) {
            wasVeryBusy = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean didJustTurnIdle(){
        if(wasVeryBusy && !isVeryBusy) {
            wasVeryBusy = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the required updates to the network, using the positions that have been reported dirty.
     * 1. positions already in the network marked dirty get re-acquired.
     * 2. Neighbors of the positions marked dirty get re-acquired, and possibly cause a recursive look-up. For example, a rail section that wasn't part of the network before now may, because of a gap being filled in with a new rail
     * @return Returns the changed objects, where removals are indicated with NetworkObject instanceof IRemovalMarker
     */
    public Collection<NetworkObject<TPos>> getNetworkUpdates(RailNetwork<TPos> network){
        if(dirtyPositions.isEmpty()) return Collections.emptyList(); //Nothing to update.

        changedObjects.clear();
        allPositions.clear();
        allPositions.addAll(network.unfilteredRailObjects.getAllNetworkObjects().keySet());

        //Remove all existing objects that were marked dirty.
        for(TPos dirtyPos : dirtyPositions) {
            if(allPositions.remove(dirtyPos)) {
                changedObjects.put(dirtyPos, objectProvider.provideRemovalMarker(dirtyPos));
            }
        }

        //Re-acquire positions that were marked dirty, and possibly recursively look up other parts.
        Stack<TPos> toEvaluate = new Stack<>();
        Set<TPos> lazyRails = new HashSet<>();
        dirtyPositions.forEach(pos -> toEvaluate.push(pos));
        int updates = 0;
        while(!toEvaluate.isEmpty()) {
            TPos curPos = toEvaluate.pop();

            if(!allPositions.contains(curPos) && !lazyRails.contains(curPos)) {
                NetworkObject<TPos> networkObject = objectProvider.provide(curPos);
                if(networkObject != null) {

                    if(networkObject instanceof NetworkRail) {
                        NetworkRail<TPos> rail = (NetworkRail<TPos>)networkObject;
                        if(!isNextToNetwork(rail, network, changedObjects.keySet())) {
                            lazyRails.add(curPos);
                            continue; //Only include rails when they are adjacent to a rail network.
                        }
                    }

                    allPositions.add(curPos);

                    for(TPos neighborPos : networkObject.getNetworkNeighbors()) {
                        toEvaluate.push(neighborPos);
                        lazyRails.remove(neighborPos); //The rail that was evaluated and was discarded earlier can now be evaluated again.
                    }

                    NetworkObject<TPos> prevObj = network.railObjects.get(curPos);
                    if(!networkObject.equals(prevObj)) { //Only mark stuff changed that actually changed
                        changedObjects.put(curPos, networkObject);
                        updates++;
                    } else {
                        changedObjects.remove(curPos); //Remove any possible removal markers that were inserted.
                    }

                }
                if(updates >= MAX_UPDATES_PER_TICK) {
                    break;
                }
            }
        }
        Log.info("" + allPositions.size() + ", updates: " + changedObjects.size());

        dirtyPositions.clear();
        while(!toEvaluate.isEmpty()) {
            TPos curPos = toEvaluate.pop();
            dirtyPositions.add(curPos);
        }

        if(dirtyPositions.isEmpty()) {
            isVeryBusy = false;
        } else if(dirtyPositions.size() > 10000) {
            isVeryBusy = true;
        }

        return changedObjects.values();
    }

    private boolean isNextToNetwork(NetworkRail<TPos> rail, RailNetwork<TPos> network, Set<TPos> changedPositions){
        for(TPos neighbor : rail.getPotentialNeighborRailLocations()) {
            if(network.unfilteredRailObjects.get(neighbor) != null || changedPositions.contains(neighbor)) {
                return true;
            }
        }
        return false;
    }

    public RailNetwork<TPos> applyUpdates(RailNetwork<TPos> network, Collection<NetworkObject<TPos>> changedObjects){
        if(changedObjects.isEmpty()) return network;

        Map<TPos, NetworkObject<TPos>> allObjects = new HashMap<>(network.unfilteredRailObjects.getAllNetworkObjects());

        for(NetworkObject<TPos> changedObject : changedObjects) {
            if(changedObject instanceof IRemovalMarker) {
                allObjects.remove(changedObject.pos);
            } else {
                allObjects.put(changedObject.pos, changedObject);
            }
        }

        if(network instanceof RailNetworkClient) {
            return new RailNetworkClient<TPos>(ImmutableMap.copyOf(allObjects));
        } else {
            return new RailNetwork<TPos>(ImmutableMap.copyOf(allObjects));
        }
    }
}
