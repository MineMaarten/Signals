package com.minemaarten.signals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.util.parsing.NetworkParser;

//@formatter:off
/**
 * Tests of the validity of the RailNetwork's created sections
 * @author Maarten
 *
 */
public class NetworkStateTests{

    /**
     * Test whether the sections are properly grouped
     */
    @Test
    public void testBasicSignalStatusses(){    
        List<String> map = new ArrayList<>();
        map.add("   +       ");
        map.add("+++++ +    ");
        map.add("  +   +0+  ");
        map.add(" +++++++++ ");
        map.add(" +  12t    ");
        map.add(" + +3 +    ");
        map.add(" ++++++    ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("t")
                     .addExpectedSignal(0, EnumHeading.WEST, EnumLampStatus.RED)
                     .addExpectedSignal(1, EnumHeading.EAST, EnumLampStatus.RED)
                     .addExpectedSignal(2, EnumHeading.SOUTH, EnumLampStatus.GREEN)
                     .addExpectedSignal(3, EnumHeading.WEST, EnumLampStatus.GREEN)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that a train spanning multiple sections is still allowed through by the signal
     */
    @Test
    public void testMultipartTrain(){    
        List<String> map = new ArrayList<>();
        map.add("++ttt++");
        map.add("+  0  +");
        map.add("+     +");
        map.add("+++++++");
        map.add("   1   ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("t")
                     .addExpectedSignal(0, EnumHeading.EAST, EnumLampStatus.GREEN)
                     .addExpectedSignal(1, EnumHeading.EAST, EnumLampStatus.RED) //A train is on the next section
                     .parse(map)
                     .validate();
    }
}
//@formatter:on
