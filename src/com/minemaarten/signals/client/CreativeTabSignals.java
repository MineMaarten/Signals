package com.minemaarten.signals.client;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import com.minemaarten.signals.init.ModBlocks;

public class CreativeTabSignals extends CreativeTabs{
    private static final CreativeTabSignals INSTANCE = new CreativeTabSignals("signals");

    public static CreativeTabSignals getInstance(){
        return INSTANCE;
    }

    public CreativeTabSignals(String name){
        super(name);
    }

    @Override
    public Item getTabIconItem(){
        return Item.getItemFromBlock(ModBlocks.blockSignal);
    }

}
