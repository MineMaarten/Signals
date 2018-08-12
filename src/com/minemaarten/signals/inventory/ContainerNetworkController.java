package com.minemaarten.signals.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentString;

import com.minemaarten.signals.api.access.ISignal.EnumForceMode;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class ContainerNetworkController extends ContainerBase<TileEntity>{

    public ContainerNetworkController(){
        super(null);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player){
        return true;
    }

    @Override
    public void handleGUIButtonPress(EntityPlayer player, int... data){
        int x = data[0];
        int z = data[1];
        int dimID = player.world.provider.getDimension();
        RailNetwork<MCPos> network = RailNetworkManager.getInstance(player.world.isRemote).getNetwork();
        EnumForceMode forceMode = EnumForceMode.values()[data[2]];

        if(RailNetworkManager.getInstance(player.world.isRemote).getState().setForceMode(network, dimID, x, z, forceMode)) {
            player.sendMessage(new TextComponentString("Forced " + (forceMode == EnumForceMode.FORCED_RED ? "red" : "green")));
        }
    }

}
