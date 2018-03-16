package com.minemaarten.signals.util.railnode;

import java.util.List;

import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.util.Pos2D;

public interface IPreNetworkParseListener{
    public void onPreNetworkParsing(List<NetworkObject<Pos2D>> networkObjects);
}
