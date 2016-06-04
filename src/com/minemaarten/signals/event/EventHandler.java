package com.minemaarten.signals.event;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.minemaarten.signals.Signals;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.lib.Constants;
import com.minemaarten.signals.proxy.CommonProxy;
import com.minemaarten.signals.rail.RailCacheManager;

public class EventHandler{
    @SubscribeEvent
    public void onCapabilityAttachment(AttachCapabilitiesEvent.Entity event){
        if(event.getEntity() instanceof EntityMinecart) {
            event.addCapability(new ResourceLocation(Constants.MOD_ID, "minecartDestinationCapability"), new CapabilityMinecartDestination.Provider());
        }
    }

    @SubscribeEvent
    public void onMinecartInteraction(MinecartInteractEvent event){
        if(!event.getMinecart().worldObj.isRemote && event.getPlayer().isSneaking()) {
            RailCacheManager.syncStationNames((EntityPlayerMP)event.getPlayer());
            event.getPlayer().openGui(Signals.instance, CommonProxy.EnumGuiId.MINECART_DESTINATION.ordinal(), event.getPlayer().worldObj, event.getMinecart().getEntityId(), -1, 0);
            event.setCanceled(true);

            /*CapabilityMinecartDestination cap = event.minecart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
            if(!cap.getDestinationStation().equals("Station5")) {
                cap.setDestinationStation("Station5");
                event.minecart.setCustomNameTag("Heading to Station5");
                event.setCanceled(true);
            }*/
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event){
        if(!event.getWorld().isRemote) RailCacheManager.getInstance(event.getWorld()).onChunkUnload(event.getChunk());
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event){
        if(!event.getWorld().isRemote) RailCacheManager.getInstance(event.getWorld()).onChunkLoad(event.getChunk());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event){
        if(!event.getWorld().isRemote) RailCacheManager.getInstance(event.getWorld()).onWorldUnload(event.getWorld());
    }

    @SubscribeEvent
    public void onNeighborChange(NeighborNotifyEvent event){
        RailCacheManager.getInstance(event.getWorld()).onNeighborChanged(event);
    }
}
