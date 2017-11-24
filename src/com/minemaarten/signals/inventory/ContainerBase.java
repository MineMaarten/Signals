package com.minemaarten.signals.inventory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import com.minemaarten.signals.inventory.slots.IPhantomSlot;
import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.NetworkUtils;
import com.minemaarten.signals.network.PacketUpdateGui;
import com.minemaarten.signals.network.SyncedField;
import com.minemaarten.signals.tileentity.IGUIButtonSensitive;

public class ContainerBase<Tile extends TileEntity> extends Container implements IGUIButtonSensitive{

    public Tile te;
    private final List<SyncedField> syncedFields = new ArrayList<>();
    private boolean firstTick = true;
    private int playerSlotsStart;

    public ContainerBase(Tile te){

        this.te = te;
        if(te != null) addSyncedFields(te);
    }

    protected void addSyncedField(SyncedField field){

        syncedFields.add(field);
        field.setLazy(false);
    }

    protected void addSyncedFields(Object annotatedObject){

        List<SyncedField> fields = NetworkUtils.getSyncedFields(annotatedObject, GuiSynced.class);
        for(SyncedField field : fields)
            addSyncedField(field);
    }

    public void updateField(int index, Object value){

        syncedFields.get(index).setValue(value);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player){

        return te == null ? false : player.getPositionVector().distanceTo(new Vec3d(te.getPos())) < 8;
    }

    @Override
    public void detectAndSendChanges(){

        super.detectAndSendChanges();
        for(int i = 0; i < syncedFields.size(); i++) {
            if(syncedFields.get(i).update() || firstTick) {
                sendToCrafters(new PacketUpdateGui(i, syncedFields.get(i)));
            }
        }
        firstTick = false;
    }

    protected void sendToCrafters(IMessage message){

        for(IContainerListener crafter : listeners) {
            if(crafter instanceof EntityPlayerMP) {
                NetworkHandler.sendTo(message, (EntityPlayerMP)crafter);
            }
        }
    }

    protected void addPlayerSlots(InventoryPlayer inventoryPlayer, int yOffset){
        addPlayerSlots(inventoryPlayer, 8, yOffset);
    }

    protected void addPlayerSlots(InventoryPlayer inventoryPlayer, int xOffset, int yOffset){

        playerSlotsStart = inventorySlots.size();

        // Add the player's inventory slots to the container
        for(int inventoryRowIndex = 0; inventoryRowIndex < 3; ++inventoryRowIndex) {
            for(int inventoryColumnIndex = 0; inventoryColumnIndex < 9; ++inventoryColumnIndex) {
                addSlotToContainer(new Slot(inventoryPlayer, inventoryColumnIndex + inventoryRowIndex * 9 + 9, xOffset + inventoryColumnIndex * 18, yOffset + inventoryRowIndex * 18));
            }
        }

        // Add the player's action bar slots to the container
        for(int actionBarSlotIndex = 0; actionBarSlotIndex < 9; ++actionBarSlotIndex) {
            addSlotToContainer(new Slot(inventoryPlayer, actionBarSlotIndex, xOffset + actionBarSlotIndex * 18, yOffset + 58));
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2){

        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = inventorySlots.get(par2);

        if(var4 != null && var4.getHasStack()) {
            ItemStack var5 = var4.getStack();
            var3 = var5.copy();

            if(par2 < playerSlotsStart) {
                if(!mergeItemStack(var5, playerSlotsStart, playerSlotsStart + 36, false)) return ItemStack.EMPTY;

                var4.onSlotChange(var5, var3);
            } else {
                if(!mergeItemStack(var5, 0, playerSlotsStart, false)) return ItemStack.EMPTY;
                var4.onSlotChange(var5, var3);
            }

            if(var5.isEmpty()) {
                var4.putStack(ItemStack.EMPTY);
            } else {
                var4.onSlotChanged();
            }

            if(var5.getCount() == var3.getCount()) return ItemStack.EMPTY;

            var4.onTake(par1EntityPlayer, var5);
        }

        return var3;
    }

    /**
     * Source: Buildcraft
     */
    /* @Override
     public ItemStack slotClick(int slotNum, int modifier, int mouseButton, EntityPlayer player){

         Slot slot = slotNum < 0 ? null : (Slot)inventorySlots.get(slotNum);
         if(slot instanceof IPhantomSlot) {
             return slotClickPhantom(slot, modifier, mouseButton, player);
         }
         return super.slotClick(slotNum, modifier, mouseButton, player);
     }
    */
    private ItemStack slotClickPhantom(Slot slot, int mouseButton, int modifier, EntityPlayer player){

        ItemStack stack = ItemStack.EMPTY;

        if(mouseButton == 2) {
            if(((IPhantomSlot)slot).canAdjust()) {
                slot.putStack(ItemStack.EMPTY);
            }
        } else if(mouseButton == 0 || mouseButton == 1) {
            InventoryPlayer playerInv = player.inventory;
            slot.onSlotChanged();
            ItemStack stackSlot = slot.getStack();
            ItemStack stackHeld = playerInv.getItemStack();

            if(!stackSlot.isEmpty()) {
                stack = stackSlot.copy();
            }

            if(stackSlot.isEmpty()) {
                if(!stackHeld.isEmpty() && slot.isItemValid(stackHeld)) {
                    fillPhantomSlot(slot, stackHeld, mouseButton, modifier);
                }
            } else if(stackHeld.isEmpty()) {
                adjustPhantomSlot(slot, mouseButton, modifier);
                slot.onTake(player, playerInv.getItemStack());
            } else if(slot.isItemValid(stackHeld)) {
                if(canStacksMerge(stackSlot, stackHeld)) {
                    adjustPhantomSlot(slot, mouseButton, modifier);
                } else {
                    fillPhantomSlot(slot, stackHeld, mouseButton, modifier);
                }
            }
        }
        return stack;
    }

    public boolean canStacksMerge(ItemStack stack1, ItemStack stack2){

        if(stack1.isEmpty() || stack2.isEmpty()) return false;
        if(!stack1.isItemEqual(stack2)) return false;
        if(!ItemStack.areItemStackTagsEqual(stack1, stack2)) return false;
        return true;

    }

    protected void adjustPhantomSlot(Slot slot, int mouseButton, int modifier){

        if(!((IPhantomSlot)slot).canAdjust()) {
            return;
        }
        ItemStack stackSlot = slot.getStack();
        int stackSize;
        if(modifier == 1) {
            stackSize = mouseButton == 0 ? (stackSlot.getCount() + 1) / 2 : stackSlot.getCount() * 2;
        } else {
            stackSize = mouseButton == 0 ? stackSlot.getCount() - 1 : stackSlot.getCount() + 1;
        }

        if(stackSize > slot.getSlotStackLimit()) {
            stackSize = slot.getSlotStackLimit();
        }

        stackSlot.setCount(stackSize);
    }

    protected void fillPhantomSlot(Slot slot, ItemStack stackHeld, int mouseButton, int modifier){

        if(!((IPhantomSlot)slot).canAdjust()) {
            return;
        }
        int stackSize = mouseButton == 0 ? stackHeld.getCount() : 1;
        if(stackSize > slot.getSlotStackLimit()) {
            stackSize = slot.getSlotStackLimit();
        }
        ItemStack phantomStack = stackHeld.copy();
        phantomStack.setCount(stackSize);

        slot.putStack(phantomStack);
    }

    @Override
    public void handleGUIButtonPress(EntityPlayer player, int... data){
        if(te instanceof IGUIButtonSensitive) ((IGUIButtonSensitive)te).handleGUIButtonPress(player, data);
    }
}
