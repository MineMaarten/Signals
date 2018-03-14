package com.minemaarten.signals.rail.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

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

    //@formatter:off
    public void updateSignalStatusses(RailNetwork<TPos> network){
        List<NetworkSignal<TPos>> allSignals = network.railObjects.getSignals().collect(Collectors.toList());
        signalToLampStatusses = new HashMap<>();
        for(NetworkSignal<TPos> signal : allSignals) {
            if(signal.type == EnumSignalType.CHAIN) throw new NotImplementedException("Chain signals not implemented yet");//TODO chain signals
            NetworkRail<TPos> rail = (NetworkRail<TPos>)network.railObjects.get(signal.getRailPos()); //Safe to cast, as invalid signals have been filtered
            NetworkRail<TPos> nextSectionRail = network.railObjects.getNeighborRails(rail.getPotentialNeighborRailLocations())
                                                                   .filter(r -> r.pos.getRelativeHeading(rail.pos) == signal.heading)
                                                                   .findFirst()
                                                                   .orElse(null);
            EnumLampStatus signalStatus;
            if(nextSectionRail != null){
                RailSection<TPos> nextSection = network.findSection(nextSectionRail.pos);
                Train<TPos> trainOnSection = nextSection.getTrain(trains);
                
                //When there's a train on the next section, and it is not a train that's exiting this signal
                signalStatus = trainOnSection != null && 
                               !trainOnSection.getPositions().contains(rail.pos) ? EnumLampStatus.RED : EnumLampStatus.GREEN;
            }else{
                signalStatus = EnumLampStatus.GREEN;
            }
            
            signalToLampStatusses.put(signal.pos, signalStatus);
        }
    }
    //@formatter:on

    public EnumLampStatus getLampStatus(TPos signalPos){
        return signalToLampStatusses.getOrDefault(signalPos, EnumLampStatus.YELLOW_BLINKING);
    }
}
