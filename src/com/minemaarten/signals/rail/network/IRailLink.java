package com.minemaarten.signals.rail.network;

import java.util.stream.Stream;

public interface IRailLink<TPos extends IPosition<TPos>> extends INetworkObject<TPos>{
    public TPos getDestinationPos();

    public int getHoldDelay();

    public Stream<NetworkRail<TPos>> getNeighborRails(RailObjectHolder<TPos> railObjects);

    public default boolean canRailConnect(TPos railPos){
        return true;
    }
}
