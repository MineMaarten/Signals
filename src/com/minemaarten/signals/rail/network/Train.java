package com.minemaarten.signals.rail.network;

import java.util.Set;

public abstract class Train<TPos extends IPosition<TPos>> {

    /**
     * The positions the train is on.
     * This may be a single position for a cart, or multiple if actually a train.
     * @return
     */
    public abstract Set<TPos> getPositions();

    public abstract RailRoute<TPos> getCurRoute();
}
