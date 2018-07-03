package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import org.apache.commons.lang3.Validate;

import com.minemaarten.signals.api.access.IStationMarker;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.PosAABB;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.MCTrain;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class TileEntityStationMarker extends TileEntityBase implements ITickable, IGUITextFieldSensitive,
        IStationMarker{
    private static int nextId;
    @GuiSynced
    private String stationName = "";
    private PosAABB<MCPos> neighborAABB;

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

    @Override
    public String getStationName(){
        return stationName;
    }

    @Override
    public void setStationName(String stationName){
        Validate.notNull(stationName);

        this.stationName = stationName;
        if(!world.isRemote) {
            RailNetworkManager.getInstance().markDirty(getMCPos());
            markDirty();
            sendUpdatePacket();
        }
    }

    public List<MCPos> getNeighborRails(){
        List<MCPos> neighbors = new ArrayList<>(1);
        for(EnumFacing d : EnumFacing.VALUES) {
            MCPos neighborPos = getMCPos().offset(d);
            NetworkObject<MCPos> rail = RailNetworkManager.getInstance().getNetwork().railObjects.get(neighborPos);
            if(rail instanceof NetworkRail) neighbors.add(rail.pos);
        }
        return neighbors;
    }

    @Override
    public void update(){
        if(!world.isRemote) {
            updateNeighborMinecarts();
        }
    }

    public void updateNeighborMinecarts(){
        if(neighborAABB == null) {
            neighborAABB = new PosAABB<>(getNeighborRails());
        }

        RailNetworkManager.getInstance().getAllTrains().forEach(this::updateNeighborMinecarts);
    }

    private void updateNeighborMinecarts(MCTrain train){
        if(train.isInAABB(neighborAABB)) {
            for(EntityMinecart cart : train.getCarts()) {
                CapabilityMinecartDestination cap = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
                if(cap.getDestinationIndex() >= 0) {
                    Pattern destinationRegex = cap.getCurrentDestinationRegex();
                    Set<MCPos> stations = RailNetworkManager.getInstance().getNetwork().getStations(train, destinationRegex);
                    if(stations.contains(getMCPos())) {
                        cap.nextDestination();
                    }
                }
            }
        }
    }

    @Override
    public void setText(int textFieldID, String text){
        setStationName(text);
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
}
