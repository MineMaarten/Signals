package com.minemaarten.signals.util.railnode;

import java.util.EnumSet;

import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.util.Pos2D;

public class RailNodeCrossing extends DefaultRailNode{

    public RailNodeCrossing(Pos2D pos){
        super(pos);
    }

    @Override
    public EnumSet<EnumHeading> getPathfindHeading(EnumHeading entryDir){
        return EnumSet.of(entryDir, entryDir.getOpposite());
    }

}
