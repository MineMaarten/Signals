package com.minemaarten.signals.rail.network.mc;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkSignal;
import com.minemaarten.signals.rail.network.mc.NetworkSerializer.EnumNetworkObject;

public class MCNetworkSignal extends NetworkSignal<MCPos> implements ISerializableNetworkObject{

    public MCNetworkSignal(MCPos pos, EnumHeading heading, EnumSignalType type){
        super(pos, heading, type);
    }

    public static MCNetworkSignal fromTag(NBTTagCompound tag){
        byte b = tag.getByte("t");
        return new MCNetworkSignal(new MCPos(tag), decodeHeadingFromByte(b), decodeSignalTypeFromByte(b));
    }

    public static MCNetworkSignal fromByteBuf(ByteBuf buf){
        MCPos pos = new MCPos(buf);
        byte b = buf.readByte();
        return new MCNetworkSignal(pos, decodeHeadingFromByte(b), decodeSignalTypeFromByte(b));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag){
        pos.writeToNBT(tag);
        tag.setByte("t", encodeToByte());
    }

    @Override
    public void writeToBuf(ByteBuf b){
        pos.writeToBuf(b);
        b.writeByte(encodeToByte());
    }

    private byte encodeToByte(){
        return (byte)((type.ordinal() << 4) | heading.ordinal());
    }

    private static EnumHeading decodeHeadingFromByte(byte b){
        return EnumHeading.VALUES[b & 0xF];
    }

    private static EnumSignalType decodeSignalTypeFromByte(byte b){
        return EnumSignalType.VALUES[b >> 4];
    }

    @Override
    public EnumNetworkObject getType(){
        return EnumNetworkObject.SIGNAL;
    }

}
