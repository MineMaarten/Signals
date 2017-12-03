package com.minemaarten.signals;

import net.minecraft.entity.item.EntityMinecart;

import com.minemaarten.signals.api.access.IDestinationAccessor;
import com.minemaarten.signals.api.access.ISignalsAccessor;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;

public class SignalsAccessor implements ISignalsAccessor{

    @Override
    public IDestinationAccessor getDestinationAccessor(EntityMinecart cart){
        return cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
    }

}
