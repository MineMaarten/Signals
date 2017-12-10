package com.minemaarten.signals.api;

import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import org.apache.commons.lang3.tuple.Pair;

/**
 * By implementing this interface you can add custom transferables to the Cart Hopper.
 * In Signals, only Items are implemented so far.
 * 
 * Implement this interface and annotate it with {@link Signals}. An instance of the class will be created an registered in the postInit phase.
 * 
 * @author Maarten
 *
 * @param <T> The Capability you want to use to transfer your medium. The Capability needs to exist in both the TileEntity and EntityMineCart.
 */
public interface ICartHopperBehaviour<T> {
    public Capability<T> getCapability();

    /**
     * 
     * @param from The source capability (could be a TE when the rail is at the bottom, or the cart when the rail is at the top).
     * @param to   The destination capability (could be a TE when the rail is at the top, or the cart when the rail is at the bottom).
     * @param filters The TileEntities horizontally adjacent to the Cart Hopper and their relative positions.
     * @return true if something did transfer (indicating the transfering was still active).
     */
    public boolean tryTransfer(T from, T to, List<Pair<TileEntity, EnumFacing>> filters);

    /**
     * Should return true when the cart is full for this medium.
     * @param capability
     * @return
     */
    public boolean isCartFull(T capability);

    /**
     * Should return true when the cart is empty for this medium, considering the filters supplied.
     * @param capability
     * @return
     */
    public boolean isCartEmpty(T capability, List<Pair<TileEntity, EnumFacing>> filters);

    /**
     * The comparator override for the cart for this behaviour. The highest override of the applicable behaviours is emitted.
     * @param capability
     * @return
     */
    public default int getComparatorInputOverride(T capability){
        return 0;
    };
}
