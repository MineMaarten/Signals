package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

import com.minemaarten.signals.tileentity.IGUIButtonSensitive;

public class PacketGuiButton extends AbstractPacket<PacketGuiButton>{
    private int buttonID;

    public PacketGuiButton(){}

    public PacketGuiButton(int buttonID){
        this.buttonID = buttonID;
    }

    @Override
    public void toBytes(ByteBuf buffer){
        buffer.writeInt(buttonID);
    }

    @Override
    public void fromBytes(ByteBuf buffer){
        buttonID = buffer.readInt();
    }

    @Override
    public void handleClientSide(EntityPlayer player){}

    @Override
    public void handleServerSide(EntityPlayer player){
        if(player.openContainer instanceof IGUIButtonSensitive) {
            ((IGUIButtonSensitive)player.openContainer).handleGUIButtonPress(buttonID, player);
        }
    }

}
