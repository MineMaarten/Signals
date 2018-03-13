package com.minemaarten.signals.rail.network;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class RailRoute<TPos> {

    public final List<RailRouteNode<TPos>> route;

    public RailRoute(List<RailRouteNode<TPos>> route){
        this.route = route;
    }

    @Override
    public String toString(){
        return StringUtils.join(route, " -> ");
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
