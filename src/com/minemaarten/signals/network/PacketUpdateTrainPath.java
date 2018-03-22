package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

import com.google.common.collect.ImmutableList;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.MCTrain;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class PacketUpdateTrainPath extends AbstractPacket<PacketUpdateTrainPath>{
    private int trainID;
    private RailRoute<MCPos> path;

    public PacketUpdateTrainPath(){}

    public PacketUpdateTrainPath(MCTrain train){
        this.trainID = train.id;
        this.path = train.getCurRoute();
    }

    @Override
    public void toBytes(ByteBuf b){
        b.writeInt(trainID);
        b.writeBoolean(path != null);
        if(path != null) {
            b.writeInt(path.routeRails.size());
            for(MCPos pos : path.routeRails) {
                pos.writeToBuf(b);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf b){
        trainID = b.readInt();
        if(b.readBoolean()) {
            int count = b.readInt();
            ImmutableList.Builder<MCPos> routeRails = ImmutableList.builder();
            for(int i = 0; i < count; i++) {
                routeRails.add(new MCPos(b));
            }
            path = new RailRoute<>(ImmutableList.of(), routeRails.build(), ImmutableList.of(), ImmutableList.of());
        }
    }

    @Override
    public void handleClientSide(EntityPlayer player){
        MCTrain train = RailNetworkManager.getInstance().getTrainByID(trainID);
        if(train != null) {
            train.setPath(path);
        }
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
