package com.minemaarten.signals.init;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.registries.GameData;

import com.minemaarten.signals.block.BlockCartHopper;
import com.minemaarten.signals.block.BlockLimiterRail;
import com.minemaarten.signals.block.BlockRailLink;
import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.block.BlockStationMarker;
import com.minemaarten.signals.tileentity.TileEntityBlockSignal;
import com.minemaarten.signals.tileentity.TileEntityChainSignal;
import com.minemaarten.signals.tileentity.TileEntityPathSignal;

public class ModBlocks{
    public static Block BLOCK_SIGNAL;
    public static Block PATH_SIGNAL;
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
        GameData.register_impl(new ItemBlock(block).setRegistryName(block.getRegistryName()));

    }
}
