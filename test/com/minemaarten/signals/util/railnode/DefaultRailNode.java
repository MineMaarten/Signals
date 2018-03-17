package com.minemaarten.signals.util.railnode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.util.Pos2D;

public class DefaultRailNode extends NetworkRail<Pos2D>{

    public boolean isDestination;
    public boolean isStart;
    private static final Object railType = new Object();

    public DefaultRailNode(Pos2D pos){
        super(pos);
    }

    public DefaultRailNode setDestination(){
        isDestination = true;
        return this;
    }

    public DefaultRailNode setStart(){
        isStart = true;
        return this;
    }

    @Override
    public List<Pos2D> getPotentialNeighborRailLocations(){
        List<Pos2D> neighbors = new ArrayList<Pos2D>(4);
        neighbors.add(new Pos2D(pos.x + 1, pos.y));
        neighbors.add(new Pos2D(pos.x - 1, pos.y));
        neighbors.add(new Pos2D(pos.x, pos.y + 1));
        neighbors.add(new Pos2D(pos.x, pos.y - 1));
        return neighbors;
    }

    @Override
    public EnumSet<EnumHeading> getPotentialNeighborRailHeadings(){
        return EnumSet.allOf(EnumHeading.class);
    }

    @Override
    public List<Pos2D> getPotentialNeighborObjectLocations(){
        return getPotentialNeighborRailLocations();
    }

    @Override
    public List<Pos2D> getPotentialPathfindNeighbors(EnumHeading entryDir){
        return EnumHeading.valuesStream().map(pos::offset).collect(Collectors.toList());
    }

    @Override
    public Object getRailType(){
        return railType;
    }

}
