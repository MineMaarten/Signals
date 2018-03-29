package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.NetworkController;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class PacketUpdateSignals extends AbstractPacket<PacketUpdateSignals>{
    private Map<MCPos, EnumLampStatus> changedSignals;

    public PacketUpdateSignals(){}

    public PacketUpdateSignals(Map<MCPos, EnumLampStatus> changedSignals){
        this.changedSignals = changedSignals;
    }

    @Override
    public void toBytes(ByteBuf b){
        b.writeInt(changedSignals.size());
        for(Map.Entry<MCPos, EnumLampStatus> entry : changedSignals.entrySet()) {
            entry.getKey().writeToBuf(b);
            b.writeByte(entry.getValue().ordinal());
        }
    }

    @Override
    public void fromBytes(ByteBuf b){
        int size = b.readInt();
        changedSignals = new HashMap<>(size);
        for(int i = 0; i < size; i++) {
            MCPos pos = new MCPos(b);
            EnumLampStatus lampStatus = EnumLampStatus.VALUES[b.readByte()];
            changedSignals.put(pos, lampStatus);
        }
    }

    @Override
    public void handleClientSide(EntityPlayer player){
        RailNetworkManager.getInstance().getState().setSignalStatusses(changedSignals);
        for(Map.Entry<MCPos, EnumLampStatus> entry : changedSignals.entrySet()) {
            NetworkController.getInstance(entry.getKey().getDimID()).updateColor(entry.getValue().color, entry.getKey().getPos());
        }
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
