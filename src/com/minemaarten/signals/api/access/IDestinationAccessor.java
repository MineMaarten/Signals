package com.minemaarten.signals.api.access;

import javax.annotation.Nonnull;

/**
 * A way to modify the destinations in carts, retrieve this for a given cart using {@link ISignalsAccessor}.
 * @author Maarten
 *
 */
public interface IDestinationAccessor{
    /**
     * Configures the destinations in the cart, in the same order as supplied.
     * This does not change the current destination index (if the cart was going to the 2nd destination, it still will).
     * For this to happen, call setDestinationIndex(index).
     * When the currently selected destination index is out of bounds of the set destinations, index 0 will automatically be selected.
     * @param destinations These are treated as regex strings, and will be compiled as such. 
     * @throws NullPointerException if the destinations array is {@code null}
     * @throws IllegalArgumentException if an element from the destinations array is {@code null}
     */
    public void setDestinations(@Nonnull String... destinations);

    /**
     * Gets the total amount of destinations configured
     * @return
     */
    public int getTotalDestinations();

    /**
     * Get the currently selected destination string.
     * @return The currently selected destination. "" if no destinations are configured for this cart.
     */
    public String getCurrentDestination();

    /**
     * Get the currently selected destination index (the index of the array from setDestinations())
     * @return The currently selected destination index. -1 if no destinations are configured for this cart.
     */
    public int getDestinationIndex();

    /**
     * Sets the currently selected destination index (the index of the array from setDestinations())
     * @param index The destination index. When an index out of bounds is supplied, index 0 will be selected.
     */
    public void setCurrentDestinationIndex(int index);
}
