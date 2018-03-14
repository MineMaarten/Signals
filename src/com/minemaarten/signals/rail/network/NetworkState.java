package com.minemaarten.signals.rail.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;

/**
 * Contains the mutable state of a rail network, like the trains (positions and routes), the signal statusses
 * @author Maarten
 *
 */
public class NetworkState<TPos extends IPosition<TPos>> {
    private final Set<Train<TPos>> trains;
    private Map<TPos, EnumLampStatus> signalToLampStatusses;

    public NetworkState(Set<Train<TPos>> trains){
        this.trains = trains;
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
        if(blockSignalStatus == EnumLampStatus.RED) { //It is not going to get any greener if there's a train in the way
            return EnumLampStatus.RED;
        } else {
            List<EnumLampStatus> nextSignalStatusses = chainSignal.getNextRailSection(network).getSignals().map(s -> getLampStatus(s.pos)).collect(Collectors.toList());

            //When we can evaluate this chain signal
            if(!nextSignalStatusses.contains(EnumLampStatus.YELLOW_BLINKING)) {
                return nextSignalStatusses.contains(EnumLampStatus.RED) ? EnumLampStatus.RED : EnumLampStatus.GREEN;
            } else {
                return EnumLampStatus.YELLOW_BLINKING;
            }
        }
    }

    private EnumLampStatus getBlockSignalStatus(RailNetwork<TPos> network, NetworkSignal<TPos> signal){
        EnumLampStatus signalStatus;

        RailSection<TPos> nextSection = signal.getNextRailSection(network);
        if(nextSection != null) {
            Train<TPos> trainOnSection = nextSection.getTrain(trains);

            //When there's a train on the next section, and it is not a train that's exiting this signal
            signalStatus = trainOnSection != null && !trainOnSection.getPositions().contains(signal.getRailPos()) ? EnumLampStatus.RED : EnumLampStatus.GREEN;
        } else {
            signalStatus = EnumLampStatus.GREEN;
        }
        return signalStatus;
    }

    public EnumLampStatus getLampStatus(TPos signalPos){
        return signalToLampStatusses.getOrDefault(signalPos, EnumLampStatus.YELLOW_BLINKING);
    }
}
