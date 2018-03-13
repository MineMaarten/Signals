package com.minemaarten.signals.rail.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class RailIterator<TPos extends IPosition<TPos>> {
    private final RailNetwork<TPos> network;

    public static enum TraverseMode{
        /**
         * With rail crossings, only the opposites connect
         */
        PATHFINDING,
        /**
         * With rail crossings, every side connects.
         */
        SECTIONS
    }

    public RailIterator(RailNetwork<TPos> network){
        this.network = network;
    }

    public void iterate(){
        //Wrap in HashSet because java doesn't guarentee mutability with Collectors.toSet()
        Set<NetworkRail<TPos>> toTraverse = new HashSet<>(network.getRails().collect(Collectors.toList()));
        Set<TPos> traversed = new HashSet<>();

        while(!toTraverse.isEmpty()) {
            NetworkRail<TPos> first = toTraverse.iterator().next();
            Map<TPos, NetworkObject<TPos>> sectionObjects = new HashMap<>();

        }
    }

    public abstract boolean canTraversePast(NetworkObject<TPos> curRail, EnumHeading traversalDir);
}
