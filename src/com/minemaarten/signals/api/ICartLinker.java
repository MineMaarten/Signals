package com.minemaarten.signals.api;

import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.entity.item.EntityMinecart;

/**
 * A way to inform Signals that carts are part of a train and need to be treated as such. It affects the following:
 * 
 * 1. The path of linked carts are treated the same. The first cart with a path is taken. This is important for Path Signals to determine if a green signal can be given.
 * 2. Block Signals and Path Signals will keep signalling green until the entire train has left the signal.
 * 
 * Implement this interface and annotate it with {@link Signals}. An instance of the class will be created an registered in the postInit phase.
 * 
 * @author Maarten
 *
 */
public interface ICartLinker{
    /**
     * Should return a set of Carts that are linked to the passed cart. The returned set may return the passed cart as well, but this is not required.
     */
    public @Nonnull Set<EntityMinecart> getLinkedCarts(@Nonnull EntityMinecart cart);
}
