package com.minemaarten.signals.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;

import com.minemaarten.signals.api.tileentity.IDestinationProvider;
import com.minemaarten.signals.capabilities.CapabilityDestinationProvider;
import com.minemaarten.signals.capabilities.destinationproviders.DestinationProviderItems;

public class ContainerItemHandlerDestination extends ContainerBase<TileEntity>{

    private final TileEntity te;
    public DestinationProviderItems provider;

    public ContainerItemHandlerDestination(TileEntity te){
        super(null);
        this.te = te;
        CapabilityDestinationProvider cap = te.getCapability(CapabilityDestinationProvider.INSTANCE, null);

        for(IDestinationProvider p : cap.getApplicableDestinationProviders()) {
            if(p instanceof DestinationProviderItems) {
                provider = (DestinationProviderItems)p;
                break;
            }
        }

        addSyncedFields(provider);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player){

        return !te.isInvalid() && player.getPositionVector().distanceTo(new Vec3d(te.getPos())) < 8;
    }

    @Override
    public void handleGUIButtonPress(EntityPlayer player, int... data){
        provider.handleGUIButtonPress(player, data);
    }
}
