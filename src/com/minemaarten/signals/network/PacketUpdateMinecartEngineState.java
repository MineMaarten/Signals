package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.tileentity.IGUITextFieldSensitive;

public class PacketUpdateMinecartEngineState extends AbstractPacket<PacketUpdateMinecartEngineState>{

	private int entityId;
    private boolean active;
    
    public PacketUpdateMinecartEngineState(){}

    public PacketUpdateMinecartEngineState(EntityMinecart cart, boolean active){
        entityId = cart.getEntityId();
        this.active = active;
    }

    @Override
    public void toBytes(ByteBuf buffer){
        buffer.writeInt(entityId);
        buffer.writeBoolean(active);
    }

    @Override
    public void fromBytes(ByteBuf buffer){
        entityId = buffer.readInt();
        active = buffer.readBoolean();
    }

    @Override
    public void handleClientSide(EntityPlayer player){
    	Entity entity = player.world.getEntityByID(entityId);
    	CapabilityMinecartDestination cap = entity.getCapability(CapabilityMinecartDestination.INSTANCE, null);
    	if(cap != null){
    		cap.setEngineActive(active);
    	}
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
