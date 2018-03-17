package com.minemaarten.signals.util.parsing;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;

import com.minemaarten.signals.lib.StreamUtils;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.NetworkUpdater;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.Train;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.TestTrain;
import com.minemaarten.signals.util.railnode.DefaultRailNode;
import com.minemaarten.signals.util.railnode.IValidatingNode;
import com.minemaarten.signals.util.railnode.RailNodeTrainProvider;

public class TestRailNetwork extends RailNetwork<Pos2D>{

    public final Pos2D start;
    public final Set<Pos2D> destinations;
    private final NetworkState<Pos2D> state;
    private final NetworkUpdater<Pos2D> networkUpdater;
    private final NetworkParser parser;

    public TestRailNetwork(NetworkParser parser, List<NetworkObject<Pos2D>> allNetworkObjects){
        super(allNetworkObjects);

        this.parser = parser;
        this.networkUpdater = new NetworkUpdater<>(parser);
        List<DefaultRailNode> startNodes = railObjects.networkObjectsOfType(DefaultRailNode.class).filter(r -> r.isStart).collect(Collectors.toList());
        if(startNodes.size() > 1) throw new IllegalStateException("Multiple start nodes defined: " + startNodes.size());
        start = startNodes.isEmpty() ? null : startNodes.get(0).pos;

        destinations = railObjects.networkObjectsOfType(DefaultRailNode.class).filter(r -> r.isDestination).map(r -> r.pos).collect(Collectors.toSet());

        Set<Train<Pos2D>> trains = railObjects.networkObjectsOfType(RailNodeTrainProvider.class).map(r -> r.provideTrain(this)).collect(Collectors.toSet());
        state = new NetworkState<Pos2D>(trains);
    }

    public RailRoute<Pos2D> pathfind(){
        return pathfind(state, start, null, destinations);
    }

    /**
     * Should supply a map with 'x' for spots that should be marked dirty, and ' ' otherwise
     * @param map
     * @return
     */
    public TestRailNetwork markDirty(List<String> map){
        if(map.size() != parser.getMapHeight()) throw new IllegalArgumentException("Map heights are inconsistent, expected " + parser.getMapHeight() + ", got " + map.size());
        if(map.get(0).length() != parser.getMapWidth()) throw new IllegalArgumentException("Map width are inconsistent, expected " + parser.getMapWidth() + ", got " + map.get(0));
        for(int y = 0; y < map.size(); y++) {
            String yLine = map.get(y);
            for(int x = 0; x < yLine.length(); x++) {
                char c = yLine.charAt(x);
                if(c == 'x') {
                    networkUpdater.markDirty(new Pos2D(x, y));
                } else if(c != ' ') {
                    throw new IllegalArgumentException("Invalid char: '" + c + "'!");
                }
            }
        }
        return this;
    }

    public void updateAndCompare(List<String> newMap){
        updateAndCompare(newMap, null);
    }

    public void updateAndCompare(List<String> newMap, List<String> expectedDiffs){
        TestRailNetwork expected = parser.parse(newMap);
        RailNetwork<Pos2D> actual = networkUpdater.applyUpdates(this, networkUpdater.getNetworkUpdates(this));

        Collection<NetworkObject<Pos2D>> expectedObjects = expected.railObjects.getAllNetworkObjects().values();
        Collection<NetworkObject<Pos2D>> actualObjects = actual.railObjects.getAllNetworkObjects().values();

        List<NetworkObject<Pos2D>> missingObjs = expectedObjects.stream().filter(o -> !actualObjects.contains(o)).collect(Collectors.toList());
        List<NetworkObject<Pos2D>> extraObjs = actualObjects.stream().filter(o -> !expectedObjects.contains(o)).collect(Collectors.toList());

        if(expectedDiffs != null) {
            List<NetworkObject<Pos2D>> unexpectedMissing = missingObjs.stream().filter(m -> expectedDiffs.get(m.pos.y).charAt(m.pos.x) != 'm').collect(Collectors.toList());
            List<NetworkObject<Pos2D>> unexpectedExtra = extraObjs.stream().filter(m -> expectedDiffs.get(m.pos.y).charAt(m.pos.x) != 'e').collect(Collectors.toList());
            Assert.assertTrue("Unexpected missing: " + unexpectedMissing + ", unexpected extra: " + unexpectedExtra, unexpectedMissing.isEmpty() && unexpectedExtra.isEmpty());
        } else {
            Assert.assertTrue("Missing: " + missingObjs + ", Extra: " + extraObjs, missingObjs.isEmpty() && extraObjs.isEmpty());
        }
    }

    public void validate(){
        if(start != null) {
            state.updateSignalStatusses(this);
            TestTrain train = (TestTrain)state.getTrainAtPositions(Stream.of(start));
            if(train != null) train.setPath(this, pathfind());
        }
        state.updateSignalStatusses(this);
        StreamUtils.ofInterface(IValidatingNode.class, railObjects).forEach(r -> r.validate(this, state));
    }
}
