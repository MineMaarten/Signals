package com.minemaarten.signals.rail.network.mc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.tileentity.TileEntityBase;

public class RailNetworkManager{

    private static final RailNetworkManager CLIENT_INSTANCE = new RailNetworkManager();
    private static final RailNetworkManager SERVER_INSTANCE = new RailNetworkManager();

    public static RailNetworkManager getInstance(){
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT ? CLIENT_INSTANCE : SERVER_INSTANCE;
    }

    private MCRailNetwork network = new MCRailNetwork(Collections.emptyMap());
    private NetworkState<MCPos> state = new NetworkState<>(Collections.emptySet());
    private final Set<MCPos> dirtyPositions = new HashSet<>(); //Positions that have possibly changed

    private RailNetworkManager(){

    }

    private void validateOnServer(){
        if(this == CLIENT_INSTANCE) throw new IllegalStateException();
    }

    /**
     * The initial nodes used to build out the network from.
     * Signals, Station Markers, rail links
     * @return
     * TODO remove after testing
     */
    private Set<NetworkRail<MCPos>> getStartNodes(){
        Set<NetworkRail<MCPos>> nodes = new HashSet<>();
        for(World world : DimensionManager.getWorlds()) {
            for(TileEntity te : world.loadedTileEntityList) {
                if(te instanceof TileEntityBase) { //Any Signals TE for testing purposes
                    for(EnumFacing facing : EnumFacing.HORIZONTALS) {
                        BlockPos pos = te.getPos().offset(facing);
                        NetworkObject<MCPos> networkObject = new NetworkObjectProvider().provide(world, pos);
                        if(networkObject instanceof MCNetworkRail) {
                            nodes.add((MCNetworkRail)networkObject);
                        }
                    }
                }
            }
        }
        return nodes;
    }

    public void initialize(){
        validateOnServer();
        initializeNetwork();
        updateState();
    }

    private void initializeNetwork(){

        NetworkObjectProvider objProvider = new NetworkObjectProvider();
        Set<NetworkRail<MCPos>> railsToTraverse = getStartNodes();
        Set<NetworkObject<MCPos>> allNetworkObjects = new HashSet<>(railsToTraverse);
        while(!railsToTraverse.isEmpty()) {
            Iterator<NetworkRail<MCPos>> iterator = railsToTraverse.iterator();
            NetworkRail<MCPos> curRail = iterator.next();
            iterator.remove();

            for(MCPos neighborPos : curRail.getPotentialNeighborRailLocations()) {
                NetworkObject<MCPos> neighbor = objProvider.provide(neighborPos);
                if(neighbor != null && allNetworkObjects.add(neighbor) && neighbor instanceof NetworkRail) {
                    railsToTraverse.add((NetworkRail<MCPos>)neighbor);
                }
            }
        }
        network = new MCRailNetwork(allNetworkObjects);
    }

    private void updateState(){
        List<EntityMinecart> carts = new ArrayList<>();

        for(World world : DimensionManager.getWorlds()) {
            for(Entity entity : world.loadedEntityList) {
                if(entity instanceof EntityMinecart) {
                    carts.add((EntityMinecart)entity);
                }
            }
        }

        state = new NetworkState<>(new NetworkObjectProvider().provideTrains(carts));
        state.updateSignalStatusses(network);
    }

    /* public RailNetwork<MCPos> getNetwork(){
         return network;
     }*/
    public EnumLampStatus getLampStatus(World world, BlockPos pos){
        return state.getLampStatus(new MCPos(world, pos));
    }

    public void markDirty(MCPos pos){
        validateOnServer();
        dirtyPositions.add(pos);
    }

    //TODO threading?
    public void onPreServerTick(){
        if(!dirtyPositions.isEmpty()) {
            updateNetwork();
            dirtyPositions.clear();
        }
    }

    /**
     * Updates the network, using the positions that have been reported dirty.
     * 1. positions already in the network marked dirty get re-acquired.
     * 2. Neighbors of the positions marked dirty get re-acquired, and possibly cause a recursive look-up. For example, a rail section that wasn't part of the network before now may, because of a gap being filled in with a new rail
     */
    private void updateNetwork(){
        Map<MCPos, NetworkObject<MCPos>> allObjects = new HashMap<>(network.railObjects.getAllNetworkObjects());

        //Remove all existing objects that were marked dirty.
        for(MCPos dirtyPos : dirtyPositions) {
            allObjects.remove(dirtyPos);
        }

        //Re-acquire positions that were marked dirty, and possibly recursively look up other parts.
        NetworkObjectProvider objProvider = new NetworkObjectProvider();
        Stack<MCPos> toEvaluate = new Stack<>();
        dirtyPositions.forEach(pos -> toEvaluate.push(pos));
        while(!toEvaluate.isEmpty()) {
            MCPos curPos = toEvaluate.pop();

            if(!allObjects.containsKey(curPos)) {
                NetworkObject<MCPos> networkObject = objProvider.provide(curPos);
                if(networkObject != null) {
                    allObjects.put(curPos, networkObject);

                    if(networkObject instanceof NetworkRail) {
                        for(MCPos neighborPos : ((NetworkRail<MCPos>)networkObject).getPotentialNeighborRailLocations()) {
                            toEvaluate.push(neighborPos);
                        }
                    }
                }
            }
        }

        network = new MCRailNetwork(allObjects);
    }

    public void onPostServerTick(){
        validateOnServer();
        updateState();
    }
}
