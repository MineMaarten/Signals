package com.minemaarten.signals.rail.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

/**
 * Entry point for dealing with rail networks. Designed to be immutable.
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailNetwork<TPos extends IPosition<TPos>> {

    public final RailObjectHolder<TPos> railObjects;
    private Map<TPos, RailSection<TPos>> railPosToRailSections = new HashMap<>();
    private Set<RailEdge<TPos>> allEdges = new HashSet<>();

    /**
     * Given a position of a path node, which edges move away from this node?
     */
    //  private Multimap<TPos, RailEdge<TPos>> positionsToEdgesForward = ArrayListMultimap.create();//TODO

    /**
     * Given a position of a path node, which edges can end up in this node?
     */
    private Multimap<TPos, RailEdge<TPos>> positionsToEdgesBackward = ArrayListMultimap.create();

    /**
     * given any rail pos, which edge belongs to this rail?
     * Intersections don't return an edge.
     */
    private Map<TPos, RailEdge<TPos>> railPosToRailEdges = new HashMap<>();

    public RailNetwork(List<NetworkObject<TPos>> allNetworkObjects){
        this.railObjects = new RailObjectHolder<>(allNetworkObjects).filterInvalidSignals();

        buildRailSections();//TODO rail section and edge building can be done in parallel? No MC dependences or interdependencies.
        buildRailEdges();
    }

    private void buildRailSections(){
        //Wrap in HashSet because java doesn't guarantee mutability with Collectors.toSet()
        Set<NetworkRail<TPos>> toTraverse = new HashSet<>(railObjects.getRails().collect(Collectors.toList()));

        while(!toTraverse.isEmpty()) {
            Iterator<NetworkRail<TPos>> toTraverseIterator = toTraverse.iterator();
            NetworkRail<TPos> first = toTraverseIterator.next();
            toTraverseIterator.remove(); //Remove so we don't evaluate it again.

            Set<NetworkRail<TPos>> sectionSet = new HashSet<>();
            sectionSet.add(first);

            Stack<NetworkRail<TPos>> sectionToTraverse = new Stack<>();
            sectionToTraverse.push(first);

            while(!sectionToTraverse.isEmpty()) {
                NetworkRail<TPos> curRail = sectionToTraverse.pop();
                List<NetworkRail<TPos>> neighbors = curRail.getSectionNeighborRails(railObjects).collect(Collectors.toList());

                for(NetworkRail<TPos> neighbor : neighbors) {
                    EnumHeading dir = neighbor.pos.getRelativeHeading(curRail.pos);
                    if(dir == null || getSignalInDir(curRail, dir) == null) { //Only when the neighbor is not on a next section, continue
                        if(dir == null || getSignalInDir(neighbor, dir.getOpposite()) == null) {
                            if(toTraverse.remove(neighbor)) {
                                sectionToTraverse.push(neighbor);
                            }

                            sectionSet.add(neighbor);
                        }
                    }
                }
            }

            addSection(new RailSection<>(railObjects, sectionSet));
        }
    }

    private NetworkSignal<TPos> getSignalInDir(NetworkRail<TPos> rail, EnumHeading dir){
        return railObjects.getNeighborSignals(rail.getPotentialNeighborObjectLocations()).filter(s -> s.heading == dir).findFirst().orElse(null);
    }

    private void addSection(RailSection<TPos> section){
        section.getRailPositions().forEach(pos -> {
            railPosToRailSections.put(pos, section);
        });
    }

    public RailSection<TPos> findSection(TPos pos){
        return railPosToRailSections.get(pos);
    }

    private void buildRailEdges(){
        //All rails need to be traversed, in every direction, because a rail may be a junction, in which case a single block is part of multiple edges.
        Set<Pair<NetworkRail<TPos>, EnumHeading>> toTraverse = new HashSet<>();
        railObjects.getRails().forEach(rail -> {
            for(EnumHeading heading : rail.getPotentialNeighborRailHeadings()) {
                toTraverse.add(new ImmutablePair<NetworkRail<TPos>, EnumHeading>(rail, heading));
            }
        });

        while(!toTraverse.isEmpty()) {
            Pair<NetworkRail<TPos>, EnumHeading> first = toTraverse.iterator().next();

            List<NetworkRail<TPos>> edge = new ArrayList<>();
            Set<NetworkRail<TPos>> edgeSet = new HashSet<>();
            edge.add(first.getLeft());
            edgeSet.add(first.getLeft());

            Stack<Pair<NetworkRail<TPos>, EnumHeading>> edgeToTraverse = new Stack<>();
            edgeToTraverse.push(first);

            boolean startHitIntersection = false;//Used to keep track if the edge stopped because of a dead end or intersection.
            boolean endHitIntersection = false;

            while(!edgeToTraverse.isEmpty()) {
                Pair<NetworkRail<TPos>, EnumHeading> curEntry = edgeToTraverse.pop();
                NetworkRail<TPos> curRail = curEntry.getLeft();
                List<NetworkRail<TPos>> neighbors = curRail.getPathfindingNeighborRails(railObjects, curEntry.getRight()).collect(Collectors.toList());
                if(neighbors.size() < 3) { //If not on an intersection, expand further
                    toTraverse.remove(curEntry);
                    for(NetworkRail<TPos> neighbor : neighbors) {
                        //Add to the current edge, make sure the list is in sequence, so that index 0 is a neighbor of index 1, which is
                        //a neighbor of index 2, etc.
                        if(edgeSet.add(neighbor)) {
                            edgeToTraverse.push(new ImmutablePair<NetworkRail<TPos>, EnumHeading>(neighbor, neighbor.pos.getRelativeHeading(curRail.pos)));
                            if(edge.get(edge.size() - 1).pos.equals(curRail.pos)) {
                                edge.add(neighbor);
                            } else if(edge.get(0).pos.equals(curRail.pos)) {
                                edge.add(0, neighbor);
                            } else {
                                throw new IllegalStateException("Currently evaluated pos is not at the start or end of an edge!");
                            }
                        }
                    }
                } else if(edge.size() == 1) { //when evaluating starting from an intersection, we can create edges that span only 2 blocks, from one intersection to the next
                    toTraverse.remove(curEntry);
                    //When evaluating from an intersection, only look at directly neighboring intersections.
                    for(NetworkRail<TPos> neighbor : neighbors) {
                        EnumHeading heading = neighbor.pos.getRelativeHeading(curRail.pos);
                        List<NetworkRail<TPos>> neighborNeighbors = neighbor.getPathfindingNeighborRails(railObjects, heading).collect(Collectors.toList());
                        if(neighborNeighbors.size() > 2) { //When the neighbor also is on an intersection, we have an edge
                            RailEdge<TPos> railEdge = new RailEdge<>(railObjects, ImmutableList.of(curRail, neighbor));
                            addEdge(railEdge, curRail.pos, true, true);
                        }
                    }
                } else {
                    if(edge.get(edge.size() - 1).pos.equals(curRail.pos)) endHitIntersection = true;
                    if(edge.get(0).pos.equals(curRail.pos)) startHitIntersection = true;
                }
            }

            //Only create edges of 2 or higher, as we are not interested in just intersections
            if(edge.size() > 1) {
                RailEdge<TPos> railEdge = new RailEdge<>(railObjects, ImmutableList.copyOf(edge));
                addEdge(railEdge, edge.get(0).pos, startHitIntersection, endHitIntersection);
            }
        }
    }

    private void addEdge(RailEdge<TPos> railEdge, TPos firstEdgePos, boolean startHitIntersection, boolean endHitIntersection){
        if(allEdges.add(railEdge)) {
            if(!firstEdgePos.equals(railEdge.startPos)) { //If the order got reversed, switch the hit flags
                boolean swap = startHitIntersection;
                startHitIntersection = endHitIntersection;
                endHitIntersection = swap;
            }
            if(!railEdge.unidirectional) positionsToEdgesBackward.put(railEdge.startPos, railEdge);
            positionsToEdgesBackward.put(railEdge.endPos, railEdge);
            for(int i = startHitIntersection ? 1 : 0; i < railEdge.length - (endHitIntersection ? 1 : 0); i++) {
                railPosToRailEdges.put(railEdge.get(i).pos, railEdge);
            }
        }
    }

    /**
     * given any rail pos, which edge belongs to this rail?
     * Intersections don't return an edge.
     */
    public RailEdge<TPos> findEdge(TPos pos){
        return railPosToRailEdges.get(pos);
    }

    public Collection<RailEdge<TPos>> findConnectedEdgesBackwards(TPos intersection){
        return positionsToEdgesBackward.get(intersection);
    }

    /**
     * Pathfinds from 'from' to one of the 'destinations', in the given direction 'direction'.
     * @param from
     * @param direction
     * @param destinations
     * @return a route, or null, if no path was found
     */
    public RailRoute<TPos> pathfind(NetworkState<TPos> state, TPos from, EnumHeading direction, Set<TPos> destinations){
        return new RailPathfinder<TPos>(this, state).pathfindToDestination(from, direction, destinations);
    }
}
