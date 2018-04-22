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
import java.util.stream.Stream;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.minemaarten.signals.Signals;
import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.config.SignalsConfig;
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
import com.minemaarten.signals.rail.network.RailNetworkClient;
import com.minemaarten.signals.rail.network.RailPathfinder;
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
            CLIENT_INSTANCE = new RailNetworkManager(true);
        }
        return CLIENT_INSTANCE;
    }

    private static RailNetworkManager getServerInstance(){
        if(SERVER_INSTANCE == null) {
            SERVER_INSTANCE = new RailNetworkManager(false);
        }
        return SERVER_INSTANCE;
    }

    private final ExecutorService railNetworkExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("signals-network-thread-%d").build());
    private Future<RailNetwork<MCPos>> networkUpdateTask;
    private RailNetwork<MCPos> network;
    private MCNetworkState state = new MCNetworkState();
    private final NetworkUpdater<MCPos> networkUpdater = new NetworkUpdater<>(new NetworkObjectProvider());

    private RailNetworkManager(boolean client){
        if(client) {
            network = RailNetworkClient.empty();
        } else {
            network = RailNetwork.empty();
        }
    }

    private void validateOnServer(){
        if(this == CLIENT_INSTANCE) throw new IllegalStateException();
    }

    private void validateOnClient(){
        if(this == SERVER_INSTANCE) throw new IllegalStateException();
    }

    /**
     * The initial nodes used to build out the network from.
     * Signals, Station Markers, rail links. Only used when force rebuilding the network.
     * @return
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

    public void rebuildNetwork(){
        validateOnServer();

        NetworkHandler.sendToAll(new PacketClearNetwork());
        network = RailNetwork.empty();
        getStartNodes().forEach(r -> networkUpdater.markDirty(r.pos));
        initTrains();
        state.update(network);
    }

    private void initTrains(){
        List<EntityMinecart> carts = new ArrayList<>();

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

    public RailNetworkClient<MCPos> getClientNetwork(){
        return (RailNetworkClient<MCPos>)network;
    }

    public MCNetworkState getState(){
        return state;
    }

    public NetworkRail<MCPos> getRail(World world, BlockPos pos){
        return getRail(new MCPos(world, pos));
    }

    public NetworkRail<MCPos> getRail(MCPos pos){
        return network.railObjects.getRail(pos);
    }

    public void loadNetwork(RailNetwork<MCPos> network, MCNetworkState state){
        networkUpdateTask = null;
        /*state.getTrackingCartsFrom(this.state); // Take carts that were loaded before this network state was loaded from nbt.
        this.network = network;
        this.state = state;*/

        NetworkHandler.sendToAll(new PacketClearNetwork());
        /*for(PacketUpdateNetwork packet : getSplitNetworkUpdatePackets(network.railObjects.getAllNetworkObjects().values())) {
            NetworkHandler.sendToAll(packet);
        }

        for(Train<MCPos> train : state.getTrains().valueCollection()) {
            NetworkHandler.sendToAll(new PacketAddOrUpdateTrain((MCTrain)train));
        }*/
    }

    public MCTrain getTrainByID(int id){
        return (MCTrain)state.getTrain(id);
    }

    public Stream<MCTrain> getAllTrains(){
        return state.getTrains().valueCollection().stream().map(t -> (MCTrain)t);
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

    public RailRoute<MCPos> pathfind(MCPos start, Train<MCPos> train, Pattern destinationRegex, EnumHeading direction){
        return new RailPathfinder<MCPos>(network, state).pathfindToDestination(start, train, destinationRegex, direction);
    }

    public void markDirty(MCPos pos){
        validateOnServer();
        networkUpdater.markDirty(pos);
    }

    public void onPreServerTick(){
        if(!SignalsConfig.enableRailNetwork) return;
        Collection<NetworkObject<MCPos>> updates = networkUpdater.getNetworkUpdates(network);
        if(!updates.isEmpty()) {
            applyUpdates(updates);

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

                if(this == CLIENT_INSTANCE) {
                    //Asynchronously update the renderers
                    railNetworkExecutor.submit(() -> {
                        network.build(); //Build the network cache off thread
                        Signals.proxy.onRailNetworkUpdated();
                    });
                }

                state.onNetworkChanged(network);
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
        if(this == SERVER_INSTANCE || networkUpdateTask == null) {

            checkForNewNetwork(true);
            networkUpdateTask = railNetworkExecutor.submit(() -> networkUpdater.applyUpdates(getNetwork(), changedObjects).build());
        } else {
            //On the client, when the network was already updating, simply schedule the new update after the current one.
            final Future<RailNetwork<MCPos>> prevTask = networkUpdateTask;
            networkUpdateTask = railNetworkExecutor.submit(() -> {
                Log.info("Entering new network");
                return networkUpdater.applyUpdates(prevTask.get(), changedObjects);//Update from the previous update, and wait for this.
            });
        }
    }

    public void clearNetwork(){
        validateOnClient();
        network = RailNetworkClient.empty();
        state.setTrains(Collections.emptyList());
        Signals.proxy.onRailNetworkUpdated();
    }

    public void onPostServerTick(){
        if(!SignalsConfig.enableRailNetwork) return;
        validateOnServer();
        checkForNewNetwork(true);
        state.update(network);
        if(networkUpdater.didJustTurnBusy()) {
            notifyAllPlayers(new TextComponentTranslation("signals.message.signals_busy"));
        }
        if(networkUpdater.didJustTurnIdle()) {
            notifyAllPlayers(new TextComponentTranslation("signals.message.signals_idle"));
        }
    }

    private void notifyAllPlayers(ITextComponent text){
        for(EntityPlayer player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
            player.sendMessage(text);
        }
    }

    public void onPreClientTick(){
        if(!SignalsConfig.enableRailNetwork) return;
        validateOnClient();
        checkForNewNetwork(false);
        //Log.info("Trains: " + getAllTrains().count());
    }

    public void onPlayerJoin(EntityPlayerMP player){
        NetworkHandler.sendTo(new PacketClearNetwork(), player);
        for(PacketUpdateNetwork packet : getSplitNetworkUpdatePackets(network.railObjects.getAllNetworkObjects().values())) {
            NetworkHandler.sendTo(packet, player);
        }
        state.onPlayerJoin(player);
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
