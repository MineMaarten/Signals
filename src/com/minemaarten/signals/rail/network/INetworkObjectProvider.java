package com.minemaarten.signals.rail.network;

public interface INetworkObjectProvider<TPos extends IPosition<TPos>> {
    public NetworkObject<TPos> provide(TPos pos);
}
