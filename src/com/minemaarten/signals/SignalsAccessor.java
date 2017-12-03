package com.minemaarten.signals;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.tileentity.TileEntity;

import com.minemaarten.signals.api.access.IDestinationAccessor;
import com.minemaarten.signals.api.access.ISignalsAccessor;
import com.minemaarten.signals.api.tileentity.IDestinationProvider;
import com.minemaarten.signals.capabilities.CapabilityDestinationProvider;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;

public class SignalsAccessor implements ISignalsAccessor{

    @Override
    public IDestinationAccessor getDestinationAccessor(EntityMinecart cart){
        return cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
    }

    @Override
    public List<IDestinationProvider> getDestinationProviders(TileEntity te){
        CapabilityDestinationProvider cap = te.getCapability(CapabilityDestinationProvider.INSTANCE, null);
        return cap == null ? Collections.emptyList() : cap.getApplicableDestinationProviders();
    }

}
