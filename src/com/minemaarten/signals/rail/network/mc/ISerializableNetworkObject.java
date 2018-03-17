package com.minemaarten.signals.rail.network.mc;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

import com.minemaarten.signals.rail.network.mc.NetworkSerializer.EnumNetworkObject;

/**
 * Deserialization is done via constructors
 * @author Maarten
 *
 */
public interface ISerializableNetworkObject{
    public void writeToNBT(NBTTagCompound tag);

    public void writeToBuf(ByteBuf b);

    public EnumNetworkObject getType();
}
