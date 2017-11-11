package com.minemaarten.signals.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.minemaarten.signals.Signals;
import com.minemaarten.signals.api.tileentity.IDestinationProvider;
import com.minemaarten.signals.capabilities.CapabilityDestinationProvider;
import com.minemaarten.signals.proxy.CommonProxy.EnumGuiId;
import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.rail.RailWrapper;
import com.minemaarten.signals.tileentity.TileEntityRailLink;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class ItemRailConfigurator extends ItemSignals{

    public ItemRailConfigurator(){
        super("rail_configurator");
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand){
        if(!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if(te != null) {
                CapabilityDestinationProvider cap = te.getCapability(CapabilityDestinationProvider.INSTANCE, null);
                if(cap != null) {
                    List<IDestinationProvider> providers = cap.getApplicableDestinationProviders();
                    List<IDestinationProvider> guiProviders = new ArrayList<IDestinationProvider>();
                    for(IDestinationProvider provider : providers)
                        if(provider.hasGui(te)) guiProviders.add(provider);
                    if(guiProviders.size() > 1) {
                        player.openGui(Signals.instance, EnumGuiId.SELECT_DESTINATION_PROVIDER.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
                        return EnumActionResult.SUCCESS;
                    } else if(!guiProviders.isEmpty()) {
                        guiProviders.get(0).openGui(te, player);
                        return EnumActionResult.SUCCESS;
                    }
                }
            }
        }
        return super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand){
        if(!worldIn.isRemote && playerIn.isSneaking()) {
            RailCacheManager.getInstance(worldIn).onWorldUnload(worldIn);
            RailCacheManager cacheManager = RailCacheManager.getInstance(worldIn);
            for(TileEntity te : worldIn.loadedTileEntityList) {
                if(te instanceof TileEntityStationMarker) {
                    cacheManager.addStationMarker((TileEntityStationMarker)te);
                }
            }
            playerIn.sendMessage(new TextComponentTranslation("signals.message.clearedCache"));
        }
        return super.onItemRightClick(worldIn, playerIn, hand);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
        ItemStack stack = playerIn.getHeldItem(hand);
        if(!worldIn.isRemote) {
            RailWrapper rail = RailCacheManager.getInstance(worldIn).getRail(worldIn, pos);
            if(rail != null) {
                setLinkedRail(stack, rail);
                playerIn.sendMessage(new TextComponentString("Pos: " + pos));
            } else {
                TileEntity te = worldIn.getTileEntity(pos);
                if(te instanceof TileEntityRailLink) {
                    rail = getLinkedRail(stack);
                    if(rail != null) {
                        ((TileEntityRailLink)te).setLinkedRail(rail);
                        playerIn.sendMessage(new TextComponentString("Linked to " + rail));
                    }
                }
            }
        }
        return super.onItemUse(playerIn, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    public void setLinkedRail(ItemStack stack, RailWrapper rail){
        if(rail != null) {
            NBTTagCompound tag = stack.getOrCreateSubCompound("linkingRail");
            tag.setInteger("x", rail.getX());
            tag.setInteger("y", rail.getY());
            tag.setInteger("z", rail.getZ());
            tag.setInteger("dim", rail.world.provider.getDimension());
        } else {
            if(stack.hasTagCompound()) stack.getTagCompound().removeTag("linkingRail");
        }
    }

    public RailWrapper getLinkedRail(ItemStack stack){
        NBTTagCompound tag = stack.getSubCompound("linkingRail");
        if(tag != null) {
            BlockPos pos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
            World world = DimensionManager.getWorld(tag.getInteger("dim"));
            if(world != null) {
                RailWrapper rail = RailCacheManager.getInstance(world).getRail(world, pos);
                return rail;
            }
        }
        return null;
    }
}
