package com.minemaarten.signals.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import org.apache.commons.lang3.NotImplementedException;

import com.minemaarten.signals.api.access.ISignal;
import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkSignal;
import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public abstract class TileEntitySignalBase extends TileEntityBase implements ITickable, ISignal{

    private boolean firstTick = true;
    private List<EntityMinecart> cartsOnBlock = new ArrayList<>();
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

    public abstract EnumSignalType getSignalType();

    protected void setLampStatus(EnumLampStatus lampStatus){
        if(forceMode == EnumForceMode.FORCED_GREEN_ONCE) {
            lampStatus = EnumLampStatus.GREEN;
        } else if(forceMode == EnumForceMode.FORCED_RED) {
            lampStatus = EnumLampStatus.RED;
        }

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
        NetworkObject<MCPos> obj = RailNetworkManager.getInstance().getNetwork().railObjects.get(getMCPos());
        return obj instanceof NetworkSignal ? (NetworkSignal<MCPos>)obj : null;
    }

    protected List<EntityMinecart> getNeighborMinecarts(){
        NetworkSignal<MCPos> signal = getSignal();
        if(signal == null) return Collections.emptyList();
        return getNeighborMinecarts(RailNetworkManager.getInstance().getNetwork().getPositionsInFront(signal));
    }

    public static List<EntityMinecart> getNeighborMinecarts(Stream<MCPos> positions){
        return positions.flatMap(TileEntitySignalBase::getCartsAt).collect(Collectors.toList());
    }

    private static Stream<EntityMinecart> getCartsAt(MCPos pos){
        return pos.getWorld().getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(pos.getPos())).stream();
    }

    private MCPos getConnectedRail(){
        throw new NotImplementedException("");
    }

    @Override
    public void update(){
        if(!world.isRemote) {
            List<EntityMinecart> carts = getNeighborMinecarts();
            for(EntityMinecart cart : carts) {
                if(!cartsOnBlock.contains(cart)) {
                    cart.timeUntilPortal = 0;
                    onCartEnteringBlock(cart);
                }
            }
            for(EntityMinecart cart : cartsOnBlock) {
                if(!carts.contains(cart)) onCartLeavingBlock(cart);
            }
            cartsOnBlock = carts;

            //TODO on event 
            EnumLampStatus lampStatus = RailNetworkManager.getInstance().getLampStatus(world, pos);
            setLampStatus(lampStatus);
        }
    }

    protected void onCartEnteringBlock(EntityMinecart cart){}

    protected void onCartLeavingBlock(EntityMinecart cart){
        if(forceMode == EnumForceMode.FORCED_GREEN_ONCE) {
            setForceMode(EnumForceMode.NONE);
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
