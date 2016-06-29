package com.minemaarten.signals.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.minemaarten.signals.client.CreativeTabSignals;
import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.rail.RailWrapper;
import com.minemaarten.signals.tileentity.TileEntityRailLink;

public class ItemRailConfigurator extends ItemSignals {

	public ItemRailConfigurator() {
		super("rail_configurator");
		setMaxStackSize(1);
	}
	
	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn,
				World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(!worldIn.isRemote){
			RailWrapper rail = RailCacheManager.getInstance(worldIn).getRail(worldIn, pos);
			if(rail != null){
				setLinkedRail(stack, rail);
				playerIn.addChatMessage(new TextComponentString("Pos: " + pos));
			}else{
				TileEntity te = worldIn.getTileEntity(pos);
				if(te instanceof TileEntityRailLink){
					rail = getLinkedRail(stack);
					if(rail != null){
						((TileEntityRailLink) te).setLinkedRail(rail);
						playerIn.addChatMessage(new TextComponentString("Linked to " + rail));
					}
				}
			}
		}
		return super.onItemUse(stack, playerIn, worldIn, pos, hand, facing, hitX, hitY, hitZ);
	}
	
	public void setLinkedRail(ItemStack stack, RailWrapper rail){
		if(rail != null){
			NBTTagCompound tag = stack.getSubCompound("linkingRail", true);
			tag.setInteger("x", rail.getX());
			tag.setInteger("y", rail.getY());
			tag.setInteger("z", rail.getZ());
			tag.setInteger("dim", rail.world.provider.getDimension());
		}else{
			if(stack.hasTagCompound()) stack.getTagCompound().removeTag("linkingRail");
		}
	}
	
	public RailWrapper getLinkedRail(ItemStack stack){
		NBTTagCompound tag = stack.getSubCompound("linkingRail", false);
		if(tag != null){
			BlockPos pos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
			World world = DimensionManager.getWorld(tag.getInteger("dim"));
			if(world != null){
				RailWrapper rail = RailCacheManager.getInstance(world).getRail(world, pos);
				return rail;
			}
		}
		return null;
	}
}
