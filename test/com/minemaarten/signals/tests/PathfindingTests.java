package com.minemaarten.signals.tests;

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
        Assert.assertEquals(0, route.routeNodes.size());
        Assert.assertEquals(4, route.routeRails.size());
    }
    
    @Test
    public void testBasicNoPath(){
        List<String> map = new ArrayList<>();
        map.add("s+ +d");
        TestRailNetwork network = NetworkParser.createDefaultParser().parse(map);
        Assert.assertNull(network.pathfind());
    }
    
    @Test
    public void testDirectionalNoPath(){
        List<String> map = new ArrayList<>();
        map.add("+s+++d");
        TestRailNetwork network = NetworkParser.createDefaultParser().setPathfindDir(EnumHeading.WEST).parse(map);
        Assert.assertNull(network.pathfind());
    }
    
    @Test
    public void testBasicNonEndStartPath(){
        List<String> map = new ArrayList<>();
        map.add("+s++d");
        TestRailNetwork network = NetworkParser.createDefaultParser().parse(map);
        RailRoute<Pos2D> route = network.pathfind();
        Assert.assertNotNull(route);
        Assert.assertEquals(0, route.routeNodes.size());
        Assert.assertEquals(4, route.routeRails.size());
    }
    
    @Test
    public void testBasicNonEndStartAndEndPath(){
        List<String> map = new ArrayList<>();
        map.add("+s++d+");
        TestRailNetwork network = NetworkParser.createDefaultParser().parse(map);
        RailRoute<Pos2D> route = network.pathfind();
        Assert.assertNotNull(route);
        Assert.assertEquals(0, route.routeNodes.size());
        Assert.assertEquals(4, route.routeRails.size());
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
    public void testBasicIntersectionNonEndStartPath(){
        List<String> map = new ArrayList<>();
        map.add("+s+++d+");
        map.add("   +   ");
        map.add("   +   ");
       
        TestRailNetwork network =  NetworkParser.createDefaultParser().parse(map);
        RailRoute<Pos2D> route = network.pathfind();
        Assert.assertNotNull(route);
        Assert.assertEquals(1, route.routeNodes.size());
        Assert.assertEquals(5, route.routeRails.size());
    }
    
    @Test
    public void testShortEdgedPath(){
        List<String> map = new ArrayList<>();
        map.add("s+01+d");
        map.add("  ++  ");
        NetworkParser.createDefaultParser()
                     .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.EAST)
                     .addExpectedIntersection(1, EnumHeading.WEST, EnumHeading.EAST)
                     .parse(map)
                     .validate();
    }
    
    @Test
    public void testShortEdgedPath2(){
        List<String> map = new ArrayList<>();
        map.add("s+01+d");
        map.add("  ++  ");
        map.add("  ++  ");
        NetworkParser.createDefaultParser()
                     .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.EAST)
                     .addExpectedIntersection(1, EnumHeading.WEST, EnumHeading.EAST)
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
    
    @Test
    public void testDirectionalRoute(){
        List<String> map = new ArrayList<>();
        map.add("s+0++++    ");
        map.add("  +^ v+ +  ");
        map.add(" +++++1+++ ");
        map.add(" d  > +    ");
        map.add(" +<+ v+    ");
        map.add(" ++2+++    ");
        NetworkParser.createDefaultParser()
                     .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.EAST)
                     .addExpectedIntersection(1, EnumHeading.NORTH, EnumHeading.SOUTH)
                     .addExpectedIntersection(2, EnumHeading.EAST, EnumHeading.WEST)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that a route is using a detour when a signal on a shorter route is red.
     */
    @Test
    public void testRedSignalPenaltyRoute(){
        List<String> map = new ArrayList<>();
        map.add("s+0++++    ");
        map.add(" v+ <v+ +  ");
        map.add(" +t+++1+++ ");
        map.add(" d^   +    ");
        map.add(" +<+ v+    ");
        map.add(" ++2+++    ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("t")
                     .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.EAST)
                     .addExpectedIntersection(1, EnumHeading.NORTH, EnumHeading.SOUTH)
                     .addExpectedIntersection(2, EnumHeading.EAST, EnumHeading.WEST)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that a path can be created when two networks are bridged via a Rail Link.
     */
    @Test
    public void testBasicRailLinkPath(){
        List<String> map = new ArrayList<>();
        map.add("s+0++");
        map.add("  + f");
        map.add("     ");
        map.add("  +  ");
        map.add("t+1+d");
        NetworkParser.createDefaultParser()
                     .addRailLink('f', 't')
                     .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.EAST)
                     .addExpectedIntersection(1, EnumHeading.WEST, EnumHeading.EAST)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that a path can be created when two networks are bridged via a Rail Link.
     */
    @Test
    public void testBasicRailLinkPathIntersection(){
        List<String> map = new ArrayList<>();
        map.add(" s+0+1+");
        map.add("   + f ");
        map.add("       ");
        map.add("   +   ");
        map.add("+t+3+d ");
        NetworkParser.createDefaultParser()
                     .addRailLink('f', 't')
                     .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.EAST)
                     .addExpectedIntersection(1, EnumHeading.WEST, null)
                     .addExpectedIntersection(3, EnumHeading.WEST, EnumHeading.EAST)
                     .parse(map)
                     .validate();
    }
    
    /**
     * A Rail Link is unidirectional, when reversed it should not result in a path.
     */
    @Test
    public void testBasicRailLinkReverseNoPath(){
        List<String> map = new ArrayList<>();
        map.add("s+0+t");
        map.add("  +  ");
        map.add("     ");
        map.add("f +  ");
        map.add("++1+d");
        TestRailNetwork network = NetworkParser.createDefaultParser()
                                               .addRailLink('f', 't')
                                               .addExpectedIntersection(0, EnumHeading.WEST, EnumHeading.EAST)
                                               .addExpectedIntersection(1, EnumHeading.WEST, EnumHeading.EAST)
                                               .parse(map);
        Assert.assertNull(network.pathfind());
    }
    
    /**
     * Expect that the train is routed straight through the rail crossing, requiring a detour
     */
    @Test
    public void testRailCrossingDetourPath(){
        List<String> map = new ArrayList<>();
        map.add("      s    ");
        map.add("      +    ");
        map.add(" +++++#    ");
        map.add(" d    ++++ ");
        map.add(" +  +    + ");
        map.add(" +++1+++++ ");
        NetworkParser.createDefaultParser()
                     .addExpectedIntersection(1, EnumHeading.EAST, EnumHeading.WEST)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Expect that the train is routed straight through the rail crossing, passing it twice
     */
    @Test
    public void testRailCrossingPath(){
        List<String> map = new ArrayList<>();
        map.add("      s    ");
        map.add("      +    ");
        map.add(" +++++#+++ ");
        map.add(" d    +  + ");
        map.add(" +    +++1 ");
        map.add("         + ");
        NetworkParser.createDefaultParser()
                     .addExpectedIntersection(1, EnumHeading.WEST, EnumHeading.NORTH)
                     .parse(map)
                     .validate();
    }
}
//@formatter:on
