package com.minemaarten.signals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.TestTrain;
import com.minemaarten.signals.util.parsing.NetworkParser;
import com.minemaarten.signals.util.parsing.TestRailNetwork;
import com.minemaarten.signals.util.railnode.RailNodeTrainProvider;

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
    
    /**
     * Assert that Chain Signals turn yellow if the following signals don't agree
     */
    @Test
    public void testChainUnresolved(){    
        List<String> map = new ArrayList<>();
        map.add("    t  ");
        map.add("    +^ ");
        map.add("+++++++");
        map.add(" 0   > ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("t")
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.YELLOW)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that Chain Signals turn red if the following signals are all red
     */
    @Test
    public void testChainRed(){    
        List<String> map = new ArrayList<>();
        map.add("    t  ");
        map.add("    +^ ");
        map.add("++++++a");
        map.add(" 0   > ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("ta")
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.RED)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that Chain Signals turn green if the following signals are all green
     */
    @Test
    public void testChainGreen(){    
        List<String> map = new ArrayList<>();
        map.add("    +  ");
        map.add("    +^ ");
        map.add("+++++++");
        map.add(" 0   > ");
        NetworkParser.createDefaultParser()
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.GREEN)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that Chain Signals can turn green for a given cart, as long as the cart routed is routed through a green signal.
     */
    @Test
    public void testBasicRouteDependentChainGreen(){    
        List<String> map = new ArrayList<>();
        map.add("    t  ");
        map.add("    +^ ");
        map.add("s+++++d");
        map.add("0    > ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("t")
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.GREEN)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that Chain Signals can turn green for a given cart, as long as the cart routed (one block away from the signal) is routed through a green signal.
     * 
     */
    @Test
    public void testBasicRouteDependentChainInFrontGreen(){    
        List<String> map = new ArrayList<>();
        map.add("    t  ");
        map.add("    +^ ");
        map.add("s+++++d");
        map.add(" 0   > ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("t")
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.GREEN)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that Chain Signals turn red for a given cart, when the cart is routed through a red signal.
     */
    @Test
    public void testBasicRouteDependentChainRed(){    
        List<String> map = new ArrayList<>();
        map.add("    +   ");
        map.add("    +^  ");
        map.add("s+++++td");
        map.add("0    >  ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("t")
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.RED)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that Chain Signals can turn green for a given cart, as long as the cart routed is routed through a green signal.
     */
    @Test
    public void testRouteDependentChainGreen(){    
        List<String> map = new ArrayList<>();
        map.add("    t  +++++++b");
        map.add("    +^ +2   +> ");
        map.add("s++++++++a v+  ");
        map.add("0    1  >   d  ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("abt")
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.GREEN)
                     .addExpectedSignal(1, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.YELLOW)
                     .addExpectedSignal(2, EnumHeading.NORTH, EnumSignalType.CHAIN, EnumLampStatus.YELLOW)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert that Chain Signals turn red for a given cart, when the cart is routed through a red signal.
     */
    @Test
    public void testRouteDependentChainRed(){    
        List<String> map = new ArrayList<>();
        map.add("    +  ++++++++");
        map.add("    +^ +2   t> ");
        map.add("s+++++++++ v+  ");
        map.add("0    1  >   d  ");
        NetworkParser.createDefaultParser()
                     .addTrainGroups("t")
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.RED)
                     .addExpectedSignal(1, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.YELLOW)
                     .addExpectedSignal(2, EnumHeading.NORTH, EnumSignalType.CHAIN, EnumLampStatus.RED)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert signals turn yellow when the next section is claimed by a train
     */
    @Test
    public void testSectionClaiming(){    
        List<String> map = new ArrayList<>();
        map.add("++++c++");
        map.add(" 0     ");
        NetworkParser.createDefaultParser()
                     .addObjCreator('c', pos -> {
                         return new RailNodeTrainProvider(pos, 'c'){
                             @Override
                            public TestTrain provideTrain(TestRailNetwork network){
                                 TestTrain train = super.provideTrain(network);
                                 train.setPosition(new Pos2D(-1, -1));//Move the train off the map
                                 train.setClaimingSection(network.findSection(pos)); //Claim the section the train was created on
                                 return train;
                             }
                         };
                     })
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.BLOCK, EnumLampStatus.YELLOW)
                     .parse(map)
                     .validate();
    }
    
    /**
     * Assert chain signals turn yellow when the 2nd section is claimed by a train
     */
    @Test
    public void testSectionClaimingChained(){    
        List<String> map = new ArrayList<>();
        map.add("    +   ");
        map.add("    +^  ");
        map.add("+s++++cd");
        map.add(" 0   1  ");
        NetworkParser.createDefaultParser()
                     .addObjCreator('c', pos -> {
                         return new RailNodeTrainProvider(pos, 'c'){
                             @Override
                            public TestTrain provideTrain(TestRailNetwork network){
                                 TestTrain train = super.provideTrain(network);
                                 train.setPosition(new Pos2D(-1, -1));//Move the train off the map
                                 train.setClaimingSection(network.findSection(pos)); //Claim the section the train was created on
                                 return train;
                             }
                         };
                     })
                     .addExpectedSignal(0, EnumHeading.EAST, EnumSignalType.CHAIN, EnumLampStatus.YELLOW)
                     .addExpectedSignal(1, EnumHeading.EAST, EnumSignalType.BLOCK, EnumLampStatus.YELLOW)
                     .parse(map)
                     .validate();
    }
}
//@formatter:on
