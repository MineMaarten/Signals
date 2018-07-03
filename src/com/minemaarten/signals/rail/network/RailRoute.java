package com.minemaarten.signals.rail.network;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

public class RailRoute<TPos extends IPosition<TPos>> {

    /**
     * All intersections part of this route
     */
    public final ImmutableList<RailRouteNode<TPos>> routeNodes;

    /**
     * All (partial) edges part of this route.
     */
    public final ImmutableList<RailEdge<TPos>> routeEdges;

    /**
     * All individual rails part of this route, in order. Used just to send to the client to visualize.
     */
    public final ImmutableList<TPos> routeRails;

    /**
     * All signals part of this route, ordered by encountering.
     */
    public final ImmutableList<NetworkSignal<TPos>> routeSignals;

    public RailRoute(ImmutableList<RailRouteNode<TPos>> routeNodes, ImmutableList<TPos> routeRails,
            ImmutableList<RailEdge<TPos>> routeEdges, ImmutableList<NetworkSignal<TPos>> routeSignals){
        this.routeNodes = routeNodes;
        this.routeRails = routeRails;
        this.routeEdges = routeEdges;
        this.routeSignals = routeSignals;
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

        public RailRouteNode<TPos> reverse(){
            return new RailRouteNode<TPos>(pos, EnumHeading.getOpposite(dirOut), EnumHeading.getOpposite(dirIn));
        }

        public boolean isValid(){
            return dirIn != null && dirOut != null;
        }

        @Override
        public String toString(){
            return dirIn.shortString() + "_(" + pos + ")_" + dirOut.shortString();
        }
    }

    public static class RailRouteResult<TPos extends IPosition<TPos>> {
        public final RailRoute<TPos> railRoute;
        public final EnumRouteResult routeResult;

        private RailRouteResult(RailRoute<TPos> railRoute, EnumRouteResult routeResult){
            this.railRoute = railRoute;
            this.routeResult = routeResult;
        }

        public static <TPos extends IPosition<TPos>> RailRouteResult<TPos> success(RailRoute<TPos> railRoute){
            return new RailRouteResult<TPos>(railRoute, EnumRouteResult.SUCCESS);
        }

        public static <TPos extends IPosition<TPos>> RailRouteResult<TPos> noStations(){
            return new RailRouteResult<TPos>(null, EnumRouteResult.NO_STATIONS);
        }

        public static <TPos extends IPosition<TPos>> RailRouteResult<TPos> noPath(){
            return new RailRouteResult<TPos>(null, EnumRouteResult.NO_PATH);
        }
    }

    public static enum EnumRouteResult{
        NO_STATIONS, NO_PATH, SUCCESS
    }
}
