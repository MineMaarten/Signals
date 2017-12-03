package com.minemaarten.signals.api.access;

import javax.annotation.Nonnull;

/**
 * A way to interact with Station Markers. Retrieve this for a given TileEntity simply by checking:
 * tileEntity instanceof IStationMarker is true.
 * @author Maarten
 *
 */
public interface IStationMarker{
    /**
     * Sets the station name
     * @param stationName
     * @throws NullPointerException when stationName is null.
     */
    public void setStationName(@Nonnull String stationName);

    /**
     * Returns the currently configured station name.
     * @return the station name, "" if empty.
     */
    public @Nonnull String getStationName();
}
