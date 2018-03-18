package com.minemaarten.signals.rail.network;

import java.util.Collections;
import java.util.Set;

import com.minemaarten.signals.rail.network.RailRoute.RailRouteNode;

/**
 * A train is a collection of one or more carts that behave like one. Notably they share a route, and multiple carts part of the same train are allowed on a rail section.
 * @author Maarten
 *
 * @param <TPos>
 */
public abstract class Train<TPos extends IPosition<TPos>> {
    private RailRoute<TPos> path;
    private int curIntersection;

    private Set<TPos> positions = Collections.emptySet();
    protected Set<RailSection<TPos>> claimedSections = Collections.emptySet();

    public abstract RailRoute<TPos> pathfind(TPos start, EnumHeading dir);

    protected abstract void updateIntersection(RailRouteNode<TPos> rail);

    /**
     * The positions the train is on.
     * This may be a single position for a cart, or multiple if actually a train.
     * @return
     */
    public final Set<TPos> getPositions(){
        return positions;
    }

    public final void setPositions(Set<TPos> positions){
        if(!this.positions.equals(positions)) { //When the train has moved
            this.positions = positions;
            updateIntersections();
        }
    }

    private void updateIntersections(){
        if(path != null && curIntersection < path.routeNodes.size() && !positions.isEmpty()) {
            RailRouteNode<TPos> curNode = path.routeNodes.get(curIntersection);
            double minDistSq = positions.stream().mapToDouble(curNode.pos::distanceSq).min().getAsDouble();
            if(minDistSq < 4) { //Less than 2 blocks away
                updateIntersection(curNode);
                curIntersection++;
            }
        }
    }

    public RailRoute<TPos> getCurRoute(){
        return path;
    }

    //@formatter:off
    public void setPath(RailNetwork<TPos> network, RailRoute<TPos> path){
        this.path = path;
        if(path != null){
            curIntersection = 0;
            //Take from the signals on the way, their sections.
            /*TODO claimedSections = path.routeEdges.stream()
                                             .flatMap(e -> e.railObjects.getSignals())
                                             .map(s -> network.findSection(s.getRailPos()))
                                             .filter(s -> s != null)
                                             .collect(Collectors.toSet());//TODO only claim up to the next block signal*/
        }else{
            claimedSections = Collections.emptySet();
        }
    }
    //@formatter:on

    /**
     * The sections other trains may not enter, because it has been claimed by this train.
     * @return
     */
    public Set<RailSection<TPos>> getClaimedSections(){
        return claimedSections;
    }

}
