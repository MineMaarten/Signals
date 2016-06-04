package com.minemaarten.signals;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.init.ModBlocks;
import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.lib.Constants;
import com.minemaarten.signals.lib.Versions;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.proxy.CommonProxy;
import com.minemaarten.signals.rail.RailManager;

@Mod(modid = Constants.MOD_ID, name = "Signals", dependencies = "required-after:Forge@[10.13.3.1388,);" + "after:Forestry")
public class Signals{

    @SidedProxy(clientSide = "com.minemaarten.signals.proxy.ClientProxy", serverSide = "com.minemaarten.signals.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Instance(Constants.MOD_ID)
    public static Signals instance;
    private ASMDataTable asmData;

    @EventHandler
    public void PreInit(FMLPreInitializationEvent event){
        event.getModMetadata().version = Versions.fullVersionString();

        NetworkRegistry.INSTANCE.registerGuiHandler(instance, proxy);
        proxy.preInit();
        ModBlocks.init();
        ModItems.init();
        CapabilityMinecartDestination.register();
        MinecraftForge.EVENT_BUS.register(new com.minemaarten.signals.event.EventHandler());
        asmData = event.getAsmData();
    }

    @EventHandler
    public void load(FMLInitializationEvent event){
        NetworkHandler.init();

        proxy.init();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event){
        proxy.postInit();
        RailManager.getInstance().initializeAPIImplementors(asmData);
    }
    

}
