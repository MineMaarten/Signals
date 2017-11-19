package com.minemaarten.signals.inventory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;

import com.minemaarten.signals.api.tileentity.IDestinationProvider;
import com.minemaarten.signals.capabilities.CapabilityDestinationProvider;

public class ContainerSelectDestinationProvider extends ContainerBase<TileEntity> {

    private final TileEntity te;
    public final List<IDestinationProvider> guiProviders = new ArrayList<IDestinationProvider>();

    public ContainerSelectDestinationProvider(TileEntity te){
        super(null);
        this.te = te;
        CapabilityDestinationProvider cap = te.getCapability(CapabilityDestinationProvider.INSTANCE, null);
        for(IDestinationProvider provider : cap.getApplicableDestinationProviders()) {
            if(provider.hasGui(te)) guiProviders.add(provider);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player){
        return !te.isInvalid() && player.getPositionVector().distanceTo(new Vec3d(te.getPos())) < 8;
    }

    @Override
    public void handleGUIButtonPress(EntityPlayer player, int... data){
        guiProviders.get(data[0]).openGui(te, player);
    }
}
