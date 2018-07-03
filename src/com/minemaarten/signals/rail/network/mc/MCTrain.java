package com.minemaarten.signals.rail.network.mc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketAddOrUpdateTrain;
import com.minemaarten.signals.network.PacketUpdateMessage;
import com.minemaarten.signals.rail.RailManager;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.RailRoute.EnumRouteResult;
import com.minemaarten.signals.rail.network.RailRoute.RailRouteNode;
import com.minemaarten.signals.rail.network.RailRoute.RailRouteResult;
import com.minemaarten.signals.rail.network.Train;

public class MCTrain extends Train<MCPos>{

    private static Map<EnumSet<EnumHeading>, EnumRailDirection> DIRS_TO_RAIL_DIR = new HashMap<>(6);

    static {
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumHeading.NORTH, EnumHeading.SOUTH), EnumRailDirection.NORTH_SOUTH);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumHeading.EAST, EnumHeading.WEST), EnumRailDirection.EAST_WEST);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumHeading.NORTH, EnumHeading.EAST), EnumRailDirection.NORTH_EAST);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumHeading.EAST, EnumHeading.SOUTH), EnumRailDirection.SOUTH_EAST);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumHeading.SOUTH, EnumHeading.WEST), EnumRailDirection.SOUTH_WEST);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumHeading.WEST, EnumHeading.NORTH), EnumRailDirection.NORTH_WEST);
    }

    public ImmutableSet<UUID> cartIDs;

    protected MCTrain(int id, ImmutableSet<UUID> cartIDs){
        super(id);
        this.cartIDs = cartIDs;
    }

    public MCTrain(ImmutableSet<UUID> cartIDs){
        super();
        this.cartIDs = cartIDs;
    }

    public MCTrain(List<EntityMinecart> carts){
        this(carts.stream().map(c -> c.getUniqueID()).collect(ImmutableSet.toImmutableSet()));
    }

    public List<EntityMinecart> getCarts(){
        return cartIDs.stream().map(id -> RailNetworkManager.getInstance().getState().getCart(id)).filter(Predicates.notNull()).collect(Collectors.toList());
    }

    public void addCartIDs(Collection<UUID> ids){
        cartIDs = Streams.concat(cartIDs.stream(), ids.stream()).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public boolean updatePositions(NetworkState<MCPos> state){
        super.updatePositions(state);
        ImmutableSet<MCPos> positions = ImmutableSet.copyOf(getCarts().stream().map(c -> new MCPos(c.world, c.getPosition())).collect(Collectors.toSet()));
        if(!positions.isEmpty()) { //Update if any cart is loaded, currently.
            return setPositions(RailNetworkManager.getInstance().getNetwork(), state, positions);
        } else {
            return false;
        }
    }

    @Override
    protected void onPositionChanged(RailNetwork<MCPos> network, NetworkState<MCPos> state){
        super.onPositionChanged(network, state);
        NetworkHandler.sendToAll(new PacketAddOrUpdateTrain(this));
        NetworkStorage.getInstance().markDirty();
    }

    @Override
    public RailRoute<MCPos> pathfind(MCPos start, EnumHeading dir){
        RailRoute<MCPos> path = null;
        for(EntityMinecart cart : getCarts()) {
            path = pathfind(cart, start, dir);
            if(path != null) break;
        }
        return path;
    }

    private RailRoute<MCPos> pathfind(EntityMinecart cart, MCPos start, EnumHeading dir){
        CapabilityMinecartDestination capability = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
        int startDestinationIndex = capability.getDestinationIndex();
        boolean firstTry = true;
        if(startDestinationIndex != -1) {
            while(true) {
                String destination = capability.getCurrentDestination();
                Pattern destinationRegex = capability.getCurrentDestinationRegex();
                List<PacketUpdateMessage> messages = new ArrayList<>();
                if(!firstTry && capability.getDestinationIndex() == startDestinationIndex) {
                    return null; //Prevent stack overflows when encountering only invalid stations
                }
                firstTry = false;

                //TODO messages.add(new PacketUpdateMessage(this, cart, "signals.message.routing_cart", destination));

                //Strategy to skip destinations that have no matching station (like ITEM routing without having items)
                RailRouteResult<MCPos> routeResult = RailNetworkManager.getInstance().pathfind(start, this, destinationRegex, dir);
                if(routeResult.routeResult == EnumRouteResult.NO_PATH) {
                    return null; //No path is no route
                } else if(routeResult.routeResult == EnumRouteResult.SUCCESS) {
                    return routeResult.railRoute;
                } else { //EnumRouteResult.NO_STATIONS
                    capability.nextDestination(); //Skip this destination
                    if(capability.getDestinationIndex() == startDestinationIndex) {
                        return null;
                    }
                }
            }
        } else {
            //      messages.add(new PacketUpdateMessage(this, cart, "signals.message.no_destination"));
            return null;
        }

        /*  if(submitMessages) {
              for(PacketUpdateMessage message : messages) {
                  NetworkHandler.sendToAllAround(message, getWorld());
              }
          }*/
        // capability.setPath(cart, path);
    }

    @Override
    protected void updateIntersection(RailRouteNode<MCPos> routeNode){
        if(routeNode.isValid()) {
            World world = routeNode.pos.getWorld();
            if(world != null) {
                BlockPos pos = routeNode.pos.getPos();
                IBlockState state = world.getBlockState(pos);
                IRail rail = RailManager.getInstance().getRail(world, pos, state);

                List<PacketUpdateMessage> messages = new ArrayList<>();
                EnumRailDirection requiredDir = DIRS_TO_RAIL_DIR.get(EnumSet.of(routeNode.dirIn, routeNode.dirOut));
                if(requiredDir != null) {
                    if(rail.getValidDirections(world, pos, state).contains(requiredDir)) {
                        rail.setDirection(world, pos, state, requiredDir);
                        String[] args = {Integer.toString(pos.getX()), Integer.toString(pos.getY()), Integer.toString(pos.getZ()), "signals.dir." + routeNode.dirIn.toString().toLowerCase(), "signals.dir." + routeNode.dirOut.toString().toLowerCase()};
                        //TODO  messages.add(new PacketUpdateMessage(this, cart, "signals.message.changing_junction", args));
                    } else {
                        Log.warning("Rail with state " + state + " does not allow setting dir " + requiredDir);
                    }
                } else {
                    Log.warning("Invalid routing node: " + routeNode);
                }

                for(PacketUpdateMessage message : messages) {
                    NetworkHandler.sendToAllAround(message, world);
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof MCTrain && ((MCTrain)obj).cartIDs.equals(cartIDs);
    }

    @Override
    public int hashCode(){
        return cartIDs.hashCode();
    }

    public void writeToNBT(NBTTagCompound tag){
        NBTTagList idList = new NBTTagList();
        for(UUID uuid : cartIDs) {
            idList.appendTag(new NBTTagLong(uuid.getMostSignificantBits()));
            idList.appendTag(new NBTTagLong(uuid.getLeastSignificantBits()));
        }
        tag.setTag("cartIDs", idList);

        NBTTagList posList = new NBTTagList();
        for(MCPos pos : positions) {
            NBTTagCompound t = new NBTTagCompound();
            pos.writeToNBT(t);
            posList.appendTag(t);
        }
        tag.setTag("positions", posList);
    }

    public static MCTrain fromNBT(NBTTagCompound tag){
        ImmutableSet.Builder<UUID> idBuilder = ImmutableSet.builder();
        NBTTagList idList = tag.getTagList("cartIDs", Constants.NBT.TAG_LONG);
        for(int i = 0; i < idList.tagCount(); i += 2) {
            long most = ((NBTTagLong)idList.get(i)).getLong();
            long least = ((NBTTagLong)idList.get(i + 1)).getLong();
            idBuilder.add(new UUID(most, least));
        }

        ImmutableSet.Builder<MCPos> posBuilder = ImmutableSet.builder();
        NBTTagList posList = tag.getTagList("positions", Constants.NBT.TAG_COMPOUND);
        for(int i = 0; i < posList.tagCount(); i++) {
            posBuilder.add(new MCPos(posList.getCompoundTagAt(i)));
        }

        MCTrain train = new MCTrain(idBuilder.build());
        train.positions = posBuilder.build();
        return train;
    }

    /* public void writeToBuf(ByteBuf b){
         b.writeInt(cartIDs.size());
         PacketBuffer pb = new PacketBuffer(b);
         for(UUID cartID : cartIDs) {
             pb.writeUniqueId(cartID);
         }

         b.writeInt(getPositions().size());
         for(MCPos pos : getPositions()) {
             pos.writeToBuf(b);
         }

         
     }

     public static MCTrain fromByteBuf(ByteBuf b){
         int ids = b.readInt();
         PacketBuffer pb = new PacketBuffer(b);
         Set<UUID> cartIDs = new HashSet<>(ids);
         for(int i = 0; i < ids; i++) {
             cartIDs.add(pb.readUniqueId());
         }

         int posCount = b.readInt();
         Set<MCPos> positions = new HashSet<>(posCount);
         for(int i = 0; i < posCount; i++) {
             positions.add(new MCPos(b));
         }

         

         MCTrain train = new MCTrainClient(cartIDs);
         train.setPositions(null, positions);
         train.setPath(null, null, path);

         return train;
     }*/

}
