package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.item.EntityMinecart;

import com.minemaarten.signals.block.BlockSignalBase.EnumLampStatus;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;

public class TileEntityChainSignal extends TileEntityBlockSignal{
    private List<TileEntitySignalBase> signalsToBeClaimed = Collections.emptyList();
    private EntityMinecart cartToClaim;

    @Override
    public boolean isValidRoute(AStarRailNode route, EntityMinecart cart){
        cartToClaim = cart;
        signalsToBeClaimed = new ArrayList<>();
        HashSet<TileEntityChainSignal> traversedSignals = new HashSet<>();
        return isValidRoute(route, traversedSignals, signalsToBeClaimed, cart);
    }

    /**
     * 
     * @param route
     * @param traversedSignals To prevent a SOE.
     * @param signalsToBeClaimed 
     * @return
     */
    private boolean isValidRoute(AStarRailNode route, Set<TileEntityChainSignal> traversedSignals, List<TileEntitySignalBase> signalsToBeClaimed, EntityMinecart cart){
        if(route != null) { //If the cart has a route, check the signal status of the signals that's on the cart's path
            while(route != null) {
                TileEntitySignalBase signal = TileEntitySignalBase.getNeighborSignal(route.getRail(), route.pathDir);
                if(signal == null) { //If not connected to a signal
                    if(route.getRail().getSignals().size() == 1) {//Try to find a single opposing signal instead.
                        return true;
                    }
                } else { //When connected to a signal
                    if(signal.getClaimingCart() != null && signal.getClaimingCart() != cart) return false; //Another cart has claimed the signal block.

                    EnumLampStatus nextSignalLampStatus = signal.getLampStatus();
                    if(signal != this && nextSignalLampStatus == EnumLampStatus.RED) return false;

                    if(signal instanceof TileEntityChainSignal) {
                        //if(signalsToBeClaimed.isEmpty() || signalsToBeClaimed.get(signalsToBeClaimed.size() - 1) instanceof TileEntityChainSignal)
                        signalsToBeClaimed.addAll(signal.getNextSignals());

                        TileEntityChainSignal chainSignal = (TileEntityChainSignal)signal;
                        if(traversedSignals.add(chainSignal)) {
                            return chainSignal.isValidRoute(route.getNextNode(), traversedSignals, signalsToBeClaimed, cart);
                        }
                    } else if(nextSignalLampStatus != EnumLampStatus.RED) {
                        return true;
                    }
                    return false;
                }

                route = route.getNextNode();
            }
            return true; //When no next signal
        } else { //When the cart has no route, check for all next signals to be green
            return isValidStatically();
        }
    }

    @Override
    public boolean isValidStatically(){
        Set<TileEntitySignalBase> signals = getNextSignals();
        return signals.stream().anyMatch(x -> x.getLampStatus() != EnumLampStatus.RED);
    }

    @Override
    public boolean shouldDelay(){
        return true;
    }

    @Override
    protected void onCartsRoutedAttempt(boolean succeeded){
        super.onCartsRoutedAttempt(succeeded);

        //If the route was successful, claim the chain signals
        if(succeeded) {
            for(TileEntitySignalBase signal : signalsToBeClaimed) {
                if(signal != this) {
                    signal.setClaimingCart(cartToClaim);
                }
            }
        }
        signalsToBeClaimed = Collections.emptyList();
        cartToClaim = null;
    }
}
