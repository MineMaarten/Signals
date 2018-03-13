package com.minemaarten.signals.rail.network;

/**
 * Immutable, and should expect to be reused in transitions from old to new networks to prevent allocations.
 * @author Maarten
 *
 * @param <TPos>
 */
public class NetworkSignal<TPos> extends NetworkObject<TPos>{

    public final EnumHeading heading;

    public NetworkSignal(TPos pos, EnumHeading heading){
        super(pos);
        this.heading = heading;
    }
}
