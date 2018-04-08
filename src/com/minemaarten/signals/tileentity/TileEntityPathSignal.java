package com.minemaarten.signals.tileentity;

import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.init.ModBlocks;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;

public class TileEntityPathSignal extends TileEntitySignalBase{

    @Override
    public EnumSignalType getSignalType(){
        return EnumSignalType.CHAIN;
    }

    /**
     * TODO remove in 1.13
     */
    @Override
    public void update(){
        super.update();
        if(!world.isRemote) {
            //Migrate
            world.setBlockState(getPos(), ModBlocks.CHAIN_SIGNAL.getDefaultState().withProperty(BlockSignalBase.FACING, getFacing()));
        }
    }
}
