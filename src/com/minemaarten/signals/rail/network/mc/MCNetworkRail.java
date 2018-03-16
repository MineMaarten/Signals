package com.minemaarten.signals.rail.network.mc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;

import com.google.common.collect.ImmutableList;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkRail;

public class MCNetworkRail extends NetworkRail<MCPos>{

    public static final ImmutableList<EnumHeading> STANDARD_NEIGHBOR_HEADINGS = ImmutableList.copyOf(EnumHeading.values());
    private final Block railType;
    private final ImmutableList<MCPos> potentialRailNeighbors, potentialObjectNeighbors;

    public MCNetworkRail(MCPos pos, Block railType){
        super(pos);
        this.railType = railType;

        //TODO dynamic
        potentialObjectNeighbors = ImmutableList.copyOf(STANDARD_NEIGHBOR_HEADINGS.stream().map(pos::offset).collect(Collectors.toList()));
        potentialRailNeighbors = ImmutableList.copyOf(potentialObjectNeighbors.stream().flatMap(this::plusOneMinusOneHeight).collect(Collectors.toList()));
    }

    private Stream<MCPos> plusOneMinusOneHeight(MCPos pos){
        return Stream.of(pos.offset(EnumFacing.UP), pos, pos.offset(EnumFacing.DOWN));
    }

    @Override
    public Object getRailType(){
        return railType;
    }

    @Override
    public List<MCPos> getPotentialNeighborRailLocations(){
        return potentialRailNeighbors;
    }

    @Override
    public List<EnumHeading> getPotentialNeighborRailHeadings(){
        return STANDARD_NEIGHBOR_HEADINGS;
    }

    @Override
    public List<MCPos> getPotentialNeighborObjectLocations(){
        return potentialObjectNeighbors;
    }

    @Override
    public List<MCPos> getPotentialPathfindNeighbors(EnumHeading entryDir){
        return potentialRailNeighbors; //TODO
    }

}
