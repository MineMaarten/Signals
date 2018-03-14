package com.minemaarten.signals.util.parsing;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.minemaarten.signals.lib.StreamUtils;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.Train;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.railnode.DefaultRailNode;
import com.minemaarten.signals.util.railnode.IValidatingNode;
import com.minemaarten.signals.util.railnode.RailNodeTrainProvider;

public class TestRailNetwork extends RailNetwork<Pos2D>{

    public final Pos2D start;
    public final Set<Pos2D> destinations;
    private final NetworkState<Pos2D> state;

    public TestRailNetwork(List<NetworkObject<Pos2D>> allNetworkObjects){
        super(allNetworkObjects);
        List<DefaultRailNode> startNodes = railObjects.networkObjectsOfType(DefaultRailNode.class).filter(r -> r.isStart).collect(Collectors.toList());
        if(startNodes.size() > 1) throw new IllegalStateException("Multiple start nodes defined: " + startNodes.size());
        start = startNodes.isEmpty() ? null : startNodes.get(0).pos;

        destinations = railObjects.networkObjectsOfType(DefaultRailNode.class).filter(r -> r.isDestination).map(r -> r.pos).collect(Collectors.toSet());

        Set<Train<Pos2D>> trains = railObjects.networkObjectsOfType(RailNodeTrainProvider.class).map(r -> r.provideTrain(this)).collect(Collectors.toSet());
        state = new NetworkState<Pos2D>(trains);
    }

    public RailRoute<Pos2D> pathfind(){
        return pathfind(start, null, destinations);
    }

    public void validate(){
        state.updateSignalStatusses(this);
        StreamUtils.ofInterface(IValidatingNode.class, railObjects).forEach(r -> r.validate(this, state));
    }
}
