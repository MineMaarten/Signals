package com.minemaarten.signals.util.railnode;

import com.minemaarten.signals.util.Pos2D;

public class RailNodeRailLinkDestination extends DefaultRailNode{

    public final char destinationID;

    public RailNodeRailLinkDestination(Pos2D pos, char destinationID){
        super(pos);
        this.destinationID = destinationID;
    }
}
