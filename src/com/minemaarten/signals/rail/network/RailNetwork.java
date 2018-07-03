package com.minemaarten.signals.rail.network;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;

/**
 * Entry point for dealing with rail networks. Designed to be immutable.
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailNetwork<TPos extends IPosition<TPos>> {
    private static final int MAX_RAILS_IN_FRONT_SIGNAL = 5;
    public final RailObjectHolder<TPos> railObjects;
    public final RailObjectHolder<TPos> unfilteredRailObjects; //All network objects, without filtered invalid signals.
    private Map<TPos, RailSection<TPos>> railPosToRailSections;
    private Set<RailEdge<TPos>> allEdges;
    private Set<RailSection<TPos>> allSections;
    private TObjectIntMap<TPos> railLinkPosToDelays;
    private final Map<TPos, List<TPos>> signalToPositionsInFrontCache = new HashMap<>();
    private final ImmutableMap<TPos, NetworkCache<TPos>> cache;
    private String[] stationNames;

    /**
     * Given a position of a path node, which edges can end up in this node?
     */
    private Multimap<TPos, RailEdge<TPos>> positionsToEdgesBackward;

    /**
     * given any rail pos, which edge belongs to this rail?
     * Intersections don't return an edge.
     */
    private Map<TPos, RailEdge<TPos>> railPosToRailEdges;

    public RailNetwork(Collection<NetworkObject<TPos>> allNetworkObjects){
        this.unfilteredRailObjects = new RailObjectHolder<>(allNetworkObjects);
        this.railObjects = unfilteredRailObjects.filterInvalidSignals();
        cache = allNetworkObjects.stream().collect(ImmutableMap.toImmutableMap(o -> o.pos, NetworkCache<TPos>::new));
    }

    public RailNetwork(ImmutableMap<TPos, NetworkObject<TPos>> allNetworkObjects){
        this.unfilteredRailObjects = new RailObjectHolder<>(allNetworkObjects);
        this.railObjects = unfilteredRailObjects.filterInvalidSignals();
        cache = allNetworkObjects.values().stream().collect(ImmutableMap.toImmutableMap(o -> o.pos, NetworkCache<TPos>::new));
    }

    public static <TPos extends IPosition<TPos>> RailNetwork<TPos> empty(){
        return new RailNetwork<>(ImmutableMap.<TPos, NetworkObject<TPos>> of());
    }

    /**
     * Build the network from the stored rail objects, if it wasn't loaded already
     */
    public RailNetwork<TPos> build(){
        if(railPosToRailSections == null) {
            synchronized(this) {
                if(railPosToRailSections == null) {
                    railPosToRailSections = new HashMap<>();
                    allEdges = new HashSet<>();
                    allSections = new HashSet<>();
                    railLinkPosToDelays = new TObjectIntHashMap<TPos>();
                    railPosToRailEdges = new HashMap<>();
                    positionsToEdgesBackward = ArrayListMultimap.create();

                    buildRailSections();//TODO rail section and edge building can be done in parallel? No MC dependences or interdependencies.

                    Set<RailEdge<TPos>> allEdges = buildRoughRailEdges();
                    mergeCrossingEdges(allEdges).forEach(edge -> addEdge(edge));

                    buildStationNames();
                    buildRailLinkToDelayMap();
                    onAfterBuild();
                }
            }
        }
        return this;
    }

    protected void onAfterBuild(){

    }

    private void buildStationNames(){
        Stream<String> stationNameStream = railObjects.getStations().stream().map(s -> s.stationName).filter(s -> !"".equals(s));
        stationNameStream = Streams.concat(stationNameStream, Stream.of("ITEM")).distinct().sorted();
        stationNames = stationNameStream.toArray(String[]::new);
    }

    public String[] getStationNames(){
        build();
        return stationNames;
    }

    private void buildRailSections(){
        //Wrap in HashSet because java doesn't guarantee mutability with Collectors.toSet()
        Set<NetworkRail<TPos>> toTraverse = new HashSet<>(railObjects.getRails());

        while(!toTraverse.isEmpty()) {
            Iterator<NetworkRail<TPos>> toTraverseIterator = toTraverse.iterator();
            NetworkRail<TPos> first = toTraverseIterator.next();
            toTraverseIterator.remove(); //Remove so we don't evaluate it again.

            Set<NetworkRail<TPos>> sectionSet = new HashSet<>();
            sectionSet.add(first);

            Stack<NetworkRail<TPos>> sectionToTraverse = new Stack<>();
            sectionToTraverse.push(first);

            while(!sectionToTraverse.isEmpty()) {
                NetworkRail<TPos> curRail = sectionToTraverse.pop();
                List<NetworkRail<TPos>> neighbors = curRail.getSectionNeighborRails(railObjects).collect(Collectors.toList());

                for(NetworkRail<TPos> neighbor : neighbors) {
                    EnumHeading dir = neighbor.pos.getRelativeHeading(curRail.pos);
                    if(dir == null || getSignalInDir(curRail, dir) == null) { //Only when the neighbor is not on a next section, continue
                        if(dir == null || getSignalInDir(neighbor, dir.getOpposite()) == null) {
                            if(toTraverse.remove(neighbor)) {
                                sectionToTraverse.push(neighbor);
                            }

                            sectionSet.add(neighbor);
                        }
                    }
                }
            }

            addSection(new RailSection<>(railObjects, sectionSet));
        }
    }

    private void buildRailLinkToDelayMap(){
        for(NetworkRailLink<TPos> railLink : railObjects.getRailLinks()) {
            if(railLink.holdDelay > 0) {
                for(EnumHeading heading : EnumHeading.VALUES) {
                    TPos neighbor = railLink.pos.offset(heading);
                    if(railObjects.getRail(neighbor) != null) {
                        railLinkPosToDelays.put(neighbor, railLink.holdDelay);
                    }
                }
            }
        }
    }

    public int getRailLinkDelayFor(TPos pos){
        build();
        return railLinkPosToDelays.get(pos);
    }

    private NetworkSignal<TPos> getSignalInDir(NetworkRail<TPos> rail, EnumHeading dir){
        return cache.get(rail.pos).getObjectNeighbors(this).getSignals().stream().filter(s -> s.heading == dir && s.getRailPos().equals(rail.pos)).findFirst().orElse(null);
    }

    public Collection<RailSection<TPos>> getAllSections(){
        build();
        return allSections;
    }

    public Collection<RailEdge<TPos>> getAllEdges(){
        build();
        return allEdges;
    }

    private void addSection(RailSection<TPos> section){
        allSections.add(section);
        section.getRailPositions().forEach(pos -> {
            railPosToRailSections.put(pos, section);
        });
    }

    public RailSection<TPos> findSection(TPos pos){
        build();
        return railPosToRailSections.get(pos);
    }

    /**
     * Build edges naively, by assuming that any possible pathfind neighbor of a given rail can map to any other neighbor of this rail.
     * This isn't always the case, for example with rail crossings, where only N<-->S and W<-->E are mapped. These are filtered out
     * in {@link RailNetwork#mergeCrossingEdges(Set)}. It turned out to be computationally easier to not having to deal with sides in this method.
     * @return
     */
    private Set<RailEdge<TPos>> buildRoughRailEdges(){
        //All rails need to be traversed, in every direction, because a rail may be a junction, in which case a single block is part of multiple edges.
        Set<NetworkRail<TPos>> toTraverse = new HashSet<>(railObjects.getRails());

        Set<NetworkRail<TPos>> edgeSet = new HashSet<>();
        List<NetworkRail<TPos>> edge = new ArrayList<>();
        Set<RailEdge<TPos>> allEdges = new HashSet<>();

        while(!toTraverse.isEmpty()) {
            NetworkRail<TPos> first = toTraverse.iterator().next();

            edge.clear();
            edge.add(first);
            edgeSet.clear();
            edgeSet.add(first);

            Stack<NetworkRail<TPos>> edgeToTraverse = new Stack<>();
            edgeToTraverse.push(first);

            while(!edgeToTraverse.isEmpty()) {
                NetworkRail<TPos> curEntry = edgeToTraverse.pop();
                List<NetworkRail<TPos>> neighbors = curEntry.getSectionNeighborRails(railObjects).collect(Collectors.toList());
                if(neighbors.size() < 3) { //If not on an intersection, expand further
                    toTraverse.remove(curEntry);
                    for(NetworkRail<TPos> neighbor : neighbors) {
                        if(edgeSet.add(neighbor)) {
                            edgeToTraverse.push(neighbor);

                            //Add to the current edge, make sure the list is in sequence, so that index 0 is a neighbor of index 1, which is
                            //a neighbor of index 2, etc.
                            if(edge.get(edge.size() - 1).pos.equals(curEntry.pos)) {
                                edge.add(neighbor);
                            } else if(edge.get(0).pos.equals(curEntry.pos)) {
                                edge.add(0, neighbor);
                            } else {
                                throw new IllegalStateException("Currently evaluated pos is not at the start or end of an edge!");
                            }
                        }
                    }
                } else if(edge.size() == 1) { //when evaluating starting from an intersection, we can create edges that span only 2 blocks, from one intersection to the next
                    toTraverse.remove(curEntry);
                    //When evaluating from an intersection, only look at directly neighboring intersections.
                    for(NetworkRail<TPos> neighbor : neighbors) {
                        List<NetworkRail<TPos>> neighborNeighbors = neighbor.getSectionNeighborRails(railObjects).collect(Collectors.toList());
                        if(neighborNeighbors.size() > 2) { //When the neighbor also is on an intersection, we have an edge
                            ImmutableList<NetworkRail<TPos>> e = ImmutableList.of(curEntry, neighbor);
                            RailEdge<TPos> railEdge = new RailEdge<>(railObjects, e);
                            allEdges.add(railEdge);
                        }
                    }
                }
            }

            //Only create edges of 2 or higher, as we are not interested in just intersections
            if(edge.size() > 1) {
                RailEdge<TPos> railEdge = new RailEdge<>(railObjects, ImmutableList.copyOf(edge));
                allEdges.add(railEdge);
            }
            // System.out.println(edgeSet.size());
        }
        return allEdges;
    }

    /**
     * Merge edges that aren't actually on intersections, when encountering rail junctions.
     * @param allEdges
     * @return
     */
    private Set<RailEdge<TPos>> mergeCrossingEdges(Set<RailEdge<TPos>> allEdges){
        Multimap<TPos, RailEdge<TPos>> connectedEdges = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        for(RailEdge<TPos> edge : allEdges) {
            connectedEdges.put(edge.startPos, edge);
            connectedEdges.put(edge.endPos, edge);
        }

        Set<RailEdge<TPos>> newEdges = new HashSet<>();
        Set<RailEdge<TPos>> curMergedEdges = new HashSet<>();
        while(!allEdges.isEmpty()) {
            RailEdge<TPos> first = allEdges.iterator().next();

            curMergedEdges.clear();
            curMergedEdges.add(first);

            RailEdge<TPos> combinedEdge = mergeEdgeFrom(connectedEdges, curMergedEdges, first, first, first.get(0));
            if(!first.startPos.equals(first.endPos)) {
                combinedEdge = mergeEdgeFrom(connectedEdges, curMergedEdges, combinedEdge, first, first.get(first.length - 1));
            }

            allEdges.removeAll(curMergedEdges);
            newEdges.add(combinedEdge);
        }

        return newEdges;
    }

    private RailEdge<TPos> mergeEdgeFrom(Multimap<TPos, RailEdge<TPos>> connectedEdges, Set<RailEdge<TPos>> curMergedEdges, RailEdge<TPos> combinedEdge, RailEdge<TPos> startEdge, NetworkRail<TPos> startPos){
        NetworkRail<TPos> curRail = startPos;
        RailEdge<TPos> prevEdge = startEdge;
        boolean hasCombined;
        do {
            hasCombined = false;
            //@formatter:off
            EnumSet<EnumHeading> validHeadings = curRail.getPathfindHeading(prevEdge.headingForEndpoint(curRail.pos));
            final RailEdge<TPos> fPrevEdge = prevEdge;
            final NetworkRail<TPos> fCurRail = curRail;
            List<RailEdge<TPos>> actualConnected = connectedEdges.get(curRail.pos)
                                                                 .stream()
                                                                 .filter(e -> e != fPrevEdge && 
                                                                         (validHeadings.contains(e.headingForEndpoint(fCurRail.pos)) ||
                                                                          e.headingForEndpoint(fCurRail.pos) == null))
                                                                 .collect(Collectors.toList());
            //@formatter:on
            if(actualConnected.size() == 1) { //When not actually on an intersection, in terms of pathfinding
                RailEdge<TPos> nextEdge = actualConnected.get(0);
                if(curMergedEdges.add(nextEdge)) {
                    combinedEdge = combinedEdge.combine(nextEdge, curRail.pos);
                    curRail = nextEdge.get(nextEdge.getIndex(nextEdge.other(curRail.pos)));
                    prevEdge = nextEdge;
                    hasCombined = true;
                }
            }

        } while(hasCombined);

        return combinedEdge;
    }

    private void addEdge(RailEdge<TPos> railEdge){
        if(allEdges.add(railEdge)) {
            if(railEdge.directionality.canTravelBackwards) positionsToEdgesBackward.put(railEdge.startPos, railEdge);
            if(railEdge.directionality.canTravelForwards) positionsToEdgesBackward.put(railEdge.endPos, railEdge);
            for(int i = 0; i < railEdge.length; i++) {
                railPosToRailEdges.put(railEdge.get(i).pos, railEdge);
            }
        }
    }

    /**
     * given any rail pos, which edge belongs to this rail?
     * Intersections don't return an edge.
     */
    public RailEdge<TPos> findEdge(TPos pos){
        build();
        return railPosToRailEdges.get(pos);
    }

    public Collection<RailEdge<TPos>> findConnectedEdgesBackwards(TPos intersection){
        build();
        return positionsToEdgesBackward.get(intersection);
    }

    /**
     * Pathfinds from 'from' to one of the 'destinations', in the given direction 'direction'.
     * @param from
     * @param direction
     * @param destinations
     * @return a route, or null, if no path was found
     */
    public RailRoute<TPos> pathfind(NetworkState<TPos> state, TPos from, EnumHeading direction, Set<TPos> destinations){
        return new RailPathfinder<TPos>(this, state).pathfindToDestination(from, direction, destinations);
    }

    /**
     * Gets up to 5 rails part of the same edge in front of the given signal, in order of the closest rail to the farthest.
     * @param network
     * @param signal
     * @return
     */
    public List<TPos> getPositionsInFront(NetworkSignal<TPos> signal){
        build();
        List<TPos> positions = signalToPositionsInFrontCache.get(signal.pos);
        if(positions == null) {

            RailEdge<TPos> edge = findEdge(signal.getRailPos());
            if(edge == null) {
                positions = Collections.emptyList();
            } else {
                TPos firstPosInFront = signal.getRailPos().offset(signal.heading.getOpposite());
                int index = edge.getIndex(signal.getRailPos());
                Object blockType = edge.get(index).getRailType();
                boolean countingUp = index < edge.length - 1 && firstPosInFront.equals(edge.get(index + 1));

                positions = new ArrayList<>(MAX_RAILS_IN_FRONT_SIGNAL);
                if(countingUp) {
                    int maxIndex = Math.min(index + MAX_RAILS_IN_FRONT_SIGNAL, edge.length);
                    for(int i = index; i < maxIndex; i++) {
                        NetworkRail<TPos> rail = edge.get(i);
                        if(blockType.equals(rail.getRailType())) {
                            positions.add(rail.pos);
                        } else {
                            break;
                        }
                    }
                } else {
                    int minIndex = Math.max(index - MAX_RAILS_IN_FRONT_SIGNAL + 1, 1);
                    for(int i = index; i >= minIndex; i--) {
                        NetworkRail<TPos> rail = edge.get(i);
                        if(blockType.equals(rail.getRailType())) {
                            positions.add(rail.pos);
                        } else {
                            break;
                        }
                    }
                }
            }
            signalToPositionsInFrontCache.put(signal.pos, positions);
        }
        return positions;
    }

    public Set<TPos> getStationRails(Train<TPos> train, Pattern destinationRegex){
        Set<TPos> rails = new HashSet<>();
        Set<String> validNames = new HashSet<>();
        List<NetworkStation<TPos>> stations = railObjects.getStations();
        for(NetworkStation<TPos> station : stations) {
            if(station.isTrainApplicable(train, destinationRegex)) {
                rails.addAll(station.getConnectedRailPositions(this));
                validNames.add(station.stationName);
            }
        }

        //Make sure to include stations that don't match themselves, but other stations with the same name do.
        for(NetworkStation<TPos> station : stations) {
            if(validNames.contains(station.stationName)) {
                rails.addAll(station.getConnectedRailPositions(this));
            }
        }
        return rails;
    }

    public Set<TPos> getStations(Train<TPos> train, Pattern destinationRegex){
        Set<TPos> stationPositions = new HashSet<>();
        Set<String> validNames = new HashSet<>();
        List<NetworkStation<TPos>> stations = railObjects.getStations();
        for(NetworkStation<TPos> station : stations) {
            if(station.isTrainApplicable(train, destinationRegex)) {
                stationPositions.add(station.pos);
                validNames.add(station.stationName);
            }
        }

        //Make sure to include stations that don't match themselves, but other stations with the same name do.
        for(NetworkStation<TPos> station : stations) {
            if(validNames.contains(station.stationName)) {
                stationPositions.add(station.pos);
            }
        }
        return stationPositions;
    }
}
