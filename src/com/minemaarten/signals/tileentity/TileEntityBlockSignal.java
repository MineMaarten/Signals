package com.minemaarten.signals.tileentity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.item.EntityMinecart;

import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.RailWrapper;

public class TileEntityBlockSignal extends TileEntitySignalBase{
    private int checkDelay = 0; //Have a delay, because for one tick, the Chain Signal will turn green between a cart leaving the section and an ordinary block signal turning red.

    @Override
    public void update(){
        super.update();
        if(!world.isRemote) {
            RailWrapper neighborRail = getConnectedRail();
            if(neighborRail != null) {
                Set<RailWrapper> rails = getRailsToNextBlockSection(neighborRail, getFacing());
                List<EntityMinecart> cartsOnNextBlock = getMinecarts(world, rails);
                EnumLampStatus lampStatus = getLampStatusBlockSignal(cartsOnNextBlock);

                if(lampStatus == EnumLampStatus.GREEN) {
                    List<EntityMinecart> routingCarts = getNeighborMinecarts();
                    Map<EntityMinecart, AStarRailNode> cartsToPath = new HashMap<>();
                    boolean isValid;

                    if(!shouldDelay() || checkDelay++ >= 2) {
                        if(!routingCarts.isEmpty()) {
                            for(EntityMinecart routingCart : routingCarts) {
                                AStarRailNode route = routeCart(routingCart, getFacing(), true);
                                if(!isValidRoute(route, routingCart)) {
                                    break;
                                }
                                cartsToPath.put(routingCart, route);
                            }
                            //When the size is equal, that means all carts had a valid route
                            isValid = routingCarts.size() == cartsToPath.size();
                        } else {
                            isValid = isValidStatically();
                        }
                    } else {
                        isValid = false;
                    }

                    lampStatus = isValid ? EnumLampStatus.GREEN : EnumLampStatus.RED;
                    setLampStatus(lampStatus, () -> routingCarts, cartsToPath::get);
                    onCartsRoutedAttempt(isValid);
                } else {
                    checkDelay = 0;
                    setLampStatus(lampStatus);
                }
            } else {
                setLampStatus(EnumLampStatus.YELLOW_BLINKING);
            }
        }
    }

    @Override
    public boolean isValidRoute(AStarRailNode route, EntityMinecart cart){
        return true;
    }

    public boolean isValidStatically(){
        return true;
    }

    public boolean shouldDelay(){
        return false;
    }

    protected void onCartsRoutedAttempt(boolean succeeded){

    }

    @Override
    protected void onCartEnteringBlock(EntityMinecart cart){
        if(getLampStatus() == EnumLampStatus.GREEN) {
            AStarRailNode path = routeCart(cart, getFacing(), true);
            if(path != null) updateSwitches(path, cart, true);
        }
    }

    /* @Override TODO add this, and decrease the check rate in update() ?
     protected void onCartLeavingBlock(EntityMinecart cart){
         super.onCartLeavingBlock(cart);
         setLampStatus(EnumLampStatus.RED);
     }*/
}
