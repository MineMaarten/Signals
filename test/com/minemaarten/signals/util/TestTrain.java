package com.minemaarten.signals.util;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.RailSection;
import com.minemaarten.signals.rail.network.Train;

public class TestTrain extends Train<Pos2D>{

    private Set<Pos2D> positions;
    private final char trainID;
    private RailRoute<Pos2D> path;
    private Set<RailSection<Pos2D>> claimedSections = Collections.emptySet();

    public TestTrain(Set<Pos2D> positions, char trainID){
        this.positions = positions;
        this.trainID = trainID;
    }

    @Override
    public Set<Pos2D> getPositions(){
        return positions;
    }

    public void setPosition(Pos2D pos){
        positions = Collections.singleton(pos);
    }

    @Override
    public RailRoute<Pos2D> getCurRoute(){
        return path;
    }

    //@formatter:off
    public void setPath(RailNetwork<Pos2D> network, RailRoute<Pos2D> path){
        this.path = path;
        if(path != null){
            //Take from the signals on the way, their sections.
            claimedSections = path.routeEdges.stream()
                                             .flatMap(e -> e.railObjects.getSignals())
                                             .map(s -> network.findSection(s.getRailPos()))
                                             .filter(s -> s != null)
                                             .collect(Collectors.toSet());
        }
    }
    //@formatter:on

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
    public Set<RailSection<Pos2D>> getClaimedSections(){
        return claimedSections;
    }
}
