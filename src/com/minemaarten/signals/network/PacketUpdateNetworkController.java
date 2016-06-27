package com.minemaarten.signals.network;

import com.minemaarten.signals.rail.NetworkController;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

public class PacketUpdateNetworkController extends AbstractPacket<PacketUpdateNetworkController> {
	private int[] colors;
	private int width, height;
	private int startX, startZ;
	private int dimension;
	
	public PacketUpdateNetworkController(){}
	
	public PacketUpdateNetworkController(int dimension, int[] colors, int width, int height, int startX, int startZ) {
		this.dimension = dimension;
		this.colors = colors;
		this.width = width;
		this.height = height;
		this.startX = startX;
		this.startZ = startZ;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		width = buf.readInt();
		height = buf.readInt();
		startX = buf.readInt();
		startZ = buf.readInt();
		colors = new int[width * height];
		for(int i = 0; i < colors.length; i++){
			colors[i] = buf.readInt();
		}
		dimension = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(width);
		buf.writeInt(height);
		buf.writeInt(startX);
		buf.writeInt(startZ);
		for(int i : colors){
			buf.writeInt(i);
		}
		buf.writeInt(dimension);
	}

	@Override
	public void handleClientSide(EntityPlayer player) {
		NetworkController.getInstance(dimension, true).setColors(colors, width, height, startX, startZ);
	}

	@Override
	public void handleServerSide(EntityPlayer player) {
		
	}

}
