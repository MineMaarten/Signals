package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.MCTrain;
import com.minemaarten.signals.rail.network.mc.MCTrainClient;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class PacketAddOrUpdateTrain extends AbstractPacket<PacketAddOrUpdateTrain>{
    private int trainID;
    private ImmutableSet<UUID> cartIDs;
    private ImmutableSet<MCPos> positions;

    public PacketAddOrUpdateTrain(){}

    public PacketAddOrUpdateTrain(MCTrain train){
        this.trainID = train.id;
        this.cartIDs = train.cartIDs;
        this.positions = train.getPositions();
    }

    @Override
    public void toBytes(ByteBuf b){
        b.writeInt(trainID);
        b.writeInt(cartIDs.size());
        PacketBuffer pb = new PacketBuffer(b);
        for(UUID cartID : cartIDs) {
            pb.writeUniqueId(cartID);
        }

        b.writeInt(positions.size());
        for(MCPos pos : positions) {
            pos.writeToBuf(b);
        }
    }

    @Override
    public void fromBytes(ByteBuf b){
        trainID = b.readInt();

        int ids = b.readInt();
        PacketBuffer pb = new PacketBuffer(b);
        Builder<UUID> cartIDs = new ImmutableSet.Builder<>();
        for(int i = 0; i < ids; i++) {
            cartIDs.add(pb.readUniqueId());
        }
        this.cartIDs = cartIDs.build();

        int posCount = b.readInt();
        Builder<MCPos> positions = new ImmutableSet.Builder<>();
        for(int i = 0; i < posCount; i++) {
            positions.add(new MCPos(b));
        }
        this.positions = positions.build();
    }

    @Override
    public void handleClientSide(EntityPlayer player){
        MCTrain train = new MCTrainClient(trainID, cartIDs);
        train.setPositions(null, null, positions);
        RailNetworkManager.getInstance().addTrain(train);
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
