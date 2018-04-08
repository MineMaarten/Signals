package com.minemaarten.signals.rail.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.google.common.base.Functions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.network.RailRoute.RailRouteNode;

/**
 * Edge used in pathfinding. Edges may be unidirectional as a result of Signals, and Rail Links.
 * Edges do not have intersections (or they would have been split into multiple edges) 
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailEdge<TPos extends IPosition<TPos>> implements Iterable<NetworkRail<TPos>>{

    private static final double RED_SIGNAL_PENALTY = 10000;

    public final RailObjectHolder<TPos> railObjects;
    public final ImmutableList<NetworkRail<TPos>> edge;

    /**
     * All signals part of this edge, in order of the edge.
     */
    private final ImmutableList<NetworkSignal<TPos>> signals;

    /**
     * Intersection on this edge. This can happen for rail crossings, where for pathfinding the intersection is considered a straight line,
     * while for actually changing the rail junctions these positions need to be considered.
     */
    private final List<RailRouteNode<TPos>> intersections, intersectionsReversed;

    /**
     * The start and end pos, which end in an intersection. 
     */
    public final TPos startPos, endPos;

    /**
     * Outwards heading
     */
    public final EnumHeading startHeading, endHeading;

    /**
     * Length in amount of rails (blocks, realistically), used as weights in pathfinding
     */
    public final int length;

    /**
     * When true, trains can only be routed from startPos to endPos, not the other way around.
     */
    public final boolean unidirectional;

    private static enum EnumDirectionalityResult{
        /**
         * Trains can be routed both ways
         */
        BIDIRECTIONAL,
        /**
         * Trains can be routed one way only, and the given order is good
         */
        UNIDIRECTIONAL_NO_CHANGE,
        /**
         * Trains can be routed one way only, and the order needs to be reversed
         */
        UNIDIRECTIONAL_REVERSE,
        /**
         * Signals are placed in opposite directions, trains cannot be routed through here at all.
         */
        ZERODIRECTIONAL
    }

    public RailEdge(RailObjectHolder<TPos> allRailObjects, ImmutableList<NetworkRail<TPos>> edge){
        this(allRailObjects, edge, null);
    }

    public RailEdge(RailObjectHolder<TPos> allRailObjects, ImmutableList<NetworkRail<TPos>> edge,
            List<RailRouteNode<TPos>> intersections){
        this.railObjects = allRailObjects.subSelection(edge);

        //Filter intersections
        if(intersections != null) {
            intersections = intersections.stream().filter(i -> railObjects.get(i.pos) != null).collect(Collectors.toList());
        }

        switch(determineDirectionality(allRailObjects, edge)){
            case BIDIRECTIONAL:
                unidirectional = false;
                break;
            case UNIDIRECTIONAL_NO_CHANGE:
                unidirectional = true;
                break;
            case UNIDIRECTIONAL_REVERSE:
                unidirectional = true;
                edge = edge.reverse();
                break;
            case ZERODIRECTIONAL:
                unidirectional = false;//TODO zerodirectional
                break;
            default:
                throw new IllegalStateException();
        }

        TPos firstPos = edge.get(0).pos;
        TPos lastPos = edge.get(edge.size() - 1).pos;

        //if bidirectional, save in a deterministic form for equals/hashcode purposes
        if(!unidirectional) {
            int compareResult = firstPos.compareTo(lastPos);

            if(compareResult == 0) { //When startPos == endPos (happens in looped tracks), we need to check the other with the neighboring positions
                compareResult = edge.get(1).pos.compareTo(edge.get(edge.size() - 2).pos);
                if(compareResult == 0) {
                    throw new IllegalStateException("");
                }
            }

            if(compareResult > 0) {
                //Reverse the order
                edge = edge.reverse();
                if(intersections != null) intersections = reverseIntersections(intersections);
                firstPos = edge.get(0).pos;
                lastPos = edge.get(edge.size() - 1).pos;
            }
        }

        this.edge = edge;
        startPos = firstPos;
        endPos = lastPos;
        startHeading = startPos.getRelativeHeading(edge.get(1).pos);
        endHeading = endPos.getRelativeHeading(edge.get(edge.size() - 2).pos);

        length = edge.size();

        this.intersections = intersections == null ? computeIntersections(allRailObjects) : intersections;
        intersectionsReversed = reverseIntersections(this.intersections);

        signals = computeSignals();
    }

    private ImmutableList<NetworkSignal<TPos>> computeSignals(){
        Multimap<TPos, NetworkSignal<TPos>> posToSignals = railObjects.getSignals().collect(Multimaps.toMultimap(NetworkSignal::getRailPos, Functions.identity(), ArrayListMultimap::create));

        ImmutableList.Builder<NetworkSignal<TPos>> builder = ImmutableList.builder();
        for(NetworkRail<TPos> rail : edge) {
            builder.addAll(posToSignals.get(rail.pos));
        }

        return builder.build();
    }

    public ImmutableList<NetworkSignal<TPos>> traverseSignalsWithFirst(TPos pos){
        if(pos.equals(startPos)) return signals;
        if(pos.equals(endPos)) return signals.reverse();
        throw new IllegalArgumentException("Pos " + pos + "not a start or end pos of edge " + this);
    }

    private List<RailRouteNode<TPos>> reverseIntersections(List<RailRouteNode<TPos>> intersections){
        List<RailRouteNode<TPos>> intersectionsReversed = new ArrayList<>(intersections.size());
        for(int i = intersections.size() - 1; i >= 0; i--) {
            intersectionsReversed.add(intersections.get(i).reverse());
        }
        return intersectionsReversed;
    }

    private List<RailRouteNode<TPos>> computeIntersections(RailObjectHolder<TPos> allRailObjects){
        List<RailRouteNode<TPos>> inter = new ArrayList<>();
        for(int i = 1; i < edge.size() - 1; i++) {
            NetworkRail<TPos> rail = edge.get(i);
            if(allRailObjects.getNeighborRailCount(rail.getPotentialNeighborRailLocations()) > 2) {
                NetworkRail<TPos> prev = edge.get(i - 1);
                NetworkRail<TPos> next = edge.get(i + 1);
                EnumHeading dirIn = rail.pos.getRelativeHeading(prev.pos);
                EnumHeading dirOut = rail.pos.getRelativeHeading(next.pos);
                inter.add(new RailRouteNode<TPos>(rail.pos, dirIn, dirOut));
            }
        }
        return inter;
    }

    /**
     * Check connecting signals and determine which way trains can be routed.
     * @param allRailObjects 
     * @param edge
     * @return
     */
    private EnumDirectionalityResult determineDirectionality(RailObjectHolder<TPos> allRailObjects, ImmutableList<NetworkRail<TPos>> edge){
        boolean forwardsOk = true;
        boolean backwardsOk = true;

        //Check signals
        for(int i = 1; i < edge.size() - 1; i++) {
            NetworkRail<TPos> prev = edge.get(i - 1);
            NetworkRail<TPos> cur = edge.get(i);
            NetworkRail<TPos> next = edge.get(i + 1);
            EnumHeading prevDir = cur.pos.getRelativeHeading(prev.pos);
            EnumHeading nextDir = next.pos.getRelativeHeading(cur.pos);

            if(prevDir == nextDir && prevDir != null) { //Only evaluate signals on a straight
                List<NetworkSignal<TPos>> signals = railObjects.getNeighborSignals(cur.getPotentialNeighborObjectLocations()).filter(s -> s.getRailPos().equals(cur.pos)).collect(Collectors.toList());
                for(NetworkSignal<TPos> signal : signals) {
                    if(signal.heading == nextDir) {
                        backwardsOk = false;
                    } else if(signal.heading == nextDir.getOpposite()) {
                        forwardsOk = false;
                    }
                }
            }
        }

        //Check rail links
        for(int i = 1; i < edge.size(); i++) {
            NetworkRail<TPos> prev = edge.get(i - 1);
            NetworkRail<TPos> cur = edge.get(i);
            EnumHeading dir = cur.pos.getRelativeHeading(prev.pos);
            if(dir == null) { //When not a direct neighbor, we have a rail link

                //When we found that there's a link from prev -> cur, forwards is ok, backwards isn't
                if(allRailObjects.findRailsLinkingTo(cur.pos).anyMatch(r -> r.pos.equals(prev.pos))) {
                    backwardsOk = false;
                } else { //If not, it must be the other way around.
                    forwardsOk = false;
                }
            }
        }

        if(backwardsOk && forwardsOk) return EnumDirectionalityResult.BIDIRECTIONAL;
        if(backwardsOk) return EnumDirectionalityResult.UNIDIRECTIONAL_REVERSE;
        if(forwardsOk) return EnumDirectionalityResult.UNIDIRECTIONAL_NO_CHANGE;
        return EnumDirectionalityResult.ZERODIRECTIONAL;
    }

    public NetworkRail<TPos> get(int index){
        return edge.get(index);
    }

    public boolean contains(TPos pos){
        return railObjects.get(pos) != null;
    }

    public boolean isAtStartOrEnd(TPos pos){
        return pos.equals(startPos) || pos.equals(endPos);
    }

    public TPos other(TPos pos){
        if(pos.equals(startPos)) return endPos;
        if(pos.equals(endPos)) return startPos;
        throw new IllegalArgumentException("Pos " + pos + "not a start or end pos of edge " + this);
    }

    public EnumHeading headingForEndpoint(TPos pos){
        if(pos.equals(startPos)) return startHeading;
        if(pos.equals(endPos)) return endHeading;
        throw new IllegalArgumentException("Pos " + pos + "not a start or end pos of edge " + this);
    }

    /**
     * Create fake edges to connect to the startPos or endPos.
     * 
     * s      d       e
     * |------|-------|
     * 
     * creates two edges with
     * 
     * s      e
     * |------|
     * 
     * and
     *        e       s
     *        |-------|
     * 
     * Only edges that can accept a direction from start to end will be added
     * 
     * @param destination
     * @return
     */
    public Collection<RailEdge<TPos>> createEntryPoints(TPos destination){
        int destinationIndex = getIndex(destination);
        List<RailEdge<TPos>> entryPoints = new ArrayList<>(2);

        entryPoints.add(subEdge(0, destinationIndex));
        RailEdge<TPos> subEdge = subEdge(destinationIndex, edge.size() - 1);
        if(!subEdge.unidirectional) {
            //If unidirectional, we can't add the reversed edge, else we can.
            entryPoints.add(subEdge);
        }

        return entryPoints;
    }

    /**
     * Creates a single fake edge to connect to the startPos or endPos.
     * 
     * s      f->     e
     * |------|-------|
     * 
     * creates a single edge with
     *        s       e
     *        |-------|
     * 
     * Only edges that can accept a direction from start to end will be added, and that match the given direction
     */
    public List<RailEdge<TPos>> createExitPoints(TPos from, EnumHeading direction){
        List<RailEdge<TPos>> exitEdges = new ArrayList<>(2);

        int destinationIndex = getIndex(from);

        TPos nextNeighbor = edge.get(destinationIndex + 1).pos;
        EnumHeading nextNeighborHeading = nextNeighbor.getRelativeHeading(from);
        if(direction == null || nextNeighborHeading == null || nextNeighborHeading == direction) {
            exitEdges.add(subEdge(destinationIndex, edge.size() - 1));
        }

        TPos prevNeighbor = edge.get(destinationIndex - 1).pos;
        EnumHeading prevNeighborHeading = prevNeighbor.getRelativeHeading(from);
        if(direction == null || prevNeighborHeading == null || prevNeighborHeading == direction) {
            RailEdge<TPos> subEdge = subEdge(0, destinationIndex);
            if(!subEdge.unidirectional) exitEdges.add(subEdge);//When not unidirectional we can evaluate 'f -> s'
        }
        return exitEdges;
    }

    public int getPathLength(NetworkState<TPos> state){
        //Penalize red signals on the way
        int pathPenalty = (int)(signals.stream().filter(s -> state.getLampStatus(s.pos) == EnumLampStatus.RED).count() * RED_SIGNAL_PENALTY);
        return length + pathPenalty;
    }

    public int getIndex(TPos pos){
        NetworkObject<TPos> destObj = railObjects.get(pos);
        Validate.notNull(destObj);
        int destinationIndex = edge.indexOf(destObj);
        if(destinationIndex < 0) throw new IllegalStateException("Edge " + this + " does not contain " + pos);
        return destinationIndex;
    }

    @Override
    public Iterator<NetworkRail<TPos>> iterator(){
        return edge.iterator();
    }

    public ImmutableList<NetworkRail<TPos>> traverseWithFirst(TPos firstPos){
        if(startPos.equals(firstPos)) return edge;
        if(endPos.equals(firstPos)) return edge.reverse();
        throw new IllegalStateException("Pos " + firstPos + " not start/end of edge " + this);
    }

    public List<RailRouteNode<TPos>> getIntersectionsWithFirst(TPos firstPos){
        if(startPos.equals(firstPos)) return intersections;
        if(endPos.equals(firstPos)) return intersectionsReversed;
        throw new IllegalStateException("Pos " + firstPos + " not start/end of edge " + this);
    }

    public RailEdge<TPos> subEdge(int startIndex, int endIndex){
        ImmutableList<NetworkRail<TPos>> subEdge = edge.subList(startIndex, endIndex + 1);
        return new RailEdge<TPos>(railObjects, subEdge, intersections);
    }

    public RailEdge<TPos> combine(RailEdge<TPos> other, TPos commonPos){
        ImmutableList.Builder<NetworkRail<TPos>> rails = new ImmutableList.Builder<>();
        ImmutableList.Builder<RailRouteNode<TPos>> intersections = new ImmutableList.Builder<>();

        TPos startPos = other(commonPos);
        ImmutableList<NetworkRail<TPos>> firstList = traverseWithFirst(startPos);
        for(int i = 0; i < firstList.size() - 1; i++) {
            rails.add(firstList.get(i));
        }
        rails.addAll(other.traverseWithFirst(commonPos));

        intersections.addAll(getIntersectionsWithFirst(startPos));
        intersections.add(new RailRouteNode<>(commonPos, EnumHeading.getOpposite(headingForEndpoint(commonPos)), EnumHeading.getOpposite(other.headingForEndpoint(commonPos))));
        intersections.addAll(other.getIntersectionsWithFirst(commonPos));

        return new RailEdge<>(railObjects.combine(other.railObjects), rails.build(), intersections.build());
    }

    public boolean canTravelFrom(TPos pos){
        return !unidirectional || pos.equals(startPos);
    }

    @Override
    public String toString(){
        return startPos + " -> " + endPos;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other){
        if(other instanceof RailEdge) {
            RailEdge<TPos> edge = (RailEdge<TPos>)other;
            return startPos.equals(edge.startPos) && endPos.equals(edge.endPos) && Objects.equals(startHeading, edge.startHeading) && Objects.equals(endHeading, edge.endHeading) && unidirectional == edge.unidirectional;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(){
        int hash = startPos.hashCode() * 13;
        hash = hash * 13 + endPos.hashCode();
        hash = hash * 13 + (startHeading == null ? 5 : startHeading.hashCode());
        hash = hash * 13 + (endHeading == null ? 5 : endHeading.hashCode());
        return hash;
    }
}
