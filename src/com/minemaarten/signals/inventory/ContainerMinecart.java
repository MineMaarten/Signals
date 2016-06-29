package com.minemaarten.signals.inventory;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.inventory.slots.SlotInventoryLimiting;
import com.minemaarten.signals.tileentity.IGUIButtonSensitive;

public class ContainerMinecart extends ContainerBase<TileEntity> implements IGUIButtonSensitive{
    private final EntityMinecart cart;
    public final boolean isMotorized;

    public ContainerMinecart(InventoryPlayer playerInv, EntityMinecart cart, boolean isMotorized){
        super(null);
        this.cart = cart;
        this.isMotorized = isMotorized;
        CapabilityMinecartDestination cap = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
        addSyncedFields(cap);
        
        if(isMotorized){
        	IInventory inv = cap.getFuelInv();
        	for(int i = 0; i < inv.getSizeInventory(); i++){
        		addSlotToContainer(new SlotInventoryLimiting(inv, i, 18 * i + 164, 70));
        	}
        	addPlayerSlots(playerInv, 128, 120);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player){

        return !cart.isDead && player.getPositionVector().distanceTo(cart.getPositionVector()) < 32;
    }

    @Override
    public void handleGUIButtonPress(EntityPlayer player, int... data){

    }
}
