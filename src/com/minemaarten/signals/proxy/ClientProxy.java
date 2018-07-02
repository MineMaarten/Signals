package com.minemaarten.signals.proxy;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.client.ClientEventHandler;
import com.minemaarten.signals.client.glasses.GlassesHUD;
import com.minemaarten.signals.client.gui.GuiCartHopper;
import com.minemaarten.signals.client.gui.GuiItemHandlerDestination;
import com.minemaarten.signals.client.gui.GuiMinecart;
import com.minemaarten.signals.client.gui.GuiNetworkController;
import com.minemaarten.signals.client.gui.GuiRailLink;
import com.minemaarten.signals.client.gui.GuiSelectDestinationProvider;
import com.minemaarten.signals.client.gui.GuiStationMarker;
import com.minemaarten.signals.client.gui.GuiTicket;
import com.minemaarten.signals.client.render.tileentity.SignalStatusRenderer;
import com.minemaarten.signals.config.SignalsConfig;
import com.minemaarten.signals.init.ModBlocks;
import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.item.ItemTicket;
import com.minemaarten.signals.lib.Constants;
import com.minemaarten.signals.tileentity.TileEntityCartHopper;
import com.minemaarten.signals.tileentity.TileEntityRailLink;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class ClientProxy extends CommonProxy{
    private final ClientEventHandler eventHandler = ClientEventHandler.INSTANCE;

    @Override
    public void preInit(){
        MinecraftForge.EVENT_BUS.register(eventHandler);
        MinecraftForge.EVENT_BUS.register(GlassesHUD.getInstance());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySignalBase.class, new SignalStatusRenderer());
    }

    @Override
    public void init(){
        for(Block block : new Block[]{ModBlocks.BLOCK_SIGNAL, ModBlocks.CHAIN_SIGNAL, ModBlocks.STATION_MARKER, ModBlocks.LIMITER_RAIL, ModBlocks.CART_HOPPER, ModBlocks.RAIL_LINK}) {
            Item item = Item.getItemFromBlock(block);
            registerItemModels(item);
        }
        registerItemModels(ModItems.RAIL_NETWORK_CONTROLLER);
        registerItemModels(ModItems.CART_ENGINE);
        if(!SignalsConfig.disableChunkLoaderUpgrades) registerItemModels(ModItems.CHUNKLOADER_UPGRADE);
        registerItemModels(ModItems.RAIL_CONFIGURATOR);
    }

    @SubscribeEvent
    public void onModelRegistration(ModelRegistryEvent event){
        for(int i = 0; i <= 4; i++) {
            ModelResourceLocation location = new ModelResourceLocation(new ResourceLocation("signals:ticket"), "inventory_" + i);
            ModelLoader.setCustomModelResourceLocation(ModItems.TICKET, i, location);
        }
    }

    private static void registerItemModels(Item item){
        NonNullList<ItemStack> stacks = NonNullList.create();
        item.getSubItems(CreativeTabs.SEARCH, stacks);
        for(ItemStack stack : stacks) {
            registerItemModel(stack);
        }
    }

    private static void registerItemModel(ItemStack stack){
        registerItemModel(stack, "");
    }

    private static void registerItemModel(ItemStack stack, String suffix){
        String resourceName = stack.getUnlocalizedName().substring(5) + suffix;
        ResourceLocation resLoc = new ResourceLocation(Constants.MOD_ID, resourceName);
        ModelBakery.registerItemVariants(stack.getItem(), resLoc);
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(stack.getItem(), stack.getItemDamage(), new ModelResourceLocation(resLoc, "inventory"));
    }

    @Override
    public EntityPlayer getPlayer(){
        return Minecraft.getMinecraft().player;
    }

    @Override
    public void addScheduledTask(Runnable runnable, boolean serverSide){
        if(serverSide) {
            super.addScheduledTask(runnable, serverSide);
        } else {
            Minecraft.getMinecraft().addScheduledTask(runnable);
        }
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
        TileEntity te = y >= 0 ? world.getTileEntity(new BlockPos(x, y, z)) : null;
        Entity entity = y == -1 ? world.getEntityByID(x) : null;
        switch(EnumGuiId.values()[ID]){
            case STATION_MARKER:
                return new GuiStationMarker((TileEntityStationMarker)te);
            case MINECART_DESTINATION:
                return new GuiMinecart(player.inventory, (EntityMinecart)entity, z == 1);
            case NETWORK_CONTROLLER:
                return new GuiNetworkController();
            case SELECT_DESTINATION_PROVIDER:
                return new GuiSelectDestinationProvider(te);
            case ITEM_HANDLER_DESTINATION:
                return new GuiItemHandlerDestination(te);
            case CART_HOPPER:
                return new GuiCartHopper((TileEntityCartHopper)te);
            case RAIL_LINK:
                return new GuiRailLink((TileEntityRailLink)te);
            case TICKET_DESTINATION:
                ItemStack stack = player.getHeldItemMainhand();
                CapabilityMinecartDestination accessor = stack.getCapability(CapabilityMinecartDestination.INSTANCE, null);
                if(accessor == null) return null;
                ItemTicket.readNBTIntoCap(stack);
                return new GuiTicket(new Container(){
                    @Override
                    public boolean canInteractWith(EntityPlayer playerIn){
                        return true;
                    }
                }, accessor, stack.getDisplayName());
            default:
                throw new IllegalStateException("No Gui for gui id: " + ID);
        }
    }

    @Override
    public boolean isSneakingInGui(){

        return GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak);
    }

    @Override
    public void onRailNetworkUpdated(){
        eventHandler.blockSectionRenderer.updateSectionRenderers();
        eventHandler.edgeRenderer.updateSectionRenderers();
        eventHandler.directionalityRenderer.updateRender();
        eventHandler.pathRenderer.updateSectionRenderers();
        eventHandler.claimRenderer.updateSectionRenderers();
    }
}
