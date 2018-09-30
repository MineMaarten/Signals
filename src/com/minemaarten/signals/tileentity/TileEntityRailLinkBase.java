package com.minemaarten.signals.tileentity;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import com.minemaarten.signals.rail.network.mc.MCPos;

public abstract class TileEntityRailLinkBase extends TileEntityBase{
    private MCPos linkedPos;

    public MCPos getLinkedPosition(){
        return linkedPos;
    }

    public boolean setLinkedPos(MCPos railPos, EntityPlayer player){
        if(!Objects.equals(railPos, linkedPos)) {
            if(isDestinationValid(railPos, player)) {
                linkedPos = railPos;
                onDestinationChanged(railPos);
                return true;
            } else {
                return false;
            }

        } else {
            return true;
        }
    }

    protected boolean isDestinationValid(MCPos destination, EntityPlayer player){
        return true;
    }

    protected void onDestinationChanged(MCPos destination){

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
