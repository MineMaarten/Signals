package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;

import com.minemaarten.signals.capabilities.CapabilityDestinationProvider;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketSpawnParticle;
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
        if(!world.isRemote) {
            updateNeighborRailCache();
            RailCacheManager.getInstance(getWorld()).removeStationMarker(this);
        }
    }

    @Override
    public void update(){
        if(!world.isRemote) {
            if(firstTick) {
                firstTick = false;
                updateNeighborRailCache();
                if(!world.isRemote) {
                    RailCacheManager.getInstance(getWorld()).addStationMarker(this);
                }
            }
            updateNeighborMinecarts();
        }
    }

    public void updateNeighborMinecarts(){
        for(EntityMinecart cart : TileEntitySignalBase.getMinecarts(world, getNeighborRails())) {
            CapabilityMinecartDestination cap = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
            if(cap.getCurrentDestinationRegex().matcher(getStationName()).matches()) {
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
    public NBTTagCompound getUpdateTag(){
        NBTTagCompound tag = super.getUpdateTag();
        tag.setString("stationName", stationName);
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket(){
        return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt){
        super.onDataPacket(net, pkt);
        stationName = pkt.getNbtCompound().getString("stationName");
    }

    public boolean isCartApplicable(EntityMinecart cart, Pattern destinationRegex){
        if(destinationRegex.matcher(getStationName()).matches()) return true;
        for(EnumFacing dir : EnumFacing.VALUES) {
            TileEntity te = getWorld().getTileEntity(getPos().offset(dir));
            if(te != null) {
                CapabilityDestinationProvider cap = te.getCapability(CapabilityDestinationProvider.INSTANCE, null);
                if(cap != null && cap.isCartApplicable(te, cart, destinationRegex)) {
                    for(int i = 0; i < 10; i++) {
                        double x = getPos().getX() + getWorld().rand.nextDouble();
                        double z = getPos().getZ() + getWorld().rand.nextDouble();
                        NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, x, getPos().getY() + 1, z, dir.getFrontOffsetX(), dir.getFrontOffsetY(), dir.getFrontOffsetZ()), getWorld());
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
