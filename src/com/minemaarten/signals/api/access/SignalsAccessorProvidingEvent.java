package com.minemaarten.signals.api.access;

import net.minecraftforge.fml.common.eventhandler.Event;

public class SignalsAccessorProvidingEvent extends Event{
    public final ISignalsAccessor accessor;

    public SignalsAccessorProvidingEvent(ISignalsAccessor accessor){
        this.accessor = accessor;
    }
}
