package com.minemaarten.signals.rail.network;

import java.util.List;

public abstract class NetworkRail<TPos> extends NetworkObject<TPos>{

    public NetworkRail(TPos pos){
        super(pos);
    }

    public abstract List<TPos> getPotentialNeighborLocations();

    //public abstract Map<TPos, EnumHeading> getPotentialPathfindNeighbors()
}
