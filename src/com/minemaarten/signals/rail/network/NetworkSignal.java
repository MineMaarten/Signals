package com.minemaarten.signals.rail.network;

/**
 * Immutable, and should expect to be reused in transitions from old to new networks to prevent allocations.
 * @author Maarten
 *
 * @param <TPos>
 */
public class NetworkSignal<TPos extends IPosition<TPos>> extends NetworkObject<TPos>{

    public static enum EnumSignalType{
        BLOCK, CHAIN;

        public static EnumSignalType[] VALUES = values();
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

    //@formatter:off
    public RailSection<TPos> getNextRailSection(RailNetwork<TPos> network){
        NetworkRail<TPos> rail = (NetworkRail<TPos>)network.railObjects.get(getRailPos()); //Safe to cast, as invalid signals have been filtered
        NetworkRail<TPos> nextSectionRail = network.railObjects.getNeighborRails(rail.getPotentialNeighborRailLocations())
                                                               .filter(r -> r.pos.getRelativeHeading(rail.pos) == heading)
                                                               .findFirst()
                                                               .orElse(null);
        return nextSectionRail != null ? network.findSection(nextSectionRail.pos) : null;
    }
    //@formatter:on
}
