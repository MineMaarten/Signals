package com.minemaarten.signals.inventory.slots;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class SlotInventoryLimiting extends Slot{

    public SlotInventoryLimiting(IInventory iinventory, int slotIndex, int posX, int posY){
        super(iinventory, slotIndex, posX, posY);
    }

    public boolean canShift(){
        return true;
    }

    @Override
    public boolean isItemValid(ItemStack stack){
        return inventory.isItemValidForSlot(getSlotIndex(), stack);
    }
}
