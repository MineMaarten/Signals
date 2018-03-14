package com.minemaarten.signals.util.railnode;

import org.junit.Assert;

import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.RailRoute.RailRouteNode;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

public class RailNodeExpectedIntersection extends DefaultRailNode implements IValidatingNode{

    private final int index;
    private final EnumHeading dirIn, dirOut;

    public RailNodeExpectedIntersection(Pos2D pos, int index, EnumHeading dirIn, EnumHeading dirOut){
        super(pos);
        this.index = index;
        this.dirIn = dirIn;
        this.dirOut = dirOut;
    }

    @Override
    public void validate(TestRailNetwork network, NetworkState<Pos2D> state){
        RailRoute<Pos2D> route = network.pathfind();
        Assert.assertNotNull(route);
        RailRouteNode<Pos2D> node = route.route.get(index);
        Assert.assertEquals("Intersection positions do not match at index " + index + ".", pos, node.pos);
        Assert.assertEquals("Input dir does not match at index " + index + ".", dirIn, node.dirIn);
        Assert.assertEquals("Output dir does not match at index " + index + ".", dirOut, node.dirOut);
    }
}
