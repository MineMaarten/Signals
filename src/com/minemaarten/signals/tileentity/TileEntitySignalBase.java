package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.google.common.base.Predicate;
import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.block.BlockSignalBase.EnumLampStatus;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketUpdateMessage;
import com.minemaarten.signals.rail.DestinationPathFinder;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.NetworkController;
import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.rail.RailWrapper;

public abstract class TileEntitySignalBase extends TileEntityBase implements ITickable{

    private boolean firstTick = true;
    private List<EntityMinecart> routedMinecarts = new ArrayList<EntityMinecart>();
    private Set<TileEntitySignalBase> nextSignals = new HashSet<TileEntitySignalBase>();
    private String text = "";
    private String arguments = "";
    private EnumForceMode forceMode = EnumForceMode.NONE;

    public enum EnumForceMode{
        NONE, FORCED_GREEN_ONCE, FORCED_RED;
    }

    public BlockPos getNeighborPos(){
        return getPos().offset(getFacing().rotateYCCW());
    }

    public RailWrapper getConnectedRail(){
        BlockPos neighborPos = getNeighborPos();
        RailWrapper rail = RailCacheManager.getInstance(getWorld()).getRail(getWorld(), neighborPos);
        return rail != null && rail.isStraightTrack() && rail.getNeighbors().size() <= 2 ? rail : null;
    }

    public List<RailWrapper> getConnectedRails(){
        RailWrapper neighbor = getConnectedRail();
        return neighbor != null ? getConnectedRails(neighbor, getFacing().getOpposite()) : new ArrayList<RailWrapper>();
    }

    public static List<RailWrapper> getConnectedRails(RailWrapper neighbor, EnumFacing traverseDirection){
        List<RailWrapper> neighbors = new ArrayList<RailWrapper>();
        Block railType = neighbor.state.getBlock();
        for(int i = 0; i < 5 && neighbor != null; i++) {
            neighbors.add(neighbor);
            Map<RailWrapper, EnumFacing> railNeighbors = neighbor.getNeighbors();
            neighbor = null;
            for(Map.Entry<RailWrapper, EnumFacing> entry : railNeighbors.entrySet()) {
                if(entry.getValue() == traverseDirection && railType == entry.getKey().state.getBlock()) {
                    neighbor = entry.getKey();
                    break;
                }
            }
        }
        return neighbors;
    }

    private IBlockState getBlockState(){
        return getWorld() != null ? getWorld().getBlockState(getPos()) : null;
    }

    protected EnumFacing getFacing(){
        IBlockState state = getBlockState();
        return state != null && state.getBlock() instanceof BlockSignalBase ? state.getValue(BlockSignalBase.FACING) : EnumFacing.NORTH;
    }

    protected void setLampStatus(EnumLampStatus lampStatus){
        if(forceMode == EnumForceMode.FORCED_GREEN_ONCE) {
            lampStatus = EnumLampStatus.GREEN;
        } else if(forceMode == EnumForceMode.FORCED_RED) {
            lampStatus = EnumLampStatus.RED;
        }
        IBlockState state = getBlockState();
        if(state.getPropertyKeys().contains(BlockSignalBase.LAMP_STATUS) && state.getValue(BlockSignalBase.LAMP_STATUS) != lampStatus) {
            getWorld().setBlockState(getPos(), state.withProperty(BlockSignalBase.LAMP_STATUS, lampStatus));
            NetworkController.getInstance(getWorld()).updateColor(this, getPos());
            if(lampStatus == EnumLampStatus.GREEN) {
                //Push carts when they're standing still.
                for(EntityMinecart cart : getNeighborMinecarts()) {
                    if(new Vec3d(cart.motionX, cart.motionY, cart.motionZ).lengthVector() < 0.01 || EnumFacing.getFacingFromVector((float)cart.motionX, 0, (float)cart.motionZ) == getFacing()) {
                        cart.motionX += getFacing().getFrontOffsetX() * 0.1;
                        cart.motionZ += getFacing().getFrontOffsetZ() * 0.1;
                        long start = System.nanoTime();
                        AStarRailNode path = routeCart(cart, getFacing(), true);
                        if(path != null) updateSwitches(path, cart, true);
                        Log.debug((System.nanoTime() - start) / 1000 + "ns");
                    }
                }
            }
        }
    }

    public EnumLampStatus getLampStatus(){
        if(getWorld() != null) {
            IBlockState state = getWorld().getBlockState(getPos());
            if(state.getPropertyKeys().contains(BlockSignalBase.LAMP_STATUS)) {
                return state.getValue(BlockSignalBase.LAMP_STATUS);
            }
        }
        return EnumLampStatus.YELLOW_BLINKING;
    }

    protected List<EntityMinecart> getNeighborMinecarts(){
        return getMinecarts(world, getConnectedRails());
    }

    protected AStarRailNode routeCart(EntityMinecart cart, EnumFacing cartDir, boolean submitMessages){
        CapabilityMinecartDestination capability = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
        String destination = capability.getCurrentDestination();
        Pattern destinationRegex = capability.getCurrentDestinationRegex();
        List<PacketUpdateMessage> messages = new ArrayList<PacketUpdateMessage>();
        AStarRailNode path = null;
        if(!destination.isEmpty()) {
            messages.add(new PacketUpdateMessage(this, cart, "signals.message.routing_cart", destination));
            EnumFacing facing = getFacing();
            if(facing == cartDir) {
                path = DestinationPathFinder.pathfindToDestination(getConnectedRail(), cart, destinationRegex, facing);
                if(path == null) { //If there's no path
                    messages.add(new PacketUpdateMessage(this, cart, "signals.message.no_path_found"));
                } else {
                    messages.add(new PacketUpdateMessage(this, cart, "signals.message.path_found"));
                }
            }
        } else {
            messages.add(new PacketUpdateMessage(this, cart, "signals.message.no_destination"));
        }

        if(submitMessages) {
            for(PacketUpdateMessage message : messages) {
                NetworkHandler.sendToAllAround(message, getWorld());
            }
        }
        capability.setPath(cart, path);
        return path;
    }

    protected void updateSwitches(AStarRailNode pathNode, EntityMinecart cart, boolean submitMessages){
        List<PacketUpdateMessage> messages = new ArrayList<PacketUpdateMessage>();
        EnumFacing lastHeading = null;
        while(pathNode != null) {
            Map<RailWrapper, EnumFacing> neighbors = pathNode.getRail().getNeighbors();
            EnumFacing heading = pathNode.getNextNode() != null ? neighbors.get(pathNode.getNextNode().getRail()) : null;
            if(neighbors.size() > 2 && heading != null && lastHeading != null) { //If on an intersection
                EnumRailDirection railDir = RailWrapper.getRailDir(EnumSet.of(heading, lastHeading.getOpposite()));

                String[] args = {Integer.toString(pathNode.getRail().getX()), Integer.toString(pathNode.getRail().getY()), Integer.toString(pathNode.getRail().getZ()), "signals.dir." + lastHeading.toString().toLowerCase(), "signals.dir." + heading.toString().toLowerCase()};

                if(pathNode.getRail().setRailDir(railDir)) {
                    messages.add(new PacketUpdateMessage(this, cart, "signals.message.changing_junction", args));
                } else {
                    messages.add(new PacketUpdateMessage(this, cart, "signals.message.changing_junction", args));
                }
            }
            lastHeading = heading;
            pathNode = pathNode.getNextNode();
            /*      if(pathNode != null && heading != null && getNeighborSignal(pathNode.getRail(), heading.getOpposite()) != null) {
                      break;
                  }*/
        }
        if(submitMessages) {
            for(PacketUpdateMessage message : messages) {
                NetworkHandler.sendToAllAround(message, getWorld());
            }
        }
    }

    protected static Set<RailWrapper> getRailsToNextBlockSection(RailWrapper curRail, EnumFacing direction){
        Set<RailWrapper> rails = new HashSet<RailWrapper>();
        Queue<Map.Entry<RailWrapper, EnumFacing>> traversingRails = new LinkedList<Map.Entry<RailWrapper, EnumFacing>>();

        for(Map.Entry<RailWrapper, EnumFacing> entry : curRail.getNeighbors().entrySet()) {
            if(entry.getValue() != direction.getOpposite()) {
                traversingRails.add(entry);
            }
        }
        rails.add(curRail); //Make sure to consider this block as traversed already, prevents traversing the tracks in reverse direction

        while(!traversingRails.isEmpty()) {
            Map.Entry<RailWrapper, EnumFacing> neighbor = traversingRails.poll();

            TileEntitySignalBase neighborSignal = getNeighborSignal(neighbor.getKey(), neighbor.getValue()); //Find a signal opposing this signal (for like merging splits)
            if(neighborSignal == null) {
                rails.add(neighbor.getKey());
                if(neighbor.getKey().getSignals().isEmpty()) {
                    //    NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.REDSTONE, neighbor.getKey().getX() + 0.5, neighbor.getKey().getY() + 0.5, neighbor.getKey().getZ() + 0.5, 0, 0, 0), curRail.world);
                    for(Map.Entry<RailWrapper, EnumFacing> entry : neighbor.getKey().getNeighbors().entrySet()) {
                        BlockPos nextNeighbor = entry.getKey();
                        if(!rails.contains(nextNeighbor)) {
                            traversingRails.add(entry);
                        }
                    }
                }
            } else {
                if(curRail.world.getTotalWorldTime() % 20 == 0) {
                    //        curRail.world.spawnEntityInWorld(new EntityFireworkRocket(curRail.world, neighborSignal.pos.getX() + 0.5, neighborSignal.pos.getY() + 0.5, neighborSignal.pos.getZ() + 0.5, new ItemStack(Items.fireworks)));
                }
                if(neighborSignal.getLampStatus() != EnumLampStatus.RED) {
                    // rails.add(neighbor.getKey());
                    // neighborSignal.setWorldObj(curRail.world); //For some reason this is necessary.
                    rails.addAll(getConnectedRails(neighbor.getKey(), neighborSignal.getFacing().getOpposite()));
                }
            }
        }

        rails.remove(curRail); //Remove it in the end as the rail next to a signal isn't considered part of the next block.
        return rails;
    }

    @SuppressWarnings("unchecked")
    protected static <T extends TileEntitySignalBase> T getNeighborSignal(World world, BlockPos pos, Class<T> teClass, EnumFacing blacklistedSignalDir){
        for(EnumFacing dir : EnumFacing.HORIZONTALS) {
            BlockPos neighbor = pos.offset(dir);
            TileEntity te = world.getTileEntity(neighbor);
            if(te != null && teClass.isAssignableFrom(te.getClass())) {
                T signal = (T)te;
                if(signal.getFacing().rotateY() == dir && signal.getFacing() != blacklistedSignalDir) return signal;
            }
        }
        return null;
    }

    public static TileEntitySignalBase getNeighborSignal(RailWrapper rail, EnumFacing blacklistedSignalDir){
        for(Map.Entry<EnumFacing, TileEntitySignalBase> entry : rail.getSignals().entrySet()) {
            if(entry.getValue().getFacing().rotateY() == entry.getKey() && entry.getValue().getFacing() != blacklistedSignalDir) return entry.getValue();
        }
        return null;
    }

    public static List<EntityMinecart> getMinecarts(World worldObj, final Collection<RailWrapper> railsOnBlock){
        if(railsOnBlock.isEmpty()) return Collections.emptyList();

        Set<World> worlds = new HashSet<World>();
        for(RailWrapper pos : railsOnBlock) {
            worlds.add(pos.world);
        }

        List<EntityMinecart> carts = new ArrayList<EntityMinecart>();
        for(World world : worlds) {
            BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            for(RailWrapper pos : railsOnBlock) {
                min.setPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
                max.setPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
            }

            carts.addAll(world.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(min, max.add(1, 2, 1)), new Predicate<EntityMinecart>(){
                @Override
                public boolean apply(EntityMinecart cart){
                    BlockPos cartPos = cart.getPosition();
                    return railsOnBlock.contains(cartPos) || railsOnBlock.contains(cartPos.down());
                }
            }));
        }
        return carts;
    }

    @Override
    public void invalidate(){
        super.invalidate();
        if(!world.isRemote) {
            RailWrapper neighbor = getConnectedRail();
            if(neighbor != null) neighbor.updateSignalCache();
            NetworkController.getInstance(getWorld()).updateColor((TileEntitySignalBase)null, getPos());
        }
    }

    @Override
    public void update(){
        if(!world.isRemote) {
            if(firstTick) {
                firstTick = false;
                RailWrapper neighbor = getConnectedRail();
                if(neighbor != null) neighbor.updateSignalCache();
                NetworkController.getInstance(getWorld()).updateColor(this, getPos());
            }
            List<EntityMinecart> carts = getNeighborMinecarts();
            for(EntityMinecart cart : carts) {
                if(!routedMinecarts.contains(cart)) {
                    cart.timeUntilPortal = 0;
                    onCartEnteringBlock(cart);
                }
            }
            for(EntityMinecart cart : routedMinecarts) {
                if(!carts.contains(cart)) onCartLeavingBlock(cart);
            }
            routedMinecarts = carts;

            RailWrapper neighbor = getConnectedRail();
            if(neighbor != null) updateConnectedSignals(neighbor);
        }
    }

    protected abstract void onCartEnteringBlock(EntityMinecart cart);

    protected void onCartLeavingBlock(EntityMinecart cart){
        if(forceMode == EnumForceMode.FORCED_GREEN_ONCE) {
            setForceMode(EnumForceMode.NONE);
        }
    }

    public void updateConnectedSignals(RailWrapper curRail){
        EnumFacing direction = getFacing();
        Set<RailWrapper> rails = new HashSet<RailWrapper>();
        Queue<Map.Entry<RailWrapper, EnumFacing>> traversingRails = new LinkedList<Map.Entry<RailWrapper, EnumFacing>>();
        Set<TileEntitySignalBase> signals = new HashSet<TileEntitySignalBase>();

        for(Map.Entry<RailWrapper, EnumFacing> entry : curRail.getNeighbors().entrySet()) {
            if(entry.getValue() != direction.getOpposite()) {
                traversingRails.add(entry);
            }
        }
        rails.add(curRail); //Make sure to consider this block as traversed already, prevents traversing the tracks in reverse direction

        while(!traversingRails.isEmpty()) {
            Map.Entry<RailWrapper, EnumFacing> neighbor = traversingRails.poll();

            TileEntitySignalBase neighborSignal = getNeighborSignal(neighbor.getKey(), neighbor.getValue()); //Find a signal opposing this signal (for like merging splits)
            if(neighborSignal == null) {
                rails.add(neighbor.getKey());
                if(neighbor.getKey().getSignals().isEmpty()) {
                    //    NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.REDSTONE, neighbor.getKey().getX() + 0.5, neighbor.getKey().getY() + 0.5, neighbor.getKey().getZ() + 0.5, 0, 0, 0), curRail.world);
                    for(Map.Entry<RailWrapper, EnumFacing> entry : neighbor.getKey().getNeighbors().entrySet()) {
                        BlockPos nextNeighbor = entry.getKey();
                        if(!rails.contains(nextNeighbor)) {
                            traversingRails.add(entry);
                        }
                    }
                } else {
                    signals.add(neighbor.getKey().getSignals().values().iterator().next());
                }
            }
        }

        if(!signals.equals(nextSignals)) {
            nextSignals = signals;
            sendUpdatePacket();
        }
    }

    public Set<TileEntitySignalBase> getNextSignals(){
        return nextSignals;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket(){
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();

        for(TileEntitySignalBase signal : nextSignals) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("x", signal.getPos().getX());
            t.setInteger("y", signal.getPos().getY());
            t.setInteger("z", signal.getPos().getZ());
            list.appendTag(t);
        }

        tag.setTag("signals", list);
        tag.setString("text", text);
        tag.setString("arguments", arguments);
        return new SPacketUpdateTileEntity(getPos(), 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt){
        if(pkt.getTileEntityType() == 0) {
            nextSignals.clear();
            NBTTagList list = pkt.getNbtCompound().getTagList("signals", 10);
            for(int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound t = list.getCompoundTagAt(i);
                BlockPos pos = new BlockPos(t.getInteger("x"), t.getInteger("y"), t.getInteger("z"));
                TileEntity te = world.getTileEntity(pos);
                if(te instanceof TileEntitySignalBase) nextSignals.add((TileEntitySignalBase)te);
            }

            text = pkt.getNbtCompound().getString("text");
            arguments = pkt.getNbtCompound().getString("arguments");
        }
    }

    protected void setMessage(String message, Object... arguments){
        text = message;
        this.arguments = "";
        for(int i = 0; i < arguments.length; i++) {
            if(i > 0) this.arguments += "\n";
            this.arguments += arguments[i].toString();
        }
        world.notifyBlockUpdate(getPos(), getBlockState(), getBlockState(), 3);
    }

    public String getMessage(){
        String[] localizedArguments = arguments.split("\n");
        for(int i = 0; i < localizedArguments.length; i++) {
            localizedArguments[i] = I18n.format(localizedArguments[i]);
        }
        return I18n.format(text, (Object[])localizedArguments);
    }

    public void setForceMode(EnumForceMode forceMode){
        this.forceMode = forceMode;
        markDirty();
        if(forceMode == EnumForceMode.FORCED_GREEN_ONCE) {
            setLampStatus(EnumLampStatus.GREEN);
            setMessage("signals.signal_message.forced_green");
        } else if(forceMode == EnumForceMode.FORCED_RED) {
            setLampStatus(EnumLampStatus.RED);
            setMessage("signals.signal_message.forced_red");
        } else {
            setMessage("");
        }
    }

    public EnumForceMode getForceMode(){
        return forceMode;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
        super.writeToNBT(tag);
        tag.setByte("forceMode", (byte)forceMode.ordinal());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag){
        super.readFromNBT(tag);
        forceMode = EnumForceMode.values()[tag.getByte("forceMode")];
    }
}
