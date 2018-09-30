package com.minemaarten.signals.rail.network.mc;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

import com.minemaarten.signals.rail.network.NetworkRailLink;
import com.minemaarten.signals.rail.network.mc.NetworkSerializer.EnumNetworkObject;

public class MCNetworkRailLink extends NetworkRailLink<MCPos> implements ISerializableNetworkObject{

    public MCNetworkRailLink(MCPos pos, MCPos destination, int holdDelay){
        super(pos, destination, holdDelay);
    }

    public static MCNetworkRailLink fromTag(NBTTagCompound tag){
        MCPos destination = null;
        if(tag.hasKey("dest")) {
            destination = new MCPos(tag.getCompoundTag("dest"));
        }
        return new MCNetworkRailLink(new MCPos(tag), destination, tag.getInteger("holdDelay"));
    }

    public static MCNetworkRailLink fromByteBuf(ByteBuf buf){
        MCPos pos = new MCPos(buf);
        MCPos destination = null;
        if(buf.readBoolean()) {
            destination = new MCPos(buf);
        }
        return new MCNetworkRailLink(pos, destination, buf.readInt());
    }

    @Override
    public void writeToNBT(NBTTagCompound tag){
        getPos().writeToNBT(tag);
        if(getDestinationPos() != null) {
            NBTTagCompound destTag = new NBTTagCompound();
            getDestinationPos().writeToNBT(destTag);
            tag.setTag("dest", destTag);
        }
        tag.setInteger("holdDelay", getHoldDelay());
    }

    @Override
    public void writeToBuf(ByteBuf b){
        getPos().writeToBuf(b);
        if(getDestinationPos() != null) {
            b.writeBoolean(true);
            getDestinationPos().writeToBuf(b);
        } else {
            b.writeBoolean(false);
        }
        b.writeInt(getHoldDelay());
    }

    @Override
    public EnumNetworkObject getType(){
        return EnumNetworkObject.RAIL_LINK;
    }

}
