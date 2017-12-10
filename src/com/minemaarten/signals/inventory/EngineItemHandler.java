package com.minemaarten.signals.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.init.ModItems;

public class EngineItemHandler implements IItemHandler{
    private final CapabilityMinecartDestination cap;
    private final IItemHandler onceInstalled;

    public EngineItemHandler(CapabilityMinecartDestination cap, IItemHandler onceInstalled){
        this.cap = cap;
        this.onceInstalled = onceInstalled;
    }

    @Override
    public int getSlots(){
        return cap.isMotorized() ? onceInstalled.getSlots() : 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot){
        return cap.isMotorized() ? onceInstalled.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate){
        if(cap.isMotorized()) return onceInstalled.insertItem(slot, stack, simulate);

        if(stack.isEmpty() || slot != 0 || stack.getItem() != ModItems.CART_ENGINE) return stack;

        if(!simulate) cap.setMotorized();

        stack = stack.copy();
        stack.shrink(1);
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate){
        if(cap.isMotorized()) return onceInstalled.extractItem(slot, amount, simulate);
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot){
        if(cap.isMotorized()) return onceInstalled.getSlotLimit(slot);
        return slot == 0 ? 1 : 0;
    }

}
