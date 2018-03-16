package com.minemaarten.signals.rail.network;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

public abstract class NetworkRail<TPos extends IPosition<TPos>> extends NetworkObject<TPos>{

    public NetworkRail(TPos pos){
        super(pos);
    }

    public abstract List<TPos> getPotentialNeighborRailLocations();

    /**
     * Signals, Rail Links, Station markers
     * @return
     */
    public abstract List<TPos> getPotentialNeighborObjectLocations();

    //TODO rail junction handling
    //public abstract Map<TPos, EnumHeading> getPotentialPathfindNeighbors()

    //@formatter:off
    public Stream<NetworkRail<TPos>> getNeighborRails(RailObjectHolder<TPos> railObjects){
        Stream<NetworkRail<TPos>> normalNeighbors = railObjects.getNeighborRails(getPotentialNeighborRailLocations());
        Stream<NetworkRail<TPos>> linkedToNeighbors = railObjects.getNeighborRailLinks(getPotentialNeighborObjectLocations())
                                                                 .filter(l -> l.getDestinationPos() != null)
                                                                 .map(l -> railObjects.get(l.getDestinationPos()))
                                                                 .filter(r -> r instanceof NetworkRail)
                                                                 .map(r -> ((NetworkRail<TPos>)r));
        
        //The actual neighbors, the connecting rail links connecting elsewhere, and the rail links that connect to this rail.
        return Streams.concat(normalNeighbors, linkedToNeighbors, railObjects.findRailsLinkingTo(pos));
    }
    //@formatter:on
}
