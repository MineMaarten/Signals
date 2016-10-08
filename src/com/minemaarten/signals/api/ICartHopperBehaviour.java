package com.minemaarten.signals.api;

import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import org.apache.commons.lang3.tuple.Pair;

public interface ICartHopperBehaviour<T> {
    public Capability<T> getCapability();

    /**
     * 
     * @param from
     * @param to
     * @param filters
     * @return true if something did transfer (indicating the transfering was still active).
     */
    public boolean tryTransfer(T from, T to, List<Pair<TileEntity, EnumFacing>> filters);

    public boolean isCartFull(T capability);

    public boolean isCartEmpty(T capability, List<Pair<TileEntity, EnumFacing>> filters);
}
