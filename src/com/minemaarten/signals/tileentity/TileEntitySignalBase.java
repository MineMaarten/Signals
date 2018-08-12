package com.minemaarten.signals.tileentity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.minemaarten.signals.api.access.ISignal;
import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkSignal;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public abstract class TileEntitySignalBase extends TileEntityBase implements ITickable, ISignal{

    private boolean firstTick = true;
    private EnumForceMode forceMode = EnumForceMode.NONE;
    private String text = "";
    private String arguments = "";

    private IBlockState getBlockState(){
        return getWorld() != null ? getWorld().getBlockState(getPos()) : null;
    }

    public EnumFacing getFacing(){
        IBlockState state = getBlockState();
        return state != null && state.getBlock() instanceof BlockSignalBase ? state.getValue(BlockSignalBase.FACING) : EnumFacing.NORTH;
    }

    public abstract EnumSignalType getSignalType();

    public void setLampStatus(EnumLampStatus lampStatus){
        IBlockState state = getBlockState();
        if(state.getPropertyKeys().contains(BlockSignalBase.LAMP_STATUS) && state.getValue(BlockSignalBase.LAMP_STATUS) != lampStatus) {
            getWorld().setBlockState(getPos(), state.withProperty(BlockSignalBase.LAMP_STATUS, lampStatus));
            if(lampStatus == EnumLampStatus.GREEN) {
                //Push carts when they're standing still.
                List<EntityMinecart> neighborMinecarts = getNeighborMinecarts();
                for(EntityMinecart cart : neighborMinecarts) {
                    if(new Vec3d(cart.motionX, cart.motionY, cart.motionZ).lengthVector() < 0.01 || EnumFacing.getFacingFromVector((float)cart.motionX, 0, (float)cart.motionZ) == getFacing()) {
                        cart.motionX += getFacing().getFrontOffsetX() * 0.1;
                        cart.motionZ += getFacing().getFrontOffsetZ() * 0.1;
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

    private NetworkSignal<MCPos> getSignal(){
        NetworkObject<MCPos> obj = RailNetworkManager.getInstance(world.isRemote).getNetwork().railObjects.get(getMCPos());
        return obj instanceof NetworkSignal ? (NetworkSignal<MCPos>)obj : null;
    }

    protected List<EntityMinecart> getNeighborMinecarts(){
        NetworkSignal<MCPos> signal = getSignal();
        if(signal == null) return Collections.emptyList();
        return getNeighborMinecarts(RailNetworkManager.getInstance(world.isRemote).getNetwork().getPositionsInFront(signal).stream());
    }

    public static List<EntityMinecart> getNeighborMinecarts(Stream<MCPos> positions){
        return positions.flatMap(TileEntitySignalBase::getCartsAt).collect(Collectors.toList());
    }

    private static Stream<EntityMinecart> getCartsAt(MCPos pos){
        World world = pos.getWorld();
        return world == null ? Stream.empty() : world.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(pos.getPos())).stream();
    }

    @Override
    public void update(){
        if(!world.isRemote) {
            if(firstTick) {
                firstTick = false;

                setLampStatus(RailNetworkManager.getInstance(world.isRemote).getLampStatus(world, pos));
                setForceMode(RailNetworkManager.getInstance(world.isRemote).getState().getForceMode(getMCPos()));
            }
        }
    }

    @Override
    public void onLoad(){
        super.onLoad();

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
            setMessage("signals.signal_message.forced_green");
        } else if(forceMode == EnumForceMode.FORCED_RED) {
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
    public SPacketUpdateTileEntity getUpdatePacket(){
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("text", text);
        tag.setString("arguments", arguments);
        return new SPacketUpdateTileEntity(getPos(), 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt){
        if(pkt.getTileEntityType() == 0) {
            text = pkt.getNbtCompound().getString("text");
            arguments = pkt.getNbtCompound().getString("arguments");
        }
    }
}
