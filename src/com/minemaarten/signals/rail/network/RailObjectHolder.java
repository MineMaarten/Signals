package com.minemaarten.signals.rail.network;

import static com.minemaarten.signals.lib.StreamUtils.ofType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimaps;

/**
 * Helper class to allow querying network objects. Designed to be immutable.
 * @author Maarten
 *
 * @param <TPos>
 */
public class RailObjectHolder<TPos extends IPosition<TPos>> implements Iterable<NetworkObject<TPos>>{
    private final ImmutableMap<TPos, NetworkObject<TPos>> allNetworkObjects;
    private ImmutableListMultimap<TPos, NetworkRailLink<TPos>> destinationsToRailLinks;

    public RailObjectHolder(Collection<NetworkObject<TPos>> allNetworkObjects){
        this(allNetworkObjects.stream());
    }

    public RailObjectHolder(Stream<NetworkObject<TPos>> allNetworkObjects){
        this(allNetworkObjects.collect(ImmutableMap.toImmutableMap((NetworkObject<TPos> n) -> n.pos, Functions.identity())));
    }

    public RailObjectHolder(ImmutableMap<TPos, NetworkObject<TPos>> allNetworkObjects){
        this.allNetworkObjects = ImmutableMap.copyOf(allNetworkObjects);
    }

    public ImmutableMap<TPos, NetworkObject<TPos>> getAllNetworkObjects(){
        return allNetworkObjects;
    }

    //Filter invalid signals, signals that are placed next to intersections, or not next to rails
    public RailObjectHolder<TPos> filterInvalidSignals(){
        Set<TPos> toRemove = new HashSet<>();
        getSignals().forEach(signal -> {
            NetworkObject<TPos> railObj = get(signal.getRailPos());
            if(railObj instanceof NetworkRail) {
                NetworkRail<TPos> rail = (NetworkRail<TPos>)railObj;
                List<NetworkRail<TPos>> neighbors = rail.getSectionNeighborRails(this).collect(Collectors.toList());
                if(neighbors.size() > 2) {
                    toRemove.add(signal.pos); //Invalid: Attached to an intersection.
                } else {
                    EnumHeading signalHeading = signal.heading;
                    if(neighbors.stream().map(n -> n.pos.getRelativeHeading(rail.pos)).anyMatch(h -> h != signalHeading && h != signalHeading.getOpposite())) {
                        toRemove.add(signal.pos);//Invalid: Not on a straight.
                    }
                }
            } else {
                toRemove.add(signal.pos); //Invalid: Not attached to a rail.
            }
        });

        if(toRemove.isEmpty()) {
            return this;//Short cut
        } else {
            return new RailObjectHolder<>(allNetworkObjects.values().stream().filter(o -> !toRemove.contains(o.pos)));
        }
    }

    public RailObjectHolder<TPos> subSelectionForPos(Collection<TPos> rails){
        return subSelection(getNeighborRails(rails).collect(Collectors.toList()));
    }

    public RailObjectHolder<TPos> subSelection(Collection<NetworkRail<TPos>> rails){
        Set<NetworkObject<TPos>> selection = new HashSet<NetworkObject<TPos>>(rails);
        for(NetworkRail<TPos> rail : rails) {
            rail.getPotentialNeighborObjectLocations().stream().map(n -> get(n)).filter(n -> n != null).forEach(n -> {
                if(!(n instanceof NetworkRail)) {
                    if(!(n instanceof NetworkSignal) || ((NetworkSignal<?>)n).getRailPos().equals(rail.pos)) { //Only signals that are connected to this rail
                        selection.add(n);
                    }
                }
            });
        }
        return new RailObjectHolder<>(selection);
    }

    public RailObjectHolder<TPos> combine(RailObjectHolder<TPos> other){
        Map<TPos, NetworkObject<TPos>> map = new HashMap<>(allNetworkObjects.size() + other.allNetworkObjects.size());
        map.putAll(allNetworkObjects);
        map.putAll(other.allNetworkObjects);
        return new RailObjectHolder<>(ImmutableMap.copyOf(map));
    }

    public NetworkObject<TPos> get(TPos pos){
        return allNetworkObjects.get(pos);
    }

    private Collection<NetworkRailLink<TPos>> findRailLinksConnectingTo(TPos pos){
        if(destinationsToRailLinks == null) {
            destinationsToRailLinks = Multimaps.index(getRailLinks().iterator(), NetworkRailLink::getDestinationPos);
        }
        return destinationsToRailLinks.get(pos);
    }

    public Stream<NetworkRail<TPos>> findRailsLinkingTo(TPos pos){
        return findRailLinksConnectingTo(pos).stream().flatMap(l -> l.getNeighborRails(this)).distinct();
    }

    public <T extends NetworkObject<TPos>> Stream<T> networkObjectsOfType(Class<T> clazz){
        return ofType(clazz, allNetworkObjects.values().stream());
    }

    public Stream<NetworkRail<TPos>> getRails(){
        return ofType(NetworkRail.class, allNetworkObjects.values().stream());
    }

    public Stream<NetworkRail<TPos>> getNeighborRails(Collection<TPos> potentialNeighbors){
        return ofType(NetworkRail.class, potentialNeighbors.stream().map(n -> allNetworkObjects.get(n)));
    }

    public NetworkRail<TPos> getRail(TPos pos){
        NetworkObject<TPos> obj = get(pos);
        return obj instanceof NetworkRail ? (NetworkRail<TPos>)obj : null;
    }

    public int getNeighborRailCount(Collection<TPos> potentialNeighbors){
        int count = 0;
        for(TPos neighbor : potentialNeighbors) {
            if(allNetworkObjects.get(neighbor) instanceof NetworkRail) {
                count++;
            }
        }
        return count;
    }

    public Stream<NetworkSignal<TPos>> getSignals(){
        return ofType(NetworkSignal.class, allNetworkObjects.values().stream());
    }

    public Stream<NetworkSignal<TPos>> getNeighborSignals(Collection<TPos> potentialNeighbors){
        return ofType(NetworkSignal.class, potentialNeighbors.stream().map(n -> allNetworkObjects.get(n)));
    }

    public Stream<NetworkRailLink<TPos>> getRailLinks(){
        return ofType(NetworkRailLink.class, allNetworkObjects.values().stream());
    }

    public Stream<NetworkRailLink<TPos>> getNeighborRailLinks(Collection<TPos> potentialNeighbors){
        return ofType(NetworkRailLink.class, potentialNeighbors.stream().map(n -> allNetworkObjects.get(n)));
    }

    public Stream<NetworkStation<TPos>> getStations(){
        return ofType(NetworkStation.class, allNetworkObjects.values().stream());
    }

    @Override
    public boolean equals(Object other){
        if(other instanceof RailObjectHolder) {
            return ((RailObjectHolder<?>)other).allNetworkObjects.equals(allNetworkObjects);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(){
        return allNetworkObjects.hashCode();
    }

    @Override
    public Iterator<NetworkObject<TPos>> iterator(){
        return allNetworkObjects.values().iterator();
    }
}
