package com.minemaarten.signals.rail.network.mc;

import java.util.Collection;
import java.util.Map;

import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.RailNetwork;

public class MCRailNetwork extends RailNetwork<MCPos>{

    public MCRailNetwork(Map<MCPos, NetworkObject<MCPos>> allNetworkObjects){
        super(allNetworkObjects);
    }

    public MCRailNetwork(Collection<NetworkObject<MCPos>> allNetworkObjects){
        super(allNetworkObjects);
    }
}
