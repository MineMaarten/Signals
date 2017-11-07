package com.minemaarten.signals.tileentity.carthopperbehaviour;

import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import org.apache.commons.lang3.tuple.Pair;

import com.minemaarten.signals.api.ICartHopperBehaviour;
import com.minemaarten.signals.api.SignalsRail;
import com.minemaarten.signals.capabilities.CapabilityDestinationProvider;
import com.minemaarten.signals.capabilities.destinationproviders.DestinationProviderItems;

@SignalsRail
public class CartHopperBehaviourItems implements ICartHopperBehaviour<IItemHandler>{

    @CapabilityInject(IItemHandler.class)
    private static final Capability<IItemHandler> ITEM_CAP = null;
    private static final int MAX_TRANSFER_RATE = 8;

    @Override
    public Capability<IItemHandler> getCapability(){
        return ITEM_CAP;
    }

    @Override
    public boolean tryTransfer(IItemHandler from, IItemHandler to, List<Pair<TileEntity, EnumFacing>> filters){
        int totalExtracted = 0;

        for(int i = 0; i < from.getSlots(); i++) {
            ItemStack extracted = from.extractItem(i, MAX_TRANSFER_RATE - totalExtracted, true);
            if(!extracted.isEmpty() && passesFilters(extracted, filters)) {
                ItemStack leftover = ItemHandlerHelper.insertItemStacked(to, extracted, false);
                int leftoverCount = !leftover.isEmpty() ? leftover.getCount() : 0;

                int actuallyExtracted = extracted.getCount() - leftoverCount;
                if(actuallyExtracted > 0) {
                    from.extractItem(i, actuallyExtracted, false);
                    totalExtracted += actuallyExtracted;
                    if(totalExtracted >= MAX_TRANSFER_RATE) break;
                }
            }
        }
        return totalExtracted > 0;
    }

    @Override
    public boolean isCartFull(IItemHandler capability){
        for(int i = 0; i < capability.getSlots(); i++) {
            if(capability.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public boolean isCartEmpty(IItemHandler capability, List<Pair<TileEntity, EnumFacing>> filters){
        for(int i = 0; i < capability.getSlots(); i++) {
            ItemStack stack = capability.getStackInSlot(i);
            if(!stack.isEmpty() && passesFilters(stack, filters)) return false; //If there still is a stack which does pass the given filters.
        }
        return true;
    }

    private boolean passesFilters(ItemStack stack, List<Pair<TileEntity, EnumFacing>> filters){
        boolean allInventoriesEmpty = true;
        for(Pair<TileEntity, EnumFacing> filter : filters) {
            if(filter.getLeft().hasCapability(CapabilityDestinationProvider.INSTANCE, null)) {
                CapabilityDestinationProvider cap = filter.getLeft().getCapability(CapabilityDestinationProvider.INSTANCE, null);
                DestinationProviderItems itemProvider = cap.getProvider(DestinationProviderItems.class);
                if(itemProvider != null) {
                    IInventory inv = (IInventory)filter.getLeft();
                    if(itemProvider.isStackApplicable(stack, inv)) return true;

                    if(allInventoriesEmpty) {
                        for(int i = 0; i < inv.getSizeInventory(); i++) {
                            if(!inv.getStackInSlot(i).isEmpty()) {
                                allInventoriesEmpty = false;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return allInventoriesEmpty;
    }
}
