package com.minemaarten.signals.init;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.minemaarten.signals.block.BlockCartHopper;
import com.minemaarten.signals.block.BlockLimiterRail;
import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.block.BlockStationMarker;
import com.minemaarten.signals.tileentity.TileEntityBlockSignal;
import com.minemaarten.signals.tileentity.TileEntityPathSignal;

public class ModBlocks{
    public static Block blockSignal;
    public static Block pathSignal;
    public static Block stationMarker;
    // public static Block railLink;
    public static Block limiterRail;
    public static Block cartHopper;

    public static void init(){
        blockSignal = new BlockSignalBase(TileEntityBlockSignal.class, "block_signal");
        pathSignal = new BlockSignalBase(TileEntityPathSignal.class, "path_signal");
        stationMarker = new BlockStationMarker();
        // railLink = new BlockRailLink();
        limiterRail = new BlockLimiterRail();
        cartHopper = new BlockCartHopper();
    }

    public static void registerBlock(Block block){
        GameRegistry.register(block.setRegistryName(block.getUnlocalizedName().substring(5)));
        GameRegistry.register(new ItemBlock(block).setRegistryName(block.getRegistryName()));

    }
}
