package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;

import java.util.Collection;

import net.minecraft.entity.player.EntityPlayer;

import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.NetworkSerializer;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class PacketUpdateNetwork extends AbstractPacket<PacketUpdateNetwork>{
    private Collection<NetworkObject<MCPos>> changedObjects;

    public PacketUpdateNetwork(){}

    public PacketUpdateNetwork(Collection<NetworkObject<MCPos>> changedObjects){
        this.changedObjects = changedObjects;
    }

    @Override
    public void toBytes(ByteBuf buf){
        new NetworkSerializer().writeToBuf(changedObjects, buf);
    }

    @Override
    public void fromBytes(ByteBuf buf){
        changedObjects = new NetworkSerializer().readFromByteBuf(buf);
    }

    @Override
    public void handleClientSide(EntityPlayer player){
        RailNetworkManager.getInstance().applyUpdates(changedObjects);
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
