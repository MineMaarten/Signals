package com.minemaarten.signals.rail.network.mc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.minemaarten.signals.lib.Constants;
import com.minemaarten.signals.rail.network.RailNetwork;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class NetworkStorage extends WorldSavedData{

    public static final String DATA_KEY = "SignalsRailNetwork";
    public static World overworld;
    private RailNetwork<MCPos> network;
    private MCNetworkState state;
    private final boolean clientSide;

    public NetworkStorage(String name){
        this(false, name);
    }

    public NetworkStorage(boolean clientSide, String name){
        super(name);
        this.clientSide = clientSide;
        network = RailNetworkManager.getInstance(clientSide).getNetwork();
        state = RailNetworkManager.getInstance(clientSide).getState();
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event){
        if(!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            overworld = event.getWorld();
            overworld.loadData(NetworkStorage.class, DATA_KEY);
        }
    }

    public static NetworkStorage getInstance(boolean clientSide){
        if(clientSide) return new NetworkStorage(clientSide, DATA_KEY);

        if(overworld != null) {
            NetworkStorage manager = (NetworkStorage)overworld.loadData(NetworkStorage.class, DATA_KEY);
            if(manager == null) {
                manager = new NetworkStorage(clientSide, DATA_KEY);
                overworld.setData(DATA_KEY, manager);
            }
            return manager;
        } else {
            throw new IllegalStateException("Overworld not initialized");
        }
    }

    public void setNetwork(RailNetwork<MCPos> network){
        this.network = network;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag){
        network = new NetworkSerializer().loadNetworkFromTag(tag);
        state = MCNetworkState.fromNBT(RailNetworkManager.getInstance(clientSide), tag);
        RailNetworkManager.getInstance(clientSide).loadNetwork(network, state);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
        new NetworkSerializer().writeToTag(network, tag);
        state.writeToNBT(tag);
        return tag;
    }
}
