package com.minemaarten.signals.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkSignal;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.Train;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.TestTrain;
import com.minemaarten.signals.util.railnode.DefaultRailNode;

/**
 * Performance tests of creating different kinds of networks
 * @author Maarten
 *
 */
public class NetworkBenchmarks{
    private static final int FULL_GRID_SIZE = 100;
    private static final int LARGE_NETWORK_SIZE = 1000;
    private static final int LARGE_NETWORK_SPACING = 50;
    private static List<NetworkObject<Pos2D>> fullGrid, largeNetwork;
    private static RailNetwork<Pos2D> fullGridNetwork, largeNetworkNetwork;
    private static NetworkState<Pos2D> fullGridState, largeNetworkState;

    @BeforeClass
    public static void prepare(){
        Random rand = new Random(1);

        fullGrid = new ArrayList<>();
        for(int x = 0; x < FULL_GRID_SIZE; x++) {
            for(int y = 0; y < FULL_GRID_SIZE; y++) {
                fullGrid.add(new DefaultRailNode(new Pos2D(x, y)));
            }
        }
        fullGridNetwork = new RailNetwork<>(fullGrid);
        fullGridState = new NetworkState<>();

        List<Train<Pos2D>> trains = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            Pos2D pos = fullGrid.get(rand.nextInt(fullGrid.size())).pos;
            trains.add(new TestTrain(fullGridNetwork, fullGridState, ImmutableSet.of(pos), 'a'));
        }
        fullGridState.setTrains(trains);

        largeNetwork = new ArrayList<>();
        for(int x = 0; x < LARGE_NETWORK_SIZE; x++) {
            for(int y = 0; y < LARGE_NETWORK_SIZE; y++) {
                if(x % LARGE_NETWORK_SPACING == 0 || y % LARGE_NETWORK_SPACING == 0) {
                    largeNetwork.add(new DefaultRailNode(new Pos2D(x, y)));
                }
                if((x - 1) % LARGE_NETWORK_SPACING == 0 && (y - 1) % LARGE_NETWORK_SPACING == 0) {
                    largeNetwork.add(new NetworkSignal<>(new Pos2D(x, y), EnumHeading.NORTH, EnumSignalType.BLOCK));
                }
            }
        }
        largeNetworkNetwork = new RailNetwork<>(largeNetwork);
        largeNetworkState = new NetworkState<>();

        trains = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            Pos2D pos = largeNetwork.get(rand.nextInt(largeNetwork.size())).pos;
            trains.add(new TestTrain(largeNetworkNetwork, largeNetworkState, ImmutableSet.of(pos), 'a'));
        }
        largeNetworkState.setTrains(trains);

    }

    //500ms
    //4ms
    @Test
    public void benchmarkUpdateFullGrid(){
        for(int i = 0; i < 100; i++) {
            fullGridState.update(fullGridNetwork);
        }
    }

    //1800ms
    //2ms
    @Test
    public void benchmarkUpdateLargeNetwork(){
        for(int i = 0; i < 100; i++) {
            largeNetworkState.update(largeNetworkNetwork);
        }
    }

    //900ms
    @Test
    public void benchmarkFullGrid(){
        for(int i = 0; i < 1; i++)
            new RailNetwork<>(fullGrid);
    }

    //700ms
    @Test
    public void benchmarkLargeNetwork(){
        for(int i = 0; i < 1; i++)
            new RailNetwork<>(largeNetwork);
    }
}
