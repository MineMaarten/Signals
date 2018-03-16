package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3d;

import com.minemaarten.signals.api.access.ISignal;
import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketUpdateMessage;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.NetworkController;
import com.minemaarten.signals.rail.RailManager;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public abstract class TileEntitySignalBase extends TileEntityBase implements ITickable, ISignal{

    private boolean firstTick = true;
    private List<EntityMinecart> routedMinecarts = new ArrayList<>();
    private String text = "";
    private String arguments = "";
    private EnumForceMode forceMode = EnumForceMode.NONE; //TODO

    private IBlockState getBlockState(){
        return getWorld() != null ? getWorld().getBlockState(getPos()) : null;
    }

    public EnumFacing getFacing(){
        IBlockState state = getBlockState();
        return state != null && state.getBlock() instanceof BlockSignalBase ? state.getValue(BlockSignalBase.FACING) : EnumFacing.NORTH;
    }

    protected void setLampStatus(EnumLampStatus lampStatus){
        setLampStatus(lampStatus, this::getNeighborMinecarts, cart -> routeCart(cart, getFacing(), true));
    }

    public abstract EnumSignalType getSignalType();

    protected void setLampStatus(EnumLampStatus lampStatus, Supplier<List<EntityMinecart>> neighborMinecartGetter, Function<EntityMinecart, AStarRailNode> pathfinder){
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
                List<EntityMinecart> neighborMinecarts = neighborMinecartGetter.get();
                for(EntityMinecart cart : neighborMinecarts) {
                    if(new Vec3d(cart.motionX, cart.motionY, cart.motionZ).lengthVector() < 0.01 || EnumFacing.getFacingFromVector((float)cart.motionX, 0, (float)cart.motionZ) == getFacing()) {
                        cart.motionX += getFacing().getFrontOffsetX() * 0.1;
                        cart.motionZ += getFacing().getFrontOffsetZ() * 0.1;
                        long start = System.nanoTime();

                        AStarRailNode path = pathfinder.apply(cart);
                        //TODO if(path != null) updateSwitches(path, cart, true);
                        Log.debug((System.nanoTime() - start) / 1000 + "ns");
                    }
                }
            }
        }
    }

    @Override
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
        return Collections.emptyList();//TODO getMinecarts(world, getConnectedRails());
    }

    protected AStarRailNode routeCart(EntityMinecart cart, EnumFacing cartDir, boolean submitMessages){
        CapabilityMinecartDestination capability = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
        String destination = capability.getCurrentDestination();
        Pattern destinationRegex = capability.getCurrentDestinationRegex();
        List<PacketUpdateMessage> messages = new ArrayList<>();
        AStarRailNode path = null;
        if(!destination.isEmpty()) {
            messages.add(new PacketUpdateMessage(this, cart, "signals.message.routing_cart", destination));
            EnumFacing facing = getFacing();
            if(facing == cartDir) {
                path = null;//TODO DestinationPathFinder.pathfindToDestination(getConnectedRail(), cart, destinationRegex, facing);
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

        if(destination.isEmpty()) { //When this cart is not being routed, rely on its linked carts, if any.
            return RailManager.getInstance().getPath(cart);
        } else {
            return path; //When this cart is supposed to be routed, do not rely on its linked carts.
        }
    }

    /*protected void updateSwitches(AStarRailNode pathNode, EntityMinecart cart, boolean submitMessages){
        List<PacketUpdateMessage> messages = new ArrayList<>();
        EnumFacing lastHeading = pathNode.getPathDir();
        while(pathNode != null) {
            Map<RailWrapper, EnumFacing> neighbors = pathNode.getRail().getNeighborsForEntryDir(lastHeading);
            EnumFacing heading = pathNode.getNextNode() != null ? neighbors.get(pathNode.getNextNode().getRail()) : null;
            if(neighbors.size() > 2 && heading != null && lastHeading != null) { //If on an intersection
                EnumRailDirection railDir = RailWrapper.getRailDir(EnumSet.of(heading, lastHeading.getOpposite()));

                String[] args = {Integer.toString(pathNode.getRail().getX()), Integer.toString(pathNode.getRail().getY()), Integer.toString(pathNode.getRail().getZ()), "signals.dir." + lastHeading.toString().toLowerCase(), "signals.dir." + heading.toString().toLowerCase()};

                if(pathNode.getRail().setRailDir(railDir)) {
                    messages.add(new PacketUpdateMessage(this, cart, "signals.message.changing_junction", args));
                } else {
                    messages.add(new PacketUpdateMessage(this, cart, "signals.message.changing_junction", args));//FIXME
                }
            }
            lastHeading = heading;
            pathNode = pathNode.getNextNode();
            if(pathNode != null && heading != null && getNeighborSignal(pathNode.getRail(), heading.getOpposite()) != null) {
                break;
            }
        }
        if(submitMessages) {
            for(PacketUpdateMessage message : messages) {
                NetworkHandler.sendToAllAround(message, getWorld());
            }
        }
    }*/

    @Override
    public void invalidate(){
        super.invalidate();
        if(!world.isRemote) {
            // RailWrapper neighbor = getConnectedRail();
            //  if(neighbor != null) neighbor.updateSignalCache(); //TODO
            NetworkController.getInstance(getWorld()).updateColor((TileEntitySignalBase)null, getPos());
        }
    }

    @Override
    public void update(){
        if(!world.isRemote) {
            if(firstTick) {
                firstTick = false;
                // RailWrapper neighbor = getConnectedRail();
                // if(neighbor != null) neighbor.updateSignalCache(); TODO
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

            EnumLampStatus lampStatus = RailNetworkManager.getInstance().getLampStatus(world, pos);
            setLampStatus(lampStatus);
        }
    }

    protected abstract void onCartEnteringBlock(EntityMinecart cart);

    protected void onCartLeavingBlock(EntityMinecart cart){
        if(forceMode == EnumForceMode.FORCED_GREEN_ONCE) {
            setForceMode(EnumForceMode.NONE);
        }
        //TODO getNextSignals().forEach(signal -> signal.setClaimingCart(null));
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

    @Override
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

    @Override
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
