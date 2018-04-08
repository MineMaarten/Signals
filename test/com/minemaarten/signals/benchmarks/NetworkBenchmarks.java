package com.minemaarten.signals.benchmarks;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.railnode.DefaultRailNode;

/**
 * Performance tests of creating different kinds of networks
 * TODO benchmark network state updating
 * @author Maarten
 *
 */
public class NetworkBenchmarks{
    private static final int FULL_GRID_SIZE = 100;
    private static final int LARGE_NETWORK_SIZE = 1000;
    private static final int LARGE_NETWORK_SPACING = 50;
    private static List<NetworkObject<Pos2D>> fullGrid, largeNetwork;

    @BeforeClass
    public static void prepare(){
        fullGrid = new ArrayList<>();
        for(int x = 0; x < FULL_GRID_SIZE; x++) {
            for(int y = 0; y < FULL_GRID_SIZE; y++) {
                fullGrid.add(new DefaultRailNode(new Pos2D(x, y)));
            }
        }

        largeNetwork = new ArrayList<>();
        for(int x = 0; x < LARGE_NETWORK_SIZE; x++) {
            for(int y = 0; y < LARGE_NETWORK_SIZE; y++) {
                if(x % LARGE_NETWORK_SPACING == 0 || y % LARGE_NETWORK_SPACING == 0) {
                    largeNetwork.add(new DefaultRailNode(new Pos2D(x, y)));
                }
                /*if((x - 1) % LARGE_NETWORK_SPACING == 0 && (y - 1) % LARGE_NETWORK_SPACING == 0) {
                    largeNetwork.add(new NetworkSignal<>(new Pos2D(x, y), EnumHeading.NORTH, EnumSignalType.BLOCK));
                }*/
            }
        }
    }

    @Test
    public void benchmarkFullGrid(){
        for(int i = 0; i < 1; i++)
            new RailNetwork<>(fullGrid);
    }

    @Test
    public void benchmarkLargeNetwork(){
        for(int i = 0; i < 1; i++)
            new RailNetwork<>(largeNetwork);
    }
}
