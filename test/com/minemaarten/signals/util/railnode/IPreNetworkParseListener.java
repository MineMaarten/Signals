package com.minemaarten.signals.util.railnode;

import java.util.List;

import com.minemaarten.signals.rail.network.INetworkObject;
import com.minemaarten.signals.util.Pos2D;

public interface IPreNetworkParseListener{
    public void onPreNetworkParsing(List<INetworkObject<Pos2D>> networkObjects);
}
