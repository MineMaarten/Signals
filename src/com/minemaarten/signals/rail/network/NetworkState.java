package com.minemaarten.signals.rail.network;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;

/**
 * Contains the mutable state of a rail network, like the trains (positions and routes), the signal statusses
 * @author Maarten
 *
 */
public class NetworkState<TPos extends IPosition<TPos>> {
    private TIntObjectMap<Train<TPos>> trains = new TIntObjectHashMap<>();
    private Map<TPos, EnumLampStatus> signalToLampStatusses = Collections.emptyMap();

    public void setTrains(Collection<? extends Train<TPos>> trains){
        this.trains = new TIntObjectHashMap<>(trains.size());
        for(Train<TPos> t : trains) {
            this.trains.put(t.id, t);
        }
    }

    public TIntObjectMap<Train<TPos>> getTrains(){
        return trains;
    }

    public Train<TPos> getTrain(int id){
        return trains.get(id);
    }

    public void removeTrain(Train<TPos> train){
        trains.remove(train.id);
    }

    public void updateSignalStatusses(RailNetwork<TPos> network){
        List<NetworkSignal<TPos>> allSignals = network.railObjects.getSignals().collect(Collectors.toList());
        signalToLampStatusses = new HashMap<>();

        //First evaluate the block signal statusses
        for(NetworkSignal<TPos> signal : allSignals) {
            if(signal.type == EnumSignalType.BLOCK) {
                EnumLampStatus signalStatus = getBlockSignalStatus(network, signal);
                signalToLampStatusses.put(signal.pos, signalStatus);
            }
        }

        //Then evaluate the chain signals
        //@formatter:off
        Set<NetworkSignal<TPos>> toEvaluate = allSignals.stream().filter(s -> s.type == EnumSignalType.CHAIN).collect(Collectors.toSet());
        while(!toEvaluate.isEmpty()){
            boolean hasEvaluated = false; //Flag to make sure we do evaluate something every cycle.
            Iterator<NetworkSignal<TPos>> iterator = toEvaluate.iterator();
            while(iterator.hasNext()){
                NetworkSignal<TPos> chainSignal = iterator.next();
                EnumLampStatus signalStatus = getChainSignalStatus(network, toEvaluate, chainSignal);
                if(signalStatus != EnumLampStatus.YELLOW_BLINKING){ //If the signal status could be evaluated
                    signalToLampStatusses.put(chainSignal.pos, signalStatus);
                    iterator.remove();
                    hasEvaluated = true;
                }
            }
            
            //If we couldn't evaluate any signals, we are probably recursively looking, break this by allowing a signal to turn green.
            if(!hasEvaluated){
                iterator = toEvaluate.iterator();
                NetworkSignal<TPos> chainSignal = iterator.next();
                
                iterator.remove();
                signalToLampStatusses.put(chainSignal.pos, EnumLampStatus.GREEN);
            }
        }
        //@formatter:on
    }

    private EnumLampStatus getChainSignalStatus(RailNetwork<TPos> network, Set<NetworkSignal<TPos>> toEvaluate, NetworkSignal<TPos> chainSignal){
        EnumLampStatus blockSignalStatus = getBlockSignalStatus(network, chainSignal);
        if(blockSignalStatus == EnumLampStatus.RED || blockSignalStatus == EnumLampStatus.YELLOW) { //It is not going to get any greener if there's a train in the way, or the next section was claimed
            return blockSignalStatus;
        } else {
            RailSection<TPos> nextRailSection = chainSignal.getNextRailSection(network);
            if(nextRailSection == null) return EnumLampStatus.GREEN; //No next section is OK
            Set<EnumLampStatus> nextSignalStatusses = nextRailSection.getSignals().map(s -> getLampStatus(s.pos)).collect(Collectors.toSet());

            //When we can evaluate this chain signal
            if(!nextSignalStatusses.contains(EnumLampStatus.YELLOW_BLINKING)) {
                if(nextSignalStatusses.isEmpty()) {
                    return EnumLampStatus.GREEN; //No signals, is OK
                } else if(nextSignalStatusses.size() > 1 || nextSignalStatusses.iterator().next() == EnumLampStatus.YELLOW) {//Multiple different statusses -> dependent on the routing
                    Train<TPos> routedTrain = getTrainAtSignal(network, chainSignal);
                    if(routedTrain != null && routedTrain.getCurRoute() != null) {
                        return evaluateCurRoutedTrain(network, routedTrain, chainSignal, new HashSet<>());
                    } else {
                        return EnumLampStatus.YELLOW; //If we are not routing a train, the status of this signal is not certain
                    }
                } else {
                    return nextSignalStatusses.iterator().next(); //Copy the status of the only other status.
                }
            } else {
                return EnumLampStatus.YELLOW_BLINKING;
            }
        }
    }

    private EnumLampStatus evaluateCurRoutedTrain(RailNetwork<TPos> network, Train<TPos> train, NetworkSignal<TPos> curSignal, Set<NetworkSignal<TPos>> traversed){
        RailRoute<TPos> route = train.getCurRoute();

        RailSection<TPos> nextRailSection = curSignal.getNextRailSection(network);
        if(nextRailSection == null) return EnumLampStatus.GREEN; //No next section is OK
        Train<TPos> trainClaimingSection = getClaimingTrain(nextRailSection);
        if(trainClaimingSection != null && !trainClaimingSection.equals(train)) {
            return EnumLampStatus.YELLOW; //Claimed by another train.
        } else {
            Stream<NetworkSignal<TPos>> nextSignals = nextRailSection.getSignals();

            //The signals that crosses the route
            NetworkSignal<TPos> signalInRoute = nextSignals.filter(s -> route.routeEdges.stream().anyMatch(e -> e.contains(s.pos))).findFirst().orElse(null);
            if(signalInRoute != null) {
                EnumLampStatus nextSignalStatus = getLampStatus(signalInRoute.pos);
                if(nextSignalStatus == EnumLampStatus.YELLOW) {
                    if(traversed.add(signalInRoute)) {
                        return evaluateCurRoutedTrain(network, train, signalInRoute, traversed);
                    } else {
                        return EnumLampStatus.YELLOW_BLINKING; //Infinite loop detected as a result of a recursive call.
                    }
                } else {
                    return nextSignalStatus; //copy whatever the next signal says (even YELLOW_BLINKING)
                }
            } else {
                return EnumLampStatus.GREEN;
            }
        }
    }

    private EnumLampStatus getBlockSignalStatus(RailNetwork<TPos> network, NetworkSignal<TPos> signal){
        RailSection<TPos> nextSection = signal.getNextRailSection(network);
        if(nextSection != null) {
            Train<TPos> trainOnSection = nextSection.getTrain(trains.valueCollection());

            //When there's a train on the next section, and it is not a train that's exiting this signal
            if(trainOnSection != null && !trainOnSection.getPositions().contains(signal.getRailPos())) {
                return EnumLampStatus.RED;
            } else {
                Train<TPos> trainClaimingSection = getClaimingTrain(nextSection);
                if(trainClaimingSection != null && !trainClaimingSection.equals(getTrainAtSignal(network, signal))) {
                    return EnumLampStatus.YELLOW; //Claimed by another train.
                } else {
                    return EnumLampStatus.GREEN;
                }
            }
        } else {
            return EnumLampStatus.GREEN;
        }
    }

    public Train<TPos> getTrainAtPositions(Stream<TPos> positions){
        return positions.flatMap(pos -> trains.valueCollection().stream().filter(t -> t.getPositions().contains(pos))).findFirst().orElse(null);
    }

    public Train<TPos> getTrainAtSignal(RailNetwork<TPos> network, NetworkSignal<TPos> signal){
        return getTrainAtPositions(network.getPositionsInFront(signal));
    }

    public EnumLampStatus getLampStatus(TPos signalPos){
        return signalToLampStatusses.getOrDefault(signalPos, EnumLampStatus.YELLOW_BLINKING);
    }

    public Train<TPos> getClaimingTrain(RailSection<TPos> section){
        return trains.valueCollection().stream().filter(t -> t.getClaimedSections().contains(section)).findFirst().orElse(null);
    }

    public void pathfindTrains(RailNetwork<TPos> network){
        List<NetworkSignal<TPos>> allSignals = network.railObjects.getSignals().collect(Collectors.toList());
        for(NetworkSignal<TPos> signal : allSignals) {
            if(signal.type == EnumSignalType.CHAIN || getLampStatus(signal.pos) == EnumLampStatus.GREEN) {
                pathfindTrains(network, signal); //Only check signals that signal green, or are route dependent
            }
        }
    }

    private void pathfindTrains(RailNetwork<TPos> network, NetworkSignal<TPos> signal){
        Train<TPos> trainAtSignal = getTrainAtSignal(network, signal);
        if(trainAtSignal != null) {
            RailRoute<TPos> route = trainAtSignal.pathfind(signal.getRailPos(), signal.heading);
            if(trainAtSignal.tryUpdatePath(network, this, route) && signal.type == EnumSignalType.CHAIN) {
                EnumLampStatus status = getChainSignalStatus(network, new HashSet<>(), signal);
                if(status != EnumLampStatus.GREEN) {
                    trainAtSignal.setPath(null); //Only claim sections when the train can actually travel to the other side of the intersection.
                }
            }
            onCartRouted(trainAtSignal, trainAtSignal.getCurRoute());
        }
    }

    protected void onCartRouted(Train<TPos> train, RailRoute<TPos> route){

    }
}
