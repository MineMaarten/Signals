package com.minemaarten.signals.util.railnode;

import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

public interface IValidatingNode{
    void validate(TestRailNetwork network, NetworkState<Pos2D> state);
}
