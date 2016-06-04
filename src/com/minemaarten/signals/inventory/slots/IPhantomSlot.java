package com.minemaarten.signals.inventory.slots;

public interface IPhantomSlot{
    /*
     * Phantom Slots don't "use" items, they are used for filters and various
     * other logic slots.
     */
    boolean canAdjust();
}
