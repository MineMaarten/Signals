package com.minemaarten.signals.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent.MissingMappings;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.GameData;

import com.minemaarten.signals.block.BlockCartHopper;
import com.minemaarten.signals.block.BlockLimiterRail;
import com.minemaarten.signals.block.BlockRailLink;
import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.block.BlockStationMarker;
import com.minemaarten.signals.lib.Constants;
import com.minemaarten.signals.tileentity.TileEntityBlockSignal;
import com.minemaarten.signals.tileentity.TileEntityChainSignal;
import com.minemaarten.signals.tileentity.TileEntityPathSignal;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class ModBlocks{
    public static Block BLOCK_SIGNAL;
    public static Block PATH_SIGNAL; //TODO Remove in 1.13
    public static Block CHAIN_SIGNAL;
    public static Block STATION_MARKER;
    public static Block RAIL_LINK;
    public static Block LIMITER_RAIL;
    public static Block CART_HOPPER;

    public static void init(){
        BLOCK_SIGNAL = new BlockSignalBase(TileEntityBlockSignal.class, "block_signal");
        PATH_SIGNAL = new BlockSignalBase(TileEntityPathSignal.class, "path_signal");
        CHAIN_SIGNAL = new BlockSignalBase(TileEntityChainSignal.class, "chain_signal");
        STATION_MARKER = new BlockStationMarker();
        RAIL_LINK = new BlockRailLink();
        LIMITER_RAIL = new BlockLimiterRail();
        CART_HOPPER = new BlockCartHopper();
    }

    public static void registerBlock(Block block){
        GameData.register_impl(block.setRegistryName(block.getUnlocalizedName().substring(5)));

        //Don't register the path signal itemblock, but remap it instead.
        if(!block.getRegistryName().getResourcePath().equals("path_signal")) {
            GameData.register_impl(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        }

    }

    /*@SubscribeEvent
    public static void OnMissingBlockMapping(MissingMappings<Block> event){
        for(MissingMappings.Mapping<Block> entry : event.getAllMappings()) {
            if(entry.key.equals(new ResourceLocation(Constants.MOD_ID, "path_signal"))) {
                entry.remap(CHAIN_SIGNAL);
            }
        }
    }*/

    //TODO remove in 1.13
    @SubscribeEvent
    public static void OnMissingItemMapping(MissingMappings<Item> event){
        for(MissingMappings.Mapping<Item> entry : event.getAllMappings()) {
            if(entry.key.equals(new ResourceLocation(Constants.MOD_ID, "path_signal"))) {
                entry.remap(Item.getItemFromBlock(CHAIN_SIGNAL));
            }
        }
    }
}
