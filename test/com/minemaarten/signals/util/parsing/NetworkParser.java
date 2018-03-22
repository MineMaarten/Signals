package com.minemaarten.signals.util.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.Validate;
import org.junit.Assert;

import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.INetworkObjectProvider;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.NetworkSignal;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.railnode.DefaultRailNode;
import com.minemaarten.signals.util.railnode.IPreNetworkParseListener;
import com.minemaarten.signals.util.railnode.RailNodeCrossing;
import com.minemaarten.signals.util.railnode.RailNodeExpectedEdge;
import com.minemaarten.signals.util.railnode.RailNodeExpectedIntersection;
import com.minemaarten.signals.util.railnode.RailNodeExpectedSection;
import com.minemaarten.signals.util.railnode.RailNodeRailLinkDestination;
import com.minemaarten.signals.util.railnode.RailNodeTrainProvider;
import com.minemaarten.signals.util.railnode.TestRailLink;
import com.minemaarten.signals.util.railnode.TestRemovalNode;
import com.minemaarten.signals.util.railnode.ValidatingRailNode;
import com.minemaarten.signals.util.railnode.ValidatingSignal;

public class NetworkParser implements INetworkObjectProvider<Pos2D>{

    private List<String> map;
    private EnumHeading pathfindDir;

    public static NetworkParser createDefaultParser(){
        NetworkParser parser = new NetworkParser();
        parser.objCreators.put('+', pos -> new DefaultRailNode(pos));
        parser.objCreators.put('#', pos -> new RailNodeCrossing(pos));
        parser.objCreators.put('d', pos -> new DefaultRailNode(pos).setDestination());
        parser.objCreators.put('s', pos -> new RailNodeTrainProvider(pos, 's').setStart());

        parser.objCreators.put('^', pos -> new NetworkSignal<>(pos, EnumHeading.NORTH, EnumSignalType.BLOCK));
        parser.objCreators.put('>', pos -> new NetworkSignal<>(pos, EnumHeading.EAST, EnumSignalType.BLOCK));
        parser.objCreators.put('v', pos -> new NetworkSignal<>(pos, EnumHeading.SOUTH, EnumSignalType.BLOCK));
        parser.objCreators.put('<', pos -> new NetworkSignal<>(pos, EnumHeading.WEST, EnumSignalType.BLOCK));
        return parser;
    }

    public final Map<Character, Function<Pos2D, NetworkObject<Pos2D>>> objCreators = new HashMap<>();

    public NetworkParser addExpectedIntersection(int index, EnumHeading expectedDirIn, EnumHeading expectedDirOut){
        objCreators.put(Character.forDigit(index, 10), pos -> new RailNodeExpectedIntersection(pos, index, expectedDirIn, expectedDirOut));
        return this;
    }

    public NetworkParser addExpectedSignal(int index, EnumHeading signalHeading, EnumSignalType signalType, EnumLampStatus expectedStatus){
        return addObjCreator(Character.forDigit(index, 10), pos -> new ValidatingSignal(pos, signalHeading, signalType){
            @Override
            public void validate(TestRailNetwork network, NetworkState<Pos2D> state){
                EnumLampStatus signalStatus = state.getLampStatus(pos);
                Assert.assertEquals("Unexpected signal status for signal " + index, expectedStatus, signalStatus);
            }
        });
    }

    public NetworkParser addTrainGroups(String groups){
        for(char c : groups.toCharArray()) {
            objCreators.put(c, pos -> new RailNodeTrainProvider(pos, c));
        }
        return this;
    }

    public NetworkParser addEdgeGroups(String groups){
        for(char c : groups.toCharArray()) {
            objCreators.put(c, pos -> new RailNodeExpectedEdge(pos, c));
        }
        return this;
    }

    public NetworkParser addSectionGroups(String groups){
        for(char c : groups.toCharArray()) {
            objCreators.put(c, pos -> new RailNodeExpectedSection(pos, c));
        }
        return this;
    }

    public NetworkParser addRailLink(char link, char destinationRail){
        objCreators.put(link, pos -> new TestRailLink(pos, destinationRail));
        objCreators.put(destinationRail, pos -> new RailNodeRailLinkDestination(pos, destinationRail));
        return this;
    }

    public NetworkParser addObjCreator(char c, Function<Pos2D, NetworkObject<Pos2D>> creator){
        objCreators.put(c, creator);
        return this;
    }

    public NetworkParser addValidator(char c, BiConsumer<NetworkRail<Pos2D>, TestRailNetwork> validator){
        return addObjCreator(c, pos -> new ValidatingRailNode(pos){
            @Override
            public void validate(TestRailNetwork network, NetworkState<Pos2D> state){
                validator.accept(this, network);
            }
        });
    }

    public NetworkParser setPathfindDir(EnumHeading pathfindDir){
        this.pathfindDir = pathfindDir;
        return this;
    }

    public TestRailNetwork parse(List<String> map){
        setMap(map);
        int xSize = map.get(0).length();

        List<NetworkObject<Pos2D>> networkObjects = new ArrayList<>();

        for(int y = 0; y < map.size(); y++) {
            for(int x = 0; x < xSize; x++) {
                Pos2D pos = new Pos2D(x, y);
                NetworkObject<Pos2D> networkObject = provide(pos);
                if(networkObject != null) {
                    networkObjects.add(networkObject);
                }
            }
        }

        networkObjects.stream().filter(o -> o instanceof IPreNetworkParseListener).forEach(o -> ((IPreNetworkParseListener)o).onPreNetworkParsing(networkObjects));

        return new TestRailNetwork(this, networkObjects, pathfindDir);
    }

    private void setMap(List<String> map){
        Validate.noNullElements(map);
        if(map.isEmpty()) throw new IllegalArgumentException("Empty network is not allowed!");
        int xSize = map.get(0).length();
        for(int y = 0; y < map.size(); y++) {
            String yLine = map.get(y);
            if(yLine.length() != xSize) throw new IllegalArgumentException("Inconsistent map lengths! Violating row: " + y + ", expecting " + xSize + ", got " + yLine.length());
        }
        this.map = map;
    }

    public int getMapWidth(){
        return map.get(0).length();
    }

    public int getMapHeight(){
        return map.size();
    }

    @Override
    public NetworkObject<Pos2D> provide(Pos2D pos){
        if(pos.y >= getMapHeight() || pos.y < 0 || pos.x >= getMapWidth() || pos.x < 0) return null;

        char c = map.get(pos.y).charAt(pos.x);
        if(c != ' ') {
            Function<Pos2D, NetworkObject<Pos2D>> objCreator = objCreators.get(c);
            if(objCreator != null) {
                return objCreator.apply(pos);
            } else {
                throw new IllegalArgumentException("Unknown character at " + pos + ": '" + c + "'!");
            }
        } else {
            return null;
        }
    }

    @Override
    public NetworkObject<Pos2D> provideRemovalMarker(Pos2D pos){
        return new TestRemovalNode(pos);
    }
}
