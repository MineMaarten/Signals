package com.minemaarten.signals.rail.network;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;
import com.minemaarten.signals.rail.network.RailRoute.RailRouteNode;

/**
 * A train is a collection of one or more carts that behave like one. Notably they share a route, and multiple carts part of the same train are allowed on a rail section.
 * @author Maarten
 *
 * @param <TPos>
 */
public abstract class Train<TPos extends IPosition<TPos>> {
    private static int curID = 0;

    public final int id; //ID used for server -> client communication.
    private RailRoute<TPos> path;
    private int curIntersection;

    private ImmutableSet<TPos> positions = ImmutableSet.of();
    protected Set<RailSection<TPos>> claimedSections = Collections.emptySet();

    public Train(){
        this(curID++);
    }

    public Train(int id){
        this.id = id;
    }

    public abstract RailRoute<TPos> pathfind(TPos start, EnumHeading dir);

    protected abstract void updateIntersection(RailRouteNode<TPos> rail);

    /**
     * The positions the train is on.
     * This may be a single position for a cart, or multiple if actually a train.
     * @return
     */
    public final ImmutableSet<TPos> getPositions(){
        return positions;
    }

    public final void setPositions(RailNetwork<TPos> network, ImmutableSet<TPos> positions){
        if(!this.positions.equals(positions)) { //When the train has moved
            this.positions = positions; //TODO sync
            updateIntersections();
            updateClaimedSections(network);
        }
    }

    protected void updateIntersections(){
        if(path != null && curIntersection < path.routeNodes.size() && !positions.isEmpty()) {
            RailRouteNode<TPos> curNode = path.routeNodes.get(curIntersection);
            double minDistSq = positions.stream().mapToDouble(curNode.pos::distanceSq).min().getAsDouble();
            if(minDistSq < 4) { //Less than 2 blocks away
                updateIntersection(curNode);
                curIntersection++;
            }
        }
    }

    protected void updateClaimedSections(RailNetwork<TPos> network){
        if(!claimedSections.isEmpty()) {
            //Remove the sections the train is now on from the claim list.
            Set<RailSection<TPos>> curSections = positions.stream().map(network::findSection).collect(Collectors.toSet());
            claimedSections.removeAll(curSections);
        }
    }

    public RailRoute<TPos> getCurRoute(){
        return path;
    }

    public void setPath(RailNetwork<TPos> network, NetworkState<TPos> state, RailRoute<TPos> path){
        if(trySetClaims(network, state, path)) {
            this.path = path;
        }
        curIntersection = 0;
    }

    protected boolean trySetClaims(RailNetwork<TPos> network, NetworkState<TPos> state, RailRoute<TPos> path){
        if(path != null) {
            claimedSections = new HashSet<>();
            for(RailEdge<TPos> edge : path.routeEdges) {
                List<NetworkSignal<TPos>> signals = edge.railObjects.getSignals().collect(Collectors.toList());
                boolean containsBlockSignal = signals.stream().anyMatch(s -> s.type == EnumSignalType.BLOCK);
                boolean containsChainSignal = signals.stream().anyMatch(s -> s.type == EnumSignalType.CHAIN);

                if(containsChainSignal) {
                    for(NetworkSignal<TPos> signal : signals) {
                        RailSection<TPos> section = signal.getNextRailSection(network);
                        if(section != null) {
                            Train<TPos> claimingTrain = state.getClaimingTrain(section);
                            if(claimingTrain == null || claimingTrain.equals(this)) {
                                claimedSections.add(section);
                            } else {
                                claimedSections = Collections.emptySet();
                                return false;
                            }
                        }
                    }
                }

                if(containsBlockSignal) break;
            }
        } else {
            claimedSections = Collections.emptySet();
        }
        return true;
    }

    /**
     * The sections other trains may not enter, because it has been claimed by this train.
     * @return
     */
    public Set<RailSection<TPos>> getClaimedSections(){
        return claimedSections;
    }

}
