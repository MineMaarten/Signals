package com.minemaarten.signals.tileentity;

import net.minecraft.entity.item.EntityMinecart;

import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;

public class TileEntityBlockSignal extends TileEntitySignalBase{

    @Override
    public EnumSignalType getSignalType(){
        return EnumSignalType.BLOCK;
    }

    @Override
    protected void onCartEnteringBlock(EntityMinecart cart){
        // TODO Auto-generated method stub

    }

    /*  @Override
      protected void onCartEnteringBlock(EntityMinecart cart){
          if(getLampStatus() == EnumLampStatus.GREEN) {
              AStarRailNode path = routeCart(cart, getFacing(), true);
              if(path != null) updateSwitches(path, cart, true);
          }
      }*/

}
