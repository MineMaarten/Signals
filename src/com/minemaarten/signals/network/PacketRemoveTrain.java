package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

import com.minemaarten.signals.rail.network.mc.MCTrain;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class PacketRemoveTrain extends AbstractPacket<PacketRemoveTrain>{
    private int trainID;

    public PacketRemoveTrain(){}

    public PacketRemoveTrain(MCTrain train){
        this.trainID = train.id;
    }

    @Override
    public void toBytes(ByteBuf b){
        b.writeInt(trainID);
    }

    @Override
    public void fromBytes(ByteBuf b){
        trainID = b.readInt();
    }

    @Override
    public void handleClientSide(EntityPlayer player){
        RailNetworkManager.getInstance().removeTrain(trainID);
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
