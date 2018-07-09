package com.minemaarten.signals.rail.network;

import java.util.Collection;
import java.util.IdentityHashMap;

import com.google.common.collect.ImmutableMap;
import com.minemaarten.signals.lib.IdentityHashSet;

public class RailNetworkClient<TPos extends IPosition<TPos>> extends RailNetwork<TPos>{

    private IdentityHashMap<RailSection<TPos>, IdentityHashSet<RailSection<TPos>>> adjacentSectionCache;
    private IdentityHashMap<RailEdge<TPos>, IdentityHashSet<RailEdge<TPos>>> adjacentEdgeCache;

    public RailNetworkClient(ImmutableMap<TPos, NetworkObject<TPos>> allNetworkObjects){
        super(allNetworkObjects);
    }

    public static <TPos extends IPosition<TPos>> RailNetworkClient<TPos> empty(){
        return new RailNetworkClient<>(ImmutableMap.<TPos, NetworkObject<TPos>> of());
    }

    @Override
    protected void onAfterBuild(){
        super.onAfterBuild();
        adjacentSectionCache = calculateAdjacentSections(getAllSections());
        adjacentEdgeCache = calculateAdjacentSections(getAllEdges());
    }

    private <T extends IAdjacentCheckable<T>> IdentityHashMap<T, IdentityHashSet<T>> calculateAdjacentSections(Collection<T> allSections){
        IdentityHashMap<T, IdentityHashSet<T>> map = new IdentityHashMap<>();
        allSections/*.parallelStream()*/.forEach(s1 -> { //Don't do in parallel to prevent draining all CPU resources.
            IdentityHashSet<T> adjacentSections = new IdentityHashSet<>();
            for(T s2 : allSections) {
                if(s1 != s2 && s1.isAdjacent(s2)) {
                    adjacentSections.add(s2);
                }
            }

            synchronized(map) {
                map.put(s1, adjacentSections);
            }
        });
        return map;
    }

    public boolean areAdjacent(RailSection<TPos> s1, RailSection<TPos> s2){
        build();
        return adjacentSectionCache.get(s1).contains(s2);
    }

    public boolean areAdjacent(RailEdge<TPos> e1, RailEdge<TPos> e2){
        build();
        return adjacentEdgeCache.get(e1).contains(e2);
    }
}
