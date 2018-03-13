package com.minemaarten.signals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.parsing.NetworkParser;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

//@formatter:off
/**
 * Tests of pathfinding through a RailNetwork.
 * @author Maarten
 *
 */
public class PathfindingTests{

    @Test
    public void testBasicPath(){
        List<String> map = new ArrayList<>();
        map.add("s++d");
        TestRailNetwork network = NetworkParser.createDefaultParser().parse(map);
        RailRoute<Pos2D> route = network.pathfind();
        Assert.assertNotNull(route);
        Assert.assertEquals(0, route.route.size());
    }

    @Test
    public void testBasicNoPath(){
        List<String> map = new ArrayList<>();
        map.add("s+ +d");
        TestRailNetwork network = NetworkParser.createDefaultParser().parse(map);
        Assert.assertNull(network.pathfind());
    }

    @Test
    public void testBasicIntersectionPath(){
        List<String> map = new ArrayList<>();
        map.add("s+0+d");
        map.add("  +  ");
        map.add("  +  ");
        NetworkParser.createDefaultParser()
                     .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.EAST)
                     .parse(map)
                     .validate();
    }

    @Test
    public void testComplicatedIntersectionPath(){
        List<String> map = new ArrayList<>();
        map.add("s+0++ +    ");
        map.add("  +   + +  ");
        map.add("  ++++1+++ ");
        map.add(" d    +    ");
        map.add(" + +  +    ");
        map.add(" ++2+++    ");
        NetworkParser.createDefaultParser()
                     .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.SOUTH)
                     .addExpectedIntersection(1, EnumHeading.WEST, EnumHeading.SOUTH)
                     .addExpectedIntersection(2, EnumHeading.EAST, EnumHeading.WEST)
                     .parse(map)
                     .validate();
    }
    
    @Test
    public void testShortestRoute(){
        List<String> map = new ArrayList<>();
        map.add("s+0++ +    ");
        map.add("  +   + +  ");
        map.add(" +1+++++++ ");
        map.add(" d    +    ");
        map.add(" + +  +    ");
        map.add(" ++++++    ");
        NetworkParser.createDefaultParser()
                     .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.SOUTH)
                     .addExpectedIntersection(1, EnumHeading.NORTH, EnumHeading.WEST)
                     .parse(map)
                     .validate();
    }
}
//@formatter:on
