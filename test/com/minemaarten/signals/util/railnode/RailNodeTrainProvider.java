package com.minemaarten.signals.util.railnode;

import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.TestTrain;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

public class RailNodeTrainProvider extends DefaultRailNode{

    private final char trainID;

    public RailNodeTrainProvider(Pos2D pos, char trainID){
        super(pos);
        this.trainID = trainID;
    }

    public TestTrain provideTrain(TestRailNetwork network, NetworkState<Pos2D> state){
        final ImmutableSet<Pos2D> positions = ImmutableSet.copyOf(network.railObjects.networkObjectsOfType(RailNodeTrainProvider.class).filter(r -> r.trainID == trainID).map(r -> r.getPos()).collect(Collectors.toSet()));

        return new TestTrain(network, state, positions, trainID);
    }
}
