package com.minemaarten.signals.rail.network;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

public abstract class NetworkRail<TPos extends IPosition<TPos>> extends NetworkObject<TPos>{

    public NetworkRail(TPos pos){
        super(pos);
    }

    /**
     * Arbitrary object to determine if rail types are equal or different. Used to take the rails in front of signals.
     * @return
     */
    public abstract Object getRailType();

    /**
     * All potential positions a neighboring rail could be. This does not take entry directions into account.
     * @return
     */
    public abstract List<TPos> getPotentialNeighborRailLocations();

    public abstract EnumSet<EnumHeading> getPotentialNeighborRailHeadings();

    /**
     * Signals, Rail Links, Station markers. This should always be equal to, or a subset of getPotentialNeighborRailLocations
     * @return
     */
    public abstract List<TPos> getPotentialNeighborObjectLocations();

    public abstract Collection<TPos> getPotentialPathfindNeighbors(EnumHeading entryDir);

    //@formatter:off
    public Stream<NetworkRail<TPos>> getRailLinkConnectedRails(RailObjectHolder<TPos> railObjects){
        Stream<NetworkRail<TPos>> linkedToNeighbors = railObjects.getNeighborRailLinks(getPotentialNeighborObjectLocations())
                .filter(l -> l.getDestinationPos() != null)
                .map(l -> railObjects.get(l.getDestinationPos()))
                .filter(r -> r instanceof NetworkRail)
                .map(r -> ((NetworkRail<TPos>)r));

        //The actual neighbors, the connecting rail links connecting elsewhere, and the rail links that connect to this rail.
        return Streams.concat(linkedToNeighbors, railObjects.findRailsLinkingTo(pos));
    }
    //@formatter:on

    public Stream<NetworkRail<TPos>> getSectionNeighborRails(RailObjectHolder<TPos> railObjects){
        Stream<NetworkRail<TPos>> normalNeighbors = railObjects.getNeighborRails(getPotentialNeighborRailLocations());
        return Streams.concat(normalNeighbors, getRailLinkConnectedRails(railObjects));
    }

    public Stream<NetworkRail<TPos>> getPathfindingNeighborRails(RailObjectHolder<TPos> railObjects, EnumHeading entryDir){
        if(entryDir == null) return getSectionNeighborRails(railObjects); //When stuff with Rail Link, we can't properly use pathfind directions
        Stream<NetworkRail<TPos>> normalNeighbors = railObjects.getNeighborRails(getPotentialPathfindNeighbors(entryDir));
        return Streams.concat(normalNeighbors, getRailLinkConnectedRails(railObjects));
    }
}
