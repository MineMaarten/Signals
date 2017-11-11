package com.minemaarten.signals.api.tileentity;

import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.minemaarten.signals.api.Signals;

/**
 * Allows your TileEntity to act as a provider for destinations when adjacent to a Station Marker.
 * Implement this interface in a separate class, and annotate it with {@link Signals}.
 * Every TileEntity will receive a new instance of this class.
 * @author MineMaarten
 *
 */
public interface IDestinationProvider{
    /**
     * Called everytime a TileEntity is created (Capabilities event).
     * @param te
     * @return true if this destination provider is applicable for this tileentity
     */
    public boolean isTileEntityApplicable(TileEntity te);

    public boolean isCartApplicable(TileEntity te, EntityMinecart cart, Pattern destinationRegex);

    public boolean hasGui(TileEntity te);

    /**
     * Will be called on the server side when the user right clicks this block with a Rail Configurator, and when hasGui returned true.
     * @param te
     * @param player
     */
    public void openGui(TileEntity te, EntityPlayer player);

    /**
     * Displayed when a TileEntity has multiple destination providers with GUI's. The user will be prompted with a selection UI
     * with the names of every destination provider as button text.
     * Can return null if hasGui return false.
     * @return
     */
    public String getLocalizedName();

    public void writeToNBT(NBTTagCompound tag);

    public void readFromNBT(NBTTagCompound tag);
}
