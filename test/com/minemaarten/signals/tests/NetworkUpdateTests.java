package com.minemaarten.signals.tests;

import java.util.Arrays;

import org.junit.Test;

import com.minemaarten.signals.util.parsing.NetworkParser;

//@formatter:off
/**
 * Tests of the validity of networks after they change and get partially marked dirty.
 * @author Maarten
 *
 */
public class NetworkUpdateTests{
    
    /**
     * Test whether the network gets updated properly when a rail gets added and the right position is invalided.
     */
    @Test
    public void testBasicNetworkInvalidation(){    
        NetworkParser.createDefaultParser()
                     .parse(Arrays.asList("++ ++"))
                     .markDirty(Arrays.asList("  x  "))
                     .updateAndCompare(Arrays.asList("+++++"));
    }
  
    /**
     * Assert that the network becomes inconsistent when positions don't get invalidated.
     */
    @Test
    public void testBasicNetworkInvalidationError(){    
        NetworkParser.createDefaultParser()
                     .parse(Arrays.asList("++ ++"))
                     .updateAndCompare(Arrays.asList("+++++"), Arrays.asList("  m  "));
    }
    
    /**
     * Test whether the network gets updated properly when a rail gets removed and the right position is invalided.
     */
    @Test
    public void testBasicNetworkRemoval(){    
        NetworkParser.createDefaultParser()
                     .parse(Arrays.asList("+++++"))
                     .markDirty(Arrays.asList("  x  "))
                     .updateAndCompare(Arrays.asList("++ ++"));
    }
}
//@formatter:on
