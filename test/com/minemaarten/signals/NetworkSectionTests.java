package com.minemaarten.signals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.minemaarten.signals.util.parsing.NetworkParser;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

//@formatter:off
/**
 * Tests of the validity of the RailNetwork's created sections
 * @author Maarten
 *
 */
public class NetworkSectionTests{

    /**
     * Test whether the sections are properly grouped
     */
    @Test
    public void testBasicSectionGrouping(){    
        List<String> map = new ArrayList<>();
        map.add("   <       ");
        map.add("11122 3    ");
        map.add("  1   3<4  ");
        map.add(" 111133444 ");
        map.add(" 1  >v3    ");
        map.add(" 1 1< 5    ");
        map.add(" 111555    ");
        NetworkParser.createDefaultParser()
                     .addSectionGroups("012345")
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that signals placed next to intersections, or not next to rails, get removed from the network.
     */
    @Test
    public void testInvalidSignalErasure(){    
        List<String> map = new ArrayList<>();
        map.add("  <        ");
        map.add("+++++ + >  ");
        map.add("  +   + +  ");
        map.add(" +++++++++ ");
        map.add(" +> < + >  ");
        map.add(" + +  +    ");
        map.add(" ++++++    ");
        TestRailNetwork network = NetworkParser.createDefaultParser().parse(map);
        Assert.assertEquals(0, network.railObjects.getSignals().count());
    }
    
    //Test rail link
}
//@formatter:on
