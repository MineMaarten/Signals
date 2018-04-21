package com.minemaarten.signals.event;

import java.util.Arrays;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

import com.minemaarten.signals.Signals;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.chunkloading.ChunkLoadManager;
import com.minemaarten.signals.config.SignalsConfig;
import com.minemaarten.signals.config.SignalsConfig.CartBlacklists;
import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.item.ItemTicket;
import com.minemaarten.signals.lib.Constants;
import com.minemaarten.signals.proxy.CommonProxy;
import com.minemaarten.signals.rail.RailManager;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class EventHandler implements IWorldEventListener{
    @SubscribeEvent
    public void onCapabilityAttachmentEntity(AttachCapabilitiesEvent<Entity> event){
        if(event.getObject() instanceof EntityMinecart) {
            event.addCapability(new ResourceLocation(Constants.MOD_ID, "minecartDestinationCapability"), new CapabilityMinecartDestination.Provider());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCapabilityAttachmentTile(AttachCapabilitiesEvent<TileEntity> event){
        RailManager.getInstance().onTileEntityCapabilityAttachEvent(event);
    }

    @SubscribeEvent
    public void onCapabilityAttachmentStack(AttachCapabilitiesEvent<ItemStack> event){
        if(event.getObject().getItem() == ModItems.TICKET) {
            event.addCapability(new ResourceLocation(Constants.MOD_ID, "ticketDestinationCapability"), new CapabilityMinecartDestination.Provider());
        }
    }

    @SubscribeEvent
    public void onMinecartInteraction(MinecartInteractEvent event){
        if(!event.getMinecart().world.isRemote) {
            ItemStack heldItem = event.getPlayer().getHeldItemMainhand();
            if(!heldItem.isEmpty()) {
                CapabilityMinecartDestination cap = event.getMinecart().getCapability(CapabilityMinecartDestination.INSTANCE, null);
                if(cap != null) {
                    if(heldItem.getItem() == ModItems.CART_ENGINE && !cap.isMotorized()) {
                        if(CartBlacklists.isBlacklisted(event.getMinecart(), SignalsConfig.cartBlacklists.cartEngines)) {
                            event.getPlayer().sendMessage(new TextComponentTranslation("signals.message.cart_engine_blacklisted"));
                        } else {
                            if(!event.getPlayer().isCreative()) {
                                heldItem.shrink(1);
                            }
                            cap.setMotorized();
                            event.getPlayer().sendMessage(new TextComponentTranslation("signals.message.cart_engine_installed"));
                            event.setCanceled(true);
                        }
                    } else if(heldItem.getItem() == ModItems.CHUNKLOADER_UPGRADE && !cap.isChunkLoading()) {
                        if(cap.setChunkloading(event.getPlayer(), event.getMinecart())) {
                            if(!event.getPlayer().isCreative()) {
                                heldItem.shrink(1);
                            }
                            event.getPlayer().sendMessage(new TextComponentTranslation("signals.message.chunkloader_installed"));
                        } else {
                            event.getPlayer().sendMessage(new TextComponentTranslation("signals.message.chunkloader_install_failed"));
                        }
                        event.setCanceled(true);
                    } else if(heldItem.getItem() == ModItems.RAIL_CONFIGURATOR) {
                        event.getPlayer().openGui(Signals.instance, CommonProxy.EnumGuiId.MINECART_DESTINATION.ordinal(), ((EntityPlayerMP)event.getPlayer()).world, event.getMinecart().getEntityId(), -1, cap.isMotorized() ? 1 : 0);
                        event.setCanceled(true);
                    } else if(heldItem.getItem() == ModItems.TICKET) {
                        if(event.getPlayer().isSneaking()) {
                            ItemTicket.setDestinations(heldItem, Arrays.asList(cap.getDestinations()));
                            event.getPlayer().sendMessage(new TextComponentTranslation("signals.message.destinations_saved", TextFormatting.GOLD + ItemTicket.getConcattedDestinations(heldItem) + TextFormatting.WHITE));
                        } else {
                            ItemTicket.applyDestinations(event.getMinecart(), heldItem);
                            event.getPlayer().sendMessage(new TextComponentTranslation("signals.message.destinations_set", TextFormatting.GOLD + ItemTicket.getConcattedDestinations(heldItem) + TextFormatting.WHITE));
                        }
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onMinecartUpdate(MinecartUpdateEvent event){
        EntityMinecart cart = event.getMinecart();
        CapabilityMinecartDestination cap = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
        if(cap != null) cap.onCartUpdate(event);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event){
        event.getWorld().addEventListener(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDimensionChange(EntityTravelToDimensionEvent event){
        CapabilityMinecartDestination cap = event.getEntity().getCapability(CapabilityMinecartDestination.INSTANCE, null);
        if(cap != null) {
            cap.travelingBetweenDimensions = !event.isCanceled();
        }
    }

    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event){
        if(!event.getWorld().isRemote && event.getEntity() instanceof EntityPlayer) {
            RailNetworkManager.getInstance().onPlayerJoin((EntityPlayerMP)event.getEntity());
        }
        if(!event.getWorld().isRemote && event.getEntity() instanceof EntityMinecart) {
            RailNetworkManager.getInstance().onMinecartJoinedWorld((EntityMinecart)event.getEntity());
            CapabilityMinecartDestination cap = event.getEntity().getCapability(CapabilityMinecartDestination.INSTANCE, null);
            if(cap != null) cap.onCartJoinWorld((EntityMinecart)event.getEntity());
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event){
        if(!event.getWorld().isRemote) {
            RailNetworkManager.getInstance().onChunkUnload(event.getChunk());
        }
    }

    @SubscribeEvent
    public void onNeighborChange(NeighborNotifyEvent event){
        if(!event.getWorld().isRemote) {
            RailNetworkManager.getInstance().markDirty(new MCPos(event.getWorld(), event.getPos()));
        }
    }

    @SubscribeEvent
    public void onPreServerTick(ServerTickEvent event){
        if(event.phase == Phase.START) {
            RailNetworkManager.getInstance().onPreServerTick();
        }
    }

    @SubscribeEvent
    public void onPostServerTick(ServerTickEvent event){
        if(event.phase == Phase.END) {
            RailNetworkManager.getInstance().onPostServerTick();
            ChunkLoadManager.INSTANCE.update();
        }
    }

    @SubscribeEvent
    public void onPreClientTick(ClientTickEvent event){
        if(event.phase == Phase.START) {
            RailNetworkManager.getInstance().onPreClientTick();
        }
    }

    @Override
    public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState, IBlockState newState, int flags){

    }

    @Override
    public void notifyLightSet(BlockPos pos){}

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2){}

    @Override
    public void playSoundToAllNearExcept(EntityPlayer player, SoundEvent soundIn, SoundCategory category, double x, double y, double z, float volume, float pitch){}

    @Override
    public void playRecord(SoundEvent soundIn, BlockPos pos){

    }

    @Override
    public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters){}

    @Override
    public void spawnParticle(int id, boolean ignoreRange, boolean p_190570_3_, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters){

    }

    @Override
    public void onEntityAdded(Entity entityIn){

    }

    @Override
    public void onEntityRemoved(Entity entityIn){
        if(entityIn instanceof EntityMinecart && !entityIn.world.isRemote) {
            CapabilityMinecartDestination cap = entityIn.getCapability(CapabilityMinecartDestination.INSTANCE, null);
            if(cap != null) cap.onCartBroken((EntityMinecart)entityIn);
        }
    }

    @Override
    public void broadcastSound(int soundID, BlockPos pos, int data){

    }

    @Override
    public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data){

    }

    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress){

    }
}
