package com.minemaarten.signals.rail.network;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetworkRailLink<TPos extends IPosition<TPos>> extends NetworkObject<TPos>{

    private final TPos destination;
    private final List<TPos> potentialNeighbors;
    public final int holdDelay;

    public NetworkRailLink(TPos pos, TPos destination, int holdDelay){
        super(pos);
        this.destination = destination;
        potentialNeighbors = EnumHeading.valuesStream().map(pos::offset).collect(Collectors.toList());
        this.holdDelay = holdDelay;
    }

    public TPos getDestinationPos(){
        return destination;
    }

    public Stream<NetworkRail<TPos>> getNeighborRails(RailObjectHolder<TPos> railObjects){
        return railObjects.getNeighborRails(potentialNeighbors);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj){
        return obj instanceof NetworkRailLink && Objects.equals(((NetworkRailLink<TPos>)obj).destination, destination);
    }

    @Override
    public int hashCode(){
        return super.hashCode() * 31 + (destination == null ? 0 : destination.hashCode());
    }
}
