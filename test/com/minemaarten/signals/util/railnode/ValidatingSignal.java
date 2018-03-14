package com.minemaarten.signals.util.railnode;

import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkSignal;
import com.minemaarten.signals.util.Pos2D;

public abstract class ValidatingSignal extends NetworkSignal<Pos2D> implements IValidatingNode{

    public ValidatingSignal(Pos2D pos, EnumHeading heading, EnumSignalType signalType){
        super(pos, heading, signalType);
    }
}
