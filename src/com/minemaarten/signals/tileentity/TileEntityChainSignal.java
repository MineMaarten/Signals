package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.item.EntityMinecart;

import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.SignalsOnRouteIterable.SignalOnRoute;

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
            for(SignalOnRoute signalOnRoute : route.getSignalsOnRoute()) {
                TileEntitySignalBase signal = signalOnRoute.signal;
                if(signalOnRoute.opposite) {
                    return true;
                } else { //When connected to a signal pointing in the right direction
                    if(signal.getClaimingCart() != null && signal.getClaimingCart() != cart) return false; //Another cart has claimed the signal block.

                    EnumLampStatus nextSignalLampStatus = signal.getLampStatus();
                    if(signal != this && nextSignalLampStatus == EnumLampStatus.RED) return false;

                    if(signal instanceof TileEntityChainSignal) {
                        signalsToBeClaimed.addAll(signal.getNextSignals());

                        TileEntityChainSignal chainSignal = (TileEntityChainSignal)signal;
                        if(!traversedSignals.add(chainSignal)) return false;
                    } else {
                        return nextSignalLampStatus != EnumLampStatus.RED;
                    }
                }
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
