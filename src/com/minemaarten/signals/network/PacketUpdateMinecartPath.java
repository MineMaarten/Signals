package com.minemaarten.signals.network;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketUpdateMinecartPath extends AbstractPacket<PacketUpdateMinecartPath> {

	private NBTTagCompound path;
	private int cartId;
	
	public PacketUpdateMinecartPath(){}
	
	public PacketUpdateMinecartPath(EntityMinecart cart){
		cartId = cart.getEntityId();
		
		Capability<CapabilityMinecartDestination> i = CapabilityMinecartDestination.INSTANCE;
		path = (NBTTagCompound)i.getStorage().writeNBT(i, cart.getCapability(i, null), null);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		cartId = buf.readInt();
		path = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(cartId);
		ByteBufUtils.writeTag(buf, path);
	}

	@Override
	public void handleClientSide(EntityPlayer player) {
		Entity entity = player.world.getEntityByID(cartId);
		if(entity != null){
			CapabilityMinecartDestination cap = entity.getCapability(CapabilityMinecartDestination.INSTANCE, null);
			if(cap != null){
				CapabilityMinecartDestination.INSTANCE.getStorage().readNBT(CapabilityMinecartDestination.INSTANCE, cap, null, path);
			}
		}
	}

	@Override
	public void handleServerSide(EntityPlayer player) {

	}

}
