package com.minemaarten.signals.init;

import net.minecraft.item.Item;

import com.minemaarten.signals.item.ItemCartEngine;
import com.minemaarten.signals.item.ItemRailConfigurator;
import com.minemaarten.signals.item.ItemRailNetworkController;
import com.minemaarten.signals.item.ItemTicket;

public class ModItems{
    public static Item RAIL_CONFIGURATOR;
    public static Item RAIL_NETWORK_CONTROLLER;
    public static Item CART_ENGINE;
    public static Item TICKET;

    public static void init(){
        RAIL_CONFIGURATOR = new ItemRailConfigurator();
        RAIL_NETWORK_CONTROLLER = new ItemRailNetworkController();
        CART_ENGINE = new ItemCartEngine();
        TICKET = new ItemTicket();
    }
}
