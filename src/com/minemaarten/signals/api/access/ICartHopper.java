package com.minemaarten.signals.api.access;

import javax.annotation.Nonnull;

/**
 * A way to interact with Cart Hoppers. Retrieve this for a given TileEntity simply by checking:
 * tileEntity instanceof ICartHopper is true.
 * @author Maarten
 *
 */
public interface ICartHopper{
    /**
     * The hopper mode specifies when the cart is allowed to continue by the cart hopper.
     * @author Maarten
     *
     */
    public enum HopperMode{
        /**Waits for the cart to be full*/
        CART_FULL,
        /**Waits for the cart to be empty of items/..., only considering items/... allowed by the filters */
        CART_EMPTY,
        /**Waits until no transfers happen anymore*/
        NO_ACTIVITY,
        /** Never allows the cart to pass*/
        NEVER
    }

    /**
     * Returns the mode the Cart Hopper is in.
     * @return
     */
    public @Nonnull HopperMode getHopperMode();

    /**
     * Configures the hopper mode
     * @param hopperMode
     * @throws NullPointerException if the hopperMode supplied is null.
     */
    public void setHopperMode(@Nonnull HopperMode hopperMode);

    /**
     * Returns true, if items are transfered between an inventory and the cart's engine inventory, false if interacting with the main inventory (if applicable)
     * @return
     */
    public boolean isInteractingWithEngine();

    /**
     * Sets whether or not the hopper should interact with the cart's engine inventory or the cart's main inventory (if applicable).
     * @param interactWithEngine
     */
    public void setInteractingWithEngine(boolean interactWithEngine);
}
