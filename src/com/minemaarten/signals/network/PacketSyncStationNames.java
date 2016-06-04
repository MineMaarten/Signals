package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import com.minemaarten.signals.rail.RailCacheManager;

public class PacketSyncStationNames extends AbstractPacket<PacketSyncStationNames>{
    private String[] stationNames;

    public PacketSyncStationNames(){

    }

    public PacketSyncStationNames(String[] stationNames){
        this.stationNames = stationNames;
    }

    @Override
    public void fromBytes(ByteBuf buf){
        stationNames = new String[buf.readInt()];
        for(int i = 0; i < stationNames.length; i++) {
            stationNames[i] = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf){
        buf.writeInt(stationNames.length);
        for(String s : stationNames)
            ByteBufUtils.writeUTF8String(buf, s);
    }

    @Override
    public void handleClientSide(EntityPlayer player){
        RailCacheManager.setAllStationNames(stationNames);
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
