package com.minemaarten.signals.tileentity;

import java.util.HashSet;
import java.util.Set;

import com.minemaarten.signals.block.BlockSignalBase.EnumLampStatus;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;

public class TileEntityChainSignal extends TileEntityBlockSignal{

    @Override
    public boolean isValidRoute(AStarRailNode route){
        Set<TileEntityChainSignal> traversedSignals = new HashSet<>();
        traversedSignals.add(this);
        return isValidRoute(route, traversedSignals);
    }

    /**
     * 
     * @param route
     * @param traversedSignals To prevent a SOE.
     * @return
     */
    private boolean isValidRoute(AStarRailNode route, Set<TileEntityChainSignal> traversedSignals){
        if(route != null) { //If the cart has a route, check the specific signal status
            while(route != null) {
                TileEntitySignalBase signal = TileEntitySignalBase.getNeighborSignal(route.getRail(), route.pathDir.getOpposite());
                if(signal == null) { //If not connected to a signal
                    if(route.getRail().getSignals().size() == 1) {//Try to find a single opposing signal instead.
                        return true;
                    }
                } else { //When connected to a signal
                    EnumLampStatus nextSignalLampStatus = signal.getLampStatus();
                    //if(nextSignalLampStatus == EnumLampStatus.GREEN) return true;
                    if(/*nextSignalLampStatus == EnumLampStatus.YELLOW &&*/signal instanceof TileEntityChainSignal) {
                        TileEntityChainSignal chainSignal = (TileEntityChainSignal)signal;
                        if(traversedSignals.add(chainSignal)) {
                            return chainSignal.isValidRoute(route.getNextNode(), traversedSignals);
                        }
                    } else if(nextSignalLampStatus == EnumLampStatus.GREEN) {
                        return true;
                    }
                    return false;
                }

                route = route.getNextNode();
            }
            return true; //When no next signal
        } else { //When the cart has no route, check for any next signal to be green
            return isValidStatically();
        }
    }

    @Override
    public boolean isValidStatically(){
        Set<TileEntitySignalBase> signals = getNextSignals();
        return signals.isEmpty() || signals.stream().anyMatch(x -> x.getLampStatus() != EnumLampStatus.RED);
    }

    @Override
    public boolean shouldDelay(){
        return true;
    }
}
