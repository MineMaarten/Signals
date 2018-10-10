package com.minemaarten.signals.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class TileEntityBase extends TileEntity{
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate){
        return oldState.getBlock() != newSate.getBlock();
    }

    protected IBlockState getBlockState(){
        return getWorld() != null ? getWorld().getBlockState(getPos()) : null;
    }

    protected void sendUpdatePacket(){
        world.notifyBlockUpdate(getPos(), getBlockState(), getBlockState(), 3);
    }

    protected MCPos getMCPos(){
        return new MCPos(world, pos);
    }

    @Override
    public void onLoad(){
        super.onLoad();
        if(!world.isRemote) {
            RailNetworkManager.getInstance(world.isRemote).markDirty(new MCPos(world, pos));
            for(EnumFacing f : EnumFacing.VALUES) {
                RailNetworkManager.getInstance(world.isRemote).markDirty(new MCPos(world, pos).offset(f));
            }
        }
    }
}
