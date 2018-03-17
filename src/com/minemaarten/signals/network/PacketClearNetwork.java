package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class PacketClearNetwork extends AbstractPacket<PacketClearNetwork>{
    public PacketClearNetwork(){}

    @Override
    public void toBytes(ByteBuf buf){

    }

    @Override
    public void fromBytes(ByteBuf buf){

    }

    @Override
    public void handleClientSide(EntityPlayer player){
        RailNetworkManager.getInstance().clearNetwork();
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
