package com.minemaarten.signals.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TileEntityBase extends TileEntity {
    @Override
    public boolean shouldRefresh(World world, BlockPos pos,
    		IBlockState oldState, IBlockState newSate) {
    	return oldState.getBlock() != newSate.getBlock();
    }
    
    private IBlockState getBlockState(){
        return getWorld() != null ? getWorld().getBlockState(getPos()) : null;
    }
    
    protected void sendUpdatePacket(){
    	world.notifyBlockUpdate(getPos(), getBlockState(), getBlockState(), 3);
    }
}
