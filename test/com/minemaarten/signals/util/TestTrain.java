package com.minemaarten.signals.util;

import java.util.Collections;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.RailRoute.RailRouteNode;
import com.minemaarten.signals.rail.network.RailSection;
import com.minemaarten.signals.rail.network.Train;

public class TestTrain extends Train<Pos2D>{

    private final char trainID;
    private Supplier<RailRoute<Pos2D>> pathfinder;

    public TestTrain(RailNetwork<Pos2D> network, ImmutableSet<Pos2D> positions, char trainID){
        setPositions(network, positions);
        this.trainID = trainID;
    }

    public void setPathfinder(Supplier<RailRoute<Pos2D>> pathfinder){
        this.pathfinder = pathfinder;
    }

    public void setPosition(RailNetwork<Pos2D> network, Pos2D pos){
        setPositions(network, ImmutableSet.of(pos));
    }

    public void setClaimingSection(RailSection<Pos2D> section){
        claimedSections = Collections.singleton(section);
    }

    @Override
    public boolean equals(Object other){
        return other instanceof TestTrain && ((TestTrain)other).trainID == trainID;
    }

    @Override
    public int hashCode(){
        return trainID;
    }

    @Override
    public RailRoute<Pos2D> pathfind(Pos2D start, EnumHeading dir){
        return pathfinder == null ? null : pathfinder.get();
    }

    @Override
    protected void updateIntersection(RailRouteNode<Pos2D> rail){

    }
}
