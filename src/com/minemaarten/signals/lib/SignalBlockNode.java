package com.minemaarten.signals.lib;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

import com.minemaarten.signals.rail.RailWrapper;

public class SignalBlockNode{
    public final List<SignalBlockNode> nextNeighbors = new ArrayList<>();
    public final BlockPos railPos;
    public final EnumRailDirection railDir;

    public SignalBlockNode(RailWrapper railWrapper){
        this(railWrapper, railWrapper.getRailDir());
    }

    public SignalBlockNode(BlockPos railPos, EnumRailDirection railDir){
        this.railPos = railPos;
        this.railDir = railDir;
    }

    public SignalBlockNode(NBTTagCompound tag){
        railPos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
        railDir = EnumRailDirection.values()[tag.getByte("railDir")];

        NBTTagList tagList = tag.getTagList("blockNode", 10);
        for(int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound t = tagList.getCompoundTagAt(i);

            SignalBlockNode neighbor = new SignalBlockNode(t);
            nextNeighbors.add(neighbor);
        }
    }

    public void toNBTTagCompound(NBTTagCompound tag){
        tag.setInteger("x", railPos.getX());
        tag.setInteger("y", railPos.getY());
        tag.setInteger("z", railPos.getZ());
        tag.setByte("railDir", (byte)railDir.ordinal());

        NBTTagList tagList = new NBTTagList();
        for(SignalBlockNode neighbor : nextNeighbors) {
            NBTTagCompound t = new NBTTagCompound();
            neighbor.toNBTTagCompound(t);
            tagList.appendTag(t);
        }
        tag.setTag("blockNode", tagList);
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof SignalBlockNode && ((SignalBlockNode)obj).railPos.equals(railPos);
    }

    @Override
    public int hashCode(){
        return railPos.hashCode();
    }
}
