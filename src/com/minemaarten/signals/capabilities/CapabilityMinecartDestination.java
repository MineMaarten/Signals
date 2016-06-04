package com.minemaarten.signals.capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.tileentity.IGUITextFieldSensitive;

public class CapabilityMinecartDestination implements IGUITextFieldSensitive{
    @CapabilityInject(CapabilityMinecartDestination.class)
    public static Capability<CapabilityMinecartDestination> INSTANCE;

    @GuiSynced
    public String destinationStations = ""; //'\n' separated list of destinations
    @GuiSynced
    private int curDestinationIndex;

    private AStarRailNode curPath;
    private List<BlockPos> nbtLoadedPath;

    public static void register(){
        CapabilityManager.INSTANCE.register(CapabilityMinecartDestination.class, new Capability.IStorage<CapabilityMinecartDestination>(){
            @Override
            public NBTBase writeNBT(Capability<CapabilityMinecartDestination> capability, CapabilityMinecartDestination instance, EnumFacing side){
                NBTTagCompound tag = new NBTTagCompound();

                tag.setString("destinations", instance.destinationStations);
                tag.setInteger("destIndex", instance.curDestinationIndex);

                if(instance.curPath != null) {
                    AStarRailNode curNode = instance.curPath;
                    NBTTagList nodeList = new NBTTagList();
                    while(curNode != null) {
                        NBTTagCompound nodeTag = new NBTTagCompound();
                        nodeTag.setInteger("x", curNode.getRail().getX());
                        nodeTag.setInteger("y", curNode.getRail().getY());
                        nodeTag.setInteger("z", curNode.getRail().getZ());
                        nodeList.appendTag(nodeTag);

                        curNode = curNode.getNextNode();
                    }
                    tag.setTag("path", nodeList);
                }
                return tag;
            }

            @Override
            public void readNBT(Capability<CapabilityMinecartDestination> capability, CapabilityMinecartDestination instance, EnumFacing side, NBTBase base){
                NBTTagCompound tag = (NBTTagCompound)base;

                instance.destinationStations = tag.getString("destinations");
                instance.curDestinationIndex = tag.getInteger("destIndex");

                if(tag.hasKey("path")) {
                    instance.nbtLoadedPath = new ArrayList<BlockPos>();
                    NBTTagList nodeList = tag.getTagList("path", 10);
                    for(int i = 0; i < nodeList.tagCount(); i++) {
                        NBTTagCompound nodeTag = nodeList.getCompoundTagAt(i);
                        instance.nbtLoadedPath.add(new BlockPos(nodeTag.getInteger("x"), nodeTag.getInteger("y"), nodeTag.getInteger("z")));
                    }
                } else {
                    instance.curPath = null;
                }
            }
        }, new Callable<CapabilityMinecartDestination>(){
            @Override
            public CapabilityMinecartDestination call() throws Exception{
                return new CapabilityMinecartDestination();
            }
        });
    }

    @Override
    public void setText(int textFieldID, String text){
        destinationStations = text;
    }

    @Override
    public String getText(int textFieldID){
        return destinationStations;
    }

    public String getDestination(int index){
        return getDestinations()[index];
    }

    private String[] getDestinations(){
        return destinationStations.equals("") ? new String[0] : destinationStations.split("\n");
    }

    public int getTotalDestinations(){
        return getDestinations().length;
    }

    public String getCurrentDestination(){
        String[] destinations = getDestinations();
        if(curDestinationIndex >= destinations.length || curDestinationIndex == -1) nextDestination();
        return curDestinationIndex >= 0 ? destinations[curDestinationIndex] : "";
    }

    public int getDestinationIndex(){
        return curDestinationIndex;
    }

    public void nextDestination(){
        String[] destinations = getDestinations();
        if(++curDestinationIndex >= destinations.length) {
            curDestinationIndex = destinations.length > 0 ? 0 : -1;
        }
    }

    public void setPath(AStarRailNode path){
        curPath = path;
        nbtLoadedPath = null;
    }

    public AStarRailNode getPath(World world){
        if(nbtLoadedPath != null) {
            AStarRailNode prevNode = null;
            for(int i = nbtLoadedPath.size() - 1; i >= 0; i--) {
                AStarRailNode curNode = new AStarRailNode(RailCacheManager.getInstance(world).getRail(world, nbtLoadedPath.get(i)), null, null);
                if(prevNode != null) curNode.checkImprovementAndUpdate(prevNode);
                prevNode = curNode;
            }
            curPath = prevNode;
            nbtLoadedPath = null;
        }
        return curPath;
    }

    public static class Provider implements ICapabilitySerializable<NBTBase>{
        private final CapabilityMinecartDestination cap = new CapabilityMinecartDestination();

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing){
            return capability == INSTANCE;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing){
            if(hasCapability(capability, facing)) {
                return (T)cap;
            } else {
                return null;
            }
        }

        @Override
        public NBTBase serializeNBT(){
            return INSTANCE.getStorage().writeNBT(INSTANCE, cap, null);
        }

        @Override
        public void deserializeNBT(NBTBase nbt){
            INSTANCE.getStorage().readNBT(INSTANCE, cap, null, nbt);
        }
    }
}
