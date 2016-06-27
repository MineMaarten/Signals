package com.minemaarten.signals.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.minemaarten.signals.Signals;
import com.minemaarten.signals.client.CreativeTabSignals;
import com.minemaarten.signals.proxy.CommonProxy;

public class ItemRailNetworkController extends ItemSignals {
	public ItemRailNetworkController() {
		setUnlocalizedName("rail_network_controller");
		setRegistryName("rail_network_controller");
		GameRegistry.register(this);
		setMaxStackSize(1);
		setCreativeTab(CreativeTabSignals.getInstance());
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack itemStackIn,
			World worldIn, EntityPlayer playerIn, EnumHand hand) {
		if(!worldIn.isRemote){
			playerIn.openGui(Signals.instance, CommonProxy.EnumGuiId.NETWORK_CONTROLLER.ordinal(), worldIn, 0, -1, 0);
        }
		return super.onItemRightClick(itemStackIn, worldIn, playerIn, hand);
	}
}
