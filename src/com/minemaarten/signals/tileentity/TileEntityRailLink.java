package com.minemaarten.signals.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.minemaarten.signals.block.BlockRailLink;
import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.rail.RailWrapper;
import com.minemaarten.signals.rail.network.mc.MCPos;

public class TileEntityRailLink extends TileEntityBase implements ITickable{
    private BlockPos linkedPos;
    private int linkedDimension;
    private RailWrapper linkedRail;

    public RailWrapper getLinkedRail(){
        if(linkedRail == null && linkedPos != null) {
            World linkedWorld = DimensionManager.getWorld(linkedDimension);
            if(linkedWorld != null) {
                if(linkedWorld.isBlockLoaded(linkedPos, false)) {
                    RailWrapper rail = RailCacheManager.getInstance(linkedDimension).getRail(linkedWorld, linkedPos);
                    if(rail != null) {
                        setLinkedRail(rail);
                    }
                    return rail;
                }
            }
        }
        return linkedRail;
    }

    public MCPos getLinkedPosition(){
        return linkedPos != null ? new MCPos(linkedDimension, linkedPos) : null;
    }

    public void onLinkedRailInvalidated(){
        linkedRail = null;
        updateLinkState();
    }

    public void setLinkedRail(RailWrapper rail){
        if(rail != linkedRail) {
            if(linkedRail != null) {
                linkedRail.unlink(this);
            }
            if(rail != null) {
                rail.link(this);
                linkedRail = rail;
                linkedPos = rail;
                linkedDimension = rail.world.provider.getDimension();
            } else {
                linkedPos = null;
                linkedDimension = 0;
            }
            updateLinkState();
        }
    }

    private void updateLinkState(){
        world.setBlockState(getPos(), world.getBlockState(getPos()).withProperty(BlockRailLink.CONNECTED, linkedRail != null), 2);
        world.notifyNeighborsOfStateChange(getPos(), getBlockType(), true); //Guarantee a block update
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
        if(linkedPos != null) {
            tag.setInteger("linkedX", linkedPos.getX());
            tag.setInteger("linkedY", linkedPos.getY());
            tag.setInteger("linkedZ", linkedPos.getZ());
        }
        tag.setInteger("linkedDim", linkedDimension);

        return super.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag){
        if(tag.hasKey("linkedX")) {
            linkedPos = new BlockPos(tag.getInteger("linkedX"), tag.getInteger("linkedY"), tag.getInteger("linkedZ"));
        } else {
            linkedPos = null;
        }
        linkedDimension = tag.getInteger("linkedDim");

        super.readFromNBT(tag);
    }

    @Override
    public void update(){
        if(!world.isRemote && world.getTotalWorldTime() % 100 == 0) getLinkedRail();
    }
}
