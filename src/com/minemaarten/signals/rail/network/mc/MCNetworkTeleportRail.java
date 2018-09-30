package com.minemaarten.signals.rail.network.mc;

import io.netty.buffer.ByteBuf;

import java.util.EnumSet;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.nbt.NBTTagCompound;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.minemaarten.signals.rail.network.IRailLink;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.RailObjectHolder;
import com.minemaarten.signals.rail.network.mc.NetworkSerializer.EnumNetworkObject;

public class MCNetworkTeleportRail extends MCNetworkRail implements IRailLink<MCPos>{
    private final MCNetworkRailLink railLink;

    public MCNetworkTeleportRail(MCPos pos, Block railBlock, EnumRailDirection curDir,
            EnumSet<EnumRailDirection> validRailDirs, MCPos destination){
        super(pos, railBlock, curDir, validRailDirs);
        railLink = new MCNetworkRailLink(pos, destination, 0 /*Fixed hold delay*/);
    }

    public MCNetworkTeleportRail(MCPos pos, String railType, EnumRailDirection curDir,
            EnumSet<EnumRailDirection> validRailDirs, MCPos destination){
        super(pos, railType, curDir, validRailDirs);
        railLink = new MCNetworkRailLink(pos, destination, 0 /*Fixed hold delay*/);
    }

    @Override
    public MCPos getDestinationPos(){
        return railLink.getDestinationPos();
    }

    @Override
    public int getHoldDelay(){
        return railLink.getHoldDelay();
    }

    @Override
    public Stream<NetworkRail<MCPos>> getNeighborRails(RailObjectHolder<MCPos> railObjects){
        NetworkRail<MCPos> rail = railObjects.getRail(getPos());
        return rail != null ? Stream.of(rail) : Stream.empty();
    }

    @Override
    protected ImmutableList<MCPos> computePotentialObjectNeighbors(){
        return Streams.concat(potentialObjectNeighborsStream(), Stream.of(getPos())).collect(ImmutableList.toImmutableList());
    }

    @Override
    public boolean canRailConnect(MCPos railPos){
        return railPos.equals(getPos());
    }

    @Override
    public EnumNetworkObject getType(){
        return EnumNetworkObject.TELEPORT_RAIL;
    }

    @Override
    public void writeToBuf(ByteBuf b){
        railLink.writeToBuf(b);
        super.writeToBuf(b);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag){
        super.writeToNBT(tag);
        railLink.writeToNBT(tag);
    }

    public static MCNetworkTeleportRail fromTag(NBTTagCompound tag){
        MCNetworkRailLink link = MCNetworkRailLink.fromTag(tag);
        return MCNetworkRail.fromTag(tag, (pos, railType, curDir, validDirs) -> new MCNetworkTeleportRail(pos, railType, curDir, validDirs, link.getDestinationPos()));
    }

    public static MCNetworkTeleportRail fromByteBuf(ByteBuf b){
        MCNetworkRailLink link = MCNetworkRailLink.fromByteBuf(b);
        return MCNetworkRail.fromByteBuf(b, (pos, railType, curDir, validDirs) -> new MCNetworkTeleportRail(pos, railType, curDir, validDirs, link.getDestinationPos()));
    }

    @Override
    public boolean equals(Object obj){
        return super.equals(obj) && obj instanceof MCNetworkTeleportRail && ((MCNetworkTeleportRail)obj).railLink.equals(railLink);
    }

    @Override
    public int hashCode(){
        return super.hashCode() * 31 + railLink.hashCode();
    }
}
