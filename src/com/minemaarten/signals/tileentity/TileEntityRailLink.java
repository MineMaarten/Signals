package com.minemaarten.signals.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.minemaarten.signals.block.BlockRailLink;
import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.rail.RailWrapper;

public class TileEntityRailLink extends TileEntityBase {
	private BlockPos linkedPos;
	private int linkedDimension;
	private RailWrapper linkedRail;
	
	public RailWrapper getLinkedRail(){
		if(linkedRail == null){
			World linkedWorld = DimensionManager.getWorld(linkedDimension);
			if(linkedWorld != null){
				if(linkedWorld.isBlockLoaded(linkedPos, false)){
					RailWrapper rail = RailCacheManager.getInstance(linkedDimension).getRail(linkedWorld, linkedPos);
					if(rail != null){
						setLinkedRail(rail);
					}
					return rail;
				}
			}
		}
		return linkedRail;
	}
	
	public void onLinkedRailInvalidated(){
		linkedRail = null;
		updateLinkState();
	}
	
	public void setLinkedRail(RailWrapper rail){
		if(rail != linkedRail){
			if(linkedRail != null){
				linkedRail.unlink(this);
			}
			if(rail != null){
				rail.link(this);
				linkedRail = rail;
				linkedPos = rail;
				linkedDimension = rail.world.provider.getDimension();
			}else{
				linkedPos = null;
				linkedDimension = 0;
			}
			updateLinkState();
		}
	}
	
	private void updateLinkState(){
		worldObj.setBlockState(getPos(), worldObj.getBlockState(getPos()).withProperty(BlockRailLink.CONNECTED, linkedRail != null), 2);
		worldObj.notifyNeighborsOfStateChange(getPos(), getBlockType()); //Guarantee a block update
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		if(linkedPos != null){
			tag.setInteger("linkedX", linkedPos.getX());
			tag.setInteger("linkedY", linkedPos.getY());
			tag.setInteger("linkedZ", linkedPos.getZ());
		}
		tag.setInteger("linkedDim", linkedDimension);
		
		return super.writeToNBT(tag);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		if(tag.hasKey("linkedX")){
			linkedPos = new BlockPos(tag.getInteger("linkedX"), tag.getInteger("linkedY"), tag.getInteger("linkedZ"));
		}else{
			linkedPos = null;
		}
		linkedDimension = tag.getInteger("linkedDim");
		
		super.readFromNBT(tag);
	}
}
