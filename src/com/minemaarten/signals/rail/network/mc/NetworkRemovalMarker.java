package com.minemaarten.signals.rail.network.mc;

import io.netty.buffer.ByteBuf;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import org.apache.commons.lang3.NotImplementedException;

import com.minemaarten.signals.rail.network.IRemovalMarker;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.mc.NetworkSerializer.EnumNetworkObject;

/**
 * Special NetworkObject, used in server->client packets, to indicate that a position is not a NetworkObject anymore.
 * @author Maarten
 *
 */
public class NetworkRemovalMarker extends NetworkObject<MCPos> implements ISerializableNetworkObject, IRemovalMarker{

    public NetworkRemovalMarker(MCPos pos){
        super(pos);
    }

    public static NetworkRemovalMarker fromTag(NBTTagCompound tag){
        return new NetworkRemovalMarker(new MCPos(tag));
    }

    public static NetworkRemovalMarker fromByteBuf(ByteBuf b){
        return new NetworkRemovalMarker(new MCPos(b));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag){
        pos.writeToNBT(tag);
    }

    @Override
    public void writeToBuf(ByteBuf b){
        pos.writeToBuf(b);
    }

    @Override
    public EnumNetworkObject getType(){
        return EnumNetworkObject.REMOVAL_MARKER;
    }

    @Override
    public List<MCPos> getNetworkNeighbors(){
        throw new NotImplementedException("");
    }

    @Override
    public boolean equals(Object obj){
        return super.equals(obj) && obj instanceof NetworkRemovalMarker;
    }

    @Override
    public int hashCode(){
        return super.hashCode();
    }
}
