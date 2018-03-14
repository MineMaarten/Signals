package com.minemaarten.signals.rail.network;

/**
 * Immutable, and should expect to be reused in transitions from old to new networks to prevent allocations.
 * @author Maarten
 *
 * @param <TPos>
 */
public class NetworkSignal<TPos extends IPosition<TPos>> extends NetworkObject<TPos>{

    public static enum EnumSignalType{
        BLOCK, CHAIN
    }

    public final EnumHeading heading;
    public final EnumSignalType type;

    public NetworkSignal(TPos pos, EnumHeading heading, EnumSignalType type){
        super(pos);
        this.heading = heading;
        this.type = type;
    }

    public TPos getRailPos(){
        return pos.offset(heading.rotateCCW());
    }
}
