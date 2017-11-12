package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import com.minemaarten.signals.block.BlockSignalBase.EnumLampStatus;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketSpawnParticle;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.RailManager;
import com.minemaarten.signals.rail.RailWrapper;

public class TileEntityPathSignal extends TileEntitySignalBase implements ITickable{

    private int pathingTimer;

    @Override
    public void update(){
        super.update();
        if(!world.isRemote && pathingTimer-- <= 0 && getForceMode() == EnumForceMode.NONE) {
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
                List<EntityMinecart> cartsOnNextBlock = getMinecarts(world, rails);

                //Fall back onto default Block Signal behaviour if there are any carts to be routed without a path. 
                //This also will fill the routed carts with a path for the next stage.
                for(EntityMinecart routingCart : routingMinecarts) {
                    if(routeCart(routingCart, getFacing(), false) == null) {
                        updateLampStatusBlockSignal(cartsOnNextBlock);
                        setMessage("signals.signal_message.cart_without_destination");
                        Log.debug("[Path Signal] Cart routed without destination. Block signal behaviour.");
                        return;
                    }
                }

                //ignore carts that are part of the same train
                cartsOnNextBlock = cartsOnNextBlock.stream().filter(cart -> !isCartLinkedToAny(routingMinecarts, cart)).collect(Collectors.toList());

                //Don't allow the cart to proceed if there are carts on the block without a path.
                for(EntityMinecart cartOnNextBlock : cartsOnNextBlock) {
                    if(RailManager.getInstance().getPath(cartOnNextBlock) == null) {
                        setLampStatus(EnumLampStatus.RED);
                        BlockPos pos = cartOnNextBlock.getPosition();
                        setMessage("signals.signal_message.cart_on_track_without_destination", pos.getX(), pos.getY(), pos.getZ());
                        Log.debug("[Path Signal] Cart on rails without destination. Red signal. Cart: " + cartOnNextBlock.getPosition());

                        for(int i = 0; i < 10; i++) {
                            double randX = pos.getX() + world.rand.nextDouble();
                            double randY = pos.getY() + world.rand.nextDouble();
                            double randZ = pos.getZ() + world.rand.nextDouble();
                            NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.REDSTONE, randX, randY, randZ, 0, 0, 0), world);
                        }

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
                            BlockPos p = cartOnNextBlock.getPosition();
                            setMessage("signals.signal_message.cart_intersecting_path", p.getX(), p.getY(), p.getZ());
                            Log.debug("[Path Signal] Cart on rails intersecting the path of the routed cart. Red signal. Cart: " + cartOnNextBlock.getPosition());

                            for(int i = 0; i < 10; i++) {
                                double randX = p.getX() + world.rand.nextDouble();
                                double randY = p.getY() + world.rand.nextDouble();
                                double randZ = p.getZ() + world.rand.nextDouble();
                                NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.REDSTONE, randX, randY, randZ, 0, 0, 0), world);
                            }
                            return;
                        }
                    }
                }
                setLampStatus(EnumLampStatus.GREEN);

                AStarRailNode path = RailManager.getInstance().getPath(routingMinecarts.get(0));
                if(path != null) updateSwitches(path, routingMinecarts.get(0), true);
            } else {
                setMessage("");
                // setMessage("signals.signal_message.standby");
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

    private List<BlockPos> getToBeTraversedCoordinates(EntityMinecart cart){
        AStarRailNode path = RailManager.getInstance().getPath(cart);
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
