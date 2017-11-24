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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import com.minemaarten.signals.client.ClientEventHandler;
import com.minemaarten.signals.client.glasses.GlassesHUD;
import com.minemaarten.signals.client.gui.GuiCartHopper;
import com.minemaarten.signals.client.gui.GuiItemHandlerDestination;
import com.minemaarten.signals.client.gui.GuiMinecart;
import com.minemaarten.signals.client.gui.GuiNetworkController;
import com.minemaarten.signals.client.gui.GuiSelectDestinationProvider;
import com.minemaarten.signals.client.gui.GuiStationMarker;
import com.minemaarten.signals.client.render.tileentity.SignalStatusRenderer;
import com.minemaarten.signals.init.ModBlocks;
import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.lib.Constants;
import com.minemaarten.signals.tileentity.TileEntityCartHopper;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class ClientProxy extends CommonProxy{
    @Override
    public void preInit(){
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(GlassesHUD.getInstance());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySignalBase.class, new SignalStatusRenderer());
    }

    @Override
    public void init(){
        for(Block block : new Block[]{ModBlocks.blockSignal, ModBlocks.pathSignal, ModBlocks.stationMarker, ModBlocks.limiterRail, ModBlocks.cartHopper, ModBlocks.railLink}) {
            Item item = Item.getItemFromBlock(block);
            registerItemModels(item);
        }
        registerItemModels(ModItems.railNetworkController);
        registerItemModels(ModItems.cartEngine);
        registerItemModels(ModItems.railConfigurator);
    }

    private void registerItemModels(Item item){
	    NonNullList<ItemStack> stacks = NonNullList.create();
        item.getSubItems(CreativeTabs.SEARCH, stacks);
        for(ItemStack stack : stacks) {
            ResourceLocation resLoc = new ResourceLocation(Constants.MOD_ID, stack.getUnlocalizedName().substring(5));
            ModelBakery.registerItemVariants(item, resLoc);
            Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, stack.getItemDamage(), new ModelResourceLocation(resLoc, "inventory"));
        }
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
        }
        throw new IllegalStateException("No Gui for gui id: " + ID);
    }

    @Override
    public boolean isSneakingInGui(){

        return GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak);
    }
}
