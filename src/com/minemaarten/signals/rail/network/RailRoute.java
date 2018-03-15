package com.minemaarten.signals.rail.network;

import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class RailRoute<TPos extends IPosition<TPos>> {

    /**
     * All intersections part of this route
     */
    public final List<RailRouteNode<TPos>> routeNodes;

    /**
     * All (partial) edges part of this route.
     */
    public final List<RailEdge<TPos>> routeEdges;

    /**
     * All individual rails part of this route, in order.
     */
    private final LinkedHashSet<NetworkRail<TPos>> routeRails;

    public RailRoute(List<RailRouteNode<TPos>> routeNodes, LinkedHashSet<NetworkRail<TPos>> routeRails,
            List<RailEdge<TPos>> routeEdges){
        this.routeNodes = routeNodes;
        this.routeRails = routeRails;
        this.routeEdges = routeEdges;
    }

    @Override
    public String toString(){
        return StringUtils.join(routeNodes, " -> ");
    }

    public static class RailRouteNode<TPos> {
        public final TPos pos;
        public final EnumHeading dirIn, dirOut;

        public RailRouteNode(TPos pos, EnumHeading dirIn, EnumHeading dirOut){
            this.pos = pos;
            this.dirIn = dirIn;
            this.dirOut = dirOut;
        }

        @Override
        public String toString(){
            return dirIn.shortString() + "_(" + pos + ")_" + dirOut.shortString();
        }
    }
}
