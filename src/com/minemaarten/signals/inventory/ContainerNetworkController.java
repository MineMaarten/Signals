package com.minemaarten.signals.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import com.minemaarten.signals.tileentity.IGUIButtonSensitive;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;
import com.minemaarten.signals.tileentity.TileEntitySignalBase.EnumForceMode;

public class ContainerNetworkController extends ContainerBase<TileEntity> implements IGUIButtonSensitive {

	public ContainerNetworkController() {
		super(null);
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

	@Override
	public void handleGUIButtonPress(EntityPlayer player, int... data) {
		BlockPos pos = new BlockPos(data[0], 0, data[1]);
		for(TileEntity te : player.worldObj.getChunkFromBlockCoords(pos).getTileEntityMap().values()){
			if(te.getPos().getX() == pos.getX() && te.getPos().getZ() == pos.getZ()){
				if(te instanceof TileEntitySignalBase){
					EnumForceMode forceMode = EnumForceMode.values()[data[2]];
					((TileEntitySignalBase) te).setForceMode(forceMode);
					player.addChatComponentMessage(new TextComponentString("Forced " + (forceMode == EnumForceMode.FORCED_RED ? "red" : "green")));
				}
			}
		}
	}

}
