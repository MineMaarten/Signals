package com.minemaarten.signals.rail.network;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetworkRailLink<TPos extends IPosition<TPos>> extends NetworkObject<TPos>{

    private final TPos destination;
    private final List<TPos> potentialNeighbors;

    public NetworkRailLink(TPos pos, TPos destination){
        super(pos);
        this.destination = destination;
        potentialNeighbors = EnumHeading.valuesStream().map(pos::offset).collect(Collectors.toList());
    }

    public TPos getDestinationPos(){
        return destination;
    }

    public Stream<NetworkRail<TPos>> getNeighborRails(RailObjectHolder<TPos> railObjects){
        return railObjects.getNeighborRails(potentialNeighbors);
    }
}
