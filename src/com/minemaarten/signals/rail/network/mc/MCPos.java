package com.minemaarten.signals.rail.network.mc;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.minemaarten.signals.lib.HeadingUtils;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.IPosition;

public class MCPos implements IPosition<MCPos>{

    private final BlockPos pos;
    private final int dimID;

    public MCPos(World world, BlockPos pos){
        this(world.provider.getDimension(), pos);
    }

    public MCPos(int dimID, BlockPos pos){
        this.dimID = dimID;
        this.pos = pos;
    }

    public MCPos(NBTTagCompound tag){
        this.dimID = tag.getInteger("d");
        this.pos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
    }

    public MCPos(ByteBuf b){
        this.dimID = b.readInt();
        this.pos = new BlockPos(b.readInt(), b.readInt(), b.readInt());
    }

    public void writeToNBT(NBTTagCompound tag){
        tag.setInteger("d", dimID);
        tag.setInteger("x", pos.getX());
        tag.setInteger("y", pos.getY());
        tag.setInteger("z", pos.getZ());
    }

    public void writeToBuf(ByteBuf b){
        b.writeInt(dimID);
        b.writeInt(pos.getX());
        b.writeInt(pos.getY());
        b.writeInt(pos.getZ());
    }

    @Nullable
    public World getWorld(){
        return DimensionManager.getWorld(dimID);
    }

    public BlockPos getPos(){
        return pos;
    }

    public int getX(){
        return pos.getX();
    }

    public int getY(){
        return pos.getY();
    }

    public int getZ(){
        return pos.getZ();
    }

    public int getDimID(){
        return dimID;
    }

    public TileEntity getLoadedTileEntity(){
        World world = getWorld();
        return world != null && world.isBlockLoaded(getPos()) ? world.getTileEntity(getPos()) : null;
    }

    public IBlockState getLoadedBlockState(){
        World world = getWorld();
        return world != null ? world.getBlockState(getPos()) : null;
    }

    @Override
    public int compareTo(MCPos o){
        int xComp = Integer.compare(pos.getX(), o.pos.getX());
        if(xComp != 0) return xComp;
        int yComp = Integer.compare(pos.getY(), o.pos.getY());
        if(yComp != 0) return yComp;
        int zComp = Integer.compare(pos.getZ(), o.pos.getZ());
        if(zComp != 0) return zComp;
        return Integer.compare(dimID, o.dimID);
    }

    @Override
    public double distanceSq(MCPos other){
        if(dimID != other.dimID) {
            return 0; //When in different dimensions, we don't know the distance, but assume none, so the AStar algorithm will keep searching.
        } else {
            return pos.distanceSq(other.pos);
        }
    }

    @Override
    public EnumHeading getRelativeHeading(MCPos from){
        if(dimID != from.dimID) return null;
        int xDiff = pos.getX() - from.pos.getX();
        int zDiff = pos.getZ() - from.pos.getZ();
        if(zDiff == 0) {
            if(xDiff == 1) return EnumHeading.EAST;
            if(xDiff == -1) return EnumHeading.WEST;
        } else if(xDiff == 0) {
            if(zDiff == 1) return EnumHeading.SOUTH;
            if(zDiff == -1) return EnumHeading.NORTH;
        }
        return null;
    }

    @Override
    public MCPos offset(EnumHeading heading){
        return offset(HeadingUtils.toFacing(heading));
    }

    public MCPos offset(EnumFacing facing){
        return new MCPos(dimID, pos.offset(facing));
    }

    //TODO height offset?
    @Override
    public List<MCPos> allHorizontalNeighbors(){
        List<MCPos> neighbors = new ArrayList<>(4);
        for(EnumHeading heading : EnumHeading.VALUES) {
            neighbors.add(offset(heading));
        }
        return neighbors;
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof MCPos) {
            MCPos other = (MCPos)obj;
            return other.dimID == dimID && other.pos.equals(pos);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(){
        return pos.hashCode() * 13 + dimID;
    }

    @Override
    public String toString(){
        return "(" + pos + ", dim:" + dimID + ")";
    }

    @Override
    public MCPos min(MCPos other){
        if(dimID != other.dimID) return null;
        return new MCPos(dimID, new BlockPos(Math.min(getX(), other.getX()), Math.min(getY(), other.getY()), Math.min(getZ(), other.getZ())));
    }

    @Override
    public MCPos max(MCPos other){
        if(dimID != other.dimID) return null;
        return new MCPos(dimID, new BlockPos(Math.max(getX(), other.getX()), Math.max(getY(), other.getY()), Math.max(getZ(), other.getZ())));
    }

    @Override
    public boolean isInAABB(MCPos min, MCPos max){
        boolean xOk = min.getX() <= getX() && getX() <= max.getX();
        boolean yOk = min.getY() <= getY() && getY() <= max.getY();
        boolean zOk = min.getZ() <= getZ() && getZ() <= max.getZ();
        return xOk && yOk && zOk;
    }

    //@formatter:off
    @Override
    public boolean intersects(MCPos pos1Min, MCPos pos1Max, MCPos pos2Min, MCPos pos2Max){
        return pos1Min.getX() <= pos2Max.getX() && pos1Max.getX() >= pos2Min.getX() && 
               pos1Min.getY() <= pos2Max.getY() && pos1Max.getY() >= pos2Min.getY() && 
               pos1Min.getZ() <= pos2Max.getZ() && pos1Max.getZ() >= pos2Min.getZ();
    } 
    //@formatter:on
}
