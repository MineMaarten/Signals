package com.minemaarten.signals.rail.network;

public interface INetworkObjectProvider<TPos extends IPosition<TPos>> {
    public INetworkObject<TPos> provide(TPos pos);

    public INetworkObject<TPos> provideRemovalMarker(TPos pos);
}
