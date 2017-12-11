package com.minemaarten.signals.client;

import net.minecraft.creativetab.CreativeTabs;
import com.minemaarten.signals.init.ModBlocks;
import net.minecraft.item.ItemStack;

public class CreativeTabSignals extends CreativeTabs{
    private static final CreativeTabSignals INSTANCE = new CreativeTabSignals("signals");

    public static CreativeTabSignals getInstance(){
        return INSTANCE;
    }

    public CreativeTabSignals(String name){
        super(name);
    }

    @Override
    public ItemStack getTabIconItem(){
        return new ItemStack(ModBlocks.BLOCK_SIGNAL);
    }

}
