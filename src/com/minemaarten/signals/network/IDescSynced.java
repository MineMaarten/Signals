package com.minemaarten.signals.network;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public interface IDescSynced{

    public static enum Type{
        TILE_ENTITY;
    }

    public Type getSyncType();

    public List<SyncedField> getDescriptionFields();

    public void writeToPacket(NBTTagCompound tag);

    public void readFromPacket(NBTTagCompound tag);

    public BlockPos getPosition();

    public void onDescUpdate();
}
