package com.minemaarten.signals.util;

import java.util.Set;

import com.minemaarten.signals.rail.network.Train;

public class TestTrain extends Train<Pos2D>{

    private final Set<Pos2D> positions;
    private final char trainID;

    public TestTrain(Set<Pos2D> positions, char trainID){
        this.positions = positions;
        this.trainID = trainID;
    }

    @Override
    public Set<Pos2D> getPositions(){
        return positions;
    }

    @Override
    public boolean equals(Object other){
        return other instanceof TestTrain && ((TestTrain)other).trainID == trainID;
    }

    @Override
    public int hashCode(){
        return trainID;
    }
}
