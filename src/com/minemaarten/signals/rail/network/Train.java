package com.minemaarten.signals.rail.network;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.minemaarten.signals.lib.IdentityHashSet;
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

    protected ImmutableSet<TPos> positions = ImmutableSet.of();
    protected Set<RailSection<TPos>> claimedSections = Collections.emptySet();

    private TObjectIntMap<TPos> railLinkHolds = new TObjectIntHashMap<TPos>();
    private IdentityHashSet<RailSection<TPos>> curSections = new IdentityHashSet<>(); //Usually 1 big for single carts

    private TPos lastPathfindLocation;
    private int pathfindTimeout; //Limit the pathfind interval
    private static final int PATHFIND_TIMEOUT = 20;

    public Train(){
        this(curID++);
    }

    public Train(int id){
        this.id = id;
    }

    /**
     * Limit the pathfinding rate.
     * @param pos
     * @return
     */
    public boolean shouldPathfind(TPos pos){
        if(!pos.equals(lastPathfindLocation) || pathfindTimeout-- <= 0) {
            lastPathfindLocation = pos;
            pathfindTimeout = PATHFIND_TIMEOUT;
            return true;
        } else {
            return false;
        }
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

    public boolean isInAABB(PosAABB<TPos> aabb){
        return isInAABB(aabb, false);
    }

    public boolean isInAABB(PosAABB<TPos> aabb, boolean includeRailLinkHolds){
        for(TPos pos : getPositions()) {
            if(aabb.isInAABB(pos)) return true;
        }

        if(includeRailLinkHolds) {
            return getRailLinkHolds().stream().anyMatch(pos -> aabb.isInAABB(pos));
        } else {
            return false;
        }
    }

    public final Set<TPos> getRailLinkHolds(){
        return railLinkHolds.keySet();
    }

    public final boolean setPositions(RailNetwork<TPos> network, NetworkState<TPos> state, ImmutableSet<TPos> positions){
        if(!this.positions.equals(positions)) { //When the train has moved
            this.positions = positions;
            updateIntersections();
            updateClaimedSections(network);
            onPositionChanged(network, state);
            return true;
        } else {
            return false;
        }
    }

    public void addRailLinkHold(TPos pos, int timeout){
        railLinkHolds.put(pos, timeout);
    }

    public void invalidate(NetworkState<TPos> state){
        //Remove the train from the train sections -> train cache
        state.updateTrainAtSections(this, curSections.keySet(), Collections.emptyList());
    }

    public boolean updatePositions(NetworkState<TPos> state){
        if(!railLinkHolds.isEmpty()) {
            TObjectIntIterator<TPos> iterator = railLinkHolds.iterator();
            while(iterator.hasNext()) {
                iterator.advance();
                if(iterator.value() == 1) {
                    iterator.remove();
                } else {
                    iterator.setValue(iterator.value() - 1);
                }
            }
        }
        return false;
    }

    protected void onPositionChanged(RailNetwork<TPos> network, NetworkState<TPos> state){
        if(network == null || state == null) return; //Client side
        if(curSections.size() == 1 && positions.size() == 1) {
            TPos position = positions.iterator().next();
            RailSection<TPos> section = curSections.keySet().iterator().next();
            if(section.containsRail(position)) {
                return; //Short-cutting the common case
            }
        }

        //Re-aqcuire the current rail sections
        IdentityHashSet<RailSection<TPos>> newSections = new IdentityHashSet<>();
        for(TPos pos : positions) {
            RailSection<TPos> section = network.findSection(pos);
            if(section != null) {
                newSections.add(section);
            }
        }
        state.updateTrainAtSections(this, curSections.keySet(), newSections.keySet());
        curSections = newSections;
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

    public boolean tryUpdatePath(RailNetwork<TPos> network, NetworkState<TPos> state, RailRoute<TPos> path){
        if(trySetClaims(network, state, path)) {
            setPath(path);
            return true;
        } else {
            setPath(null);
            return false;
        }
    }

    public void setPath(RailRoute<TPos> path){
        this.path = path;
        curIntersection = 0;
        if(path == null) clearClaims();
    }

    public void clearClaims(){
        claimedSections = Collections.emptySet();
    }

    protected boolean trySetClaims(RailNetwork<TPos> network, NetworkState<TPos> state, RailRoute<TPos> path){
        if(path != null) {
            claimedSections = new HashSet<>();

            for(NetworkSignal<TPos> signal : path.routeSignals) {
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
                if(signal.type == EnumSignalType.BLOCK) break;
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
