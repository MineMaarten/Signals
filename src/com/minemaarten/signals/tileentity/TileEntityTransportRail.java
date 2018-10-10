package com.minemaarten.signals.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

public class TileEntityTransportRail extends TileEntityBase{
    private boolean forward;

    public boolean isForward(){
        return forward;
    }

    public void toggleForward(){
        setForward(!isForward());
    }

    public void setForward(boolean forward){
        this.forward = forward;
        sendUpdatePacket();
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound){
        compound.setBoolean("forward", forward);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound){
        forward = compound.getBoolean("forward");
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound getUpdateTag(){
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("forward", forward);
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket(){
        return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt){
        if(pkt.getTileEntityType() == 0) {
            forward = pkt.getNbtCompound().getBoolean("forward");
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag){
        super.handleUpdateTag(tag);
        world.markBlockRangeForRenderUpdate(pos, pos);
    }
}
