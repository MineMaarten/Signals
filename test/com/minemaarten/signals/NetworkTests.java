package com.minemaarten.signals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.minemaarten.signals.util.parsing.NetworkParser;

//@formatter:off
/**
 * Tests of the validity of the RailNetwork's created edges, sections, etc.
 * @author Maarten
 *
 */
public class NetworkTests{

    /**
     * Test whether the middle parts of edges are properly grouped
     */
    @Test
    public void testBasicEdges(){    
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
    
    //Test unidirectional
}
//@formatter:on
