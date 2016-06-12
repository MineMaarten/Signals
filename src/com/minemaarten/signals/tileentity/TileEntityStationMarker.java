package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.rail.RailWrapper;

public class TileEntityStationMarker extends TileEntityBase implements ITickable, IGUITextFieldSensitive{
    private static int nextId;
    @GuiSynced
    private String stationName;
    private boolean firstTick = true;

    public TileEntityStationMarker(){
        stationName = "Station" + nextId++;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound){
        super.writeToNBT(compound);
        compound.setString("stationName", stationName);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound){
        super.readFromNBT(compound);
        stationName = compound.getString("stationName");
    }

    public String getStationName(){
        return stationName;
    }

    private void updateNeighborRailCache(){
        for(RailWrapper rail : getNeighborRails()) {
            rail.updateStationCache();
        }
    }

    public List<RailWrapper> getNeighborRails(){
        List<RailWrapper> neighbors = new ArrayList<RailWrapper>(1);
        for(EnumFacing d : EnumFacing.values()) {
            RailWrapper rail = RailCacheManager.getInstance(getWorld()).getRail(getWorld(), getPos().offset(d));
            if(rail != null) neighbors.add(rail);
        }
        return neighbors;
    }

    @Override
    public void invalidate(){
        super.invalidate();
        if(!worldObj.isRemote) {
            updateNeighborRailCache();
            RailCacheManager.getInstance(getWorld()).removeStationMarker(this);
        }
    }

    @Override
    public void update(){
        if(!worldObj.isRemote) {
            if(firstTick) {
                firstTick = false;
                updateNeighborRailCache();
                if(!worldObj.isRemote) {
                    RailCacheManager.getInstance(getWorld()).addStationMarker(this);
                }
            }
            updateNeighborMinecarts();
        }
    }

    public void updateNeighborMinecarts(){
        for(EntityMinecart cart : TileEntitySignalBase.getMinecarts(worldObj, getNeighborRails())) {
            CapabilityMinecartDestination cap = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
            if(cap.getCurrentDestination().equalsIgnoreCase(getStationName())) {
                cap.nextDestination();
            }
        }
    }

    @Override
    public void setText(int textFieldID, String text){
        stationName = text;
        markDirty();
        sendUpdatePacket();
    }

    @Override
    public String getText(int textFieldID){
        return getStationName();
    }
    
    @Override
    public NBTTagCompound getUpdateTag() {
    	NBTTagCompound tag = super.getUpdateTag();
    	tag.setString("stationName", stationName);
    	return tag;
    }
    
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
    	return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
    }
    
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
    	super.onDataPacket(net, pkt);
    	stationName = pkt.getNbtCompound().getString("stationName");
    }
}
