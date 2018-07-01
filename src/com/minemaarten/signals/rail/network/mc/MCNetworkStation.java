package com.minemaarten.signals.rail.network.mc;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import com.minemaarten.signals.capabilities.CapabilityDestinationProvider;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketSpawnParticle;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkStation;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.Train;
import com.minemaarten.signals.rail.network.mc.NetworkSerializer.EnumNetworkObject;

public class MCNetworkStation extends NetworkStation<MCPos> implements ISerializableNetworkObject{

    private final List<MCPos> potentialNeighbors = new ArrayList<>();

    public MCNetworkStation(MCPos pos, String stationName){
        super(pos, stationName);
        for(EnumHeading heading : EnumHeading.VALUES) {
            potentialNeighbors.add(pos.offset(heading));
        }
    }

    public static MCNetworkStation fromTag(NBTTagCompound tag){
        return new MCNetworkStation(new MCPos(tag), tag.getString("station"));
    }

    public static MCNetworkStation fromByteBuf(ByteBuf buf){
        return new MCNetworkStation(new MCPos(buf), ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag){
        pos.writeToNBT(tag);
        tag.setString("station", stationName);
    }

    @Override
    public void writeToBuf(ByteBuf b){
        pos.writeToBuf(b);
        ByteBufUtils.writeUTF8String(b, stationName);
    }

    @Override
    public List<MCPos> getNetworkNeighbors(){
        return potentialNeighbors;
    }

    @Override
    public List<MCPos> getConnectedRailPositions(RailNetwork<MCPos> network){
        List<MCPos> rails = new ArrayList<>(1);
        for(MCPos neighbor : potentialNeighbors) {
            if(network.railObjects.getRail(neighbor) != null) {
                rails.add(neighbor);
            }
        }
        return rails;
    }

    @Override
    public boolean isTrainApplicable(Train<MCPos> train, Pattern destinationRegex){
        if(super.isTrainApplicable(train, destinationRegex)) return true;

        World world = pos.getWorld();
        if(world != null) {
            MCTrain mcTrain = (MCTrain)train;
            for(EntityMinecart cart : mcTrain.getCarts()) {
                if(isCartApplicable(world, cart, destinationRegex)) return true;
            }
        }
        return false;
    }

    public boolean isCartApplicable(World world, EntityMinecart cart, Pattern destinationRegex){
        for(EnumFacing dir : EnumFacing.VALUES) {
            BlockPos neighborPos = pos.getPos().offset(dir);
            if(world.isBlockLoaded(neighborPos)) {
                TileEntity te = world.getTileEntity(neighborPos);
                if(te != null) {
                    CapabilityDestinationProvider cap = te.getCapability(CapabilityDestinationProvider.INSTANCE, null);
                    if(cap != null && cap.isCartApplicable(te, cart, destinationRegex)) {
                        for(int i = 0; i < 10; i++) {
                            double x = pos.getPos().getX() + world.rand.nextDouble();
                            double z = pos.getPos().getZ() + world.rand.nextDouble();
                            NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, x, pos.getPos().getY() + 1, z, dir.getFrontOffsetX(), dir.getFrontOffsetY(), dir.getFrontOffsetZ()), world);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public EnumNetworkObject getType(){
        return EnumNetworkObject.STATION;
    }
}
