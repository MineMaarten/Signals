package com.minemaarten.signals.rail.network.mc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.minemaarten.signals.Signals;
import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketAddOrUpdateTrain;
import com.minemaarten.signals.network.PacketClearNetwork;
import com.minemaarten.signals.network.PacketUpdateNetwork;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.NetworkUpdater;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.Train;
import com.minemaarten.signals.tileentity.TileEntityBase;

public class RailNetworkManager{

    private static RailNetworkManager CLIENT_INSTANCE;
    private static RailNetworkManager SERVER_INSTANCE;

    public static RailNetworkManager getInstance(){
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT ? getClientInstance() : getServerInstance();
    }

    private static RailNetworkManager getClientInstance(){
        if(CLIENT_INSTANCE == null) {
            CLIENT_INSTANCE = new RailNetworkManager();
        }
        return CLIENT_INSTANCE;
    }

    private static RailNetworkManager getServerInstance(){
        if(SERVER_INSTANCE == null) {
            SERVER_INSTANCE = new RailNetworkManager();
        }
        return SERVER_INSTANCE;
    }

    private final ExecutorService railNetworkExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("signals-network-thread-%d").build());
    private Future<RailNetwork<MCPos>> networkUpdateTask;
    private RailNetwork<MCPos> network = new RailNetwork<MCPos>(ImmutableMap.of());
    private MCNetworkState state = new MCNetworkState();
    private final NetworkUpdater<MCPos> networkUpdater = new NetworkUpdater<>(new NetworkObjectProvider());

    private RailNetworkManager(){

    }

    private void validateOnServer(){
        if(this == CLIENT_INSTANCE) throw new IllegalStateException();
    }

    private void validateOnClient(){
        if(this == SERVER_INSTANCE) throw new IllegalStateException();
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
        state.update(network);
    }

    private void initializeNetwork(){

        NetworkObjectProvider objProvider = new NetworkObjectProvider();
        Set<NetworkRail<MCPos>> railsToTraverse = getStartNodes();
        Set<NetworkObject<MCPos>> allNetworkObjects = new HashSet<>(railsToTraverse);
        long milli = System.currentTimeMillis();
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
        Log.info("Retrieving mc objects:" + (System.currentTimeMillis() - milli) + "ms");
        NetworkHandler.sendToAll(new PacketClearNetwork());
        for(PacketUpdateNetwork packet : getSplitNetworkUpdatePackets(network.railObjects.getAllNetworkObjects().values())) {
            NetworkHandler.sendToAll(packet);
        }
        milli = System.currentTimeMillis();
        network = new RailNetwork<MCPos>(allNetworkObjects);
        Log.info("Building network:" + (System.currentTimeMillis() - milli) + "ms");
        initTrains();
    }

    private void initTrains(){
        List<EntityMinecart> carts = new ArrayList<>();

        //TODO manage own list
        for(World world : DimensionManager.getWorlds()) {
            for(Entity entity : world.loadedEntityList) {
                if(entity instanceof EntityMinecart) {
                    carts.add((EntityMinecart)entity);
                }
            }
        }

        Set<MCTrain> trains = new NetworkObjectProvider().provideTrains(carts);
        state.setTrains(trains);
        for(MCTrain train : trains) {
            NetworkHandler.sendToAll(new PacketAddOrUpdateTrain(train));
        }
    }

    public RailNetwork<MCPos> getNetwork(){
        return network;
    }

    public MCNetworkState getState(){
        return state;
    }

    public void loadNetwork(RailNetwork<MCPos> network, MCNetworkState state){
        networkUpdateTask = null;
        this.network = network;
        this.state = state;

        NetworkHandler.sendToAll(new PacketClearNetwork());
        for(PacketUpdateNetwork packet : getSplitNetworkUpdatePackets(network.railObjects.getAllNetworkObjects().values())) {
            NetworkHandler.sendToAll(packet);
        }

        for(Train<MCPos> train : state.getTrains().valueCollection()) {
            NetworkHandler.sendToAll(new PacketAddOrUpdateTrain((MCTrain)train));
        }
    }

    public MCTrain getTrainByID(int id){
        return (MCTrain)state.getTrain(id);
    }

    public Iterable<MCTrain> getAllTrains(){
        return state.getTrains().valueCollection().stream().map(t -> (MCTrain)t).collect(Collectors.toList());
    }

    public void addTrain(MCTrain train){
        if(this == SERVER_INSTANCE) {
            NetworkHandler.sendToAll(new PacketAddOrUpdateTrain(train));
        }
        state.getTrains().put(train.id, train);
    }

    public void removeTrain(int trainID){
        state.getTrains().remove(trainID);
    }

    public EnumLampStatus getLampStatus(World world, BlockPos pos){
        return state.getLampStatus(new MCPos(world, pos));
    }

    public RailRoute<MCPos> pathfind(MCPos start, EntityMinecart cart, Pattern destinationRegex, EnumHeading direction){
        return new MCRailPathfinder(network, state).pathfindToDestination(start, cart, destinationRegex, direction);
    }

    public void markDirty(MCPos pos){
        validateOnServer();
        networkUpdater.markDirty(pos);
    }

    public void onPreServerTick(){
        Collection<NetworkObject<MCPos>> updates = networkUpdater.getNetworkUpdates(network);
        if(!updates.isEmpty()) {
            applyUpdates(updates);
            for(NetworkObject<MCPos> obj : updates) {
                //TODO remove
                // NetworkHandler.sendToAll(new PacketSpawnParticle(EnumParticleTypes.REDSTONE, obj.pos.getX() + 0.5, obj.pos.getY() + 0.5, obj.pos.getZ() + 0.5, 0, 0, 0));
            }

            for(PacketUpdateNetwork packet : getSplitNetworkUpdatePackets(updates)) {
                NetworkHandler.sendToAll(packet);
            }
        }
    }

    public void checkForNewNetwork(boolean forceWait){
        if(networkUpdateTask != null && (forceWait || networkUpdateTask.isDone())) {
            try {
                network = networkUpdateTask.get();
                networkUpdateTask = null;
                NetworkStorage.getInstance().setNetwork(network);

                Signals.proxy.onRailNetworkUpdated();
            } catch(InterruptedException e) {
                e.printStackTrace();
            } catch(ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Asynchronously calculates the new rail network.
     * This is possible because no MC interaction, and immutable objects.
     * @param changedObjects
     */
    public void applyUpdates(Collection<NetworkObject<MCPos>> changedObjects){
        checkForNewNetwork(true);
        networkUpdateTask = railNetworkExecutor.submit(() -> networkUpdater.applyUpdates(network, changedObjects));
    }

    public void clearNetwork(){
        network = new RailNetwork<>(Collections.emptyList());
        state.setTrains(Collections.emptyList());
        Signals.proxy.onRailNetworkUpdated();
    }

    public void onPostServerTick(){
        validateOnServer();
        checkForNewNetwork(true);
        state.update(network);
    }

    public void onPreClientTick(){
        validateOnClient();
        checkForNewNetwork(false);
    }

    public void onPlayerJoin(EntityPlayerMP player){
        NetworkHandler.sendTo(new PacketClearNetwork(), player);
        for(PacketUpdateNetwork packet : getSplitNetworkUpdatePackets(network.railObjects.getAllNetworkObjects().values())) {
            NetworkHandler.sendTo(packet, player);
        }
        for(Train<MCPos> train : state.getTrains().valueCollection()) {
            NetworkHandler.sendTo(new PacketAddOrUpdateTrain((MCTrain)train), player);
        }
    }

    public void onChunkUnload(Chunk chunk){
        state.onChunkUnload(chunk);
    }

    public void onMinecartJoinedWorld(EntityMinecart cart){
        state.onMinecartJoinedWorld(cart);
    }

    private static final int MAX_CHANGES_PER_PACKET = 1000;

    private List<PacketUpdateNetwork> getSplitNetworkUpdatePackets(Collection<NetworkObject<MCPos>> allChangedObjects){
        if(allChangedObjects.size() <= MAX_CHANGES_PER_PACKET) return Collections.singletonList(new PacketUpdateNetwork(allChangedObjects));

        List<PacketUpdateNetwork> packets = new ArrayList<>();
        Iterator<NetworkObject<MCPos>> iterator = allChangedObjects.iterator();
        List<NetworkObject<MCPos>> changedObjects = new ArrayList<>(MAX_CHANGES_PER_PACKET);

        while(iterator.hasNext()) {
            changedObjects.add(iterator.next());
            if(changedObjects.size() >= MAX_CHANGES_PER_PACKET) {
                packets.add(new PacketUpdateNetwork(changedObjects));
                changedObjects = new ArrayList<>(MAX_CHANGES_PER_PACKET);
            }
        }

        if(!changedObjects.isEmpty()) packets.add(new PacketUpdateNetwork(changedObjects));
        return packets;
    }
}
