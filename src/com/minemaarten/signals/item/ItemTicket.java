package com.minemaarten.signals.item;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.minemaarten.signals.Signals;
import com.minemaarten.signals.SignalsAccessor;
import com.minemaarten.signals.api.access.IDestinationAccessor;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.proxy.CommonProxy;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class ItemTicket extends ItemSignals{

    public ItemTicket(){
        super("ticket");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand){
        if(!worldIn.isRemote) {
            ItemStack stack = playerIn.getHeldItem(hand);
            if(playerIn.isSneaking()) {
                setDestinations(stack, Collections.emptyList());
                stack.clearCustomName();
                playerIn.sendMessage(new TextComponentTranslation("signals.message.cleared_ticket"));
            } else {
                playerIn.openGui(Signals.instance, CommonProxy.EnumGuiId.TICKET_DESTINATION.ordinal(), worldIn, 0, 0, 0);
            }

        }
        return super.onItemRightClick(worldIn, playerIn, hand);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand){
        ItemStack stack = player.getHeldItem(hand);
        if(!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if(te instanceof TileEntityStationMarker) {
                TileEntityStationMarker stationMarker = (TileEntityStationMarker)te;
                String stationName = stationMarker.getStationName();

                appendDestination(stack, stationName);

                String concatDestinations = getConcattedDestinations(stack);
                player.sendMessage(new TextComponentTranslation("signals.message.added_destination", TextFormatting.GOLD + stationName + TextFormatting.WHITE, TextFormatting.GOLD + concatDestinations + TextFormatting.WHITE));

                return EnumActionResult.SUCCESS;
            }
        }
        return super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> curInfo, ITooltipFlag flag){
        curInfo.add(getConcattedDestinations(stack));
        super.addInformation(stack, world, curInfo, flag);
    }

    @Override
    public int getMetadata(ItemStack stack){
        return Math.min(getDestinations(stack).size(), 4);
    }

    private static void appendDestination(ItemStack stack, String destination){
        List<String> newDestinations = Lists.newArrayList(getDestinations(stack));
        newDestinations.add(destination);
        setDestinations(stack, newDestinations);
    }

    public static void setDestinations(ItemStack stack, List<String> destinations){
        if(destinations.isEmpty()) {
            stack.removeSubCompound("destinations");
        } else {
            NBTTagCompound tag = stack.getOrCreateSubCompound("destinations");
            NBTTagList tagList = new NBTTagList();
            for(String destination : destinations) {
                tagList.appendTag(new NBTTagString(destination));
            }
            tag.setTag("destinations", tagList);
        }
    }

    public static List<String> getDestinations(ItemStack stack){
        NBTTagCompound tag = stack.getSubCompound("destinations");
        if(tag != null) {
            NBTTagList tagList = tag.getTagList("destinations", Constants.NBT.TAG_STRING);
            return StreamSupport.stream(tagList.spliterator(), false).map(nbt -> ((NBTTagString)nbt).getString()).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static void writeNBTFromCap(ItemStack stack){
        CapabilityMinecartDestination cap = getCap(stack);
        if(cap != null) setDestinations(stack, Arrays.asList(cap.getDestinations()));
    }

    public static void readNBTIntoCap(ItemStack stack){
        CapabilityMinecartDestination cap = getCap(stack);
        if(cap != null) cap.setDestinations(getDestinations(stack));
    }

    public static CapabilityMinecartDestination getCap(ItemStack stack){
        return stack.getCapability(CapabilityMinecartDestination.INSTANCE, null);
    }

    public static String getConcattedDestinations(ItemStack stack){
        List<String> destinations = getDestinations(stack);
        return destinations.isEmpty() ? "-" : StringUtils.join(destinations, ", ");
    }

    public static void applyDestinations(EntityMinecart cart, ItemStack stack){
        IDestinationAccessor accessor = new SignalsAccessor().getDestinationAccessor(cart);
        accessor.setDestinations(getDestinations(stack));
        accessor.setCurrentDestinationIndex(0);
    }
}
