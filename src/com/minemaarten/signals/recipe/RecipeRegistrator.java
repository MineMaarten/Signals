package com.minemaarten.signals.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.minemaarten.signals.init.ModBlocks;
import com.minemaarten.signals.init.ModItems;

public class RecipeRegistrator {
	public static void init(){
		register(new ItemStack(ModBlocks.limiterRail, 6), "i i", "isi", "ici", 'i', "ingotIron", 's', Items.STICK, 'c', Items.COMPARATOR);
		register(new ItemStack(ModBlocks.blockSignal, 8), "sss", "srs", "sds", 's', "stone", 'r', "dustRedstone", 'd', "dyeGreen");
		register(new ItemStack(ModBlocks.blockSignal, 8), "sss", "srs", "sds", 's', "stone", 'r', "dyeRed", 'd', "dyeGreen");
		register(new ItemStack(ModBlocks.pathSignal, 8), "sss", "srs", "sds", 's', "ingotGold", 'r', "dustRedstone", 'd', "dyeGreen");
		register(new ItemStack(ModBlocks.pathSignal, 8), "sss", "srs", "sds", 's', "ingotGold", 'r', "dyeRed", 'd', "dyeGreen");
		register(new ItemStack(ModBlocks.stationMarker), " i ", " g ", "iii", 'i', "ingotIron", 'g', "dyeGreen");
		register(new ItemStack(ModItems.railNetworkController), "qgq", "bpb", "qdq", 'q', "gemQuartz", 'g', "dustGlowstone", 'b', Blocks.STONE_BUTTON, 'p', ModBlocks.pathSignal, 'd', "gemDiamond");
		registerShapeless(new ItemStack(ModItems.cartEngine), Blocks.FURNACE, Blocks.GOLD_BLOCK);
	}
	
	private static void register(ItemStack output, Object... input){
		GameRegistry.addRecipe(new ShapedOreRecipe(output, input));
	}
	
	private static void registerShapeless(ItemStack output, Object... input){
		GameRegistry.addRecipe(new ShapelessOreRecipe(output, input));
	}
}
