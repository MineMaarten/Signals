package com.minemaarten.signals.util.railnode;

import java.util.List;

import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkRailLink;
import com.minemaarten.signals.util.Pos2D;

public class TestRailLink extends NetworkRailLink<Pos2D> implements IPreNetworkParseListener{

    private Pos2D destination;
    private final char destinationID;

    public TestRailLink(Pos2D pos, char destinationID){
        super(pos, null, 0);
        this.destinationID = destinationID;
    }

    @Override
    public Pos2D getDestinationPos(){
        return destination;
    }

    @Override
    public void onPreNetworkParsing(List<NetworkObject<Pos2D>> networkObjects){
        destination = networkObjects.stream().filter(d -> d instanceof RailNodeRailLinkDestination && ((RailNodeRailLinkDestination)d).destinationID == destinationID).findFirst().get().pos;
    }
}
