package com.minemaarten.signals.proxy;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.IGuiHandler;

import com.minemaarten.signals.inventory.ContainerBase;
import com.minemaarten.signals.inventory.ContainerItemHandlerDestination;
import com.minemaarten.signals.inventory.ContainerMinecart;
import com.minemaarten.signals.inventory.ContainerNetworkController;
import com.minemaarten.signals.inventory.ContainerSelectDestinationProvider;

public class CommonProxy implements IGuiHandler{
    public enum EnumGuiId{
        STATION_MARKER, MINECART_DESTINATION, NETWORK_CONTROLLER, SELECT_DESTINATION_PROVIDER, ITEM_HANDLER_DESTINATION, CART_HOPPER
    }

    public void preInit(){}

    public void init(){}

    public void postInit(){}

    public EntityPlayer getPlayer(){
        return null;
    }

    public void addScheduledTask(Runnable runnable, boolean serverSide){
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(runnable);
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
        TileEntity te = y >= 0 ? world.getTileEntity(new BlockPos(x, y, z)) : null;
        Entity entity = y == -1 ? world.getEntityByID(x) : null;
        switch(EnumGuiId.values()[ID]){
            case STATION_MARKER:
            case CART_HOPPER:
                return new ContainerBase<>(te);
            case MINECART_DESTINATION:
                return new ContainerMinecart(player.inventory, (EntityMinecart)entity, z == 1);
            case NETWORK_CONTROLLER:
                return new ContainerNetworkController();
            case SELECT_DESTINATION_PROVIDER:
                return new ContainerSelectDestinationProvider(te);
            case ITEM_HANDLER_DESTINATION:
                return new ContainerItemHandlerDestination(te);
        }
        throw new IllegalStateException("No Container for gui id: " + ID);
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
        return null;
    }

    public boolean isSneakingInGui(){
        return false;
    }
}
