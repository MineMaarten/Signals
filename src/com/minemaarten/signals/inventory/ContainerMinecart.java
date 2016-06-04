package com.minemaarten.signals.inventory;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.tileentity.IGUIButtonSensitive;

public class ContainerMinecart extends ContainerBase<TileEntity> implements IGUIButtonSensitive{
    private final EntityMinecart cart;

    public ContainerMinecart(EntityMinecart cart){
        super(null);
        this.cart = cart;
        CapabilityMinecartDestination cap = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
        addSyncedFields(cap);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player){

        return !cart.isDead && player.getPositionVector().distanceTo(cart.getPositionVector()) < 32;
    }

    @Override
    public void handleGUIButtonPress(int guiID, EntityPlayer player){

    }
}
