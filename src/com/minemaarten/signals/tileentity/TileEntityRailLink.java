package com.minemaarten.signals.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.minemaarten.signals.block.BlockRailLink;
import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class TileEntityRailLink extends TileEntityRailLinkBase implements IGUIButtonSensitive{
    @GuiSynced
    private int holdDelay;

    @Override
    public void handleGUIButtonPress(EntityPlayer player, int... data){
        if(data[0] != holdDelay) {
            holdDelay = data[0];
            RailNetworkManager.getInstance(world.isRemote).markDirty(getMCPos());
        }
    }

    public int getHoldDelay(){
        return holdDelay;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
        tag.setInteger("holdDelay", holdDelay);
        return super.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag){
        holdDelay = tag.getInteger("holdDelay");
        super.readFromNBT(tag);
    }

    @Override
    protected void onDestinationChanged(MCPos destination){
        super.onDestinationChanged(destination);
        RailNetworkManager.getInstance(world.isRemote).markDirty(getMCPos());
        updateLinkState();
    }

    private void updateLinkState(){
        NetworkRail<MCPos> linkedRail = getLinkedPosition() == null ? null : RailNetworkManager.getInstance(world.isRemote).getRail(getLinkedPosition());
        world.setBlockState(getPos(), world.getBlockState(getPos()).withProperty(BlockRailLink.CONNECTED, linkedRail != null), 2);
    }
}
