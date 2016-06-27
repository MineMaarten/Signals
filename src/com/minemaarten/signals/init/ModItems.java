package com.minemaarten.signals.init;

import net.minecraft.item.Item;

import com.minemaarten.signals.item.ItemRailNetworkController;


public class ModItems{
    //public static Item railConfigurator;
	public static Item railNetworkController;

    public static void init(){
    //	railConfigurator = new ItemRailConfigurator();
    	railNetworkController = new ItemRailNetworkController();
    }
}
