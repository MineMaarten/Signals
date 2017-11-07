package com.minemaarten.signals.capabilities.destinationproviders;

import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

import com.minemaarten.signals.Signals;
import com.minemaarten.signals.api.SignalsRail;
import com.minemaarten.signals.api.tileentity.IDestinationProvider;
import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.proxy.CommonProxy.EnumGuiId;
import com.minemaarten.signals.tileentity.IGUIButtonSensitive;

@SignalsRail
public class DestinationProviderItems implements IDestinationProvider, IGUIButtonSensitive{
    @CapabilityInject(IItemHandler.class)
    private static Capability<IItemHandler> ITEM_HANDLER;

    @GuiSynced
    public boolean blacklist, checkDamage = true, checkNBT, checkModSimilarity, checkOreDictionary;

    @Override
    public boolean isTileEntityApplicable(TileEntity te){
        return te instanceof IInventory;
    }

    @Override
    public boolean isCartApplicable(TileEntity te, EntityMinecart cart, Pattern destinationRegex){
        IItemHandler cap = cart.getCapability(ITEM_HANDLER, null);
        if(cap != null && destinationRegex.matcher("ITEM").matches()) {
            IInventory inv = (IInventory)te;
            for(int cartSlot = 0; cartSlot < cap.getSlots(); cartSlot++) {
                ItemStack cartStack = cap.getStackInSlot(cartSlot);
                if(cartStack != null) {
                    if(isStackApplicable(cartStack, inv)) return true;
                }
            }
        }
        return false;
    }

    public boolean isStackApplicable(ItemStack cartStack, IInventory inv){
        for(int teSlot = 0; teSlot < inv.getSizeInventory(); teSlot++) {
            ItemStack teStack = inv.getStackInSlot(teSlot);
            if(teStack != null) {
                if(blacklist != areStacksEqual(cartStack, teStack, checkDamage, checkNBT, checkOreDictionary, checkModSimilarity)) return true;
            }
        }
        return false;
    }

    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2, boolean checkMeta, boolean checkNBT, boolean checkOreDict, boolean checkModSimilarity){
        if(stack1 == null && stack2 == null) return true;
        if(stack1 == null && stack2 != null || stack1 != null && stack2 == null) return false;

        if(checkModSimilarity) {
            ResourceLocation id1 = Item.REGISTRY.getNameForObject(stack1.getItem());
            ResourceLocation id2 = Item.REGISTRY.getNameForObject(stack2.getItem());
            return id1 != null && id2 != null && id1.getResourceDomain().equals(id2.getResourceDomain());
        }
        if(checkOreDict) {
            return isSameOreDictStack(stack1, stack2);
        }

        if(stack1.getItem() != stack2.getItem()) return false;

        boolean metaSame = stack1.getItemDamage() == stack2.getItemDamage();
        boolean nbtSame = stack1.hasTagCompound() ? stack1.getTagCompound().equals(stack2.getTagCompound()) : !stack2.hasTagCompound();

        return (!checkMeta || metaSame) && (!checkNBT || nbtSame);
    }

    public static boolean isSameOreDictStack(ItemStack stack1, ItemStack stack2){
        int[] oredictIds = OreDictionary.getOreIDs(stack1);
        for(int oredictId : oredictIds) {
            List<ItemStack> oreDictStacks = OreDictionary.getOres(OreDictionary.getOreName(oredictId));
            for(ItemStack oreDictStack : oreDictStacks) {
                if(OreDictionary.itemMatches(oreDictStack, stack2, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasGui(TileEntity te){
        return true;
    }

    @Override
    public void openGui(TileEntity te, EntityPlayer player){
        player.openGui(Signals.instance, EnumGuiId.ITEM_HANDLER_DESTINATION.ordinal(), player.world, te.getPos().getX(), te.getPos().getY(), te.getPos().getZ());
    }

    @Override
    public void handleGUIButtonPress(EntityPlayer player, int... data){
        switch(data[0]){
            case 0:
                checkDamage = !checkDamage;
                break;
            case 1:
                checkNBT = !checkNBT;
                break;
            case 2:
                checkModSimilarity = !checkModSimilarity;
                break;
            case 3:
                checkOreDictionary = !checkOreDictionary;
                break;
            case 4:
                blacklist = !blacklist;
        }
    }

    @Override
    public String getLocalizedName(){
        return I18n.format("signals.gui.destination_provider.items");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag){
        //Invert checkDamage, as that one is true by default, and save disk space for all inventories not used
        byte packedData = (byte)((blacklist ? 16 : 0) + (checkDamage ? 0 : 8) + (checkNBT ? 4 : 0) + (checkModSimilarity ? 2 : 0) + (checkOreDictionary ? 1 : 0));
        if(packedData > 0) tag.setByte("filterParams", packedData);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag){
        byte packetData = tag.getByte("filterParams");
        blacklist = (packetData & 16) > 0;
        checkDamage = (packetData & 8) == 0;
        checkNBT = (packetData & 4) > 0;
        checkModSimilarity = (packetData & 2) > 0;
        checkOreDictionary = (packetData & 1) > 0;
    }
}
