package com.minemaarten.signals.api.access;

import net.minecraft.util.IStringSerializable;

/**
 * A way to interact with any signal tile entity. Retrieve this for a given TileEntity simply by checking:
 * tileEntity instanceof ISignal is true.
 * @author Maarten
 *
 */
public interface ISignal{
    public enum EnumForceMode{
        NONE, FORCED_GREEN_ONCE, FORCED_RED;
    }

    public enum EnumLampStatus implements IStringSerializable{
        /**
         * Cart is allowed to pass through
         */
        GREEN(0xFF00FF00),
        /**
         * Cart is not allowed to pass through, because of a cart in the way
         */
        RED(0xFFFF0000),
        /**
         * Cart is not allowed to pass through, but conditionally can be allowed through (as a result of routing through a chain signal)
         */
        YELLOW(0xFFFFFF00),
        /**
         * The signal is not configured right (placed next to an intersection or not next to a rail at all).
         */
        YELLOW_BLINKING(0xFF999900);

        public int color;

        private EnumLampStatus(int color){
            this.color = color;
        }

        @Override
        public String getName(){
            return toString().toLowerCase();
        }
    }

    /**
     * Gets the current lamp status of the Signal. Usually this is:
     * Red: No passing allowed, a cart is on the next block.
     * Green: Passing allowed, next block is free.
     * Yellow blinking: The signal is placed wrongly (on an intersection or not next to a rail at all).
     * Yellow: Other, claimed by a Chain Signal, Idling for a Path Signal waiting for a cart needing to be routed.
     * @return
     */
    public EnumLampStatus getLampStatus();

    /**
     * Returns the currently configured force mode (as applied from a Rail Network Controller, for example).
     * @return
     */
    public EnumForceMode getForceMode();

    /**
     * Call this method (from the server side) to apply a certain force mode to the signal.
     * @param forceMode
     */
    public void setForceMode(EnumForceMode forceMode);
}
