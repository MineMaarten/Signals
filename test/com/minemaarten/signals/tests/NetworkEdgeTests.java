package com.minemaarten.signals.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.minemaarten.signals.util.parsing.NetworkParser;

//@formatter:off
/**
 * Tests of the validity of the RailNetwork's created edges.
 * @author Maarten
 *
 */
public class NetworkEdgeTests{
    
    /**
     * Test whether the middle parts of edges are properly grouped
     */
    @Test
    public void testBasicEdgeGrouping(){    
        List<String> map = new ArrayList<>();
        map.add("00+11 7    ");
        map.add("  2   7 a  ");
        map.add(" 3+666+8+9 ");
        map.add(" 3    5    ");
        map.add(" 3 +  5 bbb");
        map.add(" 33+555 b  ");
        NetworkParser.createDefaultParser()
                     .addEdgeGroups("0123456789ab")
                     .parse(map)
                     .validate();
    }
    
    /**
     * Test whether junctions properly separate edges
     */
    @Test
    public void testBasicJunctionEdgeGrouping(){    
        List<String> map = new ArrayList<>();
        map.add("  0 ");
        map.add(" 1#1");
        map.add("  0 ");
        NetworkParser.createDefaultParser()
                     .addEdgeGroups("01")
                     .parse(map)
                     .validate();
    }
    
    @Test
    public void testEightFigureEdgeGrouping(){    
        List<String> map = new ArrayList<>();
        map.add("  1   ");
        map.add("22+000");
        map.add("  0  0");
        map.add("  0000");
        NetworkParser.createDefaultParser()
                     .addEdgeGroups("012")
                     .parse(map)
                     .validate();
    }
    
    /**
     * Test whether edges properly 'attach' to intersections
     */
    @Test
    public void testIntersectionNodes(){
        List<String> map = new ArrayList<>();
        map.add("++3++ +    ");
        map.add("  +   + +  ");
        map.add(" +3+++4+3+ ");
        map.add(" +    +    ");
        map.add(" + +  +    ");
        map.add(" ++3+++    ");
        NetworkParser.createDefaultParser()
                     .addValidator('3', (rail, network) -> {
                         Assert.assertEquals("Unexpected intersection result at " + rail.pos + ".", 3, network.findConnectedEdgesBackwards(rail.pos).size());
                     })
                     .addValidator('4', (rail, network) -> {
                         Assert.assertEquals("Unexpected intersection result at " + rail.pos + ".", 4, network.findConnectedEdgesBackwards(rail.pos).size());
                     })
                     .parse(map)
                     .validate();
    }
    
    /**
     * Test if Signals properly make edges unidirectional
     */
    @Test
    public void testEdgeDirectionality(){    
        List<String> map = new ArrayList<>();
        map.add("   <       ");
        map.add("bb+uu b    ");
        map.add("  b   b<b  ");
        map.add(" b+uuu+u+b ");
        map.add(" b  >vu    ");
        map.add(" b b< u    ");
        map.add(" bb+uuu    ");
        NetworkParser.createDefaultParser()
                     .addValidator('u', (rail, network) -> {
                       Assert.assertTrue("Expected unidirectional edge at " + rail.pos + ".", network.findEdge(rail.pos).unidirectional);  
                     })
                     .addValidator('b', (rail, network) -> {
                       Assert.assertFalse("Expected bidirectional edge at " + rail.pos + ".", network.findEdge(rail.pos).unidirectional);  
                     })
                     .parse(map)
                     .validate();
    }
  
    /**
     * Assert that a path can be created when two networks are bridged via a Rail Link.
     */
    @Test
    public void testBasicRailLinkEdges(){
        List<String> map = new ArrayList<>();
        map.add("00+22");
        map.add("  1 f");
        map.add("     ");
        map.add("  3  ");
        map.add("t2+44");
        NetworkParser.createDefaultParser()
                    .addRailLink('f', 't')
                    .addEdgeGroups("0123456789ab")
                    .parse(map)
                    .validate();
    }
}
//@formatter:on
