package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import com.minemaarten.signals.block.BlockSignalBase.EnumLampStatus;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.RailWrapper;

public class TileEntityPathSignal extends TileEntitySignalBase implements ITickable{

    private int pathingTimer;

    @Override
    public void update(){
        super.update();
        if(!worldObj.isRemote && pathingTimer-- <= 0) {
            route();
        }
    }

    public void route(){
        pathingTimer = 20;
        RailWrapper neighborRail = getConnectedRail();
        if(neighborRail != null) {
            List<EntityMinecart> routingMinecarts = getNeighborMinecarts();
            if(!routingMinecarts.isEmpty()) {
                Set<RailWrapper> rails = getRailsToNextBlockSection(neighborRail, getFacing());
                List<EntityMinecart> cartsOnNextBlock = getMinecarts(worldObj, rails);

                //Fall back onto default Block Signal behaviour if there are any carts to be routed without a path. 
                //This also will fill the routed carts with a path for the next stage.
                for(EntityMinecart routingCart : routingMinecarts) {
                    if(routeCart(routingCart, getFacing(),  false) == null) {
                        setLampStatus(cartsOnNextBlock.isEmpty() ? EnumLampStatus.GREEN : EnumLampStatus.RED);
                        setMessage("Cart routed without destination. Block signal behaviour.");
                        Log.debug("[Path Signal] Cart routed without destination. Block signal behaviour.");
                        return;
                    }
                }

                //Don't allow the cart to proceed if there are carts on the block without a path.
                for(EntityMinecart cartOnNextBlock : cartsOnNextBlock) {
                    if(getStoredPath(cartOnNextBlock) == null) {
                        setLampStatus(EnumLampStatus.RED);
                        setMessage("Cart on rails without destination. Red signal. Cart: " + cartOnNextBlock.getPosition());
                        Log.debug("[Path Signal] Cart on rails without destination. Red signal. Cart: " + cartOnNextBlock.getPosition());
                        return;
                    }
                }

                //The positions of the track that should be kept free of carts for the carts to be allowed to continue.
                Set<BlockPos> claimingPositions = new HashSet<BlockPos>();
                for(EntityMinecart routingCart : routingMinecarts) {
                    claimingPositions.addAll(getToBeTraversedCoordinates(routingCart));
                }

                for(EntityMinecart cartOnNextBlock : cartsOnNextBlock) {
                    List<BlockPos> list = getToBeTraversedCoordinates(cartOnNextBlock);
                    for(BlockPos pos : list) {
                        if(claimingPositions.contains(pos)) {
                            setLampStatus(EnumLampStatus.RED);
                            setMessage("Cart on rails intersecting the path of the routed cart. Red signal. Cart: " + cartOnNextBlock.getPosition());
                            Log.debug("[Path Signal] Cart on rails intersecting the path of the routed cart. Red signal. Cart: " + cartOnNextBlock.getPosition());
                            return;
                        }
                    }
                }
                setLampStatus(EnumLampStatus.GREEN);
               // AStarRailNode path = getStoredPath(routingMinecarts.get(0));
               // if(path != null) updateSwitches(path, routingMinecarts.get(0), true);
            } else {
            	setMessage("Standby...");
                setLampStatus(EnumLampStatus.YELLOW);
            }
        } else {
            setLampStatus(EnumLampStatus.YELLOW_BLINKING);
        }
    }

    @Override
    protected void onCartEnteringBlock(EntityMinecart cart){
        route();
    }

    private AStarRailNode getStoredPath(EntityMinecart cart){
        return cart.getCapability(CapabilityMinecartDestination.INSTANCE, null).getPath(cart.worldObj);
    }

    private List<BlockPos> getToBeTraversedCoordinates(EntityMinecart cart){
        AStarRailNode path = getStoredPath(cart);
        List<BlockPos> coords = new ArrayList<BlockPos>();
        BlockPos cartPos = cart.getPosition();
        boolean returnOnNext = false;
        while(path != null) {
            coords.add(path.getRail());
            if(returnOnNext) return coords;
            if(path.getRail().equals(cartPos)) returnOnNext = true;
            path = path.getNextNode();
        }
        return coords;
    }

}
