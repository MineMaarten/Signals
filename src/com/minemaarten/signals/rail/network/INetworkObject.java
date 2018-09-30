package com.minemaarten.signals.rail.network;

import java.util.List;

public interface INetworkObject<TPos> {
    public TPos getPos();

    public int getColor();

    public List<TPos> getNetworkNeighbors();
}
