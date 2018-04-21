package com.minemaarten.signals.tileentity;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import com.minemaarten.signals.block.BlockRailLink;
import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class TileEntityRailLink extends TileEntityBase implements IGUIButtonSensitive{
    private MCPos linkedPos;
    @GuiSynced
    private int holdDelay;

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
    public void handleGUIButtonPress(EntityPlayer player, int... data){
        if(data[0] != holdDelay) {
            holdDelay = data[0];
            RailNetworkManager.getInstance().markDirty(getMCPos());
        }
    }

    public int getHoldDelay(){
        return holdDelay;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
        if(linkedPos != null) {
            tag.setInteger("linkedX", linkedPos.getX());
            tag.setInteger("linkedY", linkedPos.getY());
            tag.setInteger("linkedZ", linkedPos.getZ());
            tag.setInteger("linkedDim", linkedPos.getDimID());
        }

        tag.setInteger("holdDelay", holdDelay);
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

        holdDelay = tag.getInteger("holdDelay");
        super.readFromNBT(tag);
    }
}
