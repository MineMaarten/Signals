package com.minemaarten.signals.tileentity;

import java.util.List;
import java.util.Set;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.ITickable;

import com.minemaarten.signals.block.BlockSignalBase.EnumLampStatus;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.RailWrapper;

public class TileEntityBlockSignal extends TileEntitySignalBase implements ITickable{
    @Override
    public void update(){
        super.update();
        if(!world.isRemote) {
            RailWrapper neighborRail = getConnectedRail();
            if(neighborRail != null) {
                Set<RailWrapper> rails = getRailsToNextBlockSection(neighborRail, getFacing());
                List<EntityMinecart> cartsOnNextBlock = getMinecarts(world, rails);
                updateLampStatusBlockSignal(cartsOnNextBlock);
            } else {
                setLampStatus(EnumLampStatus.YELLOW_BLINKING);
            }
        }
    }

    @Override
    protected void onCartEnteringBlock(EntityMinecart cart){
        if(getLampStatus() == EnumLampStatus.GREEN) {
            AStarRailNode path = routeCart(cart, getFacing(), true);
            if(path != null) updateSwitches(path, cart, true);
        }
    }

}
