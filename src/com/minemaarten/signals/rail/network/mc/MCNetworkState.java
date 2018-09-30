package com.minemaarten.signals.rail.network.mc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.Constants;

import com.google.common.collect.ImmutableSet;
import com.minemaarten.signals.api.access.ISignal.EnumForceMode;
import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.lib.StreamUtils;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketAddOrUpdateTrain;
import com.minemaarten.signals.network.PacketRemoveTrain;
import com.minemaarten.signals.network.PacketUpdateSignals;
import com.minemaarten.signals.network.PacketUpdateTrainPath;
import com.minemaarten.signals.rail.NetworkController;
import com.minemaarten.signals.rail.RailManager;
import com.minemaarten.signals.rail.network.NetworkSignal;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.Train;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;

public class MCNetworkState extends NetworkState<MCPos>{
    private final RailNetworkManager railNetworkManager;
    private Map<UUID, EntityMinecart> trackingMinecarts = new HashMap<>();
    private Map<UUID, MCTrain> cartIDsToTrains;

    public MCNetworkState(RailNetworkManager railNetworkManager){
        this.railNetworkManager = railNetworkManager;
    }

    public void onPlayerJoin(EntityPlayerMP player){
        for(Train<MCPos> train : getTrains()) {
            NetworkHandler.sendTo(new PacketAddOrUpdateTrain((MCTrain)train), player);
        }
        NetworkHandler.sendTo(new PacketUpdateSignals(signalToLampStatusses), player);
    }

    @Override
    protected void onCartRouted(Train<MCPos> train, RailRoute<MCPos> route){
        super.onCartRouted(train, route);
        NetworkHandler.sendToAll(new PacketUpdateTrainPath((MCTrain)train));
        ((MCTrain)train).getCarts().forEach(cart -> cart.timeUntilPortal = 0); //Carts that pass a signal can travel through portals immediately
    }

    @Override
    protected void onSignalsChanged(Map<MCPos, EnumLampStatus> changedSignals){
        super.onSignalsChanged(changedSignals);
        NetworkHandler.sendToAll(new PacketUpdateSignals(changedSignals));

        //Update the signals in the world.
        for(Map.Entry<MCPos, EnumLampStatus> entry : changedSignals.entrySet()) {
            MCPos mcPos = entry.getKey();
            World world = mcPos.getWorld();
            if(world != null && world.isBlockLoaded(mcPos.getPos())) {
                TileEntity te = world.getTileEntity(mcPos.getPos());
                if(te instanceof TileEntitySignalBase) {
                    ((TileEntitySignalBase)te).setLampStatus(entry.getValue());
                } else { //If there's no signal block, something's wrong
                    RailNetworkManager.getInstance(world.isRemote).markDirty(mcPos);
                }
            }
        }
    }

    public void setSignalStatusses(Map<MCPos, EnumLampStatus> statusses){
        for(Map.Entry<MCPos, EnumLampStatus> entry : statusses.entrySet()) {
            setLampStatus(entry.getKey(), entry.getValue());
        }
    }

    @Override
    protected void setLampStatus(MCPos signalPos, EnumLampStatus status){
        super.setLampStatus(signalPos, status);
        NetworkController.getInstance(signalPos.getDimID()).updateColor(status.color, signalPos.getPos());
    }

    @Override
    protected void onForceModeChanged(MCPos signalPos, EnumForceMode forceStatus){
        super.onForceModeChanged(signalPos, forceStatus);
        TileEntity te = signalPos.getLoadedTileEntity();
        if(te instanceof TileEntitySignalBase) {
            ((TileEntitySignalBase)te).setForceMode(forceStatus);
        }
    }

    public boolean setForceMode(RailNetwork<MCPos> network, int dimID, int x, int z, EnumForceMode forceMode){
        NetworkSignal<MCPos> signal = network.railObjects.getSignals().stream().filter(s -> s.getPos().getDimID() == dimID && s.getPos().getX() == x && s.getPos().getZ() == z).findFirst().orElse(null);
        if(signal != null) {
            setForceMode(network, signal.getPos(), forceMode);
            return true;
        } else {
            return false;
        }
    }

    public EntityMinecart getCart(UUID uniqueID){
        return trackingMinecarts.get(uniqueID);
    }

    public MCTrain getTrain(UUID cartID){
        if(cartIDsToTrains == null) {
            cartIDsToTrains = new HashMap<>();
            for(Train<MCPos> train : getTrains()) {
                MCTrain mcTrain = (MCTrain)train;
                for(UUID c : mcTrain.cartIDs) {
                    cartIDsToTrains.put(c, mcTrain);
                }
            }
        }
        return cartIDsToTrains.get(cartID);
    }

    public void getTrackingCartsFrom(MCNetworkState state){
        for(EntityMinecart cart : state.trackingMinecarts.values()) {
            onMinecartJoinedWorld(cart);
        }
    }

    public void onMinecartJoinedWorld(EntityMinecart cart){
        MCTrain train = findTrainForCartID(cart.getUniqueID());

        //Override any previous records, automatically resolving dimension changes of entities, where the entity in the next dimension
        //is added, before the entity in the previous dimension is noticed to be removed.
        EntityMinecart oldCart = trackingMinecarts.put(cart.getUniqueID(), cart);
        if(oldCart != null && train != null) train.onCartRemoved(oldCart);

        if(train == null) { //If the added cart does not belong to a train yet
            addTrain(ImmutableSet.of(cart.getUniqueID()));
        } else {
            train.onCartAdded(cart);
        }
    }

    private MCTrain addTrain(Collection<EntityMinecart> carts){
        return addTrain(carts.stream().map(c -> c.getUniqueID()).collect(ImmutableSet.toImmutableSet()));
    }

    private MCTrain addTrain(ImmutableSet<UUID> uuids){
        MCTrain train = new MCTrain(railNetworkManager, uuids);
        addTrain(train);
        cartIDsToTrains = null;
        NetworkHandler.sendToAll(new PacketAddOrUpdateTrain(train));
        return train;
    }

    public void removeTrain(int id){
        Train<MCPos> train = getTrain(id);
        if(train != null) {
            removeTrain(train);
            cartIDsToTrains = null;
        }
    }

    @Override
    public void setTrains(Collection<? extends Train<MCPos>> trains){
        super.setTrains(trains);
        cartIDsToTrains = null;
    }

    @Override
    public void removeTrain(Train<MCPos> train){
        super.removeTrain(train);
        NetworkHandler.sendToAll(new PacketRemoveTrain((MCTrain)train));
    }

    public void onChunkUnload(Chunk chunk){
        for(ClassInheritanceMultiMap<Entity> entities : chunk.getEntityLists()) {
            for(EntityMinecart cart : entities.getByClass(EntityMinecart.class)) {
                trackingMinecarts.remove(cart.getUniqueID()); //Remove without changing the Trains, as unloaded != removed.
                getTrains().forEach(t -> ((MCTrain)t).onCartRemoved(cart));
                //MCTrain train = findTrainForCartID(cart.getUniqueID());
                //if(train != null) train.onCartRemoved(cart);
            }
        }
    }

    @Override
    public void update(RailNetwork<MCPos> network){
        removeDeadMinecarts();
        splitUngroupedCarts();
        mergeGroupedCarts();
        super.update(network);
    }

    private void removeDeadMinecarts(){
        Iterator<EntityMinecart> iterator = trackingMinecarts.values().iterator();
        while(iterator.hasNext()) {
            EntityMinecart cart = iterator.next();
            if(cart.isDead) {
                iterator.remove();
                onCartKilled(cart);
            }
        }
    }

    private void splitUngroupedCarts(){
        List<MCTrain> trainsWithMultipleCarts = getTrainStream().map(t -> (MCTrain)t).filter(t -> t.cartIDs.size() > 1).collect(Collectors.toList());
        for(MCTrain train : trainsWithMultipleCarts) {
            List<EntityMinecart> carts = getLoadedMinecarts(train.cartIDs).collect(Collectors.toList());
            if(!carts.isEmpty()) {
                List<List<EntityMinecart>> cartGroups = new ArrayList<>(1);
                for(EntityMinecart cart : carts) {
                    addToGroup(cartGroups, cart);
                }

                //If we need to split
                if(cartGroups.size() > 1) {
                    removeTrain(train); //Remove the original train, including any unloaded minecart references.
                    for(List<EntityMinecart> cartsInGroup : cartGroups) {
                        MCTrain newTrain = addTrain(cartsInGroup);
                        //TODO copy destination capability from old train
                    }
                }
            }
        }
    }

    private void addToGroup(List<List<EntityMinecart>> cartGroups, EntityMinecart cart){
        for(List<EntityMinecart> group : cartGroups) {
            if(RailManager.getInstance().areLinked(cart, group.get(0))) {
                group.add(cart);
                return;
            }
        }

        List<EntityMinecart> newGroup = new ArrayList<>();
        newGroup.add(cart);
        cartGroups.add(newGroup);
    }

    private void mergeGroupedCarts(){
        List<MCTrain> traversedTrains = new ArrayList<>();
        for(Entry<UUID, EntityMinecart> entry : trackingMinecarts.entrySet()) {
            MCTrain train = getTrain(entry.getKey());
            if(train != null) {
                MCTrain matching = getMatchingTrain(traversedTrains, entry.getValue());
                if(matching != null) {
                    //Merge
                    removeTrain(train);
                    matching.addCartIDs(train.cartIDs);
                    cartIDsToTrains = null;
                    NetworkHandler.sendToAll(new PacketAddOrUpdateTrain(matching));
                } else {
                    traversedTrains.add(train);
                }
            }
        }
    }

    private MCTrain getMatchingTrain(List<MCTrain> trains, EntityMinecart cart){
        for(MCTrain train : trains) {
            EntityMinecart c = getLoadedMinecarts(train.cartIDs).findFirst().orElse(null);
            if(c != null && RailManager.getInstance().areLinked(c, cart)) {
                return train;
            }
        }
        return null;
    }

    public Stream<EntityMinecart> getLoadedMinecarts(Collection<UUID> uuids){
        return StreamUtils.ofType(EntityMinecart.class, uuids.stream().map(id -> trackingMinecarts.get(id)));
    }

    public void onCartKilled(EntityMinecart cart){
        MCTrain train = findTrainForCartID(cart.getUniqueID());
        if(train != null) {
            cartIDsToTrains = null;
            if(train.cartIDs.size() == 1) { //When it's the last cart
                removeTrain(train);
            } else { //When it's a train consisting of multiple carts, remove the cart from the (still existing) train.
                train.onCartRemoved(cart);
                train.cartIDs = train.cartIDs.stream().filter(uuid -> !uuid.equals(cart.getUniqueID())).collect(ImmutableSet.toImmutableSet());
                NetworkHandler.sendToAll(new PacketAddOrUpdateTrain(train));
            }
        }
    }

    private MCTrain findTrainForCartID(UUID uuid){
        return getTrainStream().map(t -> (MCTrain)t).filter(t -> t.cartIDs.contains(uuid)).findFirst().orElse(null);
    }

    public void writeToNBT(NBTTagCompound tag){
        NBTTagList trainList = new NBTTagList();
        for(Train<MCPos> train : getTrains()) {
            NBTTagCompound t = new NBTTagCompound();
            ((MCTrain)train).writeToNBT(t);
            trainList.appendTag(t);
        }
        tag.setTag("trains", trainList);

        NBTTagList forcedSignals = new NBTTagList();
        for(Map.Entry<MCPos, EnumForceMode> entry : signalForces.entrySet()) {
            NBTTagCompound t = new NBTTagCompound();
            entry.getKey().writeToNBT(t);
            t.setByte("f", (byte)entry.getValue().ordinal());
            forcedSignals.appendTag(t);
        }
        tag.setTag("forcedSignals", forcedSignals);
    }

    public static MCNetworkState fromNBT(RailNetworkManager railNetworkManager, NBTTagCompound tag){
        NBTTagList trainList = tag.getTagList("trains", Constants.NBT.TAG_COMPOUND);
        List<MCTrain> trains = new ArrayList<>();

        for(int i = 0; i < trainList.tagCount(); i++) {
            trains.add(MCTrain.fromNBT(railNetworkManager, trainList.getCompoundTagAt(i)));
        }

        Map<MCPos, EnumForceMode> forcedSignalMap = new HashMap<>();
        NBTTagList forcedSignals = tag.getTagList("forcedSignals", Constants.NBT.TAG_COMPOUND);
        for(int i = 0; i < forcedSignals.tagCount(); i++) {
            NBTTagCompound t = forcedSignals.getCompoundTagAt(i);
            forcedSignalMap.put(new MCPos(t), EnumForceMode.values()[t.getByte("f")]);
        }

        MCNetworkState state = new MCNetworkState(railNetworkManager);
        state.signalForces = forcedSignalMap;
        state.setTrains(trains);
        return state;
    }
}
