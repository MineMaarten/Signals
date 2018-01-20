package com.minemaarten.signals.lib;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.util.EnumFacing;

public class SignalBlockSection{

    private final SignalBlockNode rootNode;
    private final Set<SignalBlockNode> allSectionNodes;
    private final Set<SignalBlockNode> allAdjacentNodes;

    public SignalBlockSection(SignalBlockNode rootNode){
        this.rootNode = rootNode;
        allSectionNodes = new HashSet<SignalBlockNode>();
        addAllNodes(rootNode);
        allAdjacentNodes = allSectionNodes.stream().flatMap(SignalBlockSection::getAdjacentNodes).collect(Collectors.toSet());
    }

    /**
     * TODO Performance: could only include actual rail nodes, instead of any neigbhor.
     * @param node
     * @return
     */
    private static Stream<SignalBlockNode> getAdjacentNodes(SignalBlockNode node){
        return Arrays.asList(EnumFacing.values()).stream().map(d -> new SignalBlockNode(node.railPos.offset(d), EnumRailDirection.NORTH_SOUTH));
    }

    private void addAllNodes(SignalBlockNode curNode){
        allSectionNodes.add(curNode);
        curNode.nextNeighbors.forEach(this::addAllNodes);
    }

    public SignalBlockNode getRootNode(){
        return rootNode;
    }

    public boolean contains(SignalBlockNode node){
        return allSectionNodes.contains(node);
    }

    public boolean isAdjacent(SignalBlockSection other){
        return allAdjacentNodes.stream().anyMatch(node -> other.contains(node));
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof SignalBlockSection && ((SignalBlockSection)obj).allSectionNodes.equals(allSectionNodes);
    }

    @Override
    public int hashCode(){
        return allSectionNodes.hashCode();
    }
}
