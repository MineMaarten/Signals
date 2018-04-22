package com.minemaarten.signals.rail.network;

public interface IAdjacentCheckable<TSelf> {
    public boolean isAdjacent(TSelf other);
}
