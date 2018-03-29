package com.minemaarten.signals.tileentity;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import com.minemaarten.signals.block.BlockRailLink;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class TileEntityRailLink extends TileEntityBase{
    private MCPos linkedPos;

    public MCPos getLinkedPosition(){
        return linkedPos;
    }

    public void setLinkedPos(MCPos railPos){
        if(!Objects.equals(railPos, linkedPos)) {
            linkedPos = railPos;
            RailNetworkManager.getInstance().markDirty(getMCPos());
            updateLinkState();
        }
    }

    private void updateLinkState(){
        NetworkRail<MCPos> linkedRail = linkedPos == null ? null : RailNetworkManager.getInstance().getRail(linkedPos);
        world.setBlockState(getPos(), world.getBlockState(getPos()).withProperty(BlockRailLink.CONNECTED, linkedRail != null), 2);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
        if(linkedPos != null) {
            tag.setInteger("linkedX", linkedPos.getX());
            tag.setInteger("linkedY", linkedPos.getY());
            tag.setInteger("linkedZ", linkedPos.getZ());
            tag.setInteger("linkedDim", linkedPos.getDimID());
        }

        return super.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag){
        if(tag.hasKey("linkedX")) {
            BlockPos p = new BlockPos(tag.getInteger("linkedX"), tag.getInteger("linkedY"), tag.getInteger("linkedZ"));
            linkedPos = new MCPos(tag.getInteger("linkedDim"), p);
        } else {
            linkedPos = null;
        }

        super.readFromNBT(tag);
    }
}
