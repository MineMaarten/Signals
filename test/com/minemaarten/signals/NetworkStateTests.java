package com.minemaarten.signals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;
import com.minemaarten.signals.util.parsing.NetworkParser;

//@formatter:off
/**
 * Tests of the validity of the RailNetwork's created sections
 * @author Maarten
 *
 */
public class NetworkStateTests{

    //TODO Test chain signals with pathfinding
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
                     .addExpectedSignal(0, EnumHeading.WEST, EnumSignalType.BLOCK, EnumLampStatus.RED)
                     .addExpectedSignal(1, EnumHeading.EAST, EnumSignalType.BLOCK, EnumLampStatus.RED)
                     .addExpectedSignal(2, EnumHeading.SOUTH, EnumSignalType.BLOCK, EnumLampStatus.GREEN)
                     .addExpectedSignal(3, EnumHeading.WEST, EnumSignalType.BLOCK, EnumLampStatus.GREEN)
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
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.BLOCK, EnumLampStatus.GREEN)
                     .addExpectedSignal(1, EnumHeading.EAST, EnumSignalType.BLOCK, EnumLampStatus.RED) //A train is on the next section
                     .parse(map)
                     .validate();
    }
    
    @Test
    public void testBasicChainSignalling(){    
        List<String> map = new ArrayList<>();
        map.add("++++s+++++++t");
        map.add(" 0 1 2 3 4 5 ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("ts")
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.BLOCK, EnumLampStatus.GREEN)//The next section is free
                     .addExpectedSignal(1, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.RED) //A train is on the next section
                     .addExpectedSignal(2, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.GREEN) //The next signal is green
                     .addExpectedSignal(3, EnumHeading.EAST, EnumSignalType.BLOCK, EnumLampStatus.GREEN) //The next section is free
                     .addExpectedSignal(4, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.RED) //The next signal is red
                     .addExpectedSignal(5, EnumHeading.EAST, EnumSignalType.BLOCK, EnumLampStatus.RED) //A train is on the next section
                     .parse(map)
                     .validate();
    }
    
    @Test
    public void testRecursiveChain(){    
        List<String> map = new ArrayList<>();
        map.add("+++++++");
        map.add("+  0  +");
        map.add("+  1  +");
        map.add("+++++++");
        NetworkParser.createDefaultParser()
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.GREEN)
                     .addExpectedSignal(1, EnumHeading.WEST, EnumSignalType.CHAIN, EnumLampStatus.GREEN)
                     .parse(map)
                     .validate();
    }
}
//@formatter:on
