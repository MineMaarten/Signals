package com.minemaarten.signals.rail.network;

/**
 * Any object that participates in a rail network, currently: rails, signals and station markers.
 * Immutable, and should expect to be reused in transitions from old to new networks to prevent allocations.
 * @author Maarten
 *
 * @param <TPos>
 */
public abstract class NetworkObject<TPos> {
    public final TPos pos;

    public NetworkObject(TPos pos){
        this.pos = pos;
    }
}
